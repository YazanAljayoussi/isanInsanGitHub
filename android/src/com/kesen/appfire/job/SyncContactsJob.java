package com.kesen.appfire.job;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.kesen.appfire.events.SyncContactsFinishedEvent;
import com.kesen.appfire.utils.ContactUtils;
import com.kesen.appfire.utils.JobSchedulerSingleton;
import com.kesen.appfire.utils.SharedPreferencesManager;

import org.greenrobot.eventbus.EventBus;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)

public class SyncContactsJob extends JobService {

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        ContactUtils.syncContacts(this, new ContactUtils.OnContactSyncFinished() {
            @Override
            public void onFinish() {
                //update ui when sync is finished
                EventBus.getDefault().post(new SyncContactsFinishedEvent());
                //to prevent initial sync contacts when the app is launched for first time
                SharedPreferencesManager.setContactSynced(true);
                jobFinished(jobParameters, false);
            }
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    public static void schedule(Context context) {
        ComponentName component = new ComponentName(context, SyncContactsJob.class);


        JobInfo.Builder builder = new JobInfo.Builder(JobIds.JOB_ID_SYNC_CONTACTS, component)
                .setMinimumLatency(1)
                .setOverrideDeadline(1)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);


        JobSchedulerSingleton.getInstance().schedule(builder.build());
    }


}
