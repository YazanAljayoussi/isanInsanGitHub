package com.kesen.appfire.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mygdx.game.R;
import com.kesen.appfire.activities.ChatActivity;
import com.kesen.appfire.activities.ContactDetailsActivity;
import com.kesen.appfire.model.AudioRecyclerState;
import com.kesen.appfire.model.constants.DownloadUploadStat;
import com.kesen.appfire.model.constants.MessageType;
import com.kesen.appfire.model.realms.Message;
import com.kesen.appfire.model.realms.User;
import com.kesen.appfire.utils.BitmapUtils;
import com.kesen.appfire.utils.FileUtils;
import com.kesen.appfire.utils.FireManager;
import com.kesen.appfire.utils.GroupEvent;
import com.kesen.appfire.utils.IntentUtils;
import com.kesen.appfire.utils.ListUtil;
import com.kesen.appfire.utils.SharedPreferencesManager;
import com.kesen.appfire.utils.TimeHelper;
import com.kesen.appfire.utils.Util;
import com.kesen.appfire.views.ProgressWithCancelView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ak.sh.ay.musicwave.MusicWave;
import ca.barrenechea.widget.recyclerview.decoration.StickyHeaderAdapter;
import de.hdodenhof.circleimageview.CircleImageView;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import io.realm.OrderedRealmCollection;
import io.realm.RealmList;
import io.realm.RealmRecyclerViewAdapter;

import static com.kesen.appfire.utils.AdapterHelper.getMessageStatDrawable;
import static com.kesen.appfire.utils.AdapterHelper.getPlayIcon;
import static com.kesen.appfire.utils.AdapterHelper.getVoiceMessageIcon;
import static com.kesen.appfire.utils.AdapterHelper.isSelectedForActionMode;
import static com.kesen.appfire.utils.AdapterHelper.shouldEnableCopyItem;
import static com.kesen.appfire.utils.AdapterHelper.shouldEnableForwardButton;
import static com.kesen.appfire.utils.AdapterHelper.shouldEnableShareButton;

/**
 * Created by Devlomi on 07/08/2017.
 */

//the RealmRecyclerViewAdapter provides autoUpdate feature
//which will handle changes in list automatically with smooth animations
public class MessagingAdapter extends RealmRecyclerViewAdapter<Message, RecyclerView.ViewHolder> implements StickyHeaderAdapter, View.OnLongClickListener {
    private OrderedRealmCollection<Message> messages;
    private Context context;
    User user;
    String myThumbImg;


    //this saves/change the audio or voice state (progress,isPlaying ,maxProgress and the duration )
    HashMap<String, AudioRecyclerState> audioRecyclerState = new HashMap<>();
    // saves/change network progress if there is a download/upload process to update the UI
    HashMap<String, Integer> progressHashmap = new HashMap<>();

    //timestamps to implement the date header
    HashMap<Integer, Long> timestamps = new HashMap<>();
    List<Message> selectedItemsForActionMode = new ArrayList<>();

    int lastTimestampPos = 0;

    ChatActivity activity;
    private boolean isListContainsMedia = false;
    private boolean isGroup;
    private RealmList<User> groupUsers;


    public List<Message> getSelectedItemsForActionMode() {
        return selectedItemsForActionMode;
    }


    public MessagingAdapter(@Nullable OrderedRealmCollection<Message> data, boolean autoUpdate, Context context, User user) {
        super(data, autoUpdate);
        this.messages = data;
        this.context = context;
        this.user = user;
        isGroup = user.isGroupBool();

        if (isGroup)
            groupUsers = user.getGroup().getUsers();

        myThumbImg = SharedPreferencesManager.getThumbImg();
        getDistinctMessagesTimestamps();
        activity = (ChatActivity) context;
    }


    //date header
    @Override
    public long getHeaderId(int position) {
        if (timestamps.containsKey(position)) {
            return timestamps.get(position);
        }

        return 0;
    }

