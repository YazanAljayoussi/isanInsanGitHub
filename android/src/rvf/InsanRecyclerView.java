package rvf;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.kesen.echo.ScrollSyncer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mohammad.Obiedat on 9/20/2018.
 */

public class InsanRecyclerView extends RecyclerView {
    HashMap  <Integer,HolderContent> viewsH =new HashMap<Integer,HolderContent>();
    HashMap  <Integer,int[]> viewsLocationsHM =new HashMap<Integer,int[]>();

    static InsanRecyclerView instance;

    public InsanRecyclerView(Context context) {
        super(context);
        instance= this;
    }

    public InsanRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        instance= this;
    }

    public InsanRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        instance= this;
    }

    public void setView(int holderHashCode,View view,rvf.Adapter.ViewHolder holder)
    {
        viewsH.put(holderHashCode,new HolderContent(view,holder));
    }

    public void syncViewLocation(int holderHashCode)
    {
        int[] xyIntArray  = new int[2];
        //viewsH.get(holderHashCode).getLocationOnScreen(xyIntArray);
        viewsLocationsHM.put(holderHashCode, xyIntArray);
    }

    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        for (Map.Entry<Integer,HolderContent> entry : viewsH.entrySet()) {
            HolderContent holderContent =  entry.getValue();
            int holderHashCode = entry.getKey();

            int[] xyIntArray  = new int[2];
            holderContent.view.getLocationOnScreen(xyIntArray);

            int y= Resources.getSystem().getDisplayMetrics().heightPixels - (xyIntArray[1] + holderContent.view.getHeight());
            //Log.i("Y: ", String.valueOf(xyIntArray[1]));
            ScrollSyncer.getInstance().setCharacterPosition(xyIntArray[0], y, holderHashCode, xyIntArray[1] == 0);
        }
    }

     class HolderContent {

        View view;
        rvf.Adapter.ViewHolder holder;

        public HolderContent(View view, rvf.Adapter.ViewHolder holder) {
            this.view = view;
            this.holder = holder;
        }


    }

}
