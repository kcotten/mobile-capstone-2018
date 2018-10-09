package com.iremember.iremember;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class welcome_screen extends AppCompatActivity {
    private static final String TAG = "Hahaha";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        //Wire up the button to do something
        //..get the button
        Button Login = (Button) findViewById(R.id.button);
        //..set what happens when the user clicks
        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"This is iRemember!");
                Toast.makeText(getApplicationContext(),"Check!", Toast.LENGTH_SHORT)
                        .show();
            }

        });
    }
}

