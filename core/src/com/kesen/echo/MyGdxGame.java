
package com.kesen.echo;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

import com.badlogic.gdx.graphics.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

import com.badlogic.gdx.utils.UBJsonReader;

import java.util.Map;



public class MyGdxGame extends ApplicationAdapter implements InputProcessor {
   // public static MyGdxGame instance;
    public static Model model2;
    private OrthographicCamera camera;
    private ModelBatch modelBatch;
    private SpriteBatch spriteBatch;
    private ModelBuilder modelBuilder;
    private Model box;
    private com.badlogic.gdx.graphics.g3d.ModelInstance modelInstance;
    private Environment environment;

    public Integer dir;
    public static com.badlogic.gdx.graphics.g3d.Model model;
    private AnimationController controller;
    public static Boolean initialized = false;
    public float camera_width;
    public float camera_height;
    Image backgroundSprite;
    Texture backgroundTexture;

    public MyGdxGame() {

    }

    public MyGdxGame(int[] width_hight) {
        camera_width = width_hight[0];
       camera_height = width_hight[1];

    }


    public void iniModel() {

    }

    @Override
    public void create() {
        try {

            Gdx.gl.glClearColor( 1, 0, 0, 1 );
            Gdx.gl.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );

            Gdx.app.setLogLevel(Application.LOG_DEBUG);

            Gdx.app.debug("create", "sss");

            dir = 1;
            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
            float r = (h / w);


            camera_width = 380;
            camera_height = camera_width * r;


            camera = new OrthographicCamera(camera_width, camera_height);//100 * r);//PerspectiveCamera(75,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            camera.position.set(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 50f);
            camera.lookAt(camera.viewportWidth / 2f, camera.viewportHeight / 2f, 0f);
            backgroundTexture = new Texture("bg.jpg");

            modelBatch = new ModelBatch();

            UBJsonReader jsonReader = new UBJsonReader();

            com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
            model = modelLoader.loadModel(Gdx.files.getFileHandle("walking_3.g3db", Files.FileType.Internal));
            //    com.kesen.echo.G3dModelLoader modelLoader2 = new com.kesen.echo.G3dModelLoader(jsonReader);
            //  model2 = modelLoader2.loadModel(Gdx.files.getFileHandle("walking_3.g3db", Files.FileType.Internal));

            modelInstance = new ModelInstance(model);


            controller = new AnimationController(modelInstance);

            controller.animate("mixamo.com", -1, 1f, null, 0f);

            environment = new Environment();
            environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1.0f));

            Gdx.input.setInputProcessor(this);
            initialized = true;
            Gdx.app.debug("MyTag", "my debug message out" + count);
            // instance=this;
     /*   spriteBatch= new SpriteBatch();
        spriteBatch.begin();
        spriteBatch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        spriteBatch.end();*/

        }catch (Exception e)
        {

        }
    }



    @Override
    public void dispose() {
        super.dispose();
        model.dispose();
        modelBatch.dispose();
       Gdx.app.debug("finish","sss");
    }
    @Override
    public void pause()
    {
        super.pause();
        modelBatch.dispose();
        Gdx.app.debug("pause","sss");

    }
    int count=0;
    int firstRend=1000000;
    private RenderableProvider provider;
    @Override
    public void render() {
        try {
            super.render();
            Gdx.app.debug("render", "sss" + count);
            Gdx.gl.glClearColor(0.941f, 0.922f, 0.882f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            //controller.update(Gdx.graphics.getDeltaTime());
            camera.update();

            modelBatch.begin(camera);

    /*    provider=new RenderableProvider() {
            @Override
            public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
*/
            for (Map.Entry<Integer, CharacterContent> entry : ScrollSyncer.getInstance().getCharacters().entrySet()) {
                CharacterContent characterContent = entry.getValue();
//            Gdx.app.debug("MyTag", entry.getKey()+ ", " + entry.getValue().worldCoordinates.y+", " + count);

                if (characterContent.worldCoordinates != null) {
                    characterContent.syncLocation();
                    Integer holderHashCode = entry.getKey();
                    //   Renderable r = pool.obtain();
                    // r.set(characterContent.getModelInstance().getRenderable(r));
                    //   renderables.add(r);
                    //  if(count==0)
                    modelBatch.render(characterContent.getModelInstance(), environment);
                    count++;
                }
            }
            //         }
            //    };


            //  modelBatch.render(camera);


            modelBatch.end();
        }
       catch (Exception e) {
           Gdx.app.debug("end rend", "sss");


       }

    }

    public void rotate() {
        // camera.rotateAround(new Vector3(0f, 0f, 0f), new Vector3(0f, 1f, 0f), 1f * dir);
    }

    @Override
    public boolean keyDown(int keycode) {
        // In the real world, do not create NEW variables over and over, create
        // a temporary static member instead
        if (keycode == Input.Keys.LEFT)
            camera.rotateAround(new Vector3(0f, 0f, 0f), new Vector3(0f, 1f, 0f), 1f);
        if (keycode == Input.Keys.RIGHT)
            camera.rotateAround(new Vector3(0f, 0f, 0f), new Vector3(0f, 1f, 0f), -1f);
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