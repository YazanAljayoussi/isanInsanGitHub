package com.mygdx.game;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import rvf.Adapter;

public class Main2Activity extends AppCompatActivity {
    //TODO create InsanRecyclerView and use it
    RecyclerView recyclerView;
    Adapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        intiActivityComp();
    }

    @Override
    protected void onStart() {
        super.onStart();

        intiRV();
    }



    private void intiRV() {
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(true);
        GridLayoutManager mLayoutManager = new GridLayoutManager(Main2Activity.this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        adapter = new Adapter(Main2Activity.this);
        recyclerView.setAdapter(adapter);
    }

    private void intiActivityComp() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

    }
}
