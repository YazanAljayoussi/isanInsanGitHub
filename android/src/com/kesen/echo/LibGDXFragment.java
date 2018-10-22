package com.kesen.echo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.badlogic.gdx.utils.Timer;

/**
 * Created by Mohammad.Obiedat on 9/18/2018.
 */

public class LibGDXFragment extends AndroidFragmentApplication {
    public MyGdxGame myGdxGame;

    // 5. Add the initializeForView() code in the Fragment's onCreateView method.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myGdxGame = new MyGdxGame();

//        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
//        cfg.r = cfg.g = cfg.b = cfg.a = 8;
//
//        cfg.useGL20 = false;

        View view = initializeForView(myGdxGame);//, cfg);

//        if (graphics.getView() instanceof SurfaceView) {
//            SurfaceView glView = (SurfaceView) graphics.getView();
//            glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
//            glView.setZOrderOnTop(true);
//        }

        return view;
    }


}

