package com.kesen.appfire.utils;

import com.kesen.appfire.model.constants.DBConstants;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

//this will called when migrating from old version
public class MyMigration implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

        RealmSchema schema = realm.getSchema();


        if (oldVersion == 0) {

            schema.get("Chat")
                    .addField("notificationId", int.class)
                    .addRealmListField("unreadMessages", schema.get("Message"));


            schema.create("DeletedMessage")
                    .addField(DBConstants.MESSAGE_ID, String.class, FieldAttribute.PRIMARY_KEY);


            schema.create("Group")
                    .addField("groupId", String.class, FieldAttribute.PRIMARY_KEY)
                    .addField("isActive", boolean.class)
                    .addField("createdByNumber", String.class)
                    .addField("timestamp", long.class)
                    .addRealmListField("users", schema.get("User"))
                    .addRealmListField("adminsUids", String.class)
                    .addField("onlyAdminsCanPost", boolean.class);


            schema.get("Message")
                    .addField("isGroup", boolean.class)
                    .addField(DBConstants.IS_SEEN, boolean.class)
                    .addField("fromPhone", String.class);


            schema.get("User")
                    .addField("appVer", String.class)
                    .addField("isGroupBool", boolean.class)
                    .addRealmObjectField("group", schema.get("Group"))
                    .addField("isStoredInContacts", boolean.class);

            schema.create("GroupEvent")
                    .addField("contextStart", String.class)
                    .addField("eventType", int.class)
                    .addField("contextEnd", String.class)
                    .addField("timestamp", String.class)
                    .addField("eventId", String.class);


            schema.create("PendingGroupJob")
                    .addField("groupId", String.class, FieldAttribute.PRIMARY_KEY)
                    .addField("type", int.class)
                    .addRealmObjectField("groupEvent", schema.get("GroupEvent"));

            schema.create("JobId")
                    .addField("id", String.class)
                    .addField(DBConstants.JOB_ID, int.class)
                    .addField("isVoiceMessage", boolean.class);

            //make contacts re-sync again after adding 'isStoredInContacts' field
            SharedPreferencesManager.setContactSynced(false);
            SharedPreferencesManager.setAppVersionSaved(false);

            oldVersion++;


        }
    }
}

