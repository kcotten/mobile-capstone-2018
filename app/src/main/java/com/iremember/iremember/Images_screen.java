package com.iremember.iremember;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Images_screen extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 1;
    private static String TAG = "Images_screen";

    private Button mUploadBtn;
    private Button mTakepicture;
    private ImageView mImageView;
    private EditText mDescription;
    private EditText locale;
    private EditText mNotes;
    private Uri CameraUri;
    private String key = List_screen.imagekey;
    File file;


    // Realtimedb Stuff
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String path = "users/" + currentUser.getUid() + "/items/" + key + "/";
    String cameraPath = FirebaseAuth.getInstance().getCurrentUser().getUid();

    // our firebase realtimedb
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = db.getReference(path);

    private FirebaseStorage storage;
    private StorageReference cameraRef;

    String notes, desc;
    String notes2db, desc2db;
    String firebaseImage;
    double lat, lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        //storage = FirebaseStorage.getInstance();
        cameraRef = storageRef.child("/videos/" + cameraPath + "/userIntro.jpg");;

        mTakepicture = (Button)findViewById(R.id.takepicture);
        mUploadBtn = (Button)findViewById(R.id.upload);
        mImageView = (ImageView)findViewById(R.id.imageView3);
        mDescription = (EditText)findViewById(R.id.description);
        mNotes = findViewById(R.id.notes);
        //locale = findViewById(R.id.location);

        mTakepicture.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //file = getOutputMediaFile();
                //CameraUri = Uri.fromFile(file); // create
                //intent.putExtra(MediaStore.EXTRA_OUTPUT, CameraUri);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            }
        });

        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        startListener();

        download(mImageView);
    }

    private File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyApplication");
        /**Create the storage directory if it does not exist*/
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        /**Create a media file name*/
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");

        return mediaFile;
    }

    private void startListener() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    //Log.i(TAG, String.valueOf(dataSnapshot.child("location").child("latitude").getValue(Double.class)));
                    lat = dataSnapshot.child("location").child("latitude").getValue(Double.class);
                    lng = dataSnapshot.child("location").child("longitude").getValue(Double.class);
                    desc = dataSnapshot.child("description").getValue(String.class);
                    notes = dataSnapshot.child("notes").getValue(String.class);
                    firebaseImage = dataSnapshot.child("image").getValue(String.class);
                    try {
                        showPic();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
        if(requestCode == CAMERA_REQUEST_CODE){
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                mImageView.setImageBitmap(imageBitmap);
                encodeBitmapAndSaveToFirebase(imageBitmap);
                Toast.makeText(this, "Picture saved to:\n" + CameraUri, Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Picture snapping cancelled.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to snap picture", Toast.LENGTH_LONG).show(); }
        }
    }

    public void encodeBitmapAndSaveToFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        dbRef.child("image").setValue(imageEncoded);
        //ref.setValue(imageEncoded);
    }

    public void showPic() throws IOException {
        String s = firebaseImage;
        Bitmap imageBitmap = decodeFromFirebaseBase64(firebaseImage);
        File file = new File(String.valueOf(imageBitmap));
        mImageView.setImageBitmap(imageBitmap);

        //Picasso.with(Images_screen.this).load(file).into(mImageView);
    }

    public static Bitmap decodeFromFirebaseBase64(String image) throws IOException {
        byte[] decodedByteArray = android.util.Base64.decode(image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.length);
    }


    private void uploadImage() {
        if(CameraUri != null) {
            UploadTask uploadTask = cameraRef.putFile(CameraUri);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(Images_screen.this, "Upload failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }).addOnSuccessListener(
                new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(Images_screen.this, "Upload complete", Toast.LENGTH_LONG).show();
                }
            }).addOnProgressListener(
                new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    updateProgress(taskSnapshot);
                }
            });
        } else {
            Toast.makeText(this, "Nothing to upload", Toast.LENGTH_LONG).show();
        }
    }

    public void updateProgress(UploadTask.TaskSnapshot taskSnapshot) {
        @SuppressWarnings("VisibleForTests") long fileSize = taskSnapshot.getTotalByteCount();
        @SuppressWarnings("VisibleForTests") long uploadBytes = taskSnapshot.getBytesTransferred();
        long progress = (100 * uploadBytes) / fileSize;

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.pbar);
        progressBar.setProgress((int) progress);
    }

    private void download(final ImageView mImageView) {
        try {
            final File localFile = File.createTempFile("userIntro", "3gp");
            cameraRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(Images_screen.this, "Download complete", Toast.LENGTH_LONG).show();
                    //final ImageView cameraView = (ImageView) findViewById(R.id.imageView);
                    //cameraView.setImageURI(Uri.fromFile(localFile));
                    mImageView.setImageURI(Uri.fromFile(localFile));
                    //cameraView.show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(Images_screen.this, "Download failed: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(Images_screen.this, "Failed to create temp file: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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
