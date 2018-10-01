package com.kesen.appfire.utils;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.kesen.appfire.events.GroupActiveStateChanged;
import com.kesen.appfire.model.constants.DBConstants;
import com.kesen.appfire.model.constants.DownloadUploadStat;
import com.kesen.appfire.model.constants.MessageStat;
import com.kesen.appfire.model.constants.MessageType;
import com.kesen.appfire.model.realms.Chat;
import com.kesen.appfire.model.realms.DeletedMessage;
import com.kesen.appfire.model.realms.Group;
import com.kesen.appfire.model.realms.JobId;
import com.kesen.appfire.model.realms.Message;
import com.kesen.appfire.model.realms.PendingGroupJob;
import com.kesen.appfire.model.realms.UnUpdatedStat;
import com.kesen.appfire.model.realms.UnUpdatedVoiceMessageStat;
import com.kesen.appfire.model.realms.User;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Devlomi on 13/08/2017.
 */

//this class is responsible for all the Realm Database operations
//create,read,update,delete
public class RealmHelper {

    private static RealmHelper instance;

    //get instance of real
    private RealmHelper() {
        realm = Realm.getDefaultInstance();
    }

    private Realm realm;

    public static RealmHelper getInstance() {

        instance = new RealmHelper();

        return instance;
    }

    //save a message
    public void saveObjectToRealm(RealmObject object) {
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(object);
        realm.commitTransaction();
    }


    //get all chats ordered by the time (the newest shows on top)
    public RealmResults<Chat> getAllChats() {
        return realm.where(Chat.class).findAll().sort(DBConstants.CHAT_LAST_MESSAGE_TIMESTAMP, Sort.DESCENDING);
    }

    //get certain chat
    public Chat getChat(String id) {
        return realm.where(Chat.class).equalTo(DBConstants.CHAT_ID, id).findFirst();
    }

    //check if chat stored in realm or not
    public boolean isChatStored(String id) {
        return !realm.where(Chat.class).equalTo(DBConstants.CHAT_ID, id).findAll().isEmpty();
    }

    //delete message from realm and delete file if needed
    public void deleteMessageFromRealm(String chatId, String messageId, boolean deleteFile) {

        //check if message is exists
        Message messageToDelete = getMessage(messageId);

        if (messageToDelete == null) return;

        //get the message


        //delete the file if deleteFile is true
        if (deleteFile && !messageToDelete.isTextMessage())
            FileUtils.deleteFile(messageToDelete.getLocalPath());


        Chat chat = getChat(chatId);
        if (chat != null)
            //update last message if this is last message in chat
            updateLastMessageForChat(chatId, messageToDelete, chat);

        //delete message from realm
        realm.beginTransaction();
        messageToDelete.deleteFromRealm();
        realm.commitTransaction();


    }

    private void updateLastMessageForChat(String chatId, Message messageToDelete, Chat chat) {
        if (chat != null) {
            //get last message in this chat
            Message lastMessage = chat.getLastMessage();
            //if this is last message in chat then we want to update
            // 'Chat' with new LastMessage (the message before last message)
            if (lastMessage != null && lastMessage.getMessageId().equals(messageToDelete.getMessageId())) {
                RealmResults<Message> messagesInChat = realm.where(Message.class).equalTo(DBConstants.CHAT_ID, chatId).findAll();
                int messagesCount = messagesInChat.size();
                //check if there is more than one message in this chat
                if (messagesCount > 1) {
                    //get the message before the last message (the new message to set it as the last message)
                    Message messageToSetAsLastMessage = messagesInChat.get(messagesCount - 2);
                    //update the chat with the new last message
                    saveLastMessageForChat(messageToSetAsLastMessage);
                } else {
                    //if there are no messages in chat then just update
                    // the timestamp with the last message timestamp to keep the chat order
                    saveChatLastMessageTimestamp(chat, messagesInChat.last().getTimestamp());
                }
            }
        }
    }

    private void saveChatLastMessageTimestamp(Chat chat, String timestamp) {
        realm.beginTransaction();
        chat.setLastMessageTimestamp(timestamp);
        realm.commitTransaction();
    }

    //get certain message
    public Message getMessage(String id) {

        return realm.where(Message.class).equalTo(DBConstants.MESSAGE_ID, id).findFirst();
    }


    //get all messages in chat sorted by time
    public RealmResults<Message> getMessagesInChat(String id) {

        return realm.where(Message.class).equalTo(DBConstants.CHAT_ID, id).findAll().sort(DBConstants.TIMESTAMP);
    }

    //update chat with new last message
    public void saveLastMessageForChat(String chatId, Message message) {
        Chat chat = getChat(chatId);
        if (chat == null)
            return;


        realm.beginTransaction();
        message = realm.copyToRealmOrUpdate(message);
        //set last message
        chat.setLastMessage(message);
        //set last messsage time
        chat.setLastMessageTimestamp(message.getTimestamp());
        realm.copyToRealmOrUpdate(chat);
        realm.commitTransaction();

    }

