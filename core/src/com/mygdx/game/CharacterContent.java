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
import com.sun.org.apache.xpath.internal.operations.Bool;

import sun.rmi.runtime.Log;

class CharacterContent {
    private Model model;
    private ModelInstance modelInstance;
    private AnimationController controller;
    private boolean me;
    static private boolean mes= false;
    public CharacterContent(){
        mes= !mes;
        me= mes;
        // Model loader needs a binary json reader to decode
        UBJsonReader jsonReader = new UBJsonReader();
        // Create a model loader passing in our json reader
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);

        // Now load the model by name
        // Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj
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
        if (MyGdxGame.instance== null){
            boolean b= true;
        }
        int character_width= 50;

        screenX= me ? character_width : (int)MyGdxGame.instance.camera_width - character_width;
        float h= (float)screenY / (float)Gdx.graphics.getHeight();
        worldCoordinates = new Vector3(screenX,  h * MyGdxGame.instance.camera_height, 0);
        modelInstance.transform.setToTranslation(worldCoordinates);
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public void syncLocation() {

        controller.update(Gdx.graphics.getDeltaTime());
    }
}
