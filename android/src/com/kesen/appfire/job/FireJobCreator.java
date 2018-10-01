package com.kesen.appfire.job;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class FireJobCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {

            case JobIds.JOB_TAG_SYNC_CONTACTS:
                return new SyncContactsDailyJob();

        }
        return null;
    }
}
