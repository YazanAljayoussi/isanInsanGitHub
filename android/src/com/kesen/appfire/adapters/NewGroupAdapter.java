package com.kesen.appfire.adapters;

import android.content.Context;
import android.support.annotation.Nullable;

import com.kesen.appfire.model.realms.User;

import java.util.List;

import io.realm.OrderedRealmCollection;

public class NewGroupAdapter extends ForwardAdapter {


    public NewGroupAdapter(@Nullable OrderedRealmCollection<User> data, List<User> selectedForwardedUsers, List<User> currentGroupUsers, boolean autoUpdate,
                           Context context, OnUserClick onUserClick) {
        super(data, selectedForwardedUsers, currentGroupUsers, autoUpdate, context, onUserClick);
    }
}
