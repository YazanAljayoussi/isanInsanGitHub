package com.kesen.appfire.utils;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatDelegate;

import com.evernote.android.job.JobManager;
import com.google.android.gms.ads.MobileAds;
import com.mygdx.game.R;
import com.kesen.appfire.job.FireJobCreator;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Devlomi on 13/08/2017.
 */

public class MyApp extends Application {
    private static MyApp mApp = null;


    public static boolean isChatActivityVisible() {
        return chatActivityVisible;
    }

    public static String getCurrentChatId() {
        return currentChatId;
    }

    public static void activityResumed(String chatId) {
        chatActivityVisible = true;
        currentChatId = chatId;
    }

    public static void activityPaused() {
        chatActivityVisible = false;
        currentChatId = "";
    }


    private static boolean chatActivityVisible;
    private static String currentChatId = "";

    @Override
    public void onCreate() {
        super.onCreate();
        //add support for vector drawables on older APIs
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        //init realm
        Realm.init(this);
        //init set realm configs
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .schemaVersion(1)
                .migration(new MyMigration())
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
        //init shared prefs manager
        SharedPreferencesManager.init(this);
        //init evernote job
        JobManager.create(this).addJobCreator(new FireJobCreator());


        //initialize ads for faster loading in first time
        MobileAds.initialize(this, getString(R.string.admob_app_id));
        mApp = this;

    }

    public static Context context() {
        return mApp.getApplicationContext();
    }

    //to run multi dex
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

}
