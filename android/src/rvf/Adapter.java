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

import com.mygdx.game.R;

import java.util.ArrayList;

/**
 * Created by Mohammad.Obiedat on 9/18/2018.
 */

public class Adapter extends
        RecyclerView.Adapter<Adapter.ViewHolder> {
    private final Context context;
    ArrayList<TestComp> testArrayList = new ArrayList<TestComp>();
    int[] startPosition  = new int[2];

    public Adapter(Context context
            , ArrayList<TestComp> testArrayList
    )
    {
        this.context = context;
        this.testArrayList = testArrayList;
    }

    public Adapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType)
    {
        View view = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.adapter, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final Adapter.ViewHolder holder, int position) {
        fillInformationCard(context, position, holder);

        holder.relativeLayout_rlCover.setTag(position);
        holder.textView_xValue.setX(startPosition[0]);
        holder.textView_yValue.setY(startPosition[1]);

    }

    private void fillInformationCard(Context context, int position, ViewHolder holder) {
        holder.textView_test.setText(testArrayList.get(position).getTestStr());
        holder.textView_number.setText(String.valueOf(testArrayList.get(position).getNumber()+1));
        View fragment = (View) holder.relativeLayout_CoverFragment.getChildAt(0);
    }

    @Override
    public int getItemCount() {
        return testArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView_test,textView_number
                ,textView_xValue,textView_yValue;
        RelativeLayout relativeLayout_CoverFragment,relativeLayout_rlCover;
        public ViewHolder(View itemView) {
            super(itemView);
            relativeLayout_CoverFragment = (RelativeLayout) itemView.findViewById(R.id.CoverFragmentRL);
            relativeLayout_rlCover = (RelativeLayout) itemView.findViewById(R.id.rlCover);
            textView_test = (TextView) itemView.findViewById(R.id.test2);
            textView_number = (TextView) itemView.findViewById(R.id.test);

            textView_xValue = (TextView) itemView.findViewById(R.id.xValueTV);
            textView_yValue = (TextView) itemView.findViewById(R.id.yValueTV);

        }

    }
}