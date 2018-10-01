package com.kesen.appfire.services;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.kesen.appfire.model.constants.DBConstants;
import com.kesen.appfire.model.constants.DownloadUploadStat;
import com.kesen.appfire.model.constants.MessageType;
import com.kesen.appfire.model.constants.PendingGroupTypes;
import com.kesen.appfire.model.realms.Message;
import com.kesen.appfire.model.realms.PendingGroupJob;
import com.kesen.appfire.model.realms.PhoneNumber;
import com.kesen.appfire.model.realms.RealmContact;
import com.kesen.appfire.model.realms.RealmLocation;
import com.kesen.appfire.model.realms.User;
import com.kesen.appfire.utils.DownloadManager;
import com.kesen.appfire.utils.FireManager;
import com.kesen.appfire.utils.GroupEvent;
import com.kesen.appfire.utils.JsonUtil;
import com.kesen.appfire.utils.ListUtil;
import com.kesen.appfire.utils.NotificationHelper;
import com.kesen.appfire.utils.RealmHelper;
import com.kesen.appfire.utils.ServiceHelper;
import com.kesen.appfire.utils.SharedPreferencesManager;

import java.util.ArrayList;

import io.realm.RealmList;

public class MyFCMService extends FirebaseMessagingService {


    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        SharedPreferencesManager.setTokenSaved(false);

