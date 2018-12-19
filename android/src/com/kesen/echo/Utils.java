package com.kesen.echo;

import android.app.Activity;
import android.util.DisplayMetrics;

public class Utils {
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