    public void saveLastMessageForChat(Message message) {
        String chatId = message.getChatId();
        Chat chat = getChat(chatId);
        if (chat == null)
            return;


        realm.beginTransaction();
        message = realm.copyToRealmOrUpdate(message);
        chat.setLastMessage(message);
        chat.setLastMessageTimestamp(message.getTimestamp());
        realm.copyToRealmOrUpdate(chat);
        realm.commitTransaction();

    }


    public void saveEmptyChat(User user) {
        String chatId = user.getUid();

        if (!isChatStored(chatId)) {

            Chat chat = new Chat();
            chat.setChatId(chatId);
            chat.setUser(user);
            chat.setLastMessageTimestamp(String.valueOf(new Date().getTime()));
            chat.setNotificationId(generateNotificationId());
            saveObjectToRealm(chat);
        }

    }

    public void saveUnreadMessage(Message message) {
        String chatId = message.getChatId();
        Chat chat = getChat(chatId);
        if (chat != null) {
            int unReadCount = chat.getUnReadCount();
            RealmList<Message> unreadMessages = chat.getUnreadMessages();
            realm.beginTransaction();
            unreadMessages.add(message);
            //increment unread count by 1
            chat.setUnReadCount(++unReadCount);
            //update firstUnreadMessageId
            if (chat.getFirstUnreadMessageId().equals(""))
                chat.setFirstUnreadMessageId(message.getMessageId());
            realm.commitTransaction();
        }
    }

    public void deleteUnReadMessages(String chatId) {

        Chat chat = getChat(chatId);
        if (chat != null) {
            RealmList<Message> unreadMessages = chat.getUnreadMessages();
            realm.beginTransaction();
            chat.setUnReadCount(0);

            if (NotificationHelper.isBelowApi24()) {
                for (Message unreadMessage : unreadMessages) {
                    unreadMessage.setSeen(true);
                }
            }

            unreadMessages.clear();
            realm.commitTransaction();
        }
    }

    //this will get the last 6 messages for a chat
    //6 is because it's the max number for notifications in a grouped notification
    public List<Message> filterUnreadMessages(RealmList realmList) {

        RealmResults sort = realmList.sort(DBConstants.TIMESTAMP, Sort.DESCENDING);

        //if the all unread messages is greater than 6
        //we will only get last 6 messages
        //otherwise we will return the all unread messages list
        if (realmList.size() > 6) {
            List<Message> messageList = new ArrayList<>(sort.subList(0, 6));
            Collections.reverse(messageList);
            return messageList;
        }


        return realmList;


    }


    //this will get last 7 unread messages ,it's used for apis below 24
    public List<Message> getLast7UnreadMessages() {
        RealmResults<Message> unreadMessages = realm.where(Message.class)
                .notEqualTo(DBConstants.FROM_ID, FireManager.getUid())
                .notEqualTo(DBConstants.TYPE, MessageType.GROUP_EVENT)
                .notEqualTo(DBConstants.TYPE, MessageType.RECEIVED_DELETED_MESSAGE)
                //for old messages when migrating from V1.0
                .notEqualTo(DBConstants.MESSAGE_STAT, MessageStat.READ)
                //get only non-seen messages
                .equalTo(DBConstants.IS_SEEN, false)
                .findAll()
                //get last 7 messages
                .sort(DBConstants.TIMESTAMP, Sort.DESCENDING);


        //get only last 7 messages
        List<Message> list = new ArrayList<>();
        int newListSize = unreadMessages.size() < 7 ? unreadMessages.size() : 7;
        list.addAll(unreadMessages.subList(0, newListSize));
        //reverse the order to get last first unread message as last message
        Collections.reverse(list);
        return list;
    }


    public List<Chat> getUnreadChats() {
        return realm.where(Chat.class)
                .notEqualTo(DBConstants.UNREAD_COUNT, 0)
                .notEqualTo(DBConstants.isMuted, true)
                .findAll()
                .sort(DBConstants.CHAT_LAST_MESSAGE_TIMESTAMP, Sort.ASCENDING);


    }

    public boolean areThereUnreadChats() {
        return realm.where(Chat.class)
                .notEqualTo(DBConstants.UNREAD_COUNT, 0)
                .notEqualTo(DBConstants.isMuted, true)
                .count() > 0;

    }

    public long getUnreadChatsCount() {
        return realm.where(Chat.class)
                .notEqualTo(DBConstants.UNREAD_COUNT, 0)
                .notEqualTo(DBConstants.isMuted, true)
                .count();
    }


    public long getUnreadMessagesCount() {
        return realm.where(Chat.class)
                .notEqualTo(DBConstants.UNREAD_COUNT, 0)
                .notEqualTo(DBConstants.isMuted, true)
                .sum(DBConstants.UNREAD_COUNT)
                .longValue();
    }


