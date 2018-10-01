package com.kesen.appfire.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.mygdx.game.R;
import com.kesen.appfire.events.FetchingUserGroupsFinished;
import com.kesen.appfire.utils.AppVerUtil;
import com.kesen.appfire.utils.BitmapUtils;
import com.kesen.appfire.utils.CropImageRequest;
import com.kesen.appfire.utils.DirManager;
import com.kesen.appfire.utils.FileUtils;
import com.kesen.appfire.utils.FireConstants;
import com.kesen.appfire.utils.FireManager;
import com.kesen.appfire.utils.ServiceHelper;
import com.kesen.appfire.utils.SharedPreferencesManager;
import com.theartofdev.edmodo.cropper.CropImage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

public class SetupUserActivity extends AppCompatActivity {
    private EditText etUsernameSetup;
    private CircleImageView userImgSetup;
    private FloatingActionButton fabSetupUser;
    private ProgressBar progressBarSetupUserImg;


    String storedPhotoUrl, choosenPhoto;
    private String thumbImg;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_user);
        etUsernameSetup = findViewById(R.id.et_username_setup);
        userImgSetup = findViewById(R.id.user_img_setup);
        fabSetupUser = findViewById(R.id.fab_setup_user);
        progressBarSetupUserImg = findViewById(R.id.progress_bar_setup_user_img);

        FireConstants.usersRef.child(FireManager.getUid())
                .child("photo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //if there is no previous image stored in database for this user
                if (dataSnapshot.getValue() == null) {
                    storedPhotoUrl = "";
                    progressBarSetupUserImg.setVisibility(View.GONE);
                    return;
                }

                //otherwise get the stored user image url
                storedPhotoUrl = dataSnapshot.getValue(String.class);


                //load the image
                //we are using listener to determine when the image loading is finished
                //so we can hide the progressBar
                Glide.with(SetupUserActivity.this).load(storedPhotoUrl)
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                progressBarSetupUserImg.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                progressBarSetupUserImg.setVisibility(View.GONE);
                                return false;
                            }
                        }).into(userImgSetup);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        fabSetupUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                completeSetup();

            }
        });

        userImgSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImage();
            }
        });

        //On Done Keyboard Button Click
        etUsernameSetup.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    completeSetup();
                    return true;
                }
                return false;
            }
        });

    }

    private void completeSetup() {
        //check if user not entered his username
        if (TextUtils.isEmpty(etUsernameSetup.getText().toString())) {
            etUsernameSetup.setError(getString(R.string.username_is_empty));
        } else {

            final ProgressDialog dialog = new ProgressDialog(SetupUserActivity.this);
            dialog.setMessage(getString(R.string.loading));
            dialog.setCancelable(false);
            dialog.show();

            // if there user does not choose a new photo
            if (choosenPhoto == null) {

                //if stored photo on database not exists
                //then get the defaultUserProfilePhoto from database and download it
                if (storedPhotoUrl.equals("")) {
                    final File photoFile = DirManager.getMyPhotoPath();
                    FireConstants.mainRef.child("defaultUserProfilePhoto")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() == null) {
                                dialog.dismiss();
                                return;
                            }

                            final String photoUrl = dataSnapshot.getValue(String.class);
                            //download image
                            FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl)
                                    .getFile(photoFile)
                                    .addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                //save user info and finish setup
                                                String userName = etUsernameSetup.getText().toString();
                                                HashMap<String, Object> map = getUserInfoHashmap(userName, photoUrl, photoFile.getPath());

                                                FireConstants.usersRef.child(FireManager.getUid()).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        dialog.dismiss();

                                                        if (task.isSuccessful()) {
                                                            saveUserInfo(photoFile);
                                                        } else {
                                                            showSnackbar();
                                                        }
                                                    }
                                                });

                                            } else showSnackbar();

                                        }
                                    });

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

