package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.ICreator;
import com.mygdx.game.MyGdxGame;

public class DesktopLauncher implements ICreator {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new MyGdxGame(null), config);
	}

	@Override
	public void LibGDXInied() {

	}
}
