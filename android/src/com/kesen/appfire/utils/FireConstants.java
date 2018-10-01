package com.kesen.appfire.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Created by Devlomi on 01/08/2017.
 */

//this class contains firebase database and firebase storage paths and refs
public class FireConstants {
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private static final FirebaseStorage sotarage = FirebaseStorage.getInstance();
    public static final DatabaseReference mainRef = database.getReference();
    //users ref that contain user's data (name,phone,photo etc..)
    public static final DatabaseReference usersRef = mainRef.child("users");

    //groups ref that contains user ids and group info
    public static final DatabaseReference groupsRef = mainRef.child("groups");
    public static final DatabaseReference groupsEventsRef = mainRef.child("groupEvents");

    //this will contain all groups ids that the user participated to
    public static DatabaseReference groupsByUser = mainRef.child("groupsByUser");

    //this will get whom added the user to a group
    public static DatabaseReference groupMemberAddedBy = mainRef.child("groupMemberAddedBy");

    //this will delete a message for all users
    private static DatabaseReference deleteMessageRequests = mainRef.child("deleteMessageRequests");

    private static DatabaseReference deleteMessageRequestsForGroup = mainRef.child("deleteMessageRequestsForGroup");


    //this is the MAJOR ref ,all messages goes in this ref
    public static final DatabaseReference messages = mainRef.child("messages");

    public static final DatabaseReference groupsMessages = mainRef.child("groupsMessages");
    //this ref is for the messages sates (received,read)
    public static final DatabaseReference messageStat = mainRef.child("messages-stat");
    //this ref is for the voice messages sates (is listened or not yet)
    public static final DatabaseReference voiceMessageStat = mainRef.child("voice-messages-stat");

    //this ref is for the user state is he online or not ,if he is not online this will contain the last seen timestamp
    public static final DatabaseReference presenceRef = mainRef.child("presence");

    //this will have the user typing or recording or do nothing value when he chatting with another user
    public static final DatabaseReference typingStat = mainRef.child("typingStat").child(FireManager.getUid());

    public static final DatabaseReference groupTypingStat = mainRef.child("groupTypingStat");


    //this is used when the user blocks another user it will save the blocked uid
    public static DatabaseReference blockedUsersRef = mainRef.child("blockedUsers");

    //this will get the user id by his phone number to use it when searching for a user
    public static DatabaseReference uidByPhone = mainRef.child("uidByPhone");


    public static final StorageReference storageRef = sotarage.getReference();
    //firebase storage folders ,used when uploading or downloading
    public static final StorageReference imageRef = storageRef.child("image");
    public static final StorageReference imageProfileRef = storageRef.child("image_profile");
    public static final StorageReference videoRef = storageRef.child("video");
    public static final StorageReference voiceRef = storageRef.child("voice");
    public static final StorageReference fileRef = storageRef.child("file");
    public static final StorageReference audioRef = storageRef.child("audio");


    //MAX SIZE FOR FCM message IS 4096 ,however we want some more space for other items regardless "Content"
    public static int MAX_SIZE_STRING = 3800;

    public static DatabaseReference getMessageRef(boolean isGroup, String groupId) {
        if (isGroup)
            return groupsMessages.child(groupId);

        return messages;
    }

    public static DatabaseReference getDeleteMessageRequestsRef(String messageId, boolean isGroup, String groupId) {
        if (isGroup)
            return deleteMessageRequestsForGroup.child(groupId).child(messageId);

        return deleteMessageRequests.child(messageId);
    }

}
