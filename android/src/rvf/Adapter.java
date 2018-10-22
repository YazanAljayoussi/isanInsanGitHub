package rvf;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.kesen.echo.R;

import java.util.ArrayList;

/**
 * Created by Mohammad.Obiedat on 9/18/2018.
 */

public class Adapter extends
        RecyclerView.Adapter<Adapter.ViewHolder> {
    private final Context context;
    int[] startPosition  = new int[2];

    public Adapter(Context context
    )
    {
        this.context = context;
    }

    public Adapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType)
    {
        View view = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.adapter, viewGroup, false);

        ViewHolder holder = new ViewHolder(view);
//
//        InsanRecyclerView.instance.setView(view,holder);
        return holder;

    }

    @Override
    public void onBindViewHolder(final Adapter.ViewHolder holder, int position) {
        //TODO set animation here!
       // InsanRecyclerView.instance.syncViewLocation(holder.hashCode());
    }

    @Override
    public int getItemCount() {
        return 30;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView_xValue,textView_yValue;
        RelativeLayout relativeLayout_rlCover;
        public ViewHolder(View itemView) {
            super(itemView);
            relativeLayout_rlCover = (RelativeLayout) itemView.findViewById(R.id.rlCover);

            textView_xValue = (TextView) itemView.findViewById(R.id.xValueTV);
            textView_yValue = (TextView) itemView.findViewById(R.id.yValueTV);

        }

    }
}