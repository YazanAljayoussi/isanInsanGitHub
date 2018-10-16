package com.kesen.echo;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/*
Resposible for locating characters with-respect to recycler view
 */
public class ScrollSyncer {

    private static final ScrollSyncer instance = new ScrollSyncer();
    public boolean hash_is_busy= false;
    private ConcurrentHashMap<Integer, CharacterContent> characters;
    //private constructor to avoid client applications to use constructor
    private ScrollSyncer(){
        characters= new ConcurrentHashMap<Integer, CharacterContent>();

    }

    public ConcurrentHashMap<Integer, CharacterContent> getCharacters() {
        return characters;
    }

    public static ScrollSyncer getInstance(){
        return instance;
    }

    public void setCharacterPosition(int x, int y, Integer id, boolean off_screen){
        if (!MyGdxGame.initialized) return;
        if (MyGdxGame.model == null) return;
        hash_is_busy = true;
        if (off_screen) {
            // handle off-screen characters
            y = 10000;
        }
        CharacterContent characterContent= characters.get(id);
        if (characterContent== null){
            characterContent= new CharacterContent();
            characters.put(id, characterContent);
        }

        characterContent.setLocation(x, y);
        hash_is_busy= false;
    }
}