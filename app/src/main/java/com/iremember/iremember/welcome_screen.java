package com.iremember.iremember;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import android.app.ProgressDialog;

import java.util.Arrays;
import java.util.List;

class Welcome_screen extends AppCompatActivity {

    private static final String TAG = "Welcome_screen";
    private static int RC_SIGN_IN = 100;
    private FirebaseAuth.AuthStateListener AuthListener;
    private FirebaseAuth Auth;
    TextView email;
    TextView userName;
    Button button1;
    Button button2;

    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        for (String provider : AuthUI.SUPPORTED_PROVIDERS) {
            Log.i(this.getClass().getName(), provider);
        }

        Auth     = FirebaseAuth.getInstance();
        button1  = findViewById(R.id.button1);
        button2  = findViewById(R.id.button2);
        email    = findViewById(R.id.email);
        userName = findViewById(R.id.user);

        AuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                updateUI();
            }
        };
        Log.i(TAG, "onCreate");
    }

    private void updateUI() {
        FirebaseUser user = Auth.getCurrentUser();
        if(user == null) {
            button1.setVisibility(View.VISIBLE);
            button2.setVisibility(View.GONE);
            email.setVisibility(View.GONE);
            // change password to user?
            userName.setVisibility(View.GONE);

        } else {
            button1.setVisibility(View.GONE);
            button2.setVisibility(View.VISIBLE);
            email.setVisibility(View.VISIBLE);
            // change password to user?
            userName.setVisibility(View.VISIBLE);

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
                        Toast.makeText(Welcome_screen.this, "signed out succesfully ... ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

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
}