    //date header
    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_day, parent, false);
        return new HeaderHolder(view);
    }

    //date header
    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewholder, int position) {
        HeaderHolder mHolder = (HeaderHolder) viewholder;

        //if there are no timestamps in this day then hide the header
        //otherwise show it
        long headerId = getHeaderId(position);
        if (headerId == 0)
            mHolder.header.setVisibility(View.GONE);
        else {
            String formatted = TimeHelper.getChatTime(headerId);
            mHolder.header.setText(formatted);
        }

    }


    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return message.getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // check the type of view and return holder
        return getHolderByType(parent, viewType);
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder mHolder, int position) {

        //get itemView type
        int type = getItemViewType(position);
        final Message message = messages.get(position);

        //select the message and start action mode
        mHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                onLongClicked(view, mHolder.getAdapterPosition());
                return true;
            }

        });

        //select or un-select the message and start action mode
        mHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isInActionMode())
                    return;

                if (selectedItemsForActionMode.contains(message))
                    itemRemoved(view, message);

                else
                    itemAdded(view, message);


            }
        });

        //save selected state for scrolling
        if (isSelectedForActionMode(message, selectedItemsForActionMode))
            setBackgroundColor(mHolder.itemView, true);
        else
            setBackgroundColor(mHolder.itemView, false);

        /**
         * check message type and init holder to user it and set data in the right place for every view
         */

        switch (type) {
            case MessageType.SENT_TEXT:
                bindSentText((SentTextHolder) mHolder, message);
                break;

            case MessageType.SENT_IMAGE:
                bindSentImage((SentImageHolder) mHolder, message);
                break;

            case MessageType.SENT_VOICE_MESSAGE:
                bindSentVoice((SentVoiceMessageHolder) mHolder, message);
                break;

            case MessageType.SENT_VIDEO:
                bindSentVideo((SentVideoMessageHolder) mHolder, message);
                break;

            case MessageType.SENT_FILE:
                bindSentFile((SentFileHolder) mHolder, message);
                break;

            case MessageType.SENT_AUDIO:
                bindSentAudio((SentAudioHolder) mHolder, message);
                break;

            case MessageType.SENT_CONTACT:
                bindSentContact((SentContactHolder) mHolder, message);
                break;


            case MessageType.SENT_LOCATION:
                bindSentLocation((SentLocationHolder) mHolder, message);
                break;

            case MessageType.RECEIVED_TEXT:
                ReceivedTextHolder holder = (ReceivedTextHolder) mHolder;
                holder.bind(message);
                break;

            case MessageType.RECEIVED_IMAGE:
                ReceivedImageHolder receivedImageHolder = (ReceivedImageHolder) mHolder;
                receivedImageHolder.bind(message);
                break;

            case MessageType.RECEIVED_VOICE_MESSAGE:
                ReceivedVoiceMessageHolder receivedVoiceMessageHolder = (ReceivedVoiceMessageHolder) mHolder;
                receivedVoiceMessageHolder.bind(message);
                break;

            case MessageType.RECEIVED_VIDEO:
                ReceivedVideoMessageHolder receivedVideoMessageHolder = (ReceivedVideoMessageHolder) mHolder;
                receivedVideoMessageHolder.bind(message);
                break;

            case MessageType.RECEIVED_FILE:
                ReceivedFileHolder receivedFileHolder = (ReceivedFileHolder) mHolder;
                receivedFileHolder.bind(message);
                break;


            case MessageType.RECEIVED_AUDIO:
                ReceivedAudioHolder receivedAudioHolder = (ReceivedAudioHolder) mHolder;
                receivedAudioHolder.bind(message);
                break;

            case MessageType.RECEIVED_CONTACT:
                ReceivedContactHolder receivedContactHolder = (ReceivedContactHolder) mHolder;
                receivedContactHolder.bind(message);
                break;

            case MessageType.RECEIVED_LOCATION:
                ReceivedLocationHolder receivedLocationHolder = (ReceivedLocationHolder) mHolder;
                receivedLocationHolder.bind(message);
                break;

            case MessageType.SENT_DELETED_MESSAGE:
                SentDeletedMessageHolder sentDeletedMessageHolder = (SentDeletedMessageHolder) mHolder;
                sentDeletedMessageHolder.bind(message);
                break;

            case MessageType.RECEIVED_DELETED_MESSAGE:
                ReceivedDeletedMessageHolder receivedDeletedMessageHolder = (ReceivedDeletedMessageHolder) mHolder;
                receivedDeletedMessageHolder.bind(message);
                break;

            case MessageType.GROUP_EVENT:
                GroupEventHolder groupEventHolder = (GroupEventHolder) mHolder;
                groupEventHolder.bind(message);
                break;

        }

    }

    private void bindSentLocation(final SentLocationHolder mHolder, final Message message) {
        //get latLng to set the map location
        final LatLng latlng = message.getLocation().getLatlng();

        //set place address text
        mHolder.placeAddress.setText(message.getLocation().getAddress());

        //if the location name is only numbers
        //then hide it,otherwise set the location name
        if (!Util.isNumeric(message.getLocation().getName())) {
            mHolder.placeName.setText(message.getLocation().getName());
            mHolder.placeName.setVisibility(View.VISIBLE);
        } else
            mHolder.placeName.setVisibility(View.GONE);


        //set map location in mapView
        mHolder.setMapLocation(latlng);
        //set time
        mHolder.tvTime.setText(message.getTime());
        //imgStat (received or read)
        mHolder.imgStat.setImageResource(getMessageStatDrawable(message.getMessageStat()));


        mHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(mHolder.itemView, message);
                    else itemAdded(mHolder.itemView, message);
                } else {

                    //open this location in maps app (like google maps or uber etc..)
                    Intent openMapIntent = IntentUtils.getOpenMapIntent(message.getLocation());

                    //check if there is a maps app
                    if (openMapIntent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(openMapIntent);
                    }
                }

            }
        });


        keepActionModeItemsSelected(mHolder.itemView, message);


    }





    //same as "bindReceivedContact"
    private void bindSentContact(final SentContactHolder mHolder, final Message message) {

        mHolder.tvContactName.setText(message.getContent());
        mHolder.tvTimeContact.setText(message.getTime());
        keepActionModeItemsSelected(mHolder.itemView, message);
        mHolder.imgStatContact.setImageResource(getMessageStatDrawable(message.getMessageStat()));

        mHolder.relativeContactInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(mHolder.itemView, message);
                    else itemAdded(mHolder.itemView, message);
                } else {
                    Intent intent = new Intent(context, ContactDetailsActivity.class);
                    intent.putExtra(IntentUtils.EXTRA_MESSAGE_ID, message.getMessageId());
                    context.startActivity(intent);
                }
            }
        });

        mHolder.btnMessageContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(mHolder.itemView, message);
                    else itemAdded(mHolder.itemView, message);
                } else {
                    activity.onContactBtnMessageClick(message.getContact());
                }
            }
        });


        mHolder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClicked(mHolder.itemView, mHolder.getAdapterPosition());
                return true;
            }
        });

        mHolder.relativeContactInfo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClicked(mHolder.itemView, mHolder.getAdapterPosition());
                return true;
            }
        });

        mHolder.btnMessageContact.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClicked(mHolder.itemView, mHolder.getAdapterPosition());
                return true;
            }
        });


    }



    private void bindSentFile(final SentFileHolder mHolder, final Message message) {
        String fileExtension = Util.getFileExtensionFromPath(message.getMetadata()).toUpperCase();
        mHolder.tvFileExtension.setText(fileExtension);
        mHolder.tvFileName.setText(message.getMetadata());
        mHolder.tvTime.setText(message.getTime());
        mHolder.tvFileSize.setText(message.getFileSize());
        mHolder.msgStatFile.setImageResource(getMessageStatDrawable(message.getMessageStat()));

        hideOrShowDownloadLayout(mHolder.progressBarCancel, mHolder.btnRetry, mHolder.fileIcon, message.getDownloadUploadStat());
        keepActionModeItemsSelected(mHolder.itemView, message);

        if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
            int progress = progressHashmap.get(message.getMessageId());
            mHolder.progressBarCancel.setProgress(progress);
        }

        mHolder.progressBarCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.cancelDownloadOrUpload(message);
            }
        });

        mHolder.fileRootContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(mHolder.itemView, message);
                    else itemAdded(mHolder.itemView, message);
                } else
                    activity.onFileClick(message);
            }
        });
        mHolder.btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.upload(message);
            }
        });


    }


    private void bindSentVideo(SentVideoMessageHolder mHolder, final Message message) {
        final SentVideoMessageHolder holder = mHolder;


        holder.imgStat.setImageResource(getMessageStatDrawable(message.getMessageStat()));
        holder.tvTime.setText(message.getTime());

        if (message.getDownloadUploadStat() != DownloadUploadStat.SUCCESS) {
            holder.tvMediaDuration.setVisibility(View.GONE);
        } else {
            holder.tvMediaDuration.setVisibility(View.VISIBLE);
            holder.tvMediaDuration.setText(message.getMediaDuration());
        }


        byte[] videoThumb = BitmapUtils.encodeImageAsBytes(message.getVideoThumb());
        Glide.with(context).load(videoThumb).asBitmap().into(holder.thumbImg);


        hideOrShowDownloadLayout(holder.progressBarCancel, holder.btnRetry, message.getDownloadUploadStat());
        keepActionModeItemsSelected(holder.itemView, message);

        if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
            int progress = progressHashmap.get(message.getMessageId());

            holder.progressBarCancel.setVisibility(View.VISIBLE);
            holder.progressBarCancel.setProgress(progress);
        }

        holder.btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.upload(message);
            }
        });


        holder.progressBarCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(holder.itemView, message);
                    else itemAdded(holder.itemView, message);

                }


                activity.cancelDownloadOrUpload(message);


            }
        });

        holder.thumbImg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClicked(holder.itemView, holder.getAdapterPosition());
                return true;
            }
        });

        holder.btnPlayVideo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClicked(holder.itemView, holder.getAdapterPosition());
                return true;
            }
        });

        holder.thumbImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(holder.itemView, message);
                    else itemAdded(holder.itemView, message);

                } else
                    startImageVideoActivity(message.getLocalPath(), user, message.getMessageId(), holder.thumbImg, holder.getAdapterPosition());

            }
        });


        holder.btnPlayVideo.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View view) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(holder.itemView, message);
                    else itemAdded(holder.itemView, message);

                } else
                    startImageVideoActivity(message.getLocalPath(), user, message.getMessageId(), holder.thumbImg, holder.getAdapterPosition());

            }
        });
    }



    private void bindSentAudio(final SentAudioHolder holder, final Message message) {
        holder.tvTime.setText(message.getTime());


        holder.messageStatImg.setImageResource(getMessageStatDrawable(message.getMessageStat()));


        //Set Initial Values
        holder.seekBar.setProgress(0);
        holder.playBtn.setImageResource(getPlayIcon(false));

        hideOrShowDownloadLayout(holder.progressVoiceMessage, holder.btnRetry, holder.playBtn, message.getDownloadUploadStat());
        keepActionModeItemsSelected(holder.itemView, message);

        if (message.getDownloadUploadStat() != DownloadUploadStat.SUCCESS) {
            holder.tvAudioSize.setVisibility(View.VISIBLE);
            holder.tvAudioSize.setText(message.getMetadata());
        } else {
            holder.tvAudioSize.setVisibility(View.GONE);
        }

        if (audioRecyclerState.containsKey(message.getMessageId())) {
            AudioRecyclerState audioRecyclerState = this.audioRecyclerState.get(message.getMessageId());
            if (audioRecyclerState.getCurrentDuration() != null)
                holder.tvDuration.setText(audioRecyclerState.getCurrentDuration());

            if (audioRecyclerState.getProgress() != -1)
                holder.seekBar.setProgress(audioRecyclerState.getProgress());

            if (audioRecyclerState.getMax() != -1) {
                int max = audioRecyclerState.getMax();
                holder.seekBar.setMax(max);
            }


            holder.playBtn.setImageResource(getPlayIcon(audioRecyclerState.isPlaying()));
        } else
            holder.tvDuration.setText(message.getMediaDuration());

        if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
            int progress = progressHashmap.get(message.getMessageId());
            holder.progressVoiceMessage.setProgress(progress);
        }


        holder.playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(holder.itemView, message);
                    else itemAdded(holder.itemView, message);
                } else {


                    int progress = getPreviousProgressIfAvailable(message.getMessageId());
                    activity.playAudio(message.getMessageId(), message.getLocalPath(), holder.getAdapterPosition(), progress);


                }
            }
        });


        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    seek(seekBar, progress, message, holder.tvDuration);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        holder.progressVoiceMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.cancelDownloadOrUpload(message);
            }
        });

        holder.btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.upload(message);
            }
        });
    }



    private void bindSentVoice(SentVoiceMessageHolder mHolder, final Message message) {
        final SentVoiceMessageHolder holder = mHolder;


        holder.tvTime.setText(message.getTime());

        if (myThumbImg != null)
            Glide.with(context).load(BitmapUtils.encodeImageAsBytes(myThumbImg)).asBitmap().into(holder.circleImg);

        holder.messageStatImg.setImageResource(getMessageStatDrawable(message.getMessageStat()));
        holder.tvDuration.setText(message.getMediaDuration());

        //Set Initial Values
        holder.seekBar.setProgress(0);
        holder.playBtn.setImageResource(getPlayIcon(false));


        hideOrShowDownloadLayout(holder.progressVoiceMessage, holder.btnRetry, holder.playBtn, message.getDownloadUploadStat());
        holder.voiceMessageStat.setImageResource(getVoiceMessageIcon(message.isVoiceMessageSeen()));
        keepActionModeItemsSelected(holder.itemView, message);


        if (audioRecyclerState.containsKey(message.getMessageId())) {
            AudioRecyclerState audioRecyclerState = this.audioRecyclerState.get(message.getMessageId());

            if (audioRecyclerState.getCurrentDuration() != null)
                holder.tvDuration.setText(audioRecyclerState.getCurrentDuration());
            if (audioRecyclerState.getProgress() != -1) {
                holder.seekBar.setProgress(audioRecyclerState.getProgress());
            }
            if (audioRecyclerState.getMax() != -1) {
                int max = audioRecyclerState.getMax();
                holder.seekBar.setMax(max);
            }

            holder.playBtn.setImageResource(getPlayIcon(audioRecyclerState.isPlaying()));


        }

        if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
            int progress = progressHashmap.get(message.getMessageId());
            holder.progressVoiceMessage.setProgress(progress);
        }

        holder.playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(holder.itemView, message);
                    else itemAdded(holder.itemView, message);
                } else {
                    int progress = getPreviousProgressIfAvailable(message.getMessageId());
                    activity.playAudio(message.getMessageId(), message.getLocalPath(), holder.getAdapterPosition(), progress);
                }
            }
        });

        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seek(seekBar, progress, message, holder.tvDuration);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        holder.progressVoiceMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.cancelDownloadOrUpload(message);
            }
        });

        holder.btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.upload(message);
            }
        });
    }



    private void bindSentImage(final SentImageHolder holder, final Message message) {
        holder.tvTime.setText(message.getTime());
        holder.tvFileSizeImgDownload.setText(message.getMetadata());
        // if image deleted then show the blurred thumbnail
        if (!FileUtils.isFileExists(message.getLocalPath())) {
            byte[] thumb = BitmapUtils.encodeImageAsBytes(message.getThumb());
            try {
                Glide.with(context).load(thumb).asBitmap().into(holder.imgMsg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            try {
                Glide.with(context).load(message.getLocalPath()).into(holder.imgMsg);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ViewCompat.setTransitionName(holder.imgMsg, message.getMessageId());


        }

        holder.imgStat.setImageResource(getMessageStatDrawable(message.getMessageStat()));

        holder.imgMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(holder.itemView, message);
                    else itemAdded(holder.itemView, message);
                } else {

                    startImageVideoActivity(message.getLocalPath(), user, message.getMessageId(), holder.imgMsg, holder.getAdapterPosition());
                }
            }
        });

        hideOrShowDownloadLayout(holder.progressWithCancelView, holder.linearLayoutImgDownload, message.getDownloadUploadStat());
        keepActionModeItemsSelected(holder.itemView, message);


        if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
            int progress = progressHashmap.get(message.getMessageId());

            holder.progressWithCancelView.setVisibility(View.VISIBLE);
            holder.progressWithCancelView.setProgress(progress);
        }


        holder.progressWithCancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(holder.itemView, message);
                    else itemAdded(holder.itemView, message);
                } else {
                    activity.cancelDownloadOrUpload(message);
                }


            }
        });

        holder.imgMsg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClicked(holder.itemView, holder.getAdapterPosition());
                return true;
            }
        });

        holder.linearLayoutImgDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInActionMode()) {
                    if (selectedItemsForActionMode.contains(message))
                        itemRemoved(holder.itemView, message);
                    else itemAdded(holder.itemView, message);
                } else {
                    activity.upload(message);
                }
            }
        });


    }


    private void bindSentText(SentTextHolder mHolder, Message message) {
        SentTextHolder holder = mHolder;
        holder.tvTime.setText(message.getTime());
        holder.tvMessageContent.setText(message.getContent());
        holder.messageStatImg.setImageResource(getMessageStatDrawable(message.getMessageStat()));
        keepActionModeItemsSelected(mHolder.itemView, message);
    }

    private void seek(SeekBar seekBar, int progress, Message message, TextView tvDuration) {
        String duration = Util.milliSecondsToTimer(progress);

        //if user changed the seekbar while it's not playing
        if (seekBar.getMax() == 100) {
            int max = (int) Util.getAudioFileMillis(context, message.getLocalPath());
            if (max == 0) return;//if file not found or missing permissions
            seekBar.setMax(max);
            int realProgress = (max / 100) * progress;
            AudioRecyclerState audioRecyclerState = this.audioRecyclerState.get(message.getMessageId());
            if (audioRecyclerState == null) {
                this.audioRecyclerState.put(message.getMessageId(), new AudioRecyclerState(false, duration, realProgress));
            } else {
                audioRecyclerState.setProgress(realProgress);
            }
        }
        tvDuration.setText(duration);

        activity.seekTo(message.getMessageId(), progress);
    }


    private int getPreviousProgressIfAvailable(String messageId) {
        int progress = -1;
        if (audioRecyclerState.containsKey(messageId))
            progress = audioRecyclerState.get(messageId).getProgress();
        return progress;
    }


    //start action mode and select this message
    private void onLongClicked(View view, int pos) {
        if (messages.get(pos).getType() == MessageType.GROUP_EVENT)
            return;

        if (!isInActionMode()) {
            itemAdded(view, messages.get(pos));
        }

        //handleNewMessage activity
        activity.onActionModeStarted();
        updateToolbarButtons();
    }

    private void itemAdded(View view, Message message) {
        /* menu items visibility conditions
        Share Button:
                * only one item
                 * only media items
                 * only downloaded
        Forward Button:
                * Only downloaded items
                * any type is allowed

        Copy Button:
                * only text is allowed
         */

        if (message.getType() == MessageType.GROUP_EVENT)
            return;

        if (message.isMediaType()) {
            if (message.getFromId().equals(FireManager.getUid())) {
                isListContainsMedia = true;
            } else {
                if (message.getDownloadUploadStat() == DownloadUploadStat.SUCCESS)
                    isListContainsMedia = true;
            }
        }


        selectedItemsForActionMode.add(message);
        setBackgroundColor(view, true);
        activity.updateActionModeItemsCount(selectedItemsForActionMode.size());

        updateToolbarButtons();

    }

    //hide or show toolbar button in activity depending on conditions
    private void updateToolbarButtons() {
        if (shouldEnableCopyItem(selectedItemsForActionMode))
            activity.showCopyItem();
        else
            activity.hideCopyItem();

        if (shouldEnableForwardButton(selectedItemsForActionMode))
            activity.showForwardItem();
        else
            activity.hideForwardItem();

        if (shouldEnableShareButton(selectedItemsForActionMode))
            activity.showShareItem();
        else
            activity.hideShareItem();
    }


    private void itemRemoved(View view, Message message) {
        if (message.isMediaType())
            isListContainsMedia = false;

        setBackgroundColor(view, false);
        selectedItemsForActionMode.remove(message);
        activity.updateActionModeItemsCount(selectedItemsForActionMode.size());


        if (selectedItemsForActionMode.isEmpty()) {
            activity.exitActionMode();
        } else
            updateToolbarButtons();


    }

    public void exitActionMode() {
        selectedItemsForActionMode.clear();
        notifyDataSetChanged();
    }

    //set background color of item if it's selected
    private void setBackgroundColor(View view, boolean isAdded) {
        int addedColor = context.getResources().getColor(R.color.item_selected_background_color);
        int notAddedColor = 0x00000000;
        if (isAdded)
            view.setBackgroundColor(addedColor);
        else
            view.setBackgroundColor(notAddedColor);
    }

    // hide or show some views depending on download/upload state
    private void hideOrShowDownloadLayout(FrameLayout progressLayout, View btnRetry, int stat) {

        switch (stat) {
            case DownloadUploadStat.FAILED:
            case DownloadUploadStat.CANCELLED:
                progressLayout.setVisibility(View.GONE);
                btnRetry.setVisibility(View.VISIBLE);
                break;

            case DownloadUploadStat.LOADING:
                progressLayout.setVisibility(View.VISIBLE);
                btnRetry.setVisibility(View.GONE);
                break;

            case DownloadUploadStat.SUCCESS:
                progressLayout.setVisibility(View.GONE);
                btnRetry.setVisibility(View.GONE);
                break;
        }
    }


    private void hideOrShowDownloadLayout(FrameLayout progressLayout, View btnRetry, View btnPlay, int stat) {

        switch (stat) {
            case DownloadUploadStat.FAILED:
            case DownloadUploadStat.CANCELLED:
                progressLayout.setVisibility(View.GONE);
                btnPlay.setVisibility(View.INVISIBLE);
                btnRetry.setVisibility(View.VISIBLE);
                break;

            case DownloadUploadStat.LOADING:
                progressLayout.setVisibility(View.VISIBLE);
                btnPlay.setVisibility(View.INVISIBLE);
                btnRetry.setVisibility(View.GONE);
                break;

            case DownloadUploadStat.SUCCESS:
                progressLayout.setVisibility(View.GONE);
                btnRetry.setVisibility(View.GONE);
                btnPlay.setVisibility(View.VISIBLE);

                break;
        }
    }

    private void hideOrShowDownloadLayout(View downloadLayout, FrameLayout progressLayout, ImageButton playBtn, int stat) {

        switch (stat) {
            case DownloadUploadStat.FAILED:
            case DownloadUploadStat.CANCELLED:
                downloadLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);
                playBtn.setVisibility(View.GONE);
                break;

            case DownloadUploadStat.LOADING:
                progressLayout.setVisibility(View.VISIBLE);
//                uploadLayout.setVisibility(View.GONE);
                downloadLayout.setVisibility(View.GONE);
                playBtn.setVisibility(View.GONE);
                break;

            case DownloadUploadStat.SUCCESS:
                progressLayout.setVisibility(View.GONE);
                downloadLayout.setVisibility(View.GONE);
                playBtn.setVisibility(View.VISIBLE);
//                uploadLayout.setVisibility(View.GONE);

                break;
        }
    }


    //keep item background as selected when scroll
    private void keepActionModeItemsSelected(View itemView, Message message) {
        if (selectedItemsForActionMode.contains(message)) {
            setBackgroundColor(itemView, true);
        } else {
            setBackgroundColor(itemView, false);
        }
    }

    //start FullScreenActivity with transitions
    private void startImageVideoActivity(String path, User user, String selectedMessageId, View imgView, int pos) {
        onItemClick.onClick(path, user, selectedMessageId, imgView, pos);
    }

    //delete items from database
    //if boolean 'deleteFile' is true then delete the file from device
    public void clearItems() {
        selectedItemsForActionMode.clear();
        isListContainsMedia = false;
    }


    @Override
    public boolean onLongClick(View view) {

        if (!isInActionMode()) {


        }
        activity.onActionModeStarted();

        return true;
    }


    static class HeaderHolder extends RecyclerView.ViewHolder {
        public TextView header;

        public HeaderHolder(View itemView) {
            super(itemView);

            header = (TextView) itemView.findViewById(R.id.tv_day);
        }
    }


    class SentMessageHolder extends RecyclerView.ViewHolder {

        TextView tvTime;
        ImageView messageStatImg;


        public SentMessageHolder(View itemView) {
            super(itemView);
            tvTime = (TextView) itemView.findViewById(R.id.tv_time);
            messageStatImg = (ImageView) itemView.findViewById(R.id.message_stat_img);
        }
    }

    // sent message with type text
    public class SentTextHolder extends SentMessageHolder {

        EmojiconTextView tvMessageContent;


        public SentTextHolder(View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);

        }
    }

    // sent message with type image
    public class SentImageHolder extends SentMessageHolder {

        ImageView imgMsg, imgStat;

        LinearLayout linearLayoutImgDownload;
        TextView tvFileSizeImgDownload;
        ProgressWithCancelView progressWithCancelView;


        public SentImageHolder(View itemView) {
            super(itemView);
            imgMsg = itemView.findViewById(R.id.img_msg);
            progressWithCancelView = itemView.findViewById(R.id.progress_bar_cancel);
            linearLayoutImgDownload = itemView.findViewById(R.id.linear_layout_img_download);
            tvFileSizeImgDownload = itemView.findViewById(R.id.tv_file_size_img_download);
            imgStat = itemView.findViewById(R.id.img_stat);


        }
    }

    // received message holders
    class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView userName;


        public ReceivedMessageHolder(View itemView) {
            super(itemView);

            tvTime = (TextView) itemView.findViewById(R.id.tv_time);
            if (isGroup) {
                userName = itemView.findViewById(R.id.tv_username_group);
            }
        }

        public void bind(Message message) {
            tvTime.setText(message.getTime());
            if (isGroup && userName != null) {
                userName.setVisibility(View.VISIBLE);
                String fromId = message.getFromId();
                User userById = ListUtil.getUserById(fromId, user.getGroup().getUsers());
                if (userById != null) {
                    String name = userById.getUserName();
                    if (name != null)
                        userName.setText(name);
                } else {
                    userName.setText(message.getFromPhone());
                }

            }
        }
    }

    // received message with type text
    public class ReceivedTextHolder extends ReceivedMessageHolder {
        EmojiconTextView tvMessageContent;

        public ReceivedTextHolder(View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tv_message_content);
        }

        @Override
        public void bind(Message message) {
            super.bind(message);
            tvMessageContent.setText(message.getContent());
            keepActionModeItemsSelected(itemView, message);
        }
    }

