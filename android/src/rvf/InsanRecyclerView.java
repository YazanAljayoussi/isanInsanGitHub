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

    public void setView(View view,rvf.Adapter.ViewHolder holder)
    {
        viewsH.put(holder.hashCode(),new HolderContent(view,holder));
    }

    void iniFromHistory(){
        //InsanRecyclerView.instance.setView(itemView, this);
        //InsanRecyclerView.instance.syncViewLocation(this.hashCode());
    }
    public void syncViewLocation(Integer holderHashCode)
    {
        int[] xyIntArray  = new int[2];
        HolderContent holderContent= viewsH.get(holderHashCode);
        holderContent.view.getLocationOnScreen(xyIntArray);
        int y= Resources.getSystem().getDisplayMetrics().heightPixels - (xyIntArray[1] + holderContent.view.getHeight());
        ScrollSyncer.getInstance().setCharacterPosition(xyIntArray[0], y, holderHashCode, xyIntArray[1] == 0);
    }


    @Override
    public void onScrolled(int dx, int dy) {
        super.onScrolled(dx, dy);
        for (Map.Entry<Integer,HolderContent> entry : viewsH.entrySet()) {
            int holderHashCode = entry.getKey();
            syncViewLocation(holderHashCode);
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