    // in case if it's a received message when the chat is not exists
    // then we don't have the full user data ,we will get them later
    public void saveChatIfNotExists(Context context, Message message, String phone) {
        String chatId = message.getChatId();
        if (!isChatStored(chatId)) {

            //create new chat
            Chat chat = new Chat();
            //set chat id
            chat.setChatId(chatId);

            //create the new user
            User user = new User();
            user.setPhone(phone);
            user.setUserName(ContactUtils.queryForNameByNumber(context, phone));
            user.setStoredInContacts(ContactUtils.contactExists(context, user.getPhone()));
            user.setUid(chatId);
            chat.setUser(user);
            chat.setNotificationId(generateNotificationId());
            chat.setLastMessageTimestamp(String.valueOf(new Date().getTime()));


            saveObjectToRealm(user);
            saveObjectToRealm(chat);

        }

        //save last message
        saveLastMessageForChat(chatId, message);
    }

    // if the user started the chat the we already have the user info
    //therefore we will only create a new chat and save the last message
    public void saveChatIfNotExists(Message message, User user) {
        String chatId = message.getChatId();
        if (!isChatStored(chatId)) {

            Chat chat = new Chat();

            chat.setChatId(chatId);
            chat.setUser(user);
            chat.setNotificationId(generateNotificationId());
            chat.setLastMessageTimestamp(String.valueOf(new Date().getTime()));
            saveObjectToRealm(chat);
        }
        saveLastMessageForChat(chatId, message);

    }


    public void saveMessageFromFCM(Context context, Message message, String phone) {
        saveObjectToRealm(message);
        saveChatIfNotExists(context, message, phone);
    }

    public void saveMessageFromFCM(Message message, User user) {
        saveObjectToRealm(message);
        saveChatIfNotExists(message, user);
    }

    //update message state in realm to (sent,received or read)
    public void updateMessageStatLocally(String messageId, int messageStat) {
        Message message = getMessage(messageId);
        if (message == null)
            return;

        //if it's the same state don't update it

        if (messageStat == message.getMessageStat())
            return;

        realm.beginTransaction();
        message.setMessageStat(messageStat);
        realm.commitTransaction();


    }

    //update message state in realm to (sent,received or read)
    public void setMessagesAsReadLocally(String chatId) {
        Chat chat = getChat(chatId);
        if (chat == null)
            return;

        //if it's the same state don't update it

        RealmResults<Message> unreadMessages = realm.where(Message.class)
                .equalTo(DBConstants.CHAT_ID, chatId)
                .notEqualTo(DBConstants.FROM_ID, FireManager.getUid())
                .notEqualTo(DBConstants.TYPE, MessageType.GROUP_EVENT)
                .notEqualTo(DBConstants.MESSAGE_STAT, MessageStat.READ)
                .findAll();


        for (Message unreadMessage : unreadMessages) {
            realm.beginTransaction();
            unreadMessage.setMessageStat(MessageStat.READ);
            realm.commitTransaction();
        }


    }

    //update voice message state in realm to seen
    public void updateVoiceMessageStatLocally(String messageId) {

        Message message = getMessage(messageId);
        if (message == null)
            return;


        //don't update it if it's seen
        if (message.isVoiceMessageSeen()) return;

        realm.beginTransaction();
        message.setVoiceMessageSeen(true);
        realm.commitTransaction();


    }

    public RealmResults<Message> getObservableList(String chatId) {
        return realm.where(Message.class).equalTo(DBConstants.CHAT_ID, chatId)
                .notEqualTo(DBConstants.MESSAGE_STAT, MessageStat.READ)
                .findAll();
    }

    //get the messages that sent to other user and they are not received or read
    //so we can add listeners for them once they have changed
    public RealmResults<Message> getUnreadAndUnDeliveredSentMessages(String chatId, String senderUid) {
        return realm.where(Message.class)
                .equalTo(DBConstants.CHAT_ID, chatId)
                .equalTo(DBConstants.FROM_ID, senderUid)
                .equalTo(DBConstants.MESSAGE_STAT, MessageStat.SENT)
                .or()
                .equalTo(DBConstants.CHAT_ID, chatId)
                .equalTo(DBConstants.FROM_ID, senderUid)
                .equalTo(DBConstants.MESSAGE_STAT, MessageStat.RECEIVED)
                .findAll();
    }


    //get received messages that are not read to update them in Firebase database as read
    public RealmResults<Message> getUnReadIncomingMessages(String chatId) {
        return realm.where(Message.class)
                .equalTo(DBConstants.CHAT_ID, chatId)
                .notEqualTo(DBConstants.FROM_ID, FireManager.getUid())
                .notEqualTo(DBConstants.MESSAGE_STAT, MessageStat.READ)
                .findAll();
    }

