package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.UBJsonReader;

import java.util.Map;

public class MyGdxGame extends ApplicationAdapter implements InputProcessor {
	public static MyGdxGame instance;
	private OrthographicCamera camera;
	private ModelBatch modelBatch;
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

	public MyGdxGame(ICreator aCreator){
		creator= aCreator;
	}
	@Override
	public void create () {
		dir= 1;



		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		float r =  (h / w);

		camera_width= 250;
		camera_height= camera_width * r;


		camera = new OrthographicCamera(camera_width, camera_height);//100 * r);//PerspectiveCamera(75,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 80f);
		camera.lookAt(camera.viewportWidth / 2f, camera.viewportHeight / 2f,0f);
		//camera.near =0.1f;
		//camera.far = 300f;

		modelBatch = new ModelBatch();

		// Model loader needs a binary json reader to decode
		UBJsonReader jsonReader = new UBJsonReader();
		// Create a model loader passing in our json reader
		G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);

		// Now load the model by name
		// Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj
		if (model== null)
		model = modelLoader.loadModel(Gdx.files.getFileHandle("walking_3.g3db", Files.FileType.Internal));
		// Now create an instance.  Instance holds the positioning data, etc of an instance of your model
		//modelInstance = new ModelInstance(model);



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


		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);


		camera.update();


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