package com.kesen.appfire.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.kesen.echo.R;
import com.kesen.appfire.adapters.UsersAdapter;
import com.kesen.appfire.events.SyncContactsFinishedEvent;
import com.kesen.appfire.model.realms.User;
import com.kesen.appfire.utils.RealmHelper;
import com.kesen.appfire.utils.ServiceHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import io.realm.RealmResults;

public class NewChatActivity extends AppCompatActivity {
    private RecyclerView rvNewChat;
    UsersAdapter adapter;
    RealmResults<User> userList;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private ImageButton refreshContactsBtn;
    AdView adView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);
        init();
        setSupportActionBar(toolbar);
        userList = getListOfUsers();
        setTheAdapter();

        getSupportActionBar().setTitle(R.string.select_contact);
        //enable arrow item in toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadAd();

        refreshContactsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncContacts();
            }
        });


    }

    private void loadAd() {
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                adView.setVisibility(View.GONE);
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                adView.setVisibility(View.VISIBLE);
            }
        });

        if (getResources().getBoolean(R.bool.is_new_chat_ad_enabled))
            adView.loadAd(new AdRequest.Builder().build());
    }

    private void syncContacts() {
        progressBar.setVisibility(View.VISIBLE);
        refreshContactsBtn.setVisibility(View.GONE);
        ServiceHelper.startSyncContacts(this);
    }


    private void init() {
        rvNewChat = findViewById(R.id.rv_new_chat);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar_sync);
        adView = findViewById(R.id.ad_view);
        refreshContactsBtn = findViewById(R.id.refresh_contacts_btn);

    }

    private RealmResults<User> getListOfUsers() {
        return RealmHelper.getInstance().getListOfUsers();
    }


    private void setTheAdapter() {
        adapter = new UsersAdapter(userList, true, this);
        rvNewChat.setLayoutManager(new LinearLayoutManager(this));
        rvNewChat.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_chat, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_item_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.trim().isEmpty()) {
                    RealmResults<User> users = RealmHelper.getInstance().searchForUser(newText,false);
                    adapter = new UsersAdapter(users, true, NewChatActivity.this);
                    rvNewChat.setAdapter(adapter);
                } else {
                    adapter = new UsersAdapter(userList, true, NewChatActivity.this);
                    rvNewChat.setAdapter(adapter);
                }
                return false;
            }

        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                adapter = new UsersAdapter(userList, true, NewChatActivity.this);
                rvNewChat.setAdapter(adapter);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    //hide progress bar when sync contacts finishes
    @Subscribe
    public void onSyncFinished(SyncContactsFinishedEvent event) {
        progressBar.setVisibility(View.GONE);
        refreshContactsBtn.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
