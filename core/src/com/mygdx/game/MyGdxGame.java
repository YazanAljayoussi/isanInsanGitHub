package com.kesen.echo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.UBJsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

public class MyGdxGame extends ApplicationAdapter implements InputProcessor {
	public static MyGdxGame instance;
	private OrthographicCamera camera;
	private ModelBatch modelBatch;
	private SpriteBatch spriteBatch;
	private ModelBuilder modelBuilder;
	private Model box;
	private ModelInstance modelInstance;
	private Environment environment;
	private ICreator creator;
    public Integer dir;
	public static Model model;
	private AnimationController controller;
	public static Boolean initialized= false;
	public float camera_width;
	public float camera_height;
	Image backgroundSprite;
	Texture backgroundTexture;
	public MyGdxGame(ICreator aCreator){
		creator= aCreator;
	}
	public void iniModel(){

	}
	@Override
	public void create () {
		dir= 1;


		/*
		Set camera dimension
		 */
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		float r =  (h / w);

		camera_width= 380;
		camera_height= camera_width * r;


		camera = new OrthographicCamera(camera_width, camera_height);//100 * r);//PerspectiveCamera(75,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 50f);
		camera.lookAt(camera.viewportWidth / 2f, camera.viewportHeight / 2f,0f);


		//camera.near =0.1f;
		//camera.far = 300f;


		/*
		Set the background
		 */

		backgroundTexture = new Texture("bg.jpg");

		modelBatch = new ModelBatch();
		spriteBatch= new SpriteBatch();

		///!
		// Model loader needs a binary json reader to decode
		UBJsonReader jsonReader = new UBJsonReader();
		// Create a model loader passing in our json reader
		G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);

		// Now load the model by name
		// Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj


		if (model== null)
			//model = modelLoader.loadModel(Gdx.files.getFileHandle("namMaya.g3db", Files.FileType.Internal));
			//model = modelLoader.loadModel(Gdx.files.getFileHandle("walking_3.g3db", Files.FileType.Internal));
			//model = modelLoader.loadModel(Gdx.files.getFileHandle("untitled.g3db", Files.FileType.Internal));
		model = modelLoader.loadModel(Gdx.files.getFileHandle("noHair.g3db", Files.FileType.Internal));
		///!


		Json json = new Json();
		FileHandle file= Gdx.files.getFileHandle("animation.json", Files.FileType.Internal);
		JsonValue animations = new JsonReader().parse(file);


		modelLoader.parseAnimations(model.modelData, animations);
		model.loadAnimations(model.modelData.animations);
		model.modelData.animations.removeIndex(0);

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight,0.8f,0.8f,0.8f,1f));

//		Animation animation= modelInstance.getAnimation("mixamo.com");
//		animation.nodeAnimations
//		Json Json= new Json();
		//controller = new AnimationController(modelInstance);

		//controller.setAnimation("mixamo.com",-1);
		Gdx.input.setInputProcessor(this);
		if (creator!= null)
		creator.LibGDXInied();
		initialized= true;
		instance= this;
	}






	@Override
	public void dispose() {
		super.dispose();
		model.dispose();
		modelBatch.dispose();
	}

	@Override
	public void render () {


		//String myString = "F0EBE1";
		//Long i = Long.parseLong(myString, 16);
		//Float f = Float.intBitsToFloat(i.intValue());


		Gdx.gl.glClearColor(0.941f,0.922f,0.882f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);



		camera.update();

		/*
		render background
		 */
		//spriteBatch.begin();
		//spriteBatch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		//spriteBatch.end();



		modelBatch.begin(camera);

		for (Map.Entry<Integer, CharacterContent> entry : ScrollSyncer.getInstance().getCharacters().entrySet()) {
			CharacterContent characterContent = entry.getValue();
			if (characterContent.worldCoordinates != null) {
				characterContent.syncLocation();
				Integer holderHashCode = entry.getKey();
				modelBatch.render(characterContent.getModelInstance(), environment);
			}

		}


		modelBatch.end();


	}

	public void rotate(){
		// camera.rotateAround(new Vector3(0f, 0f, 0f), new Vector3(0f, 1f, 0f), 1f * dir);
	}

	@Override
	public boolean keyDown(int keycode) {
		// In the real world, do not create NEW variables over and over, create
		// a temporary static member instead
		if(keycode == Input.Keys.LEFT)
			camera.rotateAround(new Vector3(0f, 0f, 0f), new Vector3(0f, 1f, 0f), 1f);
		if(keycode == Input.Keys.RIGHT)
			camera.rotateAround(new Vector3(0f,0f,0f),new Vector3(0f,1f,0f), -1f);
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}