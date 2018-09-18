package rvf;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    public void onBindViewHolder(Adapter.ViewHolder holder, int position) {
        fillInformationCard(context, position, holder);
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
        TextView textView_test,textView_number;
        RelativeLayout relativeLayout_CoverFragment;
        public ViewHolder(View itemView) {
            super(itemView);
            relativeLayout_CoverFragment = (RelativeLayout) itemView.findViewById(R.id.CoverFragmentRL);
            textView_test = (TextView) itemView.findViewById(R.id.test2);
            textView_number = (TextView) itemView.findViewById(R.id.test);
        }

    }
}