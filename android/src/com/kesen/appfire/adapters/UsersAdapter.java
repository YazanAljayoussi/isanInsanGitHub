package com.kesen.appfire.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mygdx.game.R;
import com.kesen.appfire.activities.ChatActivity;
import com.kesen.appfire.activities.NewChatActivity;
import com.kesen.appfire.activities.ProfilePhotoDialog;
import com.kesen.appfire.model.realms.User;
import com.kesen.appfire.utils.BitmapUtils;
import com.kesen.appfire.utils.IntentUtils;

import java.util.List;

import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by Devlomi on 03/08/2017.
 */

//show the groupUsers from phonebook who have installed this app
public class UsersAdapter extends RealmRecyclerViewAdapter<User, RecyclerView.ViewHolder> {
    Context context;
    List<User> userList;

    public UsersAdapter(@Nullable OrderedRealmCollection<User> data, boolean autoUpdate, Context context) {
        super(data, autoUpdate);
        this.userList = data;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_user, parent, false);
        return new UserHolder(row);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final User user = userList.get(position);
        UserHolder mHolder = (UserHolder) holder;
        mHolder.tvName.setText(user.getUserName());
        mHolder.tvStatus.setText(user.getStatus());

        //if a user is not exists in phonebook
        //then hide this user
        //we want to show only the groupUsers from phoenbook who have installed this app
//        if (ContactUtils.contactExists(context, user.getPhone())) {
//            mHolder.rlltBody.setVisibility(View.VISIBLE);
//            mHolder.rlltBody.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//        } else {
//            mHolder.rlltBody.setVisibility(View.GONE);
////            prevent show blank space
//            mHolder.rlltBody.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
//        }


        mHolder.rlltBody.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(IntentUtils.UID, user.getUid());
                context.startActivity(intent);
                NewChatActivity activity = (NewChatActivity) context;
                activity.finish();
            }
        });


        mHolder.userPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ProfilePhotoDialog.class);
                intent.putExtra(IntentUtils.UID, user.getUid());
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
            }
        });

        loadUserPhoto(user, mHolder.userPhoto);

    }

    private void loadUserPhoto(final User user, final ImageView imageView) {
        if (user == null) return;
        if (user.getUid() == null) return;

        if (user.getThumbImg() != null) {
            byte[] bytes = BitmapUtils.encodeImageAsBytes(user.getThumbImg());
            Glide.with(context).load(bytes).asBitmap().into(imageView);
        }


    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class UserHolder extends RecyclerView.ViewHolder {
        private RelativeLayout rlltBody;
        private ImageView userPhoto;
        private TextView tvName, tvStatus;


        public UserHolder(View itemView) {
            super(itemView);
            rlltBody = (RelativeLayout) itemView.findViewById(R.id.rllt_body);
            userPhoto = (ImageView) itemView.findViewById(R.id.user_photo);
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
            tvStatus = (TextView) itemView.findViewById(R.id.tv_status);
        }
    }


}
