package com.kesen.appfire.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Devlomi on 01/08/2017.
 */

public class Util {
    //show short snack bar
    public static void showSnackbar(Activity activity, String message) {
        Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
    }

    public static void showSnackbar(Activity activity, String message, int length) {
        Snackbar.make(activity.findViewById(android.R.id.content), message, length);
    }

    //extract file extension from full path
    public static String getFileExtensionFromPath(String string) {
        int index = string.lastIndexOf(".");
        String fileExtension = string.substring(index + 1);
        return fileExtension;
    }

    //extract file name from full path
    public static String getFileNameFromPath(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    //this will convert milliseconds progress to a human minutes and seconds like : 02:20
    public static String milliSecondsToTimer(long milliseconds) {

        return String.format(Locale.US, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))
        );

    }


    //this will convert file size as bytes to KB or MB  size
    public static String getFileSizeFromLong(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    //this will highlight the text when user searches for message in chat
    public static Spanned highlightText(String fullText) {
        Spannable wordtoSpan = new SpannableString(fullText);
        wordtoSpan.setSpan(new BackgroundColorSpan(Color.YELLOW), 0, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return wordtoSpan;
    }


    //get video length using its file
    public static String getVideoLength(Context context, String path) {
        if (path == null) return "";
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(context, Uri.parse(path));
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMillisec = Long.parseLong(time);
            return Util.milliSecondsToTimer(timeInMillisec);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return "";
        } finally {
            retriever.release();
        }


    }

    //get audio length using its file
    public static long getAudioFileMillis(Context context, String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(context, Uri.parse(path));
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMillisec = Long.parseLong(time);
            return timeInMillisec;

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            retriever.release();
        }


    }

    //check if a string is contains digits
    public static boolean isNumeric(String s) {
        boolean isDigit = false;
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c))
                return true;
            else
                isDigit = false;
        }

        return isDigit;

    }

    public static boolean isOreoOrAbove() {
        return Build.VERSION.SDK_INT >= 26;
    }

    public static int[] getScreenSize(Activity context)
    {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        int[] arr = new int[]{width, height};
        return arr;
    }

}
