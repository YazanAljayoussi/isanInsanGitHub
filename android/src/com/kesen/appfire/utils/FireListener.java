package com.kesen.appfire.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//this class is to manage value event listener and avoid memory leaks
public class FireListener {
    //this will contain the ref as string and value event listener of this ref
    HashMap<String, ValueEventListener> databaseReferences;
    HashMap<String, ValueEventListener> voiceMessageDatabaseReferences;
    FirebaseDatabase database;

    //init
    public FireListener() {
        databaseReferences = new HashMap<>();
        voiceMessageDatabaseReferences = new HashMap<>();
        database = FirebaseDatabase.getInstance();
    }

    //add listener to the given database reference
    public void addListener(DatabaseReference ref, ValueEventListener eventListener) {
        //to ref to string to store it in hashmap
        String refString = ref.toString();
        //if it's already stored don't add it
        if (databaseReferences.containsKey(refString))
            return;

        //add value event listener
        ref.addValueEventListener(eventListener);
        //save the ref to hashmap
        databaseReferences.put(refString, eventListener);

    }


    public void addVoiceMessageListener(DatabaseReference ref, ValueEventListener eventListener) {
        String refString = ref.toString();
        if (voiceMessageDatabaseReferences.containsKey(refString)) return;
        ref.addValueEventListener(eventListener);
        voiceMessageDatabaseReferences.put(refString, eventListener);
    }

    //remove listener when the value will not change again
    public void removeListener(DatabaseReference ref, ValueEventListener eventListener) {
        ref.removeEventListener(eventListener);
        databaseReferences.remove(ref.toString());
    }

    //cleanup and remove all listeners when activity is not in foreground
    public void cleanup() {

        Iterator<Map.Entry<String, ValueEventListener>> dbRefsIt = databaseReferences.entrySet().iterator();
        while (dbRefsIt.hasNext()) {
            Map.Entry<String, ValueEventListener> entry = dbRefsIt.next();
            String stringRef = entry.getKey();
            DatabaseReference databaseReference = getRefFromString(stringRef);
            ValueEventListener valueEventListener = entry.getValue();
            databaseReference.removeEventListener(valueEventListener);
            dbRefsIt.remove();
        }


        Iterator<Map.Entry<String, ValueEventListener>> vmDbRefIt = databaseReferences.entrySet().iterator();
        while (vmDbRefIt.hasNext()) {
            Map.Entry<String, ValueEventListener> entry = vmDbRefIt.next();
            String stringRef = entry.getKey();
            DatabaseReference databaseReference = getRefFromString(stringRef);
            ValueEventListener valueEventListener = entry.getValue();
            databaseReference.removeEventListener(valueEventListener);
            vmDbRefIt.remove();
        }
    }

    //convert string ref to Database Reference
    private DatabaseReference getRefFromString(String refString) {
        return database.getReferenceFromUrl(refString);
    }


}
