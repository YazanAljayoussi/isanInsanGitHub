package com.kesen.appfire.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.kesen.appfire.events.SyncContactsFinishedEvent;
import com.kesen.appfire.utils.ContactUtils;
import com.kesen.appfire.utils.IntentUtils;
import com.kesen.appfire.utils.SharedPreferencesManager;

import org.greenrobot.eventbus.EventBus;

public class SyncContactsService extends IntentService {


    //Required Constructor
    public SyncContactsService() {
        super("SyncContactsService");
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (intent != null && intent.getAction() != null && intent.getAction()
                .equals(IntentUtils.INTENT_ACTION_SYNC_CONTACTS)) {
            ContactUtils.syncContacts(this, new ContactUtils.OnContactSyncFinished() {
                @Override
                public void onFinish() {
                    //update ui when sync is finished
                    EventBus.getDefault().post(new SyncContactsFinishedEvent());
                    //to prevent initial sync contacts when the app is launched for first time
                    SharedPreferencesManager.setContactSynced(true);
                    stopSelf();
                }
            });
        }
    }


}