    //get unread sent voice messages so we can add listeners for them once they have changed
    public RealmResults<Message> getUnReadVoiceMessages(String chatId) {
        RealmResults<Message> all = realm.where(Message.class)
                .equalTo(DBConstants.CHAT_ID, chatId)
                .equalTo(DBConstants.TYPE, MessageType.SENT_VOICE_MESSAGE)
                .equalTo(DBConstants.VOICE_MESSAGE_SEEN, false)
                .findAll();

        return all;
    }

    //get not sent messages to send them when internet is available
    public RealmResults<Message> getPendingMessages() {
        return realm.where(Message.class)
                .notEqualTo(DBConstants.TYPE, MessageType.GROUP_EVENT)
                .equalTo(DBConstants.MESSAGE_STAT, MessageStat.PENDING)
                //don't get the cancelled messages ,since the user don't want to send them
                .notEqualTo(DBConstants.DOWNLOAD_UPLOAD_STAT, DownloadUploadStat.CANCELLED)
                .findAll();
    }


    public RealmResults<Message> getUnProcessedNetworkRequests() {
        return realm.where(Message.class)
                .equalTo(DBConstants.DOWNLOAD_UPLOAD_STAT, DownloadUploadStat.LOADING)
                .findAll();
    }

    //get not sent messages to send them when internet is available
    public RealmResults<Message> getNotificationMessages() {
        return realm.where(Message.class)
                .notEqualTo(DBConstants.TYPE, MessageType.GROUP_EVENT)
                .equalTo(DBConstants.MESSAGE_STAT, MessageStat.PENDING)
                //don't get the cancelled messages ,since the user don't want to send them
                .notEqualTo(DBConstants.DOWNLOAD_UPLOAD_STAT, DownloadUploadStat.CANCELLED)
                .findAll();
    }


    //update upload state when it's finished whether it's success ,failed,loading or cancelled
    public void updateDownloadUploadStat(String messageId, int downloadUploadStat) {
        Message message = realm.where(Message.class).equalTo(DBConstants.MESSAGE_ID, messageId).findFirst();
        if (message == null)
            return;

        realm.beginTransaction();
        //if upload state is success ,update the message state to sent
        if (downloadUploadStat == DownloadUploadStat.SUCCESS)
            message.setMessageStat(MessageStat.SENT);

        message.setDownloadUploadStat(downloadUploadStat);

        realm.commitTransaction();

    }

    //update download state when it's finished whether it's success ,failed,loading or cancelled
    public void updateDownloadUploadStat(String messageId, int downloadUploadStat, String localPath) {

        Message message = realm.where(Message.class).equalTo(DBConstants.MESSAGE_ID, messageId).findFirst();
        if (message == null)
            return;

        realm.beginTransaction();
        if (downloadUploadStat == DownloadUploadStat.SUCCESS)
            message.setMessageStat(MessageStat.SENT);

        message.setDownloadUploadStat(downloadUploadStat);
        //save the downloaded file path
        message.setLocalPath(localPath);

        realm.commitTransaction();

    }

    //get video and image in chat
    public List<Message> getMediaInChat(String chatId) {


        //get the messages that are downloaded only:


        return realm.where(Message.class)
                .equalTo(DBConstants.CHAT_ID, chatId)
                .equalTo(DBConstants.TYPE, MessageType.SENT_IMAGE)
                .or()
                .equalTo(DBConstants.CHAT_ID, chatId)
                .equalTo(DBConstants.TYPE, MessageType.RECEIVED_IMAGE)
                .equalTo(DBConstants.DOWNLOAD_UPLOAD_STAT, DownloadUploadStat.SUCCESS)
                .or()
                .equalTo(DBConstants.CHAT_ID, chatId)
                .equalTo(DBConstants.TYPE, MessageType.SENT_VIDEO)
                .or()
                .equalTo(DBConstants.CHAT_ID, chatId)
                .equalTo(DBConstants.TYPE, MessageType.RECEIVED_VIDEO)
                .equalTo(DBConstants.DOWNLOAD_UPLOAD_STAT, DownloadUploadStat.SUCCESS)
                .findAll()
                .sort(DBConstants.TIMESTAMP);


    }


    public void changeDownloadOrUploadStat(String messageId, int stat) {

        RealmResults<Message> results = realm.where(Message.class).equalTo(DBConstants.MESSAGE_ID, messageId).findAll();
        for (Message message : results) {
            realm.beginTransaction();
            message.setDownloadUploadStat(stat);
            realm.commitTransaction();
        }
    }


    //get all users that have installed this app and sort them by name
    public RealmResults<User> getListOfUsers() {
        return realm.where(User.class)
                .not()
                .in(DBConstants.PHONE, new String[]{SharedPreferencesManager.getPhoneNumber()})
                .equalTo(DBConstants.isGroupBool, false)
                .equalTo(DBConstants.IS_STORED_IN_CONTACTS, true)
                .findAll()
                .sort(DBConstants.USERNAME);


    }


