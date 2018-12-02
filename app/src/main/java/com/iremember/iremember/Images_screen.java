package com.iremember.iremember;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Images_screen extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 1;
    private static String TAG = "Images_screen";

    private Button mTakepicture;
    private ImageView mImageView;
    private EditText mDescription;
    private EditText latitude;
    private EditText longitude;
    private EditText mNotes;
    private Uri CameraUri;
    private String key = List_screen.imagekey;
    File file;

    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String path = "users/" + currentUser.getUid() + "/items/" + key + "/";

    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = db.getReference(path);

    String notes, desc;
    String notes2db, desc2db;
    String firebaseImage;
    double lat, lng;
    String mCurrentPhotoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images);

        mTakepicture = findViewById(R.id.takepicture);
        mImageView = findViewById(R.id.imageView3);
        mDescription = findViewById(R.id.description);
        mNotes = findViewById(R.id.notes);
        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);

        mTakepicture.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                dispatchTakePictureIntent();
            }
        });
        startListener();

    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            file = null;
            try {
                file = createImageFile();
            } catch (IOException ex) {
            }
            if (file != null) {
                CameraUri = FileProvider.getUriForFile(this,
                        "com.iremember.iremember.android.fileprovider",
                        file);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, CameraUri);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
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
                    latitude.setText(String.format("%.2f", lat));
                    longitude.setText(String.format("%.2f", lng));

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
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), CameraUri);
                    mImageView.setImageBitmap(bitmap);
                    encodeBitmapAndSaveToFirebase(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Picture snap cancelled.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to snap picture", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void encodeBitmapAndSaveToFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        dbRef.child("image").setValue(imageEncoded);
    }

    public void showPic() throws IOException {
        String s = firebaseImage;
        Bitmap imageBitmap = decodeFromFirebaseBase64(s);
        mImageView.setImageBitmap(imageBitmap);
    }

    public static Bitmap decodeFromFirebaseBase64(String image) throws IOException {
        byte[] decodedByteArray = android.util.Base64.decode(image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.length);
    }

    public void saveEdits(View view) {
        desc2db = mDescription.getText().toString();
        notes2db = mNotes.getText().toString();
        lat = Double.parseDouble(latitude.getText().toString());
        lng = Double.parseDouble(longitude.getText().toString());
        LatLng latLng = new LatLng(lat,lng);

        dbRef.child("location").setValue(latLng);
        dbRef.child("description").setValue(desc2db);
        dbRef.child("notes").setValue(notes2db);
        finishAffinity(this);
    }

    private void finishAffinity(Images_screen images_screen) {
        finish();
    }
}
