package com.mygdx.game;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;

class CharacterContent {
    private Model model;
    private ModelInstance modelInstance;
    private AnimationController controller;
    public CharacterContent(){

        // Model loader needs a binary json reader to decode
        UBJsonReader jsonReader = new UBJsonReader();
        // Create a model loader passing in our json reader
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);

        // Now load the model by name
        // Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj
        //if (model==null)
        model = modelLoader.loadModel(Gdx.files.getFileHandle("walking_3.g3db", Files.FileType.Internal));
        // Now create an instance.  Instance holds the positioning data, etc of an instance of your model

        ModelBuilder modelBuilder = new ModelBuilder();
        //model = modelBuilder.createBox(10f,10f,10f, new Material(ColorAttribute.createDiffuse(Color.BLUE)), VertexAttributes.Usage.Position|VertexAttributes.Usage.Normal);

//		modelInstance = new ModelInstance(box);
        modelInstance = new ModelInstance(MyGdxGame.model);

//		Animation animation= modelInstance.getAnimation("mixamo.com");
//		animation.nodeAnimations
//		Json Json= new Json();
        controller = new AnimationController(modelInstance);

        controller.setAnimation("mixamo.com",-1);

    }
    public Vector3  worldCoordinates;
    public void setLocation(int screenX, int screenY) {
        float r =  1.66f;
        //screenY= 12;//(int)(100 * r * 0.5);
        screenX= (int)(50.0f);
        worldCoordinates = new Vector3(screenX, screenY, 0);
        modelInstance.transform.setToTranslation(worldCoordinates);
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public void syncLocation() {

        controller.update(Gdx.graphics.getDeltaTime());
    }
}