    //get all users that have installed this app and sort them by name
    public RealmResults<User> getForwardList() {
        return realm.where(User.class)
                .equalTo(DBConstants.isGroupBool, true)
                .equalTo(DBConstants.GROUP_DOT_IS_ACTIVE, true)
                .or()
                .equalTo(DBConstants.isGroupBool, false)
                .not()
                .in(DBConstants.PHONE, new String[]{SharedPreferencesManager.getPhoneNumber()})
                .findAll()
                .sort(DBConstants.USERNAME, Sort.DESCENDING);
    }

    //update user img if it's different
    public void updateUserImg(String uid, String imgUrl, String localPath, String oldLocalPath) {
        User user = realm.where(User.class).equalTo(DBConstants.UID, uid).findFirst();
        if (user == null)
            return;


        realm.beginTransaction();
        //save the user url in realm if it's not exists
        if (user.getPhoto() == null)
            user.setPhoto(imgUrl);
        else {
            //check if it's different
            if (!user.getPhoto().equals(imgUrl))
                user.setPhoto(imgUrl);
        }

        //set user photo path in device
        user.setUserLocalPhoto(localPath);
        realm.commitTransaction();

        //delete old photo from device
        FileUtils.deleteFile(oldLocalPath);


    }

    //get certain user
    public User getUser(String uid) {
        return realm.where(User.class).equalTo(DBConstants.UID, uid).findFirst();
    }

    public User getUserByNumber(String phone) {
        return realm.where(User.class).equalTo(DBConstants.PHONE, phone).findFirst();
    }

    //check if messages is exists in database
    public boolean isExists(String messageId) {
        return !realm.where(Message.class).equalTo(DBConstants.MESSAGE_ID, messageId).findAll().isEmpty();
    }

    //delete chat
    public void deleteChat(String chatId) {
        realm.beginTransaction();
        //delete chat
        realm.where(Chat.class).equalTo(DBConstants.CHAT_ID, chatId).findAll().deleteFirstFromRealm();
        //delete all messages in this chat
        getMessagesInChat(chatId).deleteAllFromRealm();
        realm.commitTransaction();
    }


    //set chat muted
    public void setMuted(String chatId, boolean bool) {
        Chat chat = getChat(chatId);
        if (chat != null) {
            realm.beginTransaction();
            chat.setMuted(bool);
            realm.commitTransaction();
        }
    }

    //search for a Chat by the given name
    public RealmResults<Chat> searchForChat(String query) {
        return realm.where(Chat.class)
                .contains(DBConstants.USER_USERNAME, query, Case.INSENSITIVE)
                .findAll();
    }

    //search for a user by the given name or number
    public RealmResults<User> searchForUser(String query, boolean showGroupOrNotStoredContacts) {
        RealmQuery realmQuery;
        //get users for forward activity
        if (showGroupOrNotStoredContacts) {
            realmQuery = realm.where(User.class)
                    .contains(DBConstants.USERNAME, query, Case.INSENSITIVE)
                    .equalTo(DBConstants.GROUP_DOT_IS_ACTIVE, true)
                    .equalTo(DBConstants.isGroupBool, true)
                    .or()
                    .contains(DBConstants.USERNAME, query, Case.INSENSITIVE)
                    .equalTo(DBConstants.isGroupBool, false)
                    .or()
                    .contains(DBConstants.PHONE, query)
                    .equalTo(DBConstants.isGroupBool, true)
                    .equalTo(DBConstants.GROUP_DOT_IS_ACTIVE, true)
                    .or()
                    .contains(DBConstants.PHONE, query)
                    .equalTo(DBConstants.isGroupBool, false)
                    .not()
                    .in(DBConstants.PHONE, new String[]{SharedPreferencesManager.getPhoneNumber()});
        } else {
            realmQuery = realm.where(User.class)
                    .equalTo(DBConstants.IS_STORED_IN_CONTACTS, true)
                    .equalTo(DBConstants.IS_GROUP, false)
                    .contains(DBConstants.USERNAME, query, Case.INSENSITIVE)
                    .or()
                    .equalTo(DBConstants.IS_STORED_IN_CONTACTS, true)
                    .equalTo(DBConstants.IS_GROUP, false)
                    .contains(DBConstants.PHONE, query)
                    .not()
                    .in(DBConstants.PHONE, new String[]{SharedPreferencesManager.getPhoneNumber()});
        }
        return realmQuery.findAll();
    }

    //search for a text message in certain chat with the given query
    public RealmResults<Message> searchForMessage(String chatId, String query) {
        return realm.where(Message.class)
                .equalTo(DBConstants.CHAT_ID, chatId)
                .contains(DBConstants.CONTENT, query, Case.INSENSITIVE)
                .equalTo(DBConstants.TYPE, MessageType.SENT_TEXT)
                .or()
                .equalTo(DBConstants.CHAT_ID, chatId)
                .contains(DBConstants.CONTENT, query, Case.INSENSITIVE)
                .equalTo(DBConstants.TYPE, MessageType.RECEIVED_TEXT)
                .findAll();
    }

