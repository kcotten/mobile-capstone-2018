package com.iremember.iremember;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Map_screen extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "Map_screen";
    private static final int LOCATION_REQUEST_CODE = 101;

    private GoogleMap mMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_screen);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        UiSettings mapSettings;
        mapSettings = mMap.getUiSettings();

        if (mMap != null) {
            int permission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);

            if (permission == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_REQUEST_CODE);
            }
        }

        mapSettings.setZoomControlsEnabled(true);
        mapSettings.setCompassEnabled(true);

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        LatLng UCSC = new LatLng(36.9916, -122.0583);
        Marker ucsc = mMap.addMarker(new MarkerOptions()
                .position(UCSC)
                .title("UCSC")
                .snippet("University of California Santa Cruz"));
        // mMap.moveCamera(CameraUpdateFactory.newLatLng(UCSC));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(UCSC,13));
    }

    protected void requestPermission(String permissionType,
                                     int requestCode) {

        ActivityCompat.requestPermissions(this,
                new String[]{permissionType}, requestCode
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this,
                            "Unable to show location - permission required",
                            Toast.LENGTH_LONG).show();
                } else {

                    SupportMapFragment mapFragment =
                            (SupportMapFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);
                }
            }
        }
    }
}
