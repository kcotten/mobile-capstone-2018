package com.iremember.iremember;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class Map_screen extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnMyLocationButtonClickListener {

    // constants
    private static final String TAG = "Map_screen";
    private static final int LOCATION_REQUEST_CODE = 101;
    private static final float DEFAULT_ZOOM = 13;

    // instantiate a local map object, location, and location manager
    public GoogleMap mMap;
    private Location mLastLocation;
    private LocationManager locationManager;

    // bool to help us identify whether permissions are granted, not currently in use
    // private boolean mLocationPermissionGranted;

    // our firebase user
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String path = "users/" + currentUser.getUid() + "/location";

    // our firebase realtimedb
    private FirebaseDatabase db= FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = db.getReference(path);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_screen);

        // get a local instance of the location manager, this is critical
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // this may or may not be needed due to the same call in request permissions?
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // test function, is the provider even enabled?
        // boolean test = locationManager.isProviderEnabled(locationManager.NETWORK_PROVIDER);
        Log.i(TAG, "onCreate() " /*+ String.valueOf(test)*/);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (mMap != null) {
            int permission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                // permission has been granted so begin setup for the map
                // mLocationPermissionGranted = true;
                mMap.setMyLocationEnabled(true);
                mLastLocation = getLastKnownLocation();
                double latitude = mLastLocation.getLatitude();
                double longitude = mLastLocation.getLongitude();

                LatLng latLng = new LatLng(latitude, longitude);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,DEFAULT_ZOOM));
            } else {
                requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_REQUEST_CODE);
            }
        }

        // get hold of the settings for the map and then set them
        UiSettings mapSettings;
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);
        mapSettings.setCompassEnabled(true);

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
    }

    // the my location button in the upper right of the screen
    @Override
    public void onMyLocationClick(@NonNull Location location) {
        double lat, lng;
        lat = location.getLatitude();
        lng = location.getLongitude();
        Toast.makeText(this, "Current location:\n" + String.valueOf(lat) + " " +
                String.valueOf(lng), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // which is the camera animates to the user's current position
        return false;
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
                    SupportMapFragment mapFragment =
                            (SupportMapFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.map);
                    assert mapFragment != null;
                    mapFragment.getMapAsync(this);
                }
            }
        }
    }

    // save a location to the firebase, may also want to add a pin
    public void recordLocation(View view) {
        // String locProv;
        // check for permissions per the IDE even though permissions are guaranteed to be set
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    LOCATION_REQUEST_CODE);
            return;
        }
        mLastLocation = getLastKnownLocation();

        // change the firebase to accept a LatLng instead of a location
        double lat = mLastLocation.getLatitude();
        double lng = mLastLocation.getLongitude();
        LatLng latLng = new LatLng(lat, lng);

        // send to firebase
        String key = dbRef.push().getKey();
        assert key != null;
        dbRef.child(key).child("current location").setValue(mLastLocation);

        Log.i(TAG, "recordLocation()" + String.valueOf(lat) + " " +
                String.valueOf(lng));
    }

    // here, at time 2, generate the directions back to the saved location
    public void generateDirections(View view) {
        // code here
        Log.i(TAG, "generateDirections()");
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
}
