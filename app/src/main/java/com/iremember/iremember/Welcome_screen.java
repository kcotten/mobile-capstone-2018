package com.iremember.iremember;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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

    private static final String TAG = "Welcome_screen";
    private static int RC_SIGN_IN = 100;
    private static final String TAG1 = "RealtimeDB";

    private FirebaseAuth.AuthStateListener AuthListener;
    private FirebaseAuth Auth;
    FirebaseDatabase db;
    private DatabaseReference dbRef;
    FirebaseUser currentUser;
    String name;
    String firebase_email;
    String uid;
    public static String mode;
    TextView email;
    TextView userName;
    TextView welcome;
    Button login_button;
    Button logout_button;
    Button record_location;
    Button map;
    Button tracked_items_list;
    FloatingActionButton settings;
    FloatingActionButton userLogout;
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);
        login_button = findViewById(R.id.button1);
        logout_button = findViewById(R.id.button2);
        record_location = findViewById(R.id.button3);
        map = findViewById(R.id.button4);
        tracked_items_list = findViewById(R.id.button5);
        email = findViewById(R.id.email);
        userName = findViewById(R.id.user);
        settings = findViewById(R.id.floatingActionButton);
        userLogout = findViewById(R.id.floatingActionButton2);
        welcome = findViewById(R.id.welcomeMessage);
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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button3:
                break;
            case R.id.button4:
                Intent intent = new Intent(this, Map_screen.class);
                startActivity(intent);
                break;
            case R.id.button5:
                Intent i = new Intent(this, List_screen.class);
                startActivity(i);
                break;
            default:
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
            settings.hide();
            userLogout.hide();
            welcome.setText("Welcome");
        } else {
            login_button.setVisibility(View.GONE);
            logout_button.setVisibility(View.GONE);
            email.setVisibility(View.VISIBLE);
            userName.setVisibility(View.GONE);
            record_location.setVisibility(View.GONE);
            map.setVisibility(View.VISIBLE);
            tracked_items_list.setVisibility(View.VISIBLE);
            settings.show();
            userLogout.show();

            userName.setText(user.getDisplayName());
            email.setText(user.getEmail());
            welcome.setText("Hello!");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
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

    public void signIn(View v) {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
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

    @Override
    protected void onStart() {
        super.onStart();
        Auth.addAuthStateListener(AuthListener);
    }

    public void writeUserData(String name, String firebase_email) {
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference();
        final User user = new User(name, firebase_email);

        dbRef.child("users").child(uid).addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {

                    } else {
                        dbRef.child("users").child(uid).setValue(user);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            }
        );
    }

    public void settings(View view) {
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
        }

        User(String username, String email) {
            this.username = username;
            this.email = email;
            this.setting = "walking";
        }
    }
}
