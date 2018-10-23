package com.iremember.iremember;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class Welcome_screen extends AppCompatActivity implements View.OnClickListener {

    // constants
    private static final String TAG = "Welcome_screen";
    private static int RC_SIGN_IN = 100;
    private static FirebaseUser currentUser;
    private static final String TAG1 = "RealtimeDB";


    // firebase auth
    private FirebaseAuth.AuthStateListener AuthListener;
    private FirebaseAuth Auth;

    // firebase database
    FirebaseDatabase db;
    private DatabaseReference dbRef;
    String name;
    String firebase_email;
    Uri photoUrl;
    String uid;

    // UI elements
    TextView email;
    TextView userName;
    Button login_button;
    Button logout_button;
    Button record_location;
    Button map;
    Button tracked_items_list;
    // private EditText userText;

    // add support for logins here
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        for (String provider : AuthUI.SUPPORTED_PROVIDERS) {
            Log.i(this.getClass().getName(), provider);
        }

        login_button = findViewById(R.id.button1);
        logout_button = findViewById(R.id.button2);
        record_location = findViewById(R.id.button3);
        map = findViewById(R.id.button4);
        tracked_items_list = findViewById(R.id.button5);
        email = findViewById(R.id.email);
        userName = findViewById(R.id.user);
        // userText = findViewById(R.id.userText);

        map.setOnClickListener(this);
        tracked_items_list.setOnClickListener(this);
        record_location.setOnClickListener(this);

        Auth = FirebaseAuth.getInstance();
        AuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                updateUI();
            }
        };




        //db = FirebaseDatabase.getInstance();
        //dbRef = db.getReference();
        //dbRef.addValueEventListener(changeListener);

        Log.i(TAG, "onCreate");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button3:
                Log.i("button", "5, record_location");

                break;
            case R.id.button4:
                Log.i("button", "3, map");
                //Intent intent = new Intent(this, Map_screen.class);
                //startActivity(intent);
                break;
            case R.id.button5:
                Log.i("button", "4, tracked_item_list");
                Intent i = new Intent(this, List_screen.class);
                startActivity(i);
                break;
            default:
                Log.i("button","unknown input");
                break;
        }
    }

    private void updateUI() {
        FirebaseUser user = Auth.getCurrentUser();
        if(user == null) {
            login_button.setVisibility(View.VISIBLE);
            logout_button.setVisibility(View.GONE);
            email.setVisibility(View.GONE);
            userName.setVisibility(View.GONE);
            record_location.setVisibility(View.GONE);
            map.setVisibility(View.GONE);
            tracked_items_list.setVisibility(View.GONE);
            //userText.setVisibility(View.GONE);
        } else {
            login_button.setVisibility(View.GONE);
            logout_button.setVisibility(View.VISIBLE);
            email.setVisibility(View.VISIBLE);
            userName.setVisibility(View.GONE);
            record_location.setVisibility(View.VISIBLE);
            map.setVisibility(View.VISIBLE);
            tracked_items_list.setVisibility(View.VISIBLE);
            //userText.setVisibility(View.VISIBLE);

            userName.setText(user.getDisplayName());
            email.setText(user.getEmail());
            // load profile image? have to follow a whole process
            // Picasso.with(ActivityFUIAuth.this).load(user.getPhotoUrl()).into(imgProfile);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                Log.i(TAG,"User signed in with " + response.getProviderType());

                currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    // Name, email address, and profile photo Url
                    name = currentUser.getDisplayName();
                    firebase_email = currentUser.getEmail();
                    // photoUrl = currentUser.getPhotoUrl();
                    // Check if user's email is verified
                    // boolean emailVerified = currentUser.isEmailVerified();
                    // The user's ID, unique to the Firebase project. Do NOT use this value to
                    // authenticate with your backend server, if you have one. Use
                    // FirebaseUser.getIdToken() instead.
                    uid = currentUser.getUid();
                }

                writeUserData(name, firebase_email);

                updateUI();
            } else {
                updateUI();
            }
        }
    }

    public void signIn(View v) {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    public void deleteAccount(View v) {
        AuthUI.getInstance()
                .delete(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
        });

    }

    public void signOut(View view) {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(Welcome_screen.this, "Signed Out successfully...",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /*
    public void checkEmail(View v) {
        Auth.fetchProvidersForEmail()
    }
    */

    /*
    ValueEventListener changeListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            String change = dataSnapshot.child(
                    currentUser.getUid()).child("message")
                    .getValue(String.class);

            userText.setText(change);
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
            notifyUser("Database error: " + databaseError.toException());
        }
    };
    */

    private void notifyUser(String message) {
        Toast.makeText(this, message,
                Toast.LENGTH_SHORT).show();
    }

    /*
    public void saveData(View view) {
        dbRef.child(currentUser.getUid()).child("message")
                .setValue(userText.getText().toString(), completionListener);
    }
    */

    DatabaseReference.CompletionListener completionListener =
            new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError,
                                       DatabaseReference databaseReference) {

                    if (databaseError != null) {
                        notifyUser(databaseError.getMessage());
                    }
                }
            };

    @Override
    protected void onStart() {
        super.onStart();
        Auth.addAuthStateListener(AuthListener);

        Log.i(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(TAG, "onRestoreInstanceState");
    }

    public void writeUserData(String name, String firebase_email) {
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference();
        final User user = new User(name, firebase_email);

        dbRef.child("users").child(uid).addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // User Exists
                        // Do your stuff here if user already exits
                    } else {
                        dbRef.child("users").child(uid).setValue(user);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                }
            }
        );

    }



    @IgnoreExtraProperties
    public class User {

        public String username;
        public String email;

        //public String items;
        // public List<> locations;

        public User() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public User(String username, String email) {
            this.username = username;
            this.email = email;
            //this.items = "Remembered Items";
        }

    }
}
