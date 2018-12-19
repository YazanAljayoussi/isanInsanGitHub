package com.kesen.echo;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;

class CharacterContent {
   // private Model model;
    //private ModelInstance modelInstance;
    private ModelInstance instance,ins;
  //  private AnimationController controller;
    private AnimationController animationController;
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
      //  modelInstance = new ModelInstance(MyGdxGame.model2);
       // instance=new com.badlogic.gdx.graphics.g3d.ModelInstance(MyGdxGame.model);
        ins= new ModelInstance(MyGdxGame.model);
     //   controller = new AnimationController(modelInstance);
        animationController=new AnimationController(ins);
        animationController.animate("mixamo.com", -1, 1f, null, 0f);

        //controller.set
        //controller.setAnimation("mixamo.com",-1);
        //controller.setAnimation("Armature|Armature|Take 001|BaseLayer",-1);

       // controller.setAnimation("Armature|Armature|Take 001|BaseLayer",-1);
        //controller.setAnimation("Take 001",-1);


    }
    public Vector3  worldCoordinates;
    public void setLocation(int screenX, int screenY) {

        Gdx.app.debug("set loc12","xyxy"+screenX+", "+screenY);
        int character_width= 50;
        float w = Gdx.graphics.getWidth();
        float hi = Gdx.graphics.getHeight();
        float r =  (hi / w);
      float  camera_width=380;// MyGdxGame.instance.camera_width;
       float camera_height= camera_width * r;
        screenX= me ? character_width : (int)camera_width - character_width;
        float h= (float)screenY / (float)Gdx.graphics.getHeight();
        worldCoordinates = new Vector3(screenX,  h * camera_height, 0);
        Gdx.app.debug("set loc3","sss");

        ins.transform.setToTranslation(screenX,h*camera_height,0);
        Gdx.app.debug("set loc4","sss");

    }
    public ModelInstance getModelInstance() {

        return ins;
    }

    public void syncLocation() {

        //controller.update(Gdx.graphics.getDeltaTime());
        animationController.update(Gdx.graphics.getDeltaTime());
    }


}
