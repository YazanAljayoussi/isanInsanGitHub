package com.mygdx.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.badlogic.gdx.utils.Timer;

/**
 * Created by Mohammad.Obiedat on 9/18/2018.
 */

public class LibGDXFragment extends AndroidFragmentApplication
        implements ICreator, BlankFragment.OnFragmentInteractionListener {
    public MyGdxGame myGdxGame;

    // 5. Add the initializeForView() code in the Fragment's onCreateView method.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myGdxGame = new MyGdxGame(this);
        return initializeForView(myGdxGame);
    }

    @Override
    public void LibGDXInied() {
        Timer.schedule(new Timer.Task() {
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
        myGdxGame.dir = dir;
    }
}

