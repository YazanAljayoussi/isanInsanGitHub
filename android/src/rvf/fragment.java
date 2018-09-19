package rvf;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mygdx.game.R;

/**
 * Created by Mohammad.Obiedat on 9/18/2018.
 */

public class fragment extends Fragment {
    View rootView;

    TextView textView_test;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        initializationComponentOfFeatureFragment(inflater,container);

        return  rootView;
    }

    private void initializationComponentOfFeatureFragment(LayoutInflater inflater, ViewGroup container) {
        rootView = inflater.inflate(R.layout.fragment,container,false);
        textView_test = (TextView) rootView.findViewById(R.id.test);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e("I'm Here","****");

    }
}