    //save the firebase storage path for a file in realm to use it late when forwarding
    public void changeMessageContent(String messageId, String link) {
        Message message = realm.where(Message.class).equalTo(DBConstants.MESSAGE_ID, messageId).findFirst();
        if (message == null)
            return;

        realm.beginTransaction();
        message.setContent(link);
        realm.commitTransaction();
    }


    //update video thumb (not blurred version)
    public void setVideoThumb(String messageId, String videoThumb) {
        Message message = getMessage(messageId);

        if (message == null) return;
        realm.beginTransaction();
        message.setVideoThumb(videoThumb);
        realm.commitTransaction();
    }


    //this is called when user has no internet connection and he opened a chat to see a message
    //that is NOT read before,therefore we want to save this to update the message with read state
    //once there is an internet connection
    // and the same is applied for received state
    public void saveUnUpdatedMessageStat(String myUid, String messageId, int statToBeSaved) {
        UnUpdatedStat unUpdatedStat = new UnUpdatedStat(myUid, messageId, statToBeSaved);
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(unUpdatedStat);
        realm.commitTransaction();
    }


    //same idea as saveUnUpdatedMessageStat
    public void saveUnUpdatedVoiceMessageStat(String myUid, String messageId, boolean isVoiceMessageSeen) {
        UnUpdatedVoiceMessageStat unUpdatedVoiceMessageStat = new UnUpdatedVoiceMessageStat(myUid, messageId, isVoiceMessageSeen);
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(unUpdatedVoiceMessageStat);
        realm.commitTransaction();
    }

    //get not updated messages state to update them
    public RealmResults<UnUpdatedStat> getUnUpdateMessageStat() {
        return realm.where(UnUpdatedStat.class).findAll();
    }

    public RealmResults<UnUpdatedVoiceMessageStat> getUnUpdatedVoiceMessageStat() {
        return realm.where(UnUpdatedVoiceMessageStat.class).findAll();
    }

    //delete UnUpdatedVoiceMessageStat once it's updated
    public void deleteUnUpdatedVoiceMessageStat(String messageId) {
        realm.where(UnUpdatedVoiceMessageStat.class).equalTo(DBConstants.MESSAGE_ID, messageId).findAll().deleteAllFromRealm();
    }

    //delete deleteUnUpdateStat once it's updated
    public void deleteUnUpdateStat(String messageId) {
        realm.where(UnUpdatedStat.class).equalTo(DBConstants.MESSAGE_ID, messageId).findAll().deleteAllFromRealm();
    }


    public void setUserBlocked(User user, boolean isBlocked) {
        if (user == null) return;

        realm.beginTransaction();
        user.setBlocked(isBlocked);
        realm.copyToRealmOrUpdate(user);
        realm.commitTransaction();
    }


    //update user info if it's different from stored user
    public void updateUserInfo(User newUser, User storedUser, String name, boolean isStored) {

        realm.beginTransaction();

        if (storedUser.getStatus() == null)
            storedUser.setStatus(newUser.getStatus());
        else if (!storedUser.equals(newUser.getStatus()))
            storedUser.setStatus(newUser.getStatus());

        if (storedUser.getUserName() == null)
            storedUser.setUserName(name);
        else if (!storedUser.getUserName().equals(name))
            storedUser.setUserName(name);

        if (storedUser.getThumbImg() == null)
            storedUser.setThumbImg(newUser.getThumbImg());
        else if (!storedUser.getThumbImg().equals(newUser.getThumbImg())) {
            storedUser.setThumbImg(newUser.getThumbImg());
        }

        if (storedUser.isStoredInContacts() != isStored) {
            storedUser.setStoredInContacts(isStored);
        }

        if (storedUser.getAppVer() == null && newUser.getAppVer() != null) {
            storedUser.setAppVer(newUser.getAppVer());
        } else if (!storedUser.getAppVer().equals(newUser.getAppVer()))
            storedUser.setAppVer(newUser.getAppVer());


        realm.commitTransaction();


    }


    public void updateThumbImg(String uid, String thumbImg) {
        User user = realm.where(User.class).equalTo(DBConstants.UID, uid).findFirst();
        if (user == null) return;

        realm.beginTransaction();
        user.setThumbImg(thumbImg);
        realm.copyToRealmOrUpdate(user);
        realm.commitTransaction();
    }


    public RealmResults<PendingGroupJob> getPendingGroupCreationJobs() {
        return realm.where(PendingGroupJob.class).findAll();
    }

    public void deletePendingGroupCreationJob(String groupId) {
        RealmResults<PendingGroupJob> pendingGroupJobs = realm.where(PendingGroupJob.class).equalTo("groupId", groupId).findAll();
        if (!pendingGroupJobs.isEmpty()) {
            realm.beginTransaction();
            pendingGroupJobs.deleteAllFromRealm();
            realm.commitTransaction();
        }
    }