//

                    //if datasnapshot is not ready yet or there is a connection issue
                } else if (storedPhotoUrl == null) {
                    dialog.dismiss();
                    showSnackbar();
                } else {
                    //otherwise get the stored user image from database
                    //download it and save it and save user info then finish setup
                    final File file = DirManager.getMyPhotoPath();
                    FirebaseStorage.getInstance().getReferenceFromUrl(storedPhotoUrl).getFile(file).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                HashMap<String, Object> userInfoHashmap =
                                        getUserInfoHashmap(etUsernameSetup.getText().toString(), storedPhotoUrl);

                                FireConstants.usersRef.child(FireManager.getUid())
                                        .updateChildren(userInfoHashmap)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                dialog.dismiss();
                                                if (task.isSuccessful()) {
                                                    saveUserInfo(file);
                                                } else showSnackbar();

                                            }
                                        });
                            } else showSnackbar();

                        }
                    });

                }
                //user picked an image
                //upload it  then save user info and finish setup
            } else {
                FireConstants.imageProfileRef.child(UUID.randomUUID().toString() + ".jpg")
                        .putFile(Uri.fromFile(new File(choosenPhoto)))
                        .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    String imageUrl = String.valueOf(task.getResult().getDownloadUrl());
                                    HashMap<String, Object> userInfoHashmap =
                                            getUserInfoHashmap(etUsernameSetup.getText().toString(), imageUrl, choosenPhoto);

                                    FireConstants.usersRef.child(FireManager.getUid())
                                            .updateChildren(userInfoHashmap)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    dialog.dismiss();
                                                    if (task.isSuccessful()) {
                                                        saveUserInfo(new File(choosenPhoto));
                                                    } else showSnackbar();

                                                }
                                            });
                                } else showSnackbar();

                            }
                        });
            }
        }
    }

    private void showSnackbar() {
        Snackbar.make(findViewById(android.R.id.content), R.string.no_internet_connection, Snackbar.LENGTH_SHORT).show();
    }

    @NonNull
    private HashMap<String, Object> getUserInfoHashmap(String userName, String photoUrl, String filePath) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("photo", photoUrl);
        map.put("name", userName);
        map.put("phone", FireManager.getPhoneNumber());
        map.put("status", getString(R.string.default_status));
        String appVersion = AppVerUtil.getAppVersion(this);
        if (!appVersion.equals(""))
            map.put("ver", appVersion);

        //create thumbImg and original image and compress them if the user chosen a new photo
        if (filePath != null) {
            Bitmap circleBitmap = BitmapUtils.getCircleBitmap(BitmapUtils.convertFileImageToBitmap(filePath));
            thumbImg = BitmapUtils.decodeImageAsPng(circleBitmap);
            map.put("thumbImg", thumbImg);
        }

        return map;
    }


    @NonNull
    private HashMap<String, Object> getUserInfoHashmap(String userName, String photoUrl) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("photo", photoUrl);
        map.put("name", userName);
        map.put("phone", FireManager.getPhoneNumber());
        map.put("status", getString(R.string.default_status));
        String appVersion = AppVerUtil.getAppVersion(this);
        if (!appVersion.equals(""))
            map.put("ver", appVersion);

        return map;
    }

    //save user info locally
    private void saveUserInfo(File photoFile) {
        SharedPreferencesManager.saveMyPhoto(photoFile.getPath());
        if (thumbImg == null) {
            Bitmap circleBitmap = BitmapUtils.getCircleBitmap(BitmapUtils.convertFileImageToBitmap(photoFile.getPath()));
            thumbImg = BitmapUtils.decodeImageAsPng(circleBitmap);
        }
        SharedPreferencesManager.saveMyThumbImg(thumbImg);
        SharedPreferencesManager.saveMyUsername(etUsernameSetup.getText().toString());
        SharedPreferencesManager.savePhoneNumber(FireManager.getPhoneNumber());
        SharedPreferencesManager.saveMyStatus(getString(R.string.default_status));
        SharedPreferencesManager.setAppVersionSaved(true);
        saveCountryCode();

        //show progress while getting user groups
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.show();

        ServiceHelper.fetchUserGroups(this);

    }


    @Subscribe
    public void fetchingGroupsFinished(FetchingUserGroupsFinished event) {
        SharedPreferencesManager.setUserInfoSaved(true);
        progressDialog.dismiss();
        startMainActivity();
    }


    //save country code to shared preferences (see ContactUtils class for more info)
    private void saveCountryCode() {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.createInstance(this);
        Phonenumber.PhoneNumber numberProto;
        try {
            //get the countryName code Like "+1 or +44 etc.." from the user number
            //so if the user number is like +1 444-444-44 we will save only "+1"
            numberProto = phoneUtil.parse(FireManager.getPhoneNumber(), "");
            String countryCode = phoneUtil.getRegionCodeForNumber(numberProto);
            SharedPreferencesManager.saveCountryCode(countryCode);
        } catch (NumberParseException e) {
            e.printStackTrace();
        }
    }

    private void pickImage() {
        CropImageRequest.getCropImageRequest().start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();


                File file = DirManager.getMyPhotoPath();
                try {
                    //copy image to the App Folder
                    FileUtils.copyFile(resultUri.getPath(), file);

                    Glide.with(this).load(file).into(userImgSetup);
                    choosenPhoto = file.getPath();
                    progressBarSetupUserImg.setVisibility(View.GONE);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, R.string.could_not_get_this_image, Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        EventBus.getDefault().register(this);
        super.onResume();
        if (SharedPreferencesManager.isFetchedUserGroups()) {
            if (progressDialog != null)
                progressDialog.dismiss();
            SharedPreferencesManager.setUserInfoSaved(true);
            startMainActivity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
