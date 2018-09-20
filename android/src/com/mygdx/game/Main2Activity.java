package com.mygdx.game;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;

import rvf.Adapter;
import rvf.TestComp;

public class Main2Activity extends AppCompatActivity {
    ArrayList<TestComp> testArrayList = new ArrayList<TestComp>();
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

        fillArrayList();
        intiRV();
    }

    private void fillArrayList() {
        testArrayList = new ArrayList<TestComp>();
        for (int i=0;i<20;i++)
        {
            TestComp text= new TestComp("test"+(i+1),i);
            testArrayList.add(text);
        }
    }

    private void intiRV() {
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setHasFixedSize(true);
        GridLayoutManager mLayoutManager = new GridLayoutManager(Main2Activity.this, 1);
        recyclerView.setLayoutManager(mLayoutManager);
        adapter = new Adapter(Main2Activity.this,testArrayList);
        recyclerView.setAdapter(adapter);
    }

    private void intiActivityComp() {
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

    }
}
