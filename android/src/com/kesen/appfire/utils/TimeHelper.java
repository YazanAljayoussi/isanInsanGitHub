package com.kesen.appfire.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Devlomi on 24/02/2018.
 */

public class TimeHelper {

    //this will format time and get when the user was last seen
    public static String getTimeAgo(long timestamp) {
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);

        Date timestampDate = new Date();
        timestampDate.setTime(timestamp);
        long now = System.currentTimeMillis();
        long secondsAgo = (now - timestamp) / 1000;


        int miunte = 60;
        int hour = 60 * miunte;
        int day = 24 * hour;
        int week = 7 * day;


        if (secondsAgo < miunte)
            return "" /* now */;
        else if (secondsAgo < hour)
            //minutes ago
            return secondsAgo / miunte + " minutes ago";
        else if (secondsAgo < day) {
            //hours ago
            int hoursAgo = (int) (secondsAgo / hour);
            if (hoursAgo <= 5)
                return hoursAgo + " hours ago";

            //today at + time AM or PM
            return "today at " + timeFormat.format(timestampDate);
        } else if (secondsAgo < week) {
            int daysAgo = (int) (secondsAgo / day);
            //yesterday + time AM or PM
            if (daysAgo == 1)
                return "Yesterday at " + timeFormat.format(timestampDate);

            //days ago
            return secondsAgo / day + " days ago";
        }

        //otherwise it's been a long time show the full date
        return fullDateFormat.format(timestampDate) + " at " + timeFormat.format(timestampDate);
    }


    public static String getMediaTime(long timestamp) {
        /*
        if today:
        today , 10:27PM

        if yesterday :
        yesterday , 10:28AM

        if same year:
        Feb 8 , 3:41AM

        else
        1/15/17 ,10:46PM

         */
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy/MM/dd , hh:mm a", Locale.ENGLISH);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM d", Locale.ENGLISH);
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);


        Date timestampDate = new Date();
        timestampDate.setTime(timestamp);
        long now = System.currentTimeMillis();
        long secondsAgo = (now - timestamp) / 1000;


        int miunte = 60;
        int hour = 60 * miunte;
        int day = 24 * hour;
        int week = 7 * day;


        if (secondsAgo < miunte)
            return "Just now";
        else if (secondsAgo < hour)
            return secondsAgo / miunte + " minutes ago";
        else if (secondsAgo < day) {
            int hoursAgo = (int) (secondsAgo / hour);
            if (hoursAgo <= 5)
                return hoursAgo + " hours ago";

            return "today ," + timeFormat.format(timestampDate);
        } else if (secondsAgo < week) {
            int daysAgo = (int) (secondsAgo / day);
            if (daysAgo == 1)
                return "Yesterday at " + timeFormat.format(timestampDate);

            else if (isSameYear(now, timestamp))
                return monthFormat.format(timestampDate) + ", " + timeFormat.format(timestampDate);
        }

        return fullDateFormat.format(timestampDate);
    }

    //this will return only the time of message with am or pm
    public static String getMessageTime(String timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
        Date date = new Date(Long.parseLong(timestamp));
        return format.format(date);
    }


    //get chat time
    public static String getChatTime(long timestamp) {
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);


        Date timestampDate = new Date();
        timestampDate.setTime(timestamp);
        long now = System.currentTimeMillis();

        //the last message was sent today return today
        if (isSameDay(now, timestamp)) {
            return "TODAY";
            //the last message was sent yesterday return yesterday
        } else if (isYesterday(now, timestamp)) {
            return "YESTERDAY";
        } else {
            //otherwise show the date of last message
            return fullDateFormat.format(timestampDate);
        }


    }


    //check if it's same day for the header date
    // if it's same day we will not show a new header
    public static boolean isSameDay(long timestamp1, long timestamp2) {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTimeInMillis(timestamp1);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(timestamp2);
        boolean sameYear = isSameYear(calendar1, calendar2);
        boolean sameMonth = isSameMonth(calendar1, calendar2);
        boolean sameDay = isSameDay(calendar1, calendar2);
        return (sameDay && sameMonth && sameYear);
    }

    private static boolean isSameDay(Calendar calendar1, Calendar calendar2) {
        return calendar1.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH);
    }

    private static boolean isSameMonth(Calendar calendar1, Calendar calendar2) {
        return calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH);
    }

    private static boolean isSameYear(Calendar calendar1, Calendar calendar2) {
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR);
    }

    private static boolean isYesterday(long timestamp1, long timestamp2) {
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTimeInMillis(timestamp1);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(timestamp2);
        boolean isYesterday = calendar1.get(Calendar.DAY_OF_MONTH) - 1 == calendar2.get(Calendar.DAY_OF_MONTH);
        return isSameYear(calendar1, calendar2) && isSameMonth(calendar1, calendar2) && isYesterday;
    }

    //check if two dates are in the same year
    public static boolean isSameYear(long timestamp1, long timestamp2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();

        cal1.setTimeInMillis(timestamp1);
        cal2.setTimeInMillis(timestamp2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    public static String getDate(long timestamp) {
        Date date = new Date();
        date.setTime(timestamp);
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
        return fullDateFormat.format(date);
    }

    //this method will check if message time has passed , if the user wants to delete the message for everyone
    public static boolean isMessageTimePassed(long serverTime, long messageTime) {
        return Math.floor((serverTime - messageTime) / 60000) > 15;
    }
}
