package com.kesen.appfire.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.kesen.appfire.events.FetchingUserGroupsFinished;
import com.kesen.appfire.model.realms.Message;
import com.kesen.appfire.utils.DownloadManager;
import com.kesen.appfire.utils.FireManager;
import com.kesen.appfire.utils.GroupEvent;
import com.kesen.appfire.utils.GroupManager;
import com.kesen.appfire.utils.IntentUtils;
import com.kesen.appfire.utils.RealmHelper;
import com.kesen.appfire.utils.SharedPreferencesManager;

import org.greenrobot.eventbus.EventBus;


/**
 * Created by Devlomi on 31/12/2017.
 */

//this is responsible for sending and receiving files/data from firebase using Download Manager Class
public class NetworkService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(IntentUtils.INTENT_ACTION_UPDATE_GROUP)) {

                GroupEvent groupEvent = intent.getParcelableExtra(IntentUtils.EXTRA_GROUP_EVENT);
                String groupId = intent.getStringExtra(IntentUtils.EXTRA_GROUP_ID);


                GroupManager.updateGroup(this, groupId, groupEvent, null);
            }
            if (intent.getAction().equals(IntentUtils.INTENT_ACTION_FETCH_AND_CREATE_GROUP)) {
                String groupId = intent.getStringExtra(IntentUtils.EXTRA_GROUP_ID);
                GroupManager.fetchAndCreateGroup(this, groupId, null);

            } else if (intent.getAction().equals(IntentUtils.INTENT_ACTION_FETCH_USER_GROUPS)) {
                GroupManager.fetchUserGroups(new GroupManager.OnComplete() {
                    @Override
                    public void onComplete(boolean isSuccessful) {
                        if (isSuccessful) {
                            SharedPreferencesManager.setFetchUserGroupsSaved(true);
                            EventBus.getDefault().post(new FetchingUserGroupsFinished());
                        }
                    }
                });

            } else if (intent.getAction().equals(IntentUtils.INTENT_ACTION_HANDLE_REPLY)) {
                String messageId = intent.getStringExtra(IntentUtils.EXTRA_MESSAGE_ID);
                final Message message = RealmHelper.getInstance().getMessage(messageId);
                if (message != null) {
                    DownloadManager.sendMessage(message, new DownloadManager.OnComplete() {
                        @Override
                        public void onComplete(boolean isSuccessful) {
                            if (isSuccessful) {
                                //set other unread messages as read
                                if (!message.isGroup())
                                    FireManager.setMessagesAsRead(NetworkService.this, message.getChatId());
                                //update unread count to 0
                            }
                        }
                    });
                }
            } else {
                String messageId = intent.getStringExtra(IntentUtils.EXTRA_MESSAGE_ID);
                if (intent.getAction().equals(IntentUtils.INTENT_ACTION_UPDATE_MESSAGE_STATE)) {
                    String myUid = intent.getStringExtra(IntentUtils.EXTRA_MY_UID);
                    int state = intent.getIntExtra(IntentUtils.EXTRA_STAT, 0);
                    updateMessageStat(messageId, myUid, state);
                } else if (intent.getAction().equals(IntentUtils.INTENT_ACTION_UPDATE_VOICE_MESSAGE_STATE)) {
                    String myUid = intent.getStringExtra(IntentUtils.EXTRA_MY_UID);
                    updateVoiceMessageStat(messageId, myUid);
                } else {
                    Message message = RealmHelper.getInstance().getMessage(messageId);
                    if (message != null) {
                        DownloadManager.request(message, null);
                    }
                }
            }
        }
        return START_STICKY;
    }


    public void updateMessageStat(final String messageId, final String myUid, final int state) {
        FireManager.updateMessageStat(myUid, messageId, state, new FireManager.OnComplete() {
            @Override
            public void onComplete(boolean isSuccessful) {
                if (isSuccessful) {
                    RealmHelper.getInstance().updateMessageStatLocally(messageId, state);
                    RealmHelper.getInstance().deleteUnUpdateStat(messageId);
                } else {
                    RealmHelper.getInstance().saveUnUpdatedMessageStat(myUid, messageId, state);
                }
            }
        });

    }


    public void updateVoiceMessageStat(final String messageId, final String myUid) {
        FireManager.updateVoiceMessageStat(myUid, messageId, new FireManager.OnComplete() {
            @Override
            public void onComplete(boolean isSuccessful) {
                if (isSuccessful) {
                    RealmHelper.getInstance().updateVoiceMessageStatLocally(messageId);
                    RealmHelper.getInstance().deleteUnUpdatedVoiceMessageStat(messageId);
                } else {
                    RealmHelper.getInstance().saveUnUpdatedVoiceMessageStat(myUid, messageId, true);
                }
            }
        });
    }


    @Override
    public void onDestroy() {
        DownloadManager.cancelAllTasks();
//        EventBus.getDefault().unregister(this);
        super.onDestroy();
        startService(new Intent(this, NetworkService.class));


    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