    public void deleteGroupMember(String groupId, String userToDeleteUid) {
        User groupUser = getUser(groupId);
        User userToDelete = getUser(userToDeleteUid);


        if (groupUser.getGroup() != null && userToDelete != null) {
            RealmList<User> users = groupUser.getGroup().getUsers();
            RealmList<String> adminsUids = groupUser.getGroup().getAdminsUids();


            realm.beginTransaction();
            users.remove(userToDelete);
            if (adminsUids.contains(userToDeleteUid))
                adminsUids.remove(userToDeleteUid);
            realm.commitTransaction();


        }
    }

    public void addUsersToGroup(String groupId, ArrayList<User> newUsers) {
        User user = RealmHelper.getInstance().getUser(groupId);
        if (user != null && user.getGroup() != null && user.getGroup().getUsers() != null) {
            RealmList<User> users = user.getGroup().getUsers();
            realm.beginTransaction();
            users.addAll(newUsers);
            realm.commitTransaction();
        }
    }

    public void addUsersToGroup(String groupId, User newUser) {
        User user = RealmHelper.getInstance().getUser(groupId);
        if (user != null && user.getGroup() != null && user.getGroup().getUsers() != null) {
            RealmList<User> users = user.getGroup().getUsers();
            if (!users.contains(newUser)) {
                realm.beginTransaction();
                users.add(newUser);
                realm.commitTransaction();
            }
        }
    }

    public void setGroupAdmin(String groupId, String adminUid, boolean setAdmin) {
        User user = getUser(groupId);
        if (user != null && user.getGroup() != null) {
            realm.beginTransaction();
            RealmList<String> adminsUids = user.getGroup().getAdminsUids();

            if (!setAdmin) {
                adminsUids.remove(adminUid);
            } else if (!adminsUids.contains(adminUid)) {
                adminsUids.add(adminUid);
            }
            realm.commitTransaction();
        }
    }


    public void changeGroupName(String groupId, String groupTitle) {
        User user = getUser(groupId);
        if (user != null && user.getGroup() != null) {
            realm.beginTransaction();
            if (!user.getUserName().equals(groupId)) {
                user.setUserName(groupTitle);
            }
            realm.commitTransaction();
        }
    }

    public void exitGroup(String groupId) {
        User groupUser = RealmHelper.getInstance().getUser(groupId);
        if (groupUser != null && groupUser.getGroup() != null) {
            Group group = groupUser.getGroup();
            realm.beginTransaction();
            group.setActive(false);
            group.getAdminsUids().clear();
            RealmList<User> users = group.getUsers();
            User user = SharedPreferencesManager.getCurrentUser();
            if (users.contains(user)) {
                users.remove(user);
            }
            realm.commitTransaction();
        }
    }


    public void saveDeletedMessage(String messageId) {
        DeletedMessage deletedMessage = new DeletedMessage(messageId);
        saveObjectToRealm(deletedMessage);
    }

    public DeletedMessage getDeletedMessage(String messageId) {
        return realm.where(DeletedMessage.class).equalTo(DBConstants.MESSAGE_ID, messageId).findFirst();
    }

    private void deleteDeletedMessage(String messageId) {
        DeletedMessage deletedMessage = getDeletedMessage(messageId);
        if (deletedMessage == null) return;

        realm.beginTransaction();
        deletedMessage.deleteFromRealm();
        realm.commitTransaction();

    }

    //set message as deleted (Delete for everyone)
    public void setMessageDeleted(String messageId) {
        Message message = getMessage(messageId);

        if (message == null) {
            saveDeletedMessage(messageId);
            return;
        }


        //if it's already deleted(if it's in group the delete event occurred twice)
        if (MessageType.isDeletedMessage(message.getType()))
            return;

        realm.beginTransaction();

        if (message.isMediaType()) {
            FileUtils.deleteFile(message.getLocalPath());
            message.setLocalPath(null);
        }

        if (MessageType.isSentType(message.getType()))
            message.setType(MessageType.SENT_DELETED_MESSAGE);
        else
            message.setType(MessageType.RECEIVED_DELETED_MESSAGE);

        String chatId = message.getChatId();
        Chat chat = getChat(chatId);
        if (chat != null) {
            chat.getUnreadMessages().remove(message);
            int unreadCount = chat.getUnReadCount();
            if (unreadCount > 0)
                chat.setUnReadCount(--unreadCount);
        }


        realm.commitTransaction();

        deleteDeletedMessage(messageId);
    }

    public int generateNotificationId() {
        Number maxId = realm.where(Chat.class).max(DBConstants.NOTIFICATION_ID);

        // If there are no rows, currentId is null, so the next id must be 1
        // If currentId is not null, increment it by 1
        int nextId = (maxId == null) ? 1 : maxId.intValue() + 1;
        return nextId;
    }

