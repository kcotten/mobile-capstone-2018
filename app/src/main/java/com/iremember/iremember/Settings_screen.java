package com.iremember.iremember;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Settings_screen extends AppCompatActivity {
    private static String TAG = "Settings_screen";
    Switch mswitch;
    String mode = "";
    Boolean b;
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String path = "users/" + currentUser.getUid() + "/setting";
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = db.getReference(path);
    private DatabaseReference dbRef2 = db.getReference("users/" + currentUser.getUid());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_screen);
        getMode();
        Toast toast =
            Toast.makeText(this,"Default is Walking",Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
        mswitch = findViewById(R.id.switch1);
        mswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    dbRef.setValue("driving");
                } else {
                    dbRef.setValue("walking");
                }
            }
        });
    }

    private void getMode() {
        dbRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(mode == "") {
                    mode = dataSnapshot.child("setting").getValue(String.class);
                    assert mode != null;
                    b = (mode.equals("driving"));
                    mswitch.setChecked(b);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
}
