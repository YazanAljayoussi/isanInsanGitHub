package com.kesen.appfire.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.mygdx.game.R;
import com.kesen.appfire.activities.ChatActivity;
import com.kesen.appfire.activities.MainActivity;
import com.kesen.appfire.model.constants.DownloadUploadStat;
import com.kesen.appfire.model.constants.MessageStat;
import com.kesen.appfire.model.realms.Chat;
import com.kesen.appfire.model.realms.Message;
import com.kesen.appfire.model.realms.User;
import com.kesen.appfire.receivers.HandleReplyReceiver;

import java.util.HashMap;
import java.util.List;

import io.realm.RealmList;
import me.leolin.shortcutbadger.ShortcutBadger;

public class NotificationHelper extends ContextWrapper {

    private static final String KEY_NOTIFICATION_GROUP = "handleNewMessage-group";
    public static final String LABEL_REPLY = "Reply";
    public static final String KEY_PRESSED_ACTION = "KEY_PRESSED_ACTION";
    public static final String KEY_TEXT_REPLY = "KEY_TEXT_REPLY";

    //this is used to handleNewMessage on devices below API24 since it will be only one notification
    public static final int ID_NOTIFICATION = 1;
    public static final int ID_NOTIFICATION_AUDIO = -2;
    public static final int ID_GROUP_NOTIFICATION = -1;

    public static final String NOTIFICATION_CHANNEL_NAME_MESSAGES = "Messages Notifications";
    public static final String NOTIFICATION_CHANNEL_ID_MESSAGES = "Messages_Notifications_ID";
    public static final String NOTIFICATION_CHANNEL_NAME_AUDIO = "Audio Notifications";
    public static final String NOTIFICATION_CHANNEL_ID_AUDIO = "Audio_Notifications_ID";

    private NotificationManager manager;


    public NotificationHelper(Context base) {
        super(base);
        //create notification channels for android Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel messagesChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_MESSAGES,
                    NOTIFICATION_CHANNEL_NAME_MESSAGES, NotificationManager.IMPORTANCE_HIGH);

            messagesChannel.setVibrationPattern(getVibrationPattern());

            getManager().createNotificationChannel(messagesChannel);

