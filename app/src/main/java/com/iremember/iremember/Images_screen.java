package com.iremember.iremember;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.UUID;

public class Images_screen extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE=1;
    private static String TAG = "Images_screen";

    private Button mUploadBtn;
    private Button mTakepicture;
    private ImageView mImageView;
    private EditText mDescription;
    private EditText locale;
    private EditText mNotes;
    private Uri filePath;
    private String key = List_screen.imagekey;



    // Realtimedb Stuff
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String path = "users/" + currentUser.getUid() + "/items/" + key + "/";

    // our firebase realtimedb
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = db.getReference(path);

    private FirebaseStorage storage;
    private StorageReference storageReference;

    String notes, desc;
    String notes2db, desc2db;
    double lat, lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        mTakepicture = (Button)findViewById(R.id.takepicture);
        mUploadBtn = (Button)findViewById(R.id.upload);
        mImageView = (ImageView)findViewById(R.id.imageView3);
        mDescription = (EditText)findViewById(R.id.description);
        mNotes = findViewById(R.id.notes);
        locale = findViewById(R.id.location);

        mTakepicture.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            }
        });
        mUploadBtn.setOnClickListener(
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        startListener();
    }

    private void startListener() {
        dbRef.addValueEventListener(new ValueEventListener() {
            /*
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.exists()) {
                    Log.i(TAG, String.valueOf(dataSnapshot.child("location").child("latitude").getValue(Double.class)));
                    //lat = dataSnapshot.child("location").child("latitude").getValue(Double.class);
                    //lng = dataSnapshot.child("location").child("longitude").getValue(Double.class);
                    desc = dataSnapshot.child("description").getValue(String.class);
                    //Log.i(TAG, desc);

                    notes = dataSnapshot.child("notes").getValue(String.class);
                    mDescription.setText(desc);
                    mNotes.setText(notes);

                } else {
                    dbRef.removeEventListener(this);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            */
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    //Log.i(TAG, String.valueOf(dataSnapshot.child("location").child("latitude").getValue(Double.class)));
                    lat = dataSnapshot.child("location").child("latitude").getValue(Double.class);
                    lng = dataSnapshot.child("location").child("longitude").getValue(Double.class);
                    desc = dataSnapshot.child("description").getValue(String.class);
                    notes = dataSnapshot.child("notes").getValue(String.class);
                    locale.setText("Latitude: "+String.format("%.2f", lat)+", Longitude: "+String.format("%.2f", lng));
                    mDescription.setText(desc);
                    mNotes.setText(notes);

                } else {
                    dbRef.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data!= null && data.getData() !=null){
            filePath = data.getData();

            /*StorageReference filepath = mStorage.child("Photos").child(filePath.getLastPathSegment());

            /filepath.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                    Uri downloadUri = taskSnapshot.getStorage().getDownloadUrl().getResult();
                    Picasso.get().load(downloadUri).fit().into(mImageView);

                    Toast.makeText(Images.this,"Uploading Finished ...", Toast.LENGTH_LONG).show();
                }
            });*/
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                mImageView.setImageBitmap(bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    private void uploadImage() {

        if(filePath != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(Images_screen.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(Images_screen.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        desc2db = mDescription.getText().toString();
        notes2db = mNotes.getText().toString();

        dbRef.child("description").setValue(desc2db);
        dbRef.child("notes").setValue(notes2db);
    }
}