    public int generateJobId() {
        Number maxId = realm.where(JobId.class).max(DBConstants.JOB_ID);

        // If there are no rows, currentId is null, so the next id must be 1
        // If currentId is not null, increment it by 1
        int nextId = (maxId == null) ? 1 : maxId.intValue() + 1;
        return nextId;
    }

    public void setOnlyAdminsCanPost(String groupId, boolean b) {
        User user = getUser(groupId);
        if (user != null) {
            if (user.getGroup().isOnlyAdminsCanPost() == b)
                return;

            realm.beginTransaction();
            user.getGroup().setOnlyAdminsCanPost(b);
            realm.commitTransaction();
        }

    }

    //this will update the group, add,remove a user,set a user as an admin,
    //check for group info change,etc..
    public List<String> updateGroup(String groupId, DataSnapshot info, DataSnapshot usersSnapshot) {

        User groupUser = getUser(groupId);
        if (groupUser == null) return null;

        Group group = groupUser.getGroup();


        Boolean onlyAdminsCanPost = info.child("onlyAdminsCanPost").getValue(Boolean.class);
        String groupName = info.child("name").getValue(String.class);
        String thumbImg = info.child("thumbImg").getValue(String.class);

        RealmList<User> users = group.getUsers();
        RealmList<String> adminsUids = group.getAdminsUids();

        List<String> unfetchedUsers = new ArrayList<>();

        List<String> serverUids = new ArrayList<>();
        List<String> storedUids = new ArrayList<>();


        for (User user : group.getUsers()) {
            storedUids.add(user.getUid());
        }

        realm.beginTransaction();

        if (group.isOnlyAdminsCanPost() != onlyAdminsCanPost)
            group.setOnlyAdminsCanPost(onlyAdminsCanPost);

        if (!groupUser.getUserName().equals(groupName))
            groupUser.setUserName(groupName);


        if (!groupUser.getThumbImg().equals(thumbImg))
            groupUser.setThumbImg(thumbImg);


        for (DataSnapshot dataSnapshot : usersSnapshot.getChildren()) {
            String uid = dataSnapshot.getKey();
            Boolean isAdmin = dataSnapshot.getValue(Boolean.class);
            serverUids.add(uid);

            if (isAdmin) {
                if (!adminsUids.contains(uid)) {
                    adminsUids.add(uid);
                }
            } else {
                if (adminsUids.contains(uid)) {
                    adminsUids.remove(uid);
                }
            }
        }

        //get only unique items from two lists and act against it
        List<String> distinct = ListUtil.distinct(storedUids, serverUids);

        for (String uid : distinct) {
            //addition event
            if (serverUids.contains(uid)) {
                User user = getUser(uid);
                if (user != null) {
                    users.add(user);
                    if (usersSnapshot.child(uid).getValue(Boolean.class)) {
                        adminsUids.add(uid);
                    }
                } else {
                    //if it's a new user then add him to hashmap to fetch his data late
                    unfetchedUsers.add(uid);
                }

                //if the uid is current user's id then set the group as active
                if (uid.equals(FireManager.getUid())) {
                    group.setActive(true);
                    EventBus.getDefault().post(new GroupActiveStateChanged(groupId, true));
                }
            }
            //otherwise it's a deletion event
            else {
                //get user from group
                User userById = ListUtil.getUserById(uid, users);
                //check if exists
                if (userById != null) {
                    //remove him from group
                    users.remove(userById);
                    //if current user is removed set group active to false
                    if (uid.equals(FireManager.getUid())) {
                        group.setActive(false);
                        EventBus.getDefault().post(new GroupActiveStateChanged(groupId, false));

                    }
                }
            }
        }

        realm.commitTransaction();
        return unfetchedUsers;
    }

    public void saveJobId(JobId jobId) {
        realm.beginTransaction();
        realm.copyToRealm(jobId);
        realm.commitTransaction();
    }

    public int getJobId(String messageId, boolean isVoiceMessage) {
        JobId jobId = realm.where(JobId.class).equalTo(DBConstants.ID, messageId).equalTo(DBConstants.isVoiceMessage, isVoiceMessage).findFirst();
        if (jobId != null) {
            return jobId.getJobId();
        }
        return -1;
    }

    public String getJobId(int id, boolean isVoiceMessage) {
        JobId jobId = realm.where(JobId.class).equalTo(DBConstants.JOB_ID, id).equalTo(DBConstants.isVoiceMessage, isVoiceMessage).findFirst();
        if (jobId != null) {
            return jobId.getId();
        }
        return "";
    }


    public void deleteJobId(String id, boolean isVoiceMessage) {
        JobId jobId = realm.where(JobId.class).equalTo(DBConstants.ID, id).equalTo(DBConstants.isVoiceMessage, isVoiceMessage).findFirst();
        if (jobId != null) {
            realm.beginTransaction();
            jobId.deleteFromRealm();
            realm.commitTransaction();
        }

    }


}



