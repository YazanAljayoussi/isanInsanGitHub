package com.kesen.echo;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication.Callbacks;
import com.badlogic.gdx.utils.Timer;

import rvf.Adapter;
import rvf.InsanRecyclerView;


public class 	AndroidLauncher extends FragmentActivity implements Callbacks
		, BlankFragment.OnFragmentInteractionListener {
	private GameFragment gameFragment;
	private BlankFragment controlFragment;
	InsanRecyclerView recyclerView;
	Adapter adapter;
	Toolbar toolbar;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate( savedInstanceState);
		setContentView(R.layout.activity_main_lgx);
		statusBarColor();
		toolbar = findViewById(R.id.toolbar);
		//setSupportActionBar(toolbar);
		// 6. Finally, replace the AndroidLauncher activity content with the Libgdx Fragment.
		gameFragment = new GameFragment();
		//controlFragment = new BlankFragment();

		FragmentTransaction transaction =
				getSupportFragmentManager().beginTransaction();


		//transaction.add(R.id.send_fragment, controlFragment);
		transaction.add(R.id.characters_fragment, gameFragment);

		transaction.commit();
		intiActivityComp();



	}
	private void intiActivityComp() {
		recyclerView = (InsanRecyclerView) findViewById(R.id.recycler_view);
	}

	@Override
	protected void onStart() {
		super.onStart();

		intiRV();
	}

	private void intiRV() {
		recyclerView.setNestedScrollingEnabled(false);
		recyclerView.setHasFixedSize(true);
		GridLayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 1);
		recyclerView.setLayoutManager(mLayoutManager);
		adapter = new Adapter(getApplicationContext());
		recyclerView.setAdapter(adapter);
	}
	@Override
	public void onFragmentInteraction(Integer dir) {
		gameFragment.myGdxGame.dir= dir;
	}


	// 4. Create a Class that extends AndroidFragmentApplication which is the Fragment implementation for Libgdx.
	public static class GameFragment
			extends AndroidFragmentApplication
	{
		public com.kesen.echo.MyGdxGame myGdxGame;
		// 5. Add the initializeForView() code in the Fragment's onCreateView method.
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			myGdxGame= new com.kesen.echo.MyGdxGame();
			return initializeForView(myGdxGame);
		}

	}


	@Override
	public void exit() {}





	@Override
	public void onPointerCaptureChanged(boolean hasCapture) {

	}

	private void statusBarColor() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
			Window window = this.getWindow();
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.setStatusBarColor(ContextCompat.getColor(this, R.color.statusBar));
		}
	}


}
