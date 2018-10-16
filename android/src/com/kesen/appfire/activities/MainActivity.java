package com.kesen.appfire.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.kesen.echo.R;
import com.kesen.appfire.activities.settings.SettingsActivity;
import com.kesen.appfire.adapters.ChatsAdapter;
import com.kesen.appfire.job.SaveTokenJob;
import com.kesen.appfire.job.SetLastSeenJob;
import com.kesen.appfire.job.SyncContactsDailyJob;
import com.kesen.appfire.model.constants.GroupEventTypes;
import com.kesen.appfire.model.constants.MessageStat;
import com.kesen.appfire.model.constants.MessageType;
import com.kesen.appfire.model.constants.TypingStat;
import com.kesen.appfire.model.realms.Chat;
import com.kesen.appfire.model.realms.Message;
import com.kesen.appfire.model.realms.User;
import com.kesen.appfire.services.FCMRegistrationService;
import com.kesen.appfire.services.InternetConnectedListener;
import com.kesen.appfire.services.NetworkService;
import com.kesen.appfire.utils.AppVerUtil;
import com.kesen.appfire.utils.FireConstants;
import com.kesen.appfire.utils.FireListener;
import com.kesen.appfire.utils.FireManager;
import com.kesen.appfire.utils.GroupEvent;
import com.kesen.appfire.utils.GroupManager;
import com.kesen.appfire.utils.GroupTyping;
import com.kesen.appfire.utils.IntentUtils;
import com.kesen.appfire.utils.NetworkHelper;
import com.kesen.appfire.utils.PresenceUtil;
import com.kesen.appfire.utils.RealmHelper;
import com.kesen.appfire.utils.ServiceHelper;
import com.kesen.appfire.utils.SharedPreferencesManager;
import com.kesen.appfire.utils.UnProcessedJobs;
import com.kesen.appfire.utils.Util;

import java.util.ArrayList;
import java.util.List;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.RealmResults;

public class MainActivity extends BaseActivity implements GroupTyping.GroupTypingListener {
    private FloatingActionButton openNewChatFab;
    private RecyclerView rvChats;
    ChatsAdapter adapter;
    LinearLayoutManager linearLayoutManager;
    RealmResults<Chat> chatList;
    OrderedRealmCollectionChangeListener<RealmResults<Chat>> changeListener;


    ValueEventListener typingEventListener, voiceMessageListener, lastMessageStatListener;
    PresenceUtil presenceUtil;
    public boolean isInActionMode = false;
    Toolbar toolbar;
    private TextView tvSelectedChatCount;
    FireListener fireListener;
    private boolean isInSearchMode = false;
    SearchView searchView;
    AdView adView;
    List<GroupTyping> groupTypingList;


