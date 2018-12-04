package com.iremember.iremember;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class List_screen extends AppCompatActivity implements
        LocationListener {
    // constants
    private static final int REQUEST_CODE = 1;
    private static final String TAG = "List_screen";
    private static final int LOCATION_REQUEST_CODE = 101;
    private static final float DEFAULT_ZOOM = 13;

    // UI elements
    private ListView dataListView;
    private EditText itemText;
    private Button findButton;
    private Button deleteButton;
    private Boolean searchMode = false;
    private Boolean itemSelected = false;
    // iterator
    private int selectedPosition = 0;

    // firebase User path for storage
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String path = "users/" + (currentUser != null ? currentUser.getUid() : null) + "/items";

    // get access to the firebase at path
    private FirebaseDatabase db= FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = db.getReference(path);

    // the list!
    ArrayList<String> listItems = new ArrayList<>();
    ArrayList<String> listKeys = new ArrayList<>();
    ArrayAdapter<String> adapter;

    // need a location when an item is created
    private Location mLastLocation;
    private LocationManager locationManager;
    public Criteria criteria;
    public String bestProvider;
    double latitude;
    double longitude;
    LatLng latLng;
    private String specialkey;
    static public String imagekey = "";
    private int key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_screen);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        getLocation();

        Log.i(TAG, path);

        dataListView = findViewById(R.id.dataListView);
        itemText = findViewById(R.id.itemText);
        findButton = findViewById(R.id.findBtn);
        deleteButton = findViewById(R.id.deleteBtn);
        deleteButton.setEnabled(false);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, listItems);
        dataListView.setAdapter(adapter);
        dataListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        dataListView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                imagekey = listKeys.get(position);
                selectedPosition = position; itemSelected = true;
                deleteButton.setEnabled(true);
            }
        });

        dataListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

                if (ContextCompat.checkSelfPermission(List_screen.this,
                        permissions[0]) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(List_screen.this,
                        permissions[1]) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(List_screen.this,
                        permissions[2]) == PackageManager.PERMISSION_GRANTED) {
                    imagekey = listKeys.get(position);
                    Log.i(TAG, imagekey);
                    Intent intent = new Intent(view.getContext(), Images_screen.class);
                    startActivity(intent);
                } else {
                    ActivityCompat.requestPermissions(List_screen.this, permissions, REQUEST_CODE);
                }
                return false;
            }

        });


    }

    // search for items in the list
    @SuppressLint("SetTextI18n")
    public void findItems(View view) {
        Query query;

        if (!searchMode) {
            findButton.setText("Clear");
            query = dbRef.orderByChild("description").equalTo(itemText.getText().toString());
            searchMode = true;
        } else {
            searchMode = false;
            findButton.setText("Search");
            query = dbRef.orderByKey();
        }

        if (itemSelected) {
            dataListView.setItemChecked(selectedPosition, false);
            itemSelected = false;
            deleteButton.setEnabled(false);
        }

        query.addListenerForSingleValueEvent(queryValueListener);
    }

    // here we listen for search in the realtimeDB
    ValueEventListener queryValueListener = new ValueEventListener() {
        @Override public void onDataChange(DataSnapshot dataSnapshot) {
            Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
            Iterator<DataSnapshot> iterator = snapshotIterator.iterator();
            adapter.clear();
            listKeys.clear();
            while (iterator.hasNext()) {
                DataSnapshot next = iterator.next();
                String match = next.child("description").getValue(String.class);
                String key = next.getKey();
                listKeys.add(key);
                adapter.add(match);
            }
        }

        @Override public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    // add an item to the list
    public void addItem(View view) {
        String item = itemText.getText().toString();
        latLng = new LatLng(latitude, longitude);
        if(TextUtils.isEmpty(item)) {
            itemText.setError("Item cannot be empty");
            return;
        }
        specialkey = dbRef.push().getKey();
        itemText.setText("");
        assert specialkey != null;

        dbRef.child(specialkey).child("description").setValue(item);

        adapter.notifyDataSetChanged();

        dbRef.child(specialkey).child("notes").setValue("");
        dbRef.child(specialkey).child("image").setValue("");
        dbRef.child(specialkey).child("location").setValue(latLng);
    }

    // delete an item from the list
    public void deleteItem(View view) {
        dataListView.setItemChecked(selectedPosition, false);
        dbRef.child(listKeys.get(selectedPosition)).removeValue();
    }

    // listen for changes in the list
    private void addChildEventListener() {
        ChildEventListener childListener = new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                adapter.add(dataSnapshot.child("description").getValue(String.class));
                listKeys.add(dataSnapshot.getKey());
            }

            @Override public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
                adapter.notifyDataSetChanged();
            }

            @Override public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {

            }

            @Override public void onChildRemoved(DataSnapshot dataSnapshot) throws IndexOutOfBoundsException {
                String key = dataSnapshot.getKey();
                int index = listKeys.indexOf(key);

                if (index != -1) {
                    listItems.remove(index);
                    listKeys.remove(index);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        dbRef.addChildEventListener(childListener);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        latLng = new LatLng(latitude, longitude);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //adapter.notifyDataSetChanged();
        adapter.clear();
        addChildEventListener();
    }

    // find the location
    public void getLocation() {
        locationManager = (LocationManager)  this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true));

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = getLastKnownLocation();
            if (mLastLocation != null) {
                latitude = mLastLocation.getLatitude();
                longitude = mLastLocation.getLongitude();
            } else {
                locationManager.requestLocationUpdates(bestProvider, 1000, 0, this);
            }

        } else {
            requestPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    LOCATION_REQUEST_CODE);
        }
    }

    // manually find the best location provider, auto was not working
    private Location getLastKnownLocation() {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            // this is not an error and is safe to ignore as far as I, kris, knows
            // all the documentation states that this function is guaranteed to be non-null
            @SuppressLint("MissingPermission") Location l = locationManager.getLastKnownLocation(provider);
            Log.d("last loc, p: %s, l: %s", provider + " " + l);

            if (l == null) {
                continue;
            }
            if (bestLocation == null
                    || l.getAccuracy() < bestLocation.getAccuracy()) {
                Log.d("best last location: %s", " " + l);
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return bestLocation;
    }

    // request permissions
    protected void requestPermission(String permissionType, int requestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionType}, requestCode
        );
    }

    // after the user is done interacting with the activity, handle the results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            "Unable to show location - permission required",
                            Toast.LENGTH_LONG).show();
                } else {
                    // permissions are set, therefore...
                }
            }
        }
    }

    public void editEntry(View view) {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

        if (ContextCompat.checkSelfPermission(List_screen.this,
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(List_screen.this,
                permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(List_screen.this,
                permissions[2]) == PackageManager.PERMISSION_GRANTED) {
            if(TextUtils.isEmpty(imagekey)) {
                Toast toast =
                        Toast.makeText(this,"Must select an item.",Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
                return;
            }
            Log.i(TAG, imagekey);
            Intent intent = new Intent(view.getContext(), Images_screen.class);
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(List_screen.this, permissions, REQUEST_CODE);
        }
    }
}