            NotificationChannel audioChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_AUDIO,
                    NOTIFICATION_CHANNEL_NAME_AUDIO, NotificationManager.IMPORTANCE_DEFAULT);
            audioChannel.setSound(null, null);

            getManager().createNotificationChannel(audioChannel);

        }
    }

    private long[] getVibrationPattern() {
        return SharedPreferencesManager.isVibrateEnabled() ? new long[]{200, 200} : new long[0];
    }


    //get summary text (30 messages from 2 chats for example)
    private String getSubText(int messagesCount, int chatsCount) {
        String chats = chatsCount == 1 ? " Chat" : " Chats";
        String messages = messagesCount == 1 ? " Message" : " Messages";
        if (chatsCount <= 1) {
            return messagesCount + " New " + messages;
        }
        return messagesCount + messages + " from " + chatsCount + chats;
    }

    private String getUserNameWithNumOfMessages(int unreadCount,
                                                String userName) {
        if (unreadCount == 0 || unreadCount == 1)
            return userName;

        return userName + " " + "(" + unreadCount + " Messages" + ")" + " ";

    }

    public static boolean isBelowApi24() {
        return Build.VERSION.SDK_INT < 24;
    }

    //get message content with emoji icon if needed
    private String getMessageContent(Message message) {
        String contentText;
        //if it's a text message we don't need to show an Emoji
        if (message.isTextMessage())
            contentText = message.getContent();
        else {
            //if it's a voice message add mic icon at the start along with voice message duration in ()
            if (message.isVoiceMessage()) {
                contentText = MessageTypeHelper.getEmojiIcon(message.getType()) + " " + MessageTypeHelper.getTypeText(message.getType()) + " (" + message.getMediaDuration() + ")";
                //if it's a contact message show contact icon + the contact name
            } else if (message.isContactMessage()) {
                contentText = MessageTypeHelper.getEmojiIcon(message.getType()) + " " + message.getContact().getName() + MessageTypeHelper.getTypeText(message.getType());
            } else
                //otherwise get the needed emoji(image,video,file location etc..)
                contentText = MessageTypeHelper.getEmojiIcon(message.getType()) + " " + MessageTypeHelper.getTypeText(message.getType());
        }
        return contentText;
    }

    private Bitmap getProfilePhotoAsBitmap(String thumbImg) {
        if (thumbImg != null)
            return BitmapUtils.encodeImage(thumbImg);

        //if thumbImg is not exists like the user data is not downloaded yet
        //then get the default placeholder image
        return BitmapUtils.getBitmapFromVectorDrawable(this, R.drawable.user_img);

    }

    private void saveMessageAndUpdateCount(Message message, String phone) {

        //set message as seen if same chat is already open
        if (isBelowApi24() && MyApp.getCurrentChatId().equals(message.getChatId()))
            message.setSeen(true);

        //save message
        RealmHelper.getInstance().saveMessageFromFCM(this, message, phone);

        //if the current activity is not alive OR the activity chatId is not the same with the current chat id
        //then increment unread count
        if (!MyApp.getCurrentChatId().equals(message.getChatId()))
            RealmHelper.getInstance().saveUnreadMessage(message);

    }

    private void saveMessageAndUpdateCount(Message message, User user) {

        //set message as seen if same chat is already open
        if (isBelowApi24() && MyApp.getCurrentChatId().equals(message.getChatId()))
            message.setSeen(true);

        //save message
        RealmHelper.getInstance().saveMessageFromFCM(message, user);

        //if the current activity is not alive OR the activity chatId is not the same with the current chat id
        //then increment unread count
        if (!MyApp.getCurrentChatId().equals(message.getChatId()))
            RealmHelper.getInstance().saveUnreadMessage(message);


    }

    private String getSenderName(String userName, String groupName) {

        if (isBelowApi24() && groupName != null) {
            return userName + " @ " + groupName;
        }


        return userName;
    }

    //dismiss notification when open the chat activity with certain user
    public void dismissNotification(String chatId, boolean decrementCount) {

        Chat chat = RealmHelper.getInstance().getChat(chatId);
        if (chat != null) {

            int notificationId = isBelowApi24() ? ID_NOTIFICATION : chat.getNotificationId();
            getManager().cancel(notificationId);
            if (decrementCount) {
                updateNotificationCount(0);
                RealmHelper.getInstance().deleteUnReadMessages(chatId);
            }
            //dismiss grouped notification if there are no notifications left
            if (!isBelowApi24() && !RealmHelper.getInstance().areThereUnreadChats()) {
                //dismiss grouped notifications
                getManager().cancel(ID_GROUP_NOTIFICATION);
            }
        }
    }

    //update Notification Count badge (in launcher)
    public void updateNotificationCount(int messagesCount) {

        if (messagesCount >= 0) {
            ShortcutBadger.applyCount(this, messagesCount);
        }
    }

    //fire notification
    public void handleNewMessage(final String phone, final Message message) {

        final String chatId = message.getChatId();


        //if unknown number contacted us ,we want to download his data and save it in local db
        if (!message.isGroup() && RealmHelper.getInstance().getUser(chatId) == null)
            FireManager.fetchUserDataAndSaveIt(this, phone);

        //check if auto download is enabled for current network type
        if (SharedPreferencesManager.canDownload(message.getType(), NetworkHelper.getCurrentNetworkType(this))) {
            //set state to downloading
            message.setDownloadUploadStat(DownloadUploadStat.LOADING);
            //save message to database
            if (message.isGroup()) {
                User user = RealmHelper.getInstance().getUser(chatId);
                saveMessageAndUpdateCount(message, user);
            } else
                saveMessageAndUpdateCount(message, phone);

            //start auto download
            ServiceHelper.startNetworkRequest(this, message.getMessageId());
        } else {
            //save message to database
            if (message.isGroup()) {
                User user = RealmHelper.getInstance().getUser(chatId);
                saveMessageAndUpdateCount(message, user);
            } else
                saveMessageAndUpdateCount(message, phone);
        }


        String messageId = message.getMessageId();

        if (!message.isGroup())
            setMessageStat(messageId);


        //if the current activity is not alive OR the activity chatId is not the same with the current chat id
        //then fire notification
        if (!chatId.equals(MyApp.getCurrentChatId())) {
            fireNotification(message.getChatId());
        }


    }

    private void fireNotification(String newMessageChatId) {
        boolean isNotificationsEnabled = SharedPreferencesManager.isNotificationEnabled();
        long unreadMessagesCount = RealmHelper.getInstance().getUnreadMessagesCount();

        //below api24 (no notifications grouping feature)
        if (isBelowApi24()) {
            //store chats in hash map as it may occurs more than once rather than getting the same chat every time, it's kinda 'caching' logic
            HashMap<String, Chat> chatHashMap = new HashMap<>();
            //get last7Unread messages because it's the maximum number for messages to show in a notification
            List<Message> last7UnreadMessages = RealmHelper.getInstance().getLast7UnreadMessages();
            final NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("");
            //get all unread chats count
            int chatsCount = (int) RealmHelper.getInstance().getUnreadChatsCount();
            String lastMessageTimestamp;

            Chat lastChat;

            //if message was deleted dismiss notification
            if (last7UnreadMessages.isEmpty()) {
                getManager().cancel(ID_NOTIFICATION);
            } else {

                for (int i = 0; i < last7UnreadMessages.size(); i++) {

                    Message unreadMessage = last7UnreadMessages.get(i);

                    String chatId = unreadMessage.getChatId();
                    Chat chat;
                    //if chat is exists in hashmap get it
                    if (chatHashMap.containsKey(chatId)) {
                        chat = chatHashMap.get(chatId);
                        //otherwise get it from realm and save it to hashmap
                    } else {
                        chat = RealmHelper.getInstance().getChat(chatId);
                        chatHashMap.put(chatId, chat);
                    }

                    lastChat = chat;

                    lastMessageTimestamp = unreadMessage.getTimestamp();

                    //if chat has not enable notifications do nothing
                    //otherwise fill the notification
                    if (isNotificationsEnabled && !chat.isMuted()) {
                        String sender = "";
                        if (chat.getUser().isGroupBool()) {
                            RealmList<User> users = chat.getUser().getGroup().getUsers();
                            User user = ListUtil.getUserById(unreadMessage.getFromId(), users);
                            if (user != null) {
                                sender = getSenderName(user.getUserName(), chat.getUser().getUserName());
                            }
                        } else {
                            if (chatsCount > 1)
                                sender = getSenderName(chat.getUser().getUserName(), null);
                        }

                        messagingStyle.addMessage(getMessageContent(unreadMessage), Long.parseLong(unreadMessage.getTimestamp()), sender);

                    }


                    NotificationCompat.Builder notificationBuilder =
                            createNotificationBuilder(
                                    ""
                                    , ""
                                    , chat, chatsCount);


                    if (chatHashMap.size() > 1) {
                        String appName = this.getResources().getString(R.string.app_name);
                        messagingStyle.setConversationTitle(appName);
                        notificationBuilder.setContentIntent(getPendingIntent(null));

                    } else {
                        messagingStyle.setConversationTitle(lastChat.getUser().getUserName());
                        notificationBuilder.setContentIntent(getPendingIntent(lastChat));
                    }


                    notificationBuilder.setSubText(getSubText((int) unreadMessagesCount, chatsCount));


                    notificationBuilder.setStyle(messagingStyle);

                    //set the timestamp of the last message
                    if (!lastMessageTimestamp.equals(""))
                        notificationBuilder.setWhen(Long.parseLong(lastMessageTimestamp));


                    //notify once after filling the messages
                    if (i == last7UnreadMessages.size() - 1)
                        getManager().notify(ID_NOTIFICATION, notificationBuilder.build());
                }
            }

            updateNotificationCount((int) unreadMessagesCount);

            //api24+ (grouping feature)
        } else {

            List<Chat> unreadChats = RealmHelper.getInstance().getUnreadChats();

            for (Chat unreadChat : unreadChats) {

                final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                final NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("");

                if (isNotificationsEnabled && !unreadChat.isMuted()) {


                    String userNameWithNumOfMessages = getUserNameWithNumOfMessages(unreadChat.getUnReadCount(), unreadChat.getUser().getUserName());

                    messagingStyle.setConversationTitle(userNameWithNumOfMessages);


                    //if it's a group or it's below api24 we will use messaging style
                    boolean isGroup = unreadChat.getUser().isGroupBool();

                    List<Message> messageList = RealmHelper.getInstance().filterUnreadMessages(unreadChat.getUnreadMessages());
                    if (isGroup) {
                        RealmList<User> users = unreadChat.getUser().getGroup().getUsers();

                        for (Message unreadMessage : messageList) {
                            User user = ListUtil.getUserById(unreadMessage.getFromId(), users);
                            if (user != null) {
                                String sender = getSenderName(user.getUserName(), unreadChat.getUser().getUserName());
                                messagingStyle.addMessage(getMessageContent(unreadMessage), Long.parseLong(unreadMessage.getTimestamp()), sender);
                            }
                        }
                    } else {
                        for (Message unreadMessage : messageList) {
                            inboxStyle.addLine(getMessageContent(unreadMessage));
                        }
                    }


                    NotificationCompat.Builder notificationBuilder =
                            createNotificationBuilder(
                                    userNameWithNumOfMessages
                                    , getMessageContent(unreadChat.getUnreadMessages().last())
                                    , unreadChat, unreadChats.size());

                    //if it's a group we will use messaging style,otherwise we will user inboxStyle
                    notificationBuilder.setStyle(isGroup || isBelowApi24() ? messagingStyle : inboxStyle);

                    if (!unreadChat.getLastMessageTimestamp().equals(""))
                        notificationBuilder.setWhen(Long.parseLong(unreadChat.getLastMessageTimestamp()));


                    NotificationCompat.Builder groupNotification;

                    PendingIntent pendingIntent = getPendingIntent(unreadChat);
                    notificationBuilder.setContentIntent(pendingIntent);

                    //if it's a new message fire the sound and vibration if enabled,otherwise just show other notifications without bombarding the user
                    notificationBuilder.setOnlyAlertOnce(newMessageChatId.equals(unreadChat.getChatId()) ? false : true);


                    int notificationId = unreadChat.getNotificationId();
                    //group multiple notification in one grouped notification
                    groupNotification = createNotificationBuilder("", "", null, unreadChats.size());

                    if (!unreadChat.getLastMessageTimestamp().equals(""))
                        groupNotification.setWhen(Long.parseLong(unreadChat.getLastMessageTimestamp()));


                    //group notification by user id
                    groupNotification.setGroupSummary(true).setGroup(KEY_NOTIFICATION_GROUP);

                    //if it's a new message fire the sound and vibration if enabled,other wise just show other notifications without bombarding the user
                    groupNotification.setOnlyAlertOnce(true);

                    groupNotification.setContentTitle("");

                    //set Summary (eg. 10 Messages from 3 Chat)
                    String subText = getSubText((int) unreadMessagesCount, unreadChats.size());

                    groupNotification.setSubText(subText);

                    notificationBuilder.setGroup(KEY_NOTIFICATION_GROUP);


                    notificationBuilder.addAction(getReplyActionInput(unreadChat));


                    getManager().notify(notificationId, notificationBuilder.build());
                    getManager().notify(ID_GROUP_NOTIFICATION, groupNotification.build());

                }
            }
            updateNotificationCount((int) unreadMessagesCount);
        }

    }


    //get onClick intent
    private PendingIntent getPendingIntent(Chat chat) {
        PendingIntent pendingIntent;
        if (isBelowApi24()) {
            //if it's only from one user then open the chatActivity with this user
            if (chat != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra(IntentUtils.UID, chat.getChatId());
                //adding stack for user (to prevent kill the app when click back since there is no previous activity launched
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntentWithParentStack(intent);
                pendingIntent = stackBuilder.getPendingIntent(ID_NOTIFICATION, PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                //otherwise there are multiple messages from multiple users therefore just open Main Activity
                Intent intent = new Intent(this, MainActivity.class);
                pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

        } else {
//        start chatActivity with the clicked user
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(IntentUtils.UID, chat.getChatId());
            //adding stack for user (to prevent kill the app when click back since there is no previous activity launched
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(intent);
            pendingIntent = stackBuilder.getPendingIntent(chat.getNotificationId(), PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return pendingIntent;
    }

    private NotificationCompat.Builder createNotificationBuilder(
            String title, String message, Chat chat, int chatsCount) {


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID_MESSAGES)
                //set app icon
                .setSmallIcon(R.drawable.ic_noti_icon)
                .setContentTitle(title)
                .setContentText(message)
                //color
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                //Notification Sound (get it from shared preferences)
                .setSound(SharedPreferencesManager.getRingtone())
                //high priority to make it show as heads-up
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                //set vibrate if it's enabled by the user
                .setVibrate(getVibrationPattern());

        if (chat != null) {
            User user = chat.getUser();
            Bitmap largeIcon = getProfilePhotoAsBitmap(user.getThumbImg());

            if (!isBelowApi24() || chatsCount == 1)
                builder.setLargeIcon(largeIcon);

        } else {
            Bitmap largeIcon = getProfilePhotoAsBitmap(null);
            builder.setLargeIcon(largeIcon);
        }


        return builder;
    }


    //update the sender with message state (received)
    private void setMessageStat(String messageId) {
        ServiceHelper.startUpdateMessageStatRequest(this, messageId, FireManager.getUid(), MessageStat.RECEIVED);
    }


    //set reply from notification action
    private NotificationCompat.Action getReplyActionInput(Chat chat) {
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel(this.getString(R.string.reply))
                .build();

        PendingIntent replyIntent = PendingIntent.getBroadcast(this
                , chat.getNotificationId()
                , getMessageReplyIntent(LABEL_REPLY, chat.getChatId())
                , PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(android.R.drawable.sym_def_app_icon,
                        this.getString(R.string.reply), replyIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        return replyAction;
    }

    //start service when click on Reply and pass the user
    private Intent getMessageReplyIntent(String label, String chatId) {
        Intent intent = new Intent(this, HandleReplyReceiver.class);
        intent
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(IntentUtils.INTENT_ACTION_HANDLE_REPLY)
                .putExtra(KEY_PRESSED_ACTION, label)
                .putExtra(IntentUtils.UID, chatId);
        return intent;
    }

    public void messageDeleted(Message message) {
        if (message != null) {
            String chatId = message.getChatId();
            if (!MyApp.getCurrentChatId().equals(chatId)) {
                Chat chat = RealmHelper.getInstance().getChat(chatId);
                if (chat != null) {
                    //if it's below api24 update notifications
                    if (isBelowApi24()) {
                        fireNotification(null);
                    } else {
                        //if it's higher than API 24
                        // check if the chat has unread messages
                        //if so update notifications
                        if (chat.getUnReadCount() > 0) {
                            fireNotification(message.getChatId());
                        } else {
                            //otherwise dismiss notification
                            dismissNotification(chatId, false);
                        }
                    }
                }
            }
        }
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    public Notification getAudioNotification() {
        return new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID_AUDIO)
                //set app icon
                .setSmallIcon(R.drawable.ic_noti_icon)
                .setContentTitle(getResources().getString(R.string.playing_audio))
                .setContentText(getResources().getString(R.string.playing_audio))
                //color
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }
}