    public boolean isInActionMode() {
        return isInActionMode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        setSupportActionBar(toolbar);
        presenceUtil = new PresenceUtil();
        getChats();
        setTheAdapter();
        startServices();
        fireListener = new FireListener();


        AdListener adListener = new AdListener() {
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
        };


        adView.setAdListener(adListener);

        if (getResources().getBoolean(R.bool.is_main_ad_enabled))
            adView.loadAd(new AdRequest.Builder().build());


        openNewChatFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, NewChatActivity.class));
            }
        });


        listenForTypingStat();
        listenForVoiceMessageStat();
        listenForLastMessageStat();
        listenForMessagesChanges();


        //save app ver if it's not saved before
        if (!SharedPreferencesManager.isAppVersionSaved()) {
            FireConstants.usersRef.child(FireManager.getUid()).child("ver").setValue(AppVerUtil.getAppVersion(this)).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    SharedPreferencesManager.setAppVersionSaved(true);
                }
            });
        }


    }

    //add a listener for the last message if the user has replied from the notification
    private void listenForMessagesChanges() {
        changeListener = new OrderedRealmCollectionChangeListener<RealmResults<Chat>>() {
            @Override
            public void onChange(RealmResults<Chat> chats, OrderedCollectionChangeSet changeSet) {

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();

                if (modifications.length != 0) {
                    Chat chat = chats.get(modifications[0].startIndex);
                    Message lastMessage = chat.getLastMessage();

                    if (lastMessage != null && lastMessage.getMessageStat() == MessageStat.PENDING
                            || lastMessage != null && lastMessage.getMessageStat() == MessageStat.SENT) {
                        addMessageStatListener(chat.getChatId(), lastMessage);
                    }
                }
            }
        };
    }

    //listen for lastMessage stat if it's received or read by the other user
    private void listenForLastMessageStat() {
        lastMessageStatListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) return;

                int val = dataSnapshot.getValue(Integer.class);
                String key = dataSnapshot.getKey();
                RealmHelper.getInstance().updateMessageStatLocally(key, val);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void addVoiceMessageStatListener() {
        for (Chat chat : chatList) {
            Message lastMessage = chat.getLastMessage();
            if (lastMessage != null && lastMessage.getType() != MessageType.GROUP_EVENT && lastMessage.isVoiceMessage()
                    && lastMessage.getFromId().equals(FireManager.getUid())
                    && !lastMessage.isVoiceMessageSeen()) {
                DatabaseReference reference = FireConstants.voiceMessageStat.child(lastMessage.getChatId()).child(lastMessage.getMessageId());
                fireListener.addListener(reference, voiceMessageListener);
            }
        }
    }

    private void addMessageStatListener() {
        for (Chat chat : chatList) {
            Message lastMessage = chat.getLastMessage();
            if (lastMessage != null && lastMessage.getType() != MessageType.GROUP_EVENT && lastMessage.getMessageStat() != MessageStat.READ) {
                DatabaseReference reference = FireConstants.messageStat.child(chat.getChatId()).child(lastMessage.getMessageId());
                fireListener.addListener(reference, lastMessageStatListener);
            }
        }
    }

    private void addMessageStatListener(String chatId, Message lastMessage) {
        if (lastMessage != null && lastMessage.getType() != MessageType.GROUP_EVENT && lastMessage.getMessageStat() != MessageStat.READ) {
            DatabaseReference reference = FireConstants.messageStat.child(chatId).child(lastMessage.getMessageId());
            fireListener.addListener(reference, lastMessageStatListener);
        }
    }

    //if the lastMessage is a Voice message then we want to
    //listen if it's listened by the other user
    private void listenForVoiceMessageStat() {
        voiceMessageListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.getValue() == null) {
                    return;
                }

                String key = dataSnapshot.getKey();
                RealmHelper.getInstance().updateVoiceMessageStatLocally(key);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };


    }

    //listen if other user is typing to this user
    private void listenForTypingStat() {
        typingEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null)
                    return;

                int stat = dataSnapshot.getValue(Integer.class);
                String uid = dataSnapshot.getRef().getParent().getKey();

                //create temp chat object to get the index of the uid
                Chat chat = new Chat();
                chat.setChatId(uid);
                int i = chatList.indexOf(chat);
                //if chat is not exists in the list return
                if (i == -1) return;

                ChatsAdapter.ChatsHolder vh = (ChatsAdapter.ChatsHolder) rvChats.findViewHolderForAdapterPosition(i);

                if (vh == null) {
                    return;
                }


                adapter.getTypingStatHashmap().put(chat.getChatId(), stat);
                TextView typingTv = vh.tvTypingStat;
                TextView lastMessageTv = vh.tvLastMessage;
                ImageView lastMessageReadIcon = vh.imgReadTagChats;


                //if other user is typing or recording to this user
                //then hide last message textView with all its contents
                if (stat == TypingStat.TYPING || stat == TypingStat.RECORDING) {
                    lastMessageTv.setVisibility(View.GONE);
                    lastMessageReadIcon.setVisibility(View.GONE);
                    typingTv.setVisibility(View.VISIBLE);

                    if (stat == TypingStat.TYPING)
                        typingTv.setText(getResources().getString(R.string.typing));
                    else if (stat == TypingStat.RECORDING)
                        typingTv.setText(getResources().getString(R.string.recording));

                    //in case there is no typing or recording event
                    //revert back to normal mode and show last message
                } else {
                    adapter.getTypingStatHashmap().remove(chat.getChatId());
                    typingTv.setVisibility(View.GONE);
                    lastMessageTv.setVisibility(View.VISIBLE);
                    Message lastMessage = chatList.get(i).getLastMessage();
                    if (lastMessage != null &&
                            lastMessage.getType() != MessageType.GROUP_EVENT
                            && !MessageType.isDeletedMessage(lastMessage.getType())
                            && lastMessage.getFromId().equals(FireManager.getUid())) {
                        lastMessageReadIcon.setVisibility(View.VISIBLE);
                    }

                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };


    }

    //adding typing listeners for all chats
    private void addTypingStatListener() {
        if (FireManager.getUid() == null)
            return;

        for (Chat chat : chatList) {
            User user = chat.getUser();
            if (user.isGroupBool() && user.getGroup().isActive()) {

                if (groupTypingList == null)
                    groupTypingList = new ArrayList<>();

                GroupTyping groupTyping = new GroupTyping(user.getGroup().getUsers(), user.getUid(), this);
                groupTypingList.add(groupTyping);

            } else {
                String receiverUid = user.getUid();
                DatabaseReference typingStat = FireConstants.mainRef.child("typingStat").child(receiverUid)
                        .child(FireManager.getUid());
                fireListener.addListener(typingStat, typingEventListener);
            }
        }

    }


    private void startServices() {
        if (!Util.isOreoOrAbove()) {
            startService(new Intent(this, NetworkService.class));
            startService(new Intent(this, InternetConnectedListener.class));
            startService(new Intent(this, FCMRegistrationService.class));

        } else {
            if (!SharedPreferencesManager.isTokenSaved())
                SaveTokenJob.schedule(this, null);

            SetLastSeenJob.schedule(this);
            UnProcessedJobs.process(this);
        }

        //sync contacts for the first time
        //if (!SharedPreferencesManager.isContactSynced())
        {
            ServiceHelper.startSyncContacts(MainActivity.this);
        }

        SyncContactsDailyJob.schedule();

    }


    private void init() {
        openNewChatFab = findViewById(R.id.open_new_chat_fab);
        rvChats = findViewById(R.id.rv_chats);
        toolbar = findViewById(R.id.toolbar);
        tvSelectedChatCount = findViewById(R.id.tv_selected_chat);
        adView = findViewById(R.id.ad_view);


        //prefix for a bug in older APIs
        openNewChatFab.bringToFront();
    }


    private void getChats() {
        chatList = RealmHelper.getInstance().getAllChats();
    }

    @Override
    protected void onResume() {
        super.onResume();

        presenceUtil.onResume();
        addTypingStatListener();
        addVoiceMessageStatListener();
        addMessageStatListener();
        chatList.addChangeListener(changeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        presenceUtil.onPause();
        fireListener.cleanup();
        if (groupTypingList != null) {
            for (GroupTyping groupTyping : groupTypingList) {
                groupTyping.cleanUp();
            }
        }
        chatList.removeChangeListener(changeListener);
    }


    private void setTheAdapter() {
        adapter = new ChatsAdapter(chatList, true, this);
        linearLayoutManager = new LinearLayoutManager(this);
        rvChats.setLayoutManager(linearLayoutManager);
        rvChats.setAdapter(adapter);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_list, menu);
        MenuItem menuItem = menu.findItem(R.id.search_item);
        searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }

        });
        //revert back to original adapter
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                adapter = new ChatsAdapter(chatList, true, MainActivity.this);
                rvChats.setAdapter(adapter);
                exitSearchMode();
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_delete:
                deleteItemClicked();
                break;

            case R.id.menu_item_mute:
                muteItemClicked();
                break;
            case R.id.settings_item:
                settingsItemClicked();
                break;

            case R.id.search_item:
                searchItemClicked();
                break;

            case R.id.new_group_item:
                createGroupClicked();
                break;


            case R.id.exit_group_item:
                exitGroupClicked();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exitGroupClicked() {
        if (!NetworkHelper.isConnected(this))
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirmation)
                .setMessage(R.string.exit_group)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        for (final Chat chat : adapter.getSelectedChatForActionMode()) {
                            GroupManager.exitGroup(chat.getChatId(), FireManager.getUid(), new GroupManager.OnComplete() {
                                @Override
                                public void onComplete(boolean isSuccessful) {
                                    RealmHelper.getInstance().exitGroup(chat.getChatId());
                                    GroupEvent groupEvent = new GroupEvent(SharedPreferencesManager.getPhoneNumber(), GroupEventTypes.USER_LEFT_GROUP, null);
                                    groupEvent.createGroupEvent(chat.getUser(), null);

                                }
                            });
                        }
                        exitActionMode();
                    }
                })
                .show();

    }

    private void createGroupClicked() {
        startActivity(new Intent(this, NewGroupActivity.class));
    }

    private void searchItemClicked() {
        if (isInActionMode)
            exitActionMode();

        isInSearchMode = true;


    }

    private void updateMutedIcon(MenuItem menuItem, boolean isMuted) {
        if (menuItem != null)
            menuItem.setIcon(isMuted ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
    }

    private void settingsItemClicked() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void muteItemClicked() {

        for (Chat chat : adapter.getSelectedChatForActionMode()) {
            if (chat.isMuted()) {
                RealmHelper.getInstance().setMuted(chat.getChatId(), false);
            } else {
                RealmHelper.getInstance().setMuted(chat.getChatId(), true);
            }
        }

        exitActionMode();
    }

    private void deleteItemClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.confirmation)
                .setMessage(R.string.delete_conversation_confirmation)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        for (Chat chat : adapter.getSelectedChatForActionMode()) {
                            RealmHelper.getInstance().deleteChat(chat.getChatId());
                        }
                        exitActionMode();
                    }
                })
                .show();

    }


    public void addItemToActionMode(int itemsCount) {

        tvSelectedChatCount.setText(itemsCount + "");


        if (itemsCount > 1) {
            if (isHasMutedItem())
                setMenuItemVisibility(false);
                //if there is no muted item then the user may select multiple chats and mute them all in once
            else
                updateMutedIcon(toolbar.getMenu().findItem(R.id.menu_item_mute), false);


        } else if (itemsCount == 1) {
            boolean isMuted = adapter.getSelectedChatForActionMode().get(0).isMuted();
            //in case if it's hidden before
            setMenuItemVisibility(true);
            updateMutedIcon(toolbar.getMenu().findItem(R.id.menu_item_mute), isMuted);
        }

        updateGroupItems();
    }


    private void setMenuItemVisibility(boolean b) {
        if (toolbar.getMenu() != null && toolbar.getMenu().findItem(R.id.menu_item_mute) != null)
            toolbar.getMenu().findItem(R.id.menu_item_mute).setVisible(b);
    }

    private boolean isHasMutedItem() {
        for (Chat chat : adapter.getSelectedChatForActionMode()) {
            if (chat.isMuted())
                return true;
        }
        return false;
    }

    private boolean isHasGroupItem() {
        for (Chat chat : adapter.getSelectedChatForActionMode()) {
            User user = chat.getUser();
            if (user.isGroupBool() && user.getGroup().isActive())
                return true;
        }
        return false;
    }

    private boolean areAllOfChatsGroups() {

        boolean b = false;

        for (Chat chat : adapter.getSelectedChatForActionMode()) {
            User user = chat.getUser();
            if (user.isGroupBool() && user.getGroup().isActive())
                b = true;
            else {
                return false;
            }
        }

        return b;

    }


    public void onActionModeStarted() {
        if (isInSearchMode)
            exitSearchMode();

        if (!isInActionMode) {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.menu_action_chat_list);
        }

        updateMutedIcon(toolbar.getMenu().findItem(R.id.menu_item_mute), adapter.getSelectedChatForActionMode().get(0).isMuted());

        updateGroupItems();

        isInActionMode = true;

        tvSelectedChatCount.setVisibility(View.VISIBLE);

    }

    private void updateGroupItems() {
        MenuItem deleteItem = toolbar.getMenu().findItem(R.id.menu_item_delete);
        if (deleteItem != null) {
            if (isHasGroupItem()) {
                toolbar.getMenu().findItem(R.id.menu_item_delete).setVisible(false);
                if (areAllOfChatsGroups())
                    toolbar.getMenu().findItem(R.id.exit_group_item).setVisible(true);
                else
                    toolbar.getMenu().findItem(R.id.exit_group_item).setVisible(false);
            } else {
                toolbar.getMenu().findItem(R.id.menu_item_delete).setVisible(true);

            }
        }
    }

    public void exitActionMode() {
        adapter.exitActionMode();
        isInActionMode = false;
        tvSelectedChatCount.setVisibility(View.GONE);
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.menu_chat_list);
        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        if (isInActionMode)
            exitActionMode();
        else if (isInSearchMode)
            exitSearchMode();

        else
            super.onBackPressed();
    }

    //start user profile (Dialog-Like Activity)
    public void userProfileClicked(User user) {
        Intent intent = new Intent(this, ProfilePhotoDialog.class);
        intent.putExtra(IntentUtils.UID, user.getUid());
        startActivity(intent);
    }

    public void exitSearchMode() {
        isInSearchMode = false;

    }

    @Override
    public void onAllNotTyping(String groupId) {


        Chat tempChat = new Chat();
        tempChat.setChatId(groupId);
        int i = chatList.indexOf(tempChat);

        if (i == -1) return;
        Chat chat = chatList.get(i);

        ChatsAdapter.ChatsHolder vh = (ChatsAdapter.ChatsHolder) rvChats.findViewHolderForAdapterPosition(i);

        if (vh == null) {
            return;
        }


        TextView typingTv = vh.tvTypingStat;
        TextView lastMessageTv = vh.tvLastMessage;
        ImageView lastMessageReadIcon = vh.imgReadTagChats;

        adapter.getTypingStatHashmap().remove(chat.getChatId());
        typingTv.setVisibility(View.GONE);
        lastMessageTv.setVisibility(View.VISIBLE);
        Message lastMessage = chatList.get(i).getLastMessage();
        if (lastMessage != null &&
                lastMessage.getType() != MessageType.GROUP_EVENT
                && !MessageType.isDeletedMessage(lastMessage.getType())
                && lastMessage.getFromId().equals(FireManager.getUid())) {
            lastMessageReadIcon.setVisibility(View.VISIBLE);
        }


    }

    @Override
    public void onTyping(int state, String groupId, User user) {
        Chat tempChat = new Chat();
        tempChat.setChatId(groupId);
        int i = chatList.indexOf(tempChat);

        if (i == -1) return;
if (user == null)return;
        Chat chat = chatList.get(i);


        ChatsAdapter.ChatsHolder vh = (ChatsAdapter.ChatsHolder) rvChats.findViewHolderForAdapterPosition(i);

        if (vh == null) {
            return;
        }


        adapter.getTypingStatHashmap().put(chat.getChatId(), state);
        TextView typingTv = vh.tvTypingStat;
        TextView lastMessageTv = vh.tvLastMessage;
        ImageView lastMessageReadIcon = vh.imgReadTagChats;


        //if other user is typing or recording to this user
        //then hide last message textView with all its contents
        if (state == TypingStat.TYPING || state == TypingStat.RECORDING) {
            lastMessageTv.setVisibility(View.GONE);
            lastMessageReadIcon.setVisibility(View.GONE);
            typingTv.setVisibility(View.VISIBLE);
            typingTv.setText(user.getUserName() + " is " + TypingStat.getStatString(MainActivity.this, state));
        }
    }
}




