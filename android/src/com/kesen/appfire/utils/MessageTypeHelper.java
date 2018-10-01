package com.kesen.appfire.utils;

import com.mygdx.game.R;
import com.kesen.appfire.model.constants.MessageType;

/**
 * Created by Devlomi on 19/03/2018.
 */

public class MessageTypeHelper {
    public static String getTypeText(int type) {
        switch (type) {
            case MessageType.SENT_IMAGE:
            case MessageType.RECEIVED_IMAGE:
                return "Photo";

            case MessageType.SENT_VIDEO:
            case MessageType.RECEIVED_VIDEO:
                return "Video";


            case MessageType.SENT_VOICE_MESSAGE:
            case MessageType.RECEIVED_VOICE_MESSAGE:
                return "Voice Message";


            case MessageType.SENT_AUDIO:
            case MessageType.RECEIVED_AUDIO:
                return "Audio";


            case MessageType.SENT_FILE:
            case MessageType.RECEIVED_FILE:
                return "File";


            case MessageType.SENT_LOCATION:
            case MessageType.RECEIVED_LOCATION:
                return "Location";


            default:
                return "";
        }

    }

    public static int getMessageTypeDrawable(int type) {
        switch (type) {
            case MessageType.SENT_IMAGE:
            case MessageType.RECEIVED_IMAGE:
                return R.drawable.ic_photo_camera;

            case MessageType.SENT_VIDEO:
            case MessageType.RECEIVED_VIDEO:
                return R.drawable.ic_videocam;


            case MessageType.SENT_VOICE_MESSAGE:
            case MessageType.RECEIVED_VOICE_MESSAGE:
                return R.drawable.mic_icon;


            case MessageType.SENT_AUDIO:
            case MessageType.RECEIVED_AUDIO:
                return R.drawable.ic_music_note;

            case MessageType.SENT_CONTACT:
            case MessageType.RECEIVED_CONTACT:

                return R.drawable.ic_person;

            case MessageType.SENT_LOCATION:
            case MessageType.RECEIVED_LOCATION:
                return R.drawable.ic_location_on;

            default:
                return R.drawable.ic_insert_drive_file;
        }

    }

    //this is to show emoji icon at start of the notification
    public static String getEmojiIcon(int type) {

        switch (type) {

            case MessageType.RECEIVED_IMAGE:
                return "\uD83D\uDCF7";


            case MessageType.RECEIVED_VIDEO:
                return "\uD83D\uDCF9";


            case MessageType.RECEIVED_VOICE_MESSAGE:
                return "\uD83C\uDFA4";


            case MessageType.RECEIVED_AUDIO:
                return "\uD83C\uDFB5";

            case MessageType.RECEIVED_CONTACT:
                return "\uD83D\uDC65";

            case MessageType.RECEIVED_LOCATION:
                return "\uD83D\uDCCD";

            default:
                return "\uD83D\uDCC1";
        }
    }


}