        ServiceHelper.saveToken(this, s);
    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);



        if (remoteMessage.getData().containsKey("event")) {
            //this will called when something is changed in group.
            // like member removed,added,admin changed, group info changed...
            if (remoteMessage.getData().get("event").equals("group_event")) {
                String groupId = remoteMessage.getData().get("groupId");
                String eventId = remoteMessage.getData().get("eventId");
                String contextStart = remoteMessage.getData().get("contextStart");
                int eventType = Integer.parseInt(remoteMessage.getData().get("eventType"));
                String contextEnd = remoteMessage.getData().get("contextEnd");



                //if this event was by the admin himself  OR if the event already exists do nothing
                if (contextStart.equals(SharedPreferencesManager.getPhoneNumber())
                        || RealmHelper.getInstance().getMessage(eventId) != null) {
                    return;
                }


                GroupEvent groupEvent = new GroupEvent(contextStart, eventType, contextEnd, eventId);
                PendingGroupJob pendingGroupJob = new PendingGroupJob(groupId, PendingGroupTypes.CHANGE_EVENT, groupEvent);
                RealmHelper.getInstance().saveObjectToRealm(pendingGroupJob);
                ServiceHelper.updateGroupInfo(this, groupId, groupEvent);

            } else if (remoteMessage.getData().get("event").equals("new_group")) {

                String groupId = remoteMessage.getData().get("groupId");
                User user = RealmHelper.getInstance().getUser(groupId);

                //if the group is not exists,fetch and download it
                if (user == null) {
                    PendingGroupJob pendingGroupJob = new PendingGroupJob(groupId, PendingGroupTypes.CREATION_EVENT,null);
                    RealmHelper.getInstance().saveObjectToRealm(pendingGroupJob);
                    ServiceHelper.fetchAndCreateGroup(this, groupId);
                } else {

                    RealmList<User> users = user.getGroup().getUsers();
                    User userById = ListUtil.getUserById(FireManager.getUid(), users);

                    //if the group is not active or the group does not contain current user
                    // then fetch and download it and set it as Active
                    if (!user.getGroup().isActive() || !users.contains(userById)) {
                        PendingGroupJob pendingGroupJob = new PendingGroupJob(groupId, PendingGroupTypes.CREATION_EVENT,null);
                        RealmHelper.getInstance().saveObjectToRealm(pendingGroupJob);
                        ServiceHelper.fetchAndCreateGroup(this, groupId);
                    }
                }


            } else if (remoteMessage.getData().get("event").equals("message_deleted")) {
                String messageId = remoteMessage.getData().get("messageId");
                Message message = RealmHelper.getInstance().getMessage(messageId);
                RealmHelper.getInstance().setMessageDeleted(messageId);

                if (message != null) {
                    if (message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
                        if (MessageType.isSentType(message.getType())) {
                            DownloadManager.cancelUpload(message.getMessageId());
                        } else
                            DownloadManager.cancelDownload(message.getMessageId());
                    }
                    new NotificationHelper(this).messageDeleted(message);
                }


            }
        } else {
            final String messageId = remoteMessage.getData().get(DBConstants.MESSAGE_ID);

            //if message is deleted do not save it
            if (RealmHelper.getInstance().getDeletedMessage(messageId) != null)
                return;

            boolean isGroup = remoteMessage.getData().containsKey("isGroup");
            //getting data from fcm message and convert it to a message
            final String phone = remoteMessage.getData().get(DBConstants.PHONE);
            final String content = remoteMessage.getData().get(DBConstants.CONTENT);
            final String timestamp = remoteMessage.getData().get(DBConstants.TIMESTAMP);
            final int type = Integer.parseInt(remoteMessage.getData().get(DBConstants.TYPE));
            //get sender uid
            final String fromId = remoteMessage.getData().get(DBConstants.FROM_ID);
            String toId = remoteMessage.getData().get(DBConstants.TOID);
            final String metadata = remoteMessage.getData().get(DBConstants.METADATA);
            //convert sent type to received
            int convertedType = MessageType.convertSentToReceived(type);

            //if it's a group message and the message sender is the same
            if (fromId.equals(FireManager.getUid()))
                return;

            //create the message
            final Message message = new Message();
            message.setContent(content);
            message.setTimestamp(timestamp);
            message.setFromId(fromId);
            message.setType(convertedType);
            message.setMessageId(messageId);
            message.setMetadata(metadata);
            message.setToId(toId);
            message.setChatId(isGroup ? toId : fromId);
            message.setGroup(isGroup);
            if (isGroup)
                message.setFromPhone(phone);
            //set default state
            message.setDownloadUploadStat(DownloadUploadStat.FAILED);

            //check if it's text message
            if (MessageType.isSentText(type)) {
                //set the state to default
                message.setDownloadUploadStat(DownloadUploadStat.DEFAULT);


                //check if it's a contact
            } else if (remoteMessage.getData().containsKey(DBConstants.CONTACT)) {
                message.setDownloadUploadStat(DownloadUploadStat.DEFAULT);
                //get the json contact as String
                String jsonString = remoteMessage.getData().get(DBConstants.CONTACT);
                //convert contact numbers from JSON to ArrayList
                ArrayList<PhoneNumber> phoneNumbersList = JsonUtil.getPhoneNumbersList(jsonString);
                // convert it to RealmContact and set the contact name using content
                RealmContact realmContact = new RealmContact(content, phoneNumbersList);

                message.setContact(realmContact);


                //check if it's a location message
            } else if (remoteMessage.getData().containsKey(DBConstants.LOCATION)) {
                message.setDownloadUploadStat(DownloadUploadStat.DEFAULT);
                //get the json location as String
                String jsonString = remoteMessage.getData().get(DBConstants.LOCATION);
                //convert location from JSON to RealmLocation
                RealmLocation location = JsonUtil.getRealmLocationFromJson(jsonString);
                message.setLocation(location);
            }

            //check if it's image or Video
            else if (remoteMessage.getData().containsKey(DBConstants.THUMB)) {
                final String thumb = remoteMessage.getData().get(DBConstants.THUMB);

                //Check if it's Video and set Video Duration
                if (remoteMessage.getData().containsKey(DBConstants.MEDIADURATION)) {
                    final String mediaDuration = remoteMessage.getData().get(DBConstants.MEDIADURATION);
                    message.setMediaDuration(mediaDuration);
                }

                message.setThumb(thumb);


                //check if it's Voice Message or Audio File
            } else if (remoteMessage.getData().containsKey(DBConstants.MEDIADURATION)
                    && type == MessageType.SENT_VOICE_MESSAGE || type == MessageType.SENT_AUDIO) {

                //set audio duration
                final String mediaDuration = remoteMessage.getData().get(DBConstants.MEDIADURATION);


                message.setMediaDuration(mediaDuration);

                //check if it's a File
            } else if (remoteMessage.getData().containsKey(DBConstants.FILESIZE)) {
                String fileSize = remoteMessage.getData().get(DBConstants.FILESIZE);


                message.setFileSize(fileSize);

            }


            //Save it to database and fire notification
            new NotificationHelper(this).handleNewMessage(phone, message);

        }
    }

}