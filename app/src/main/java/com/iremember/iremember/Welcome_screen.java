package com.iremember.iremember;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
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
    private static final String TAG1 = "RealtimeDB";

    // firebase auth
    private FirebaseAuth.AuthStateListener AuthListener;
    private FirebaseAuth Auth;

    // firebase database
    FirebaseDatabase db;
    private DatabaseReference dbRef;
    FirebaseUser currentUser;
    String name;
    String firebase_email;
    String uid;
    public static String mode;

    // UI elements
    TextView email;
    TextView userName;
    Button login_button;
    Button logout_button;
    Button record_location;
    Button map;
    Button tracked_items_list;

    // add support for login here, warning is because we only implement a single sign in method
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build());

    // really busy initialization, could maybe be broken down into helper functions
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

        map.setOnClickListener(this);
        tracked_items_list.setOnClickListener(this);
        record_location.setOnClickListener(this);

        // firebase, instantiate Auth and then create a listener to call our updateUI
        Auth = FirebaseAuth.getInstance();
        AuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                updateUI();
            }
        };

        Log.i(TAG, "onCreate");
    }

    // button handling
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button3:
                Log.i("button", "5, record_location");

                break;
            case R.id.button4:
                Log.i("button", "3, map");
                Intent intent = new Intent(this, Map_screen.class);
                startActivity(intent);
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

    // update UI helper function, hide auth required items if not logged in
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
        } else {
            login_button.setVisibility(View.GONE);
            logout_button.setVisibility(View.VISIBLE);
            email.setVisibility(View.VISIBLE);
            userName.setVisibility(View.GONE);
            record_location.setVisibility(View.GONE);
            map.setVisibility(View.VISIBLE);
            tracked_items_list.setVisibility(View.VISIBLE);

            userName.setText(user.getDisplayName());
            email.setText(user.getEmail());
            // load profile image? have to follow a whole process
            // Picasso.with(ActivityFUIAuth.this).load(user.getPhotoUrl()).into(imgProfile);
        }
    }

    // is the user signed in? update the UI accordingly
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                Log.i(TAG,"User signed in with " + (response != null ? response.getProviderType() : null));

                currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    name = currentUser.getDisplayName();
                    firebase_email = currentUser.getEmail();
                    uid = currentUser.getUid();
                }

                writeUserData(name, firebase_email);

                updateUI();
            } else {
                updateUI();
            }
        }
    }

    // user sign in for firebaseAuth
    public void signIn(View v) {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    // user can delete account from the firebase, not implemented in the UI
    public void deleteAccount(View v) {
        AuthUI.getInstance()
                .delete(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
        });

    }

    // sign out of firebase auth
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
    // toast helper function is we need it
    private void notifyUser(String message) {
        Toast.makeText(this, message,
                Toast.LENGTH_SHORT).show();
    }
    */
    // lifecycle methods, originally did some bundle stuff here but most of that code got removed

    // Start the firebase authorization listener
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

    // firebase helper function
    public void writeUserData(String name, String firebase_email) {
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference();
        final User user = new User(name, firebase_email);

        dbRef.child("users").child(uid).addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Log.i(TAG1, "user exists");
                        // User Exists
                        // Do your stuff here if user already exits
                    } else {
                        Log.i(TAG1, "user created");
                        dbRef.child("users").child(uid).setValue(user);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                }
            }
        );

    }

    public void settings(View view) {
        Log.i("Floating Action Button", "Settings_screen Launched");
        Intent intent = new Intent(this, Settings_screen.class);
        startActivity(intent);
    }

    @IgnoreExtraProperties
    public class User {
        String username;
        public String email;
        public String setting;

        public User() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
            Log.i(TAG, "user constructor called");
        }

        User(String username, String email) {
            this.username = username;
            this.email = email;
            this.setting = "walking";
        }

    }
}
