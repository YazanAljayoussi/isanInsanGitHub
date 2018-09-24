package com.mygdx.game;

import java.util.HashMap;

/*
Resposible for locating characters with-respect to recycler view
 */
public class ScrollSyncer {

    private static final ScrollSyncer instance = new ScrollSyncer();
    private HashMap<Integer, CharacterContent> characters;
    //private constructor to avoid client applications to use constructor
    private ScrollSyncer(){
        characters= new HashMap<Integer, CharacterContent>();

    }

    public HashMap<Integer, CharacterContent> getCharacters() {
        return characters;
    }

    public static ScrollSyncer getInstance(){
        return instance;
    }

    public void setCharacterPosition(int x, int y, Integer id){
        if (!MyGdxGame.initialized) return;
        CharacterContent characterContent= characters.get(id);
        if (characterContent== null){
            characterContent= new CharacterContent();
            characters.put(id, characterContent);
        }

        characterContent.setLocation(x, y);
    }
}