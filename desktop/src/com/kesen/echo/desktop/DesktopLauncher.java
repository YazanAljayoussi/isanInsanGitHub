package com.kesen.echo.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.kesen.echo.ICreator;
import com.kesen.echo.MyGdxGame;

public class DesktopLauncher implements ICreator {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new MyGdxGame(), config);
	}

	@Override
	public void LibGDXInied() {

	}
}