// received message with type image

    public class ReceivedImageHolder extends ReceivedMessageHolder {
        ImageView imgMsg;
        LinearLayout linearLayoutImgDownload;
        TextView tvFileSizeImgDownload;
        private ProgressWithCancelView progressBarCancel;


        public ReceivedImageHolder(View itemView) {
            super(itemView);
            imgMsg = itemView.findViewById(R.id.img_msg);
            linearLayoutImgDownload = itemView.findViewById(R.id.linear_layout_img_download);
            tvFileSizeImgDownload = itemView.findViewById(R.id.tv_file_size_img_download);
            progressBarCancel = itemView.findViewById(R.id.progress_bar_cancel);
        }

        @Override
        public void bind(final Message message) {
            super.bind(message);
            tvFileSizeImgDownload.setText(message.getMetadata());


            //if the image is not downloaded show thumb img
            if (message.getLocalPath() == null) {
                byte[] thumb = BitmapUtils.encodeImageAsBytes(message.getThumb());
                try {
                    Glide.with(context).load(thumb).asBitmap().into(imgMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (!new File(message.getLocalPath()).exists()) {
                // if image deleted from device then show the blurred thumbnail
                byte[] thumb = BitmapUtils.encodeImageAsBytes(message.getThumb());
//            imgMsg.setImageBitmap(thumb);
                try {
                    Glide.with(context).load(thumb).asBitmap().into(imgMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                //these try catch exceptions because glide does not support set tag to an image view
                try {
                    Glide.with(context).load(Uri.fromFile(new File(message.getLocalPath()))).into(imgMsg);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }

                ViewCompat.setTransitionName(imgMsg, message.getMessageId());


            }


            imgMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else
                            itemAdded(itemView, message);

                    } else {
                        if (message.getDownloadUploadStat() == DownloadUploadStat.SUCCESS)
                            startImageVideoActivity(message.getLocalPath(), user, message.getMessageId(), imgMsg, getAdapterPosition());
                    }
                }
            });


            imgMsg.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClicked(itemView, getAdapterPosition());
                    return true;
                }
            });
            linearLayoutImgDownload.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClicked(itemView, getAdapterPosition());
                    return true;
                }
            });


            hideOrShowDownloadLayout(progressBarCancel, linearLayoutImgDownload, message.getDownloadUploadStat());
            keepActionModeItemsSelected(itemView, message);


            if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
                int progress = progressHashmap.get(message.getMessageId());
                progressBarCancel.setVisibility(View.VISIBLE);
                progressBarCancel.setProgress(progress);
            }


            progressBarCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);

                    } else {
                        activity.cancelDownloadOrUpload(message);
                    }


                }
            });

            linearLayoutImgDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);

                    } else
                        activity.download(message);
                }
            });
        }
    }


    public class ReceivedVoiceMessageHolder extends ReceivedMessageHolder {
        public ImageView playBtn;
        public SeekBar seekBar;
        private CircleImageView circleImg;
        public TextView tvTime, tvDuration;
        private ProgressWithCancelView progressBarCancel;
        private ImageButton btnRetry;
        private ImageView voiceMessageStat;


        public ReceivedVoiceMessageHolder(View itemView) {
            super(itemView);
            playBtn = (ImageView) itemView.findViewById(R.id.voice_play_btn);
            seekBar = (SeekBar) itemView.findViewById(R.id.voice_seekbar);
            circleImg = (CircleImageView) itemView.findViewById(R.id.voice_circle_img);
            tvTime = (TextView) itemView.findViewById(R.id.tv_time);
            tvDuration = (TextView) itemView.findViewById(R.id.tv_duration);
            voiceMessageStat = itemView.findViewById(R.id.voice_message_stat);
            progressBarCancel = itemView.findViewById(R.id.progress_bar_cancel);
            btnRetry = itemView.findViewById(R.id.btn_retry);
        }

        @Override
        public void bind(final Message message) {
            super.bind(message);
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    onLongClicked(view, getAdapterPosition());
                    return true;
                }
            });

            //set initial values
            seekBar.setProgress(0);
            playBtn.setImageResource(getPlayIcon(false));

            loadUserPhoto(message.getFromId(), circleImg);
            tvDuration.setText(message.getMediaDuration());

            hideOrShowDownloadLayout(progressBarCancel, btnRetry, playBtn, message.getDownloadUploadStat());
            voiceMessageStat.setImageResource(getVoiceMessageIcon(message.isVoiceMessageSeen()));

            keepActionModeItemsSelected(itemView, message);


            if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
                int progress = progressHashmap.get(message.getMessageId());
                progressBarCancel.setProgress(progress);
            }

            if (audioRecyclerState.containsKey(message.getMessageId())) {
                AudioRecyclerState mAudioRecyclerState = audioRecyclerState.get(message.getMessageId());

                if (mAudioRecyclerState.getCurrentDuration() != null)
                    tvDuration.setText(mAudioRecyclerState.getCurrentDuration());
                if (mAudioRecyclerState.getProgress() != -1)
                    seekBar.setProgress(mAudioRecyclerState.getProgress());

                if (mAudioRecyclerState.getMax() != -1) {
                    int max = mAudioRecyclerState.getMax();
                    seekBar.setMax(max);
                }

                playBtn.setImageResource(getPlayIcon(mAudioRecyclerState.isPlaying()));


            } else {
                playBtn.setImageResource(getPlayIcon(false));
            }

            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);

                    } else
                        activity.playAudio(message.getMessageId(), message.getLocalPath(), getAdapterPosition(), getPreviousProgressIfAvailable(message.getMessageId()));

                }
            });

            progressBarCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.cancelDownloadOrUpload(message);
                }
            });

            btnRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.download(message);
                }
            });


            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser)
                        seek(seekBar, progress, message, tvDuration);

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }

    public class SentVoiceMessageHolder extends SentMessageHolder {
        public ImageView playBtn;
        public SeekBar seekBar;
        public CircleImageView circleImg;
        public TextView tvTime, tvDuration;
        public ProgressWithCancelView progressVoiceMessage;
        public ImageButton btnRetry;
        private ImageView voiceMessageStat;


        public SentVoiceMessageHolder(View itemView) {
            super(itemView);
            playBtn = (ImageView) itemView.findViewById(R.id.voice_play_btn);
            seekBar = (SeekBar) itemView.findViewById(R.id.voice_seekbar);
            circleImg = (CircleImageView) itemView.findViewById(R.id.voice_circle_img);
            tvTime = (TextView) itemView.findViewById(R.id.tv_time);
            tvDuration = (TextView) itemView.findViewById(R.id.tv_duration);
            progressVoiceMessage = itemView.findViewById(R.id.progress_voice_message);
            voiceMessageStat = itemView.findViewById(R.id.voice_message_stat);
            btnRetry = itemView.findViewById(R.id.btn_retry);

        }

    }

    public class SentVideoMessageHolder extends SentMessageHolder {
        private ImageView thumbImg;
        private ImageButton btnPlayVideo;
        private ProgressWithCancelView progressBarCancel;
        private TextView tvMediaDuration;


        private Button btnRetry;


        private ImageView imgStat;


        public SentVideoMessageHolder(View itemView) {
            super(itemView);
            thumbImg = itemView.findViewById(R.id.thumb_img);
            btnPlayVideo = itemView.findViewById(R.id.btn_play_video);
            progressBarCancel = itemView.findViewById(R.id.progress_bar_cancel);
            btnRetry = itemView.findViewById(R.id.btn_retry);
            imgStat = itemView.findViewById(R.id.img_stat);
            tvMediaDuration = itemView.findViewById(R.id.tv_media_duration);
        }

    }


    public class ReceivedVideoMessageHolder extends ReceivedMessageHolder {
        private ImageView thumbImg;
        private ProgressWithCancelView progressBarCancel;
        private LinearLayout linearLayoutVideoDownload;
        private TextView tvFileSizeVideoDownload;
        private ImageButton btnPlayVideo;
        private LinearLayout container;
        private TextView tvMediaDuration;


        public ReceivedVideoMessageHolder(View itemView) {
            super(itemView);
            thumbImg = itemView.findViewById(R.id.thumb_img);
            linearLayoutVideoDownload = itemView.findViewById(R.id.linear_layout_video_download);
            tvFileSizeVideoDownload = itemView.findViewById(R.id.tv_file_size_video_download);
            btnPlayVideo = itemView.findViewById(R.id.btn_play_video);
            container = itemView.findViewById(R.id.container);
            progressBarCancel = itemView.findViewById(R.id.progress_bar_cancel);
            tvMediaDuration = itemView.findViewById(R.id.tv_media_duration);
        }

        @Override
        public void bind(final Message message) {
            super.bind(message);
            container.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClicked(itemView, getAdapterPosition());
                    return true;
                }
            });


            btnPlayVideo.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClicked(itemView, getAdapterPosition());
                    return true;
                }
            });


            linearLayoutVideoDownload.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClicked(itemView, getAdapterPosition());
                    return true;
                }
            });


            //set duration
            tvMediaDuration.setText(message.getMediaDuration());

            //Video is not downloaded yet
            //show the blurred thumb
            if (message.getLocalPath() == null) {
                byte[] thumb = BitmapUtils.encodeImageAsBytes(message.getThumb());
                Glide.with(context).load(thumb).asBitmap().into(thumbImg);

                //set video size
                tvFileSizeVideoDownload.setText(message.getMetadata());
            } else {

                //if it's downloaded but the user deleted the file from device
                if (!FileUtils.isFileExists(message.getLocalPath())) {
                    //show the blurred image
                    byte[] thumb = BitmapUtils.encodeImageAsBytes(message.getThumb());
                    Glide.with(context).load(thumb).asBitmap().into(thumbImg);
                } else {
                    //if it's downloaded ,show the Video Thumb (Without blur)
                    byte[] thumb = BitmapUtils.encodeImageAsBytes(message.getVideoThumb());
                    Glide.with(context).load(thumb).asBitmap().into(thumbImg);

                }


            }


            hideOrShowDownloadLayout(linearLayoutVideoDownload, progressBarCancel, btnPlayVideo, message.getDownloadUploadStat());
            keepActionModeItemsSelected(itemView, message);


            if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
                int progress = progressHashmap.get(message.getMessageId());
                progressBarCancel.setProgress(progress);
            }


            container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);

                    } else {
                        if (message.getDownloadUploadStat() == DownloadUploadStat.SUCCESS) {
                            startImageVideoActivity(message.getLocalPath(), user, message.getMessageId(), thumbImg, getAdapterPosition());
                        }
                    }
                }
            });
            btnPlayVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);

                    } else if (message.getDownloadUploadStat() == DownloadUploadStat.SUCCESS) {
                        startImageVideoActivity(message.getLocalPath(), user, message.getMessageId(), thumbImg, getAdapterPosition());
                    }
                }
            });


            progressBarCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.cancelDownloadOrUpload(message);
                }
            });


            linearLayoutVideoDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);

                    } else
                        activity.download(message);
                }
            });
        }
    }

    public class TimestampHolder extends RecyclerView.ViewHolder {
        private TextView label;


        public TimestampHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.tv_day);
        }
    }

    public class SentFileHolder extends RecyclerView.ViewHolder {
        private TextView tvFileSize;
        private TextView tvFileName;
        private TextView tvFileExtension;
        private ImageView msgStatFile;
        private TextView tvTime;
        private ProgressWithCancelView progressBarCancel;
        private ImageButton btnRetry;
        private RelativeLayout fileRootContainer;
        private ImageView fileIcon;


        public SentFileHolder(View itemView) {
            super(itemView);
            tvFileSize = itemView.findViewById(R.id.tv_file_size);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileExtension = itemView.findViewById(R.id.tv_file_extension);
            msgStatFile = itemView.findViewById(R.id.msg_stat_file);
            tvTime = itemView.findViewById(R.id.tv_time);
            progressBarCancel = itemView.findViewById(R.id.progress_bar_cancel);
            btnRetry = itemView.findViewById(R.id.btn_retry);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileRootContainer = itemView.findViewById(R.id.file_root_container);

        }
    }

    public class ReceivedFileHolder extends ReceivedMessageHolder {
        private ImageView fileIcon;
        private TextView tvFileName;
        private TextView tvFileExtension;

        private ProgressWithCancelView progressBarCancel;
        private TextView tvFileSize;
        private ImageButton btnRetry;
        private LinearLayout fileRootContainer;


        public ReceivedFileHolder(View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.file_icon);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileExtension = itemView.findViewById(R.id.tv_file_extension);
            tvFileSize = itemView.findViewById(R.id.tv_file_size);
            btnRetry = itemView.findViewById(R.id.btn_retry);
            progressBarCancel = itemView.findViewById(R.id.progress_bar_cancel);
            fileRootContainer = itemView.findViewById(R.id.file_root_container);


        }

        @Override
        public void bind(final Message message) {
            super.bind(message);
            //get file extension
            String fileExtension = Util.getFileExtensionFromPath(message.getMetadata()).toUpperCase();
            tvFileExtension.setText(fileExtension);
            //set file name
            tvFileName.setText(message.getMetadata());

            //file size
            tvFileSize.setText(message.getFileSize());

            hideOrShowDownloadLayout(progressBarCancel, btnRetry, fileIcon, message.getDownloadUploadStat());
            keepActionModeItemsSelected(itemView, message);


            if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
                int progress = progressHashmap.get(message.getMessageId());
                progressBarCancel.setProgress(progress);
            }

            progressBarCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.cancelDownloadOrUpload(message);
                }
            });

            fileRootContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);
                    } else {
                        if (message.getDownloadUploadStat() != DownloadUploadStat.SUCCESS)
                            return;

                        activity.onFileClick(message);
                    }
                }
            });

            btnRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.download(message);
                }
            });
        }
    }

    public class SentAudioHolder extends RecyclerView.ViewHolder {
        public MusicWave waveView;
        private ProgressWithCancelView progressVoiceMessage;
        private ImageButton btnRetry;
        public ImageView playBtn;
        public SeekBar seekBar;
        private TextView tvAudioSize;
        private TextView tvTime;
        private ImageView messageStatImg;
        public TextView tvDuration;
        public ImageView imgHeadset;


        public SentAudioHolder(View itemView) {
            super(itemView);
            waveView = itemView.findViewById(R.id.wave_view);
            progressVoiceMessage = itemView.findViewById(R.id.progress_voice_message);
            btnRetry = itemView.findViewById(R.id.btn_retry);
            playBtn = itemView.findViewById(R.id.voice_play_btn);
            seekBar = itemView.findViewById(R.id.voice_seekbar);
            tvAudioSize = itemView.findViewById(R.id.tv_audio_size);
            tvTime = itemView.findViewById(R.id.tv_time);
            messageStatImg = itemView.findViewById(R.id.message_stat_img);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            imgHeadset = itemView.findViewById(R.id.img_headset);
        }
    }

    public class ReceivedAudioHolder extends ReceivedMessageHolder {
        public MusicWave waveView;
        private ProgressWithCancelView progressVoiceMessage;
        private ImageButton btnRetry;
        public ImageView playBtn;
        public SeekBar seekBar;
        private TextView tvAudioSize;
        private TextView tvTime;
        public TextView tvDuration;
        public ImageView imgHeadset;


        public ReceivedAudioHolder(View itemView) {
            super(itemView);
            waveView = itemView.findViewById(R.id.wave_view);
            progressVoiceMessage = itemView.findViewById(R.id.progress_bar_cancel);
            btnRetry = itemView.findViewById(R.id.btn_retry);
            playBtn = itemView.findViewById(R.id.voice_play_btn);
            seekBar = itemView.findViewById(R.id.voice_seekbar);
            tvAudioSize = itemView.findViewById(R.id.tv_audio_size);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            imgHeadset = itemView.findViewById(R.id.img_headset);
        }

        @Override
        public void bind(final Message message) {
            super.bind(message);


            //Set Initial Values
            seekBar.setProgress(0);
            playBtn.setImageResource(getPlayIcon(false));


            hideOrShowDownloadLayout(progressVoiceMessage, btnRetry, playBtn, message.getDownloadUploadStat());
            keepActionModeItemsSelected(itemView, message);

            //if it's sending set the audio size
            if (message.getDownloadUploadStat() != DownloadUploadStat.SUCCESS) {
                tvAudioSize.setVisibility(View.VISIBLE);
                tvAudioSize.setText(message.getMetadata());
            } else {
                //otherwise hide the audio textview
                tvAudioSize.setVisibility(View.GONE);
            }


            if (audioRecyclerState.containsKey(message.getMessageId())) {
                AudioRecyclerState mAudioRecyclerState = audioRecyclerState.get(message.getMessageId());


                if (mAudioRecyclerState.getCurrentDuration() != null)
                    tvDuration.setText(mAudioRecyclerState.getCurrentDuration());

                if (mAudioRecyclerState.getProgress() != -1) {
                    seekBar.setProgress(mAudioRecyclerState.getProgress());
                }

                if (mAudioRecyclerState.getMax() != -1) {
                    int max = mAudioRecyclerState.getMax();
                    seekBar.setMax(max);
                }


                if (mAudioRecyclerState.isPlaying()) {
                    imgHeadset.setVisibility(View.GONE);
                    waveView.setVisibility(View.VISIBLE);
                } else {
                    imgHeadset.setVisibility(View.VISIBLE);
                    waveView.setVisibility(View.GONE);
                }

                playBtn.setImageResource(getPlayIcon(mAudioRecyclerState.isPlaying()));

            } else {
                tvDuration.setText(message.getMediaDuration());
                imgHeadset.setVisibility(View.VISIBLE);
                waveView.setVisibility(View.GONE);
            }


            //Loading Progress
            if (progressHashmap.containsKey(message.getMessageId()) && message.getDownloadUploadStat() == DownloadUploadStat.LOADING) {
                int progress = progressHashmap.get(message.getMessageId());
                progressVoiceMessage.setProgress(progress);
            }


            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);
                    } else {

                        int progress = getPreviousProgressIfAvailable(message.getMessageId());
                        activity.playAudio(message.getMessageId(), message.getLocalPath(), getAdapterPosition(), progress);


                    }
                }
            });


            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser)
                        seek(seekBar, progress, message, tvDuration);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            //cancel download process
            progressVoiceMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.cancelDownloadOrUpload(message);
                }
            });


            //re-download this
            btnRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.download(message);
                }
            });

        }
    }

    public class SentContactHolder extends RecyclerView.ViewHolder {
        private RelativeLayout relativeContactInfo;
        private TextView tvContactName;
        private ImageView imgStatContact;
        private TextView tvTimeContact;
        private Button btnMessageContact;
        private FrameLayout container;


        public SentContactHolder(View itemView) {
            super(itemView);
            relativeContactInfo = itemView.findViewById(R.id.relative_contact_info);
            tvContactName = itemView.findViewById(R.id.tv_contact_name);
            imgStatContact = itemView.findViewById(R.id.img_stat_contact);
            tvTimeContact = itemView.findViewById(R.id.tv_time_contact);
            btnMessageContact = itemView.findViewById(R.id.btn_message_contact);
            container = itemView.findViewById(R.id.container);

        }
    }


    public class ReceivedContactHolder extends ReceivedMessageHolder {
        private RelativeLayout relativeContactInfo;
        private TextView tvContactName;
        private Button btnMessageContact;
        private Button btnAddContact;


        public ReceivedContactHolder(View itemView) {
            super(itemView);
            relativeContactInfo = itemView.findViewById(R.id.relative_contact_info);
            tvContactName = itemView.findViewById(R.id.tv_contact_name);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnMessageContact = itemView.findViewById(R.id.btn_message_contact);
            btnAddContact = itemView.findViewById(R.id.btn_add_contact);
        }

        @Override
        public void bind(final Message message) {
            super.bind(message);
            //set contact name
            tvContactName.setText(message.getContent());

            keepActionModeItemsSelected(itemView, message);


            //show contact info
            relativeContactInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);
                    } else {
                        Intent intent = new Intent(context, ContactDetailsActivity.class);
                        intent.putExtra(IntentUtils.EXTRA_MESSAGE_ID, message.getMessageId());
                        context.startActivity(intent);
                    }
                }
            });

            //send a message to this contact if installed this app
            btnMessageContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);
                    } else {
                        activity.onContactBtnMessageClick(message.getContact());
                    }
                }
            });

            //add this contact to phonebook
            btnAddContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);
                    } else {
                        Intent addContactIntent = IntentUtils.getAddContactIntent(message.getContact());
                        context.startActivity(addContactIntent);
                    }
                }
            });

            //select this message and start action mode
            relativeContactInfo.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClicked(itemView, getAdapterPosition());
                    return true;
                }
            });

            //select this message and start action mode
            btnAddContact.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClicked(itemView, getAdapterPosition());
                    return true;
                }
            });

            //select this message and start action mode
            btnMessageContact.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongClicked(itemView, getAdapterPosition());
                    return true;
                }
            });


        }
    }


    public class SentLocationHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback {
        private GoogleMap mGoogleMap;
        private LatLng mMapLocation;
        private TextView placeName;
        private TextView placeAddress;
        private MapView mapView;
        private TextView tvTime;
        private ImageView imgStat;


        public SentLocationHolder(View itemView) {
            super(itemView);

            mapView = itemView.findViewById(R.id.map_view);
            placeName = itemView.findViewById(R.id.place_name);
            placeAddress = itemView.findViewById(R.id.place_address);
            tvTime = itemView.findViewById(R.id.tv_time);
            imgStat = itemView.findViewById(R.id.img_stat);

            mapView.onCreate(null);
            mapView.getMapAsync(this);


        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mGoogleMap = googleMap;

            MapsInitializer.initialize(context);
            googleMap.getUiSettings().setMapToolbarEnabled(false);

            // If we have mapView data, update the mapView content.
            if (mMapLocation != null) {
                updateMapContents();
            }
        }

        public void setMapLocation(LatLng location) {
            mMapLocation = location;

            // If the mapView is ready, update its content.
            if (mGoogleMap != null) {
                updateMapContents();
            }
        }

        protected void updateMapContents() {
            // Since the mapView is re-used, need to remove pre-existing mapView features.
            mGoogleMap.clear();

            // Update the mapView feature data and camera position.
            mGoogleMap.addMarker(new MarkerOptions().position(mMapLocation));

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mMapLocation, 17f);
            mGoogleMap.moveCamera(cameraUpdate);
        }
    }

    public class ReceivedLocationHolder extends ReceivedMessageHolder implements OnMapReadyCallback {
        private GoogleMap mGoogleMap;
        private LatLng mMapLocation;
        private TextView placeName;
        private TextView placeAddress;
        private MapView mapView;
        private TextView tvTime;


        public ReceivedLocationHolder(View itemView) {
            super(itemView);

            mapView = itemView.findViewById(R.id.map_view);
            placeName = itemView.findViewById(R.id.place_name);
            placeAddress = itemView.findViewById(R.id.place_address);
            tvTime = itemView.findViewById(R.id.tv_time);


            mapView.onCreate(null);
            mapView.getMapAsync(this);


        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            mGoogleMap = googleMap;

            MapsInitializer.initialize(context);
            googleMap.getUiSettings().setMapToolbarEnabled(false);

            // If we have mapView data, update the mapView content.
            if (mMapLocation != null) {
                updateMapContents();
            }
        }

        public void setMapLocation(LatLng location) {
            mMapLocation = location;

            // If the mapView is ready, update its content.
            if (mGoogleMap != null) {
                updateMapContents();
            }
        }

        @Override
        public void bind(final Message message) {
            super.bind(message);
            final LatLng latlng = message.getLocation().getLatlng();


            placeAddress.setText(message.getLocation().getAddress());

            if (!Util.isNumeric(message.getLocation().getName())) {
                placeName.setText(message.getLocation().getName());
                placeName.setVisibility(View.VISIBLE);
            } else
                placeName.setVisibility(View.GONE);


            setMapLocation(latlng);


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isInActionMode()) {
                        if (selectedItemsForActionMode.contains(message))
                            itemRemoved(itemView, message);
                        else itemAdded(itemView, message);
                    } else {
                        Intent openMapIntent = IntentUtils.getOpenMapIntent(message.getLocation());
                        if (openMapIntent.resolveActivity(context.getPackageManager()) != null) {
                            context.startActivity(openMapIntent);
                        }
                    }

                }
            });

            keepActionModeItemsSelected(itemView, message);

        }


        protected void updateMapContents() {
            // Since the mapView is re-used, need to remove pre-existing mapView features.
            mGoogleMap.clear();

            // Update the mapView feature data and camera position.
            mGoogleMap.addMarker(new MarkerOptions().position(mMapLocation));

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mMapLocation, 17f);
            mGoogleMap.moveCamera(cameraUpdate);
        }
    }

    class SentDeletedMessageHolder extends RecyclerView.ViewHolder {
        private TextView tvTime;


        public SentDeletedMessageHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);

        }

        public void bind(Message message) {
            tvTime.setText(message.getTime());
        }


    }

    class ReceivedDeletedMessageHolder extends ReceivedMessageHolder {

        public ReceivedDeletedMessageHolder(View itemView) {
            super(itemView);
        }


    }

    class GroupEventHolder extends RecyclerView.ViewHolder {
        private TextView tvGroupEvent;


        public GroupEventHolder(View itemView) {
            super(itemView);
            tvGroupEvent = itemView.findViewById(R.id.tv_group_event);
        }

        public void bind(Message message) {
            tvGroupEvent.setText(GroupEvent.extractString(message.getContent(), user.getGroup().getUsers()));
        }
    }

    private void getDistinctMessagesTimestamps() {

        for (int i = 0; i < messages.size(); i++) {
            long timestamp = Long.parseLong(messages.get(i).getTimestamp());
            if (i == 0) {
                timestamps.put(i, timestamp);
                lastTimestampPos = i;
            } else {
                long oldTimestamp = Long.parseLong(messages.get(i - 1).getTimestamp());
                if (!TimeHelper.isSameDay(timestamp, oldTimestamp)) {
                    timestamps.put(i, timestamp);
                    lastTimestampPos = i;
                }
            }
        }
    }


    //update timestamps if needed when a new message inserted
    public void messageInserted() {
        int index = messages.size() - 1;
        long newTimestamp = Long.parseLong(messages.get(index).getTimestamp());
        if (timestamps.isEmpty()) {
            timestamps.put(index, newTimestamp);
            lastTimestampPos = index;
            return;
        }

        long lastTimestamp = timestamps.get(lastTimestampPos);
        if (!TimeHelper.isSameDay(lastTimestamp, newTimestamp)) {
            timestamps.put(index, newTimestamp);
            lastTimestampPos = index;
        }


    }

    private boolean isInActionMode() {
        return activity.isInActionMode;
    }


    private void loadUserPhoto(String fromId, final ImageView imageView) {
        //if it's a group load the user image
        if (isGroup && groupUsers != null) {
            User mUser = ListUtil.getUserById(fromId, groupUsers);
            if (mUser != null && mUser.getThumbImg() != null) {
                Glide.with(context).load(BitmapUtils.encodeImageAsBytes(mUser.getThumbImg())).asBitmap().into(imageView);
            }
        } else {
            if (user.getThumbImg() != null)
                Glide.with(context).load(BitmapUtils.encodeImageAsBytes(user.getThumbImg())).asBitmap().into(imageView);

        }
    }


    private RecyclerView.ViewHolder getHolderByType(ViewGroup parent, int viewType) {
        switch (viewType) {
            case MessageType.DAY_ROW:
                return new MessagingAdapter.TimestampHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_day, parent, false));


            case MessageType.SENT_DELETED_MESSAGE:
                return new MessagingAdapter.SentDeletedMessageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sent_deleted_message, parent, false));

            case MessageType.RECEIVED_DELETED_MESSAGE:
                return new MessagingAdapter.ReceivedDeletedMessageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_received_deleted_message, parent, false));

            case MessageType.SENT_TEXT:
                return new MessagingAdapter.SentTextHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sent_message_text, parent, false));
            case MessageType.SENT_IMAGE:
                return new MessagingAdapter.SentImageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sent_message_img, parent, false));
            case MessageType.RECEIVED_TEXT:
                return new MessagingAdapter.ReceivedTextHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_received_message_text, parent, false));
            case MessageType.RECEIVED_IMAGE:
                return new MessagingAdapter.ReceivedImageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_received_message_img, parent, false));
            case MessageType.SENT_VOICE_MESSAGE:
                return new MessagingAdapter.SentVoiceMessageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sent_voice_message, parent, false));
            case MessageType.RECEIVED_VOICE_MESSAGE:
                return new MessagingAdapter.ReceivedVoiceMessageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_received_message_voice, parent, false));
            case MessageType.RECEIVED_VIDEO:
                return new MessagingAdapter.ReceivedVideoMessageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_received_message_video, parent, false));
            case MessageType.SENT_VIDEO:
                return new MessagingAdapter.SentVideoMessageHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sent_message_video, parent, false));
            case MessageType.SENT_FILE:
                return new MessagingAdapter.SentFileHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sent_file, parent, false));
            case MessageType.RECEIVED_FILE:
                return new MessagingAdapter.ReceivedFileHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_received_file, parent, false));
            case MessageType.SENT_AUDIO:
                return new MessagingAdapter.SentAudioHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sent_audio, parent, false));
            case MessageType.RECEIVED_AUDIO:
                return new MessagingAdapter.ReceivedAudioHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_received_audio, parent, false));
            case MessageType.SENT_CONTACT:
                return new MessagingAdapter.SentContactHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sent_contact, parent, false));
            case MessageType.RECEIVED_CONTACT:
                return new MessagingAdapter.ReceivedContactHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_received_contact, parent, false));
            case MessageType.SENT_LOCATION:
                return new MessagingAdapter.SentLocationHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sent_location, parent, false));
            case MessageType.RECEIVED_LOCATION:
                return new MessagingAdapter.ReceivedLocationHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_received_location, parent, false));
            case MessageType.GROUP_EVENT:
                return new GroupEventHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_event, parent, false));
        }


        return null;
    }

    public boolean isListContainsMedia() {
        return isListContainsMedia;
    }

    public HashMap<String, AudioRecyclerState> getVoiceMessageStateHashmap() {
        return audioRecyclerState;
    }

    public HashMap<String, Integer> getProgressHashmap() {
        return progressHashmap;
    }

    //this is for image/video onClick so we can handle transitions in the Activity NOT in the Adapter
    public interface OnClickListener {
        void onClick(String path, User user, String selectedMessageId, View imgView, int pos);

    }

    OnClickListener onItemClick;

    public void setOnItemClick(OnClickListener onItemClick) {
        this.onItemClick = onItemClick;
    }

}
