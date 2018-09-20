package com.mygdx.game;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication.Callbacks;
import com.badlogic.gdx.utils.Timer;

import java.util.ArrayList;

import rvf.Adapter;
import rvf.InsanRecyclerView;
import rvf.TestComp;


public class AndroidLauncher extends FragmentActivity implements Callbacks
		, BlankFragment.OnFragmentInteractionListener {
	private GameFragment gameFragment;
	private BlankFragment controlFragment;
	ArrayList<TestComp> testArrayList = new ArrayList<TestComp>();
	InsanRecyclerView recyclerView;
	Adapter adapter;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// 6. Finally, replace the AndroidLauncher activity content with the Libgdx Fragment.
		gameFragment = new GameFragment();
		controlFragment = new BlankFragment();

		FragmentTransaction transaction =
				getSupportFragmentManager().beginTransaction();

		transaction.add(R.id.message_fragment, gameFragment);
		transaction.add(R.id.send_fragment, controlFragment);


		transaction.commit();
		intiActivityComp();

	}
	private void intiActivityComp() {
		recyclerView = (InsanRecyclerView) findViewById(R.id.recycler_view);
	}

	@Override
	protected void onStart() {
		super.onStart();

		fillArrayList();
		intiRV();
		recyclerView.findViewHolderForAdapterPosition(0);
		recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
			@Override
			public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
				return false;
			}

			@Override
			public void onTouchEvent(RecyclerView rv, MotionEvent e) {
			}

			@Override
			public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

			}
		});
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
		GridLayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 1);
		recyclerView.setLayoutManager(mLayoutManager);
		adapter = new Adapter(getApplicationContext(),testArrayList);
		recyclerView.setAdapter(adapter);
	}
	@Override
	public void onFragmentInteraction(Integer dir) {
		gameFragment.myGdxGame.dir= dir;
	}


	// 4. Create a Class that extends AndroidFragmentApplication which is the Fragment implementation for Libgdx.
	public static class GameFragment
			extends AndroidFragmentApplication
			implements ICreator, BlankFragment.OnFragmentInteractionListener
	{
		public MyGdxGame myGdxGame;
		// 5. Add the initializeForView() code in the Fragment's onCreateView method.
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			myGdxGame= new MyGdxGame(this);
			return initializeForView(myGdxGame);
		}

		@Override
		public void LibGDXInied() {
			Timer.schedule(new Timer.Task(){
							   @Override
							   public void run() {
								   myGdxGame.rotate();
							   }
						   }
					, 1
					, 0.01f
			);
		}

		@Override
		public void onFragmentInteraction(Integer dir) {
			myGdxGame.dir= dir;
		}
	}


	@Override
	public void exit() {}





	@Override
	public void onPointerCaptureChanged(boolean hasCapture) {

	}
}
