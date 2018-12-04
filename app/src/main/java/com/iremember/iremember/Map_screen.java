package com.iremember.iremember;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Map_screen extends FragmentActivity implements
        OnMapReadyCallback,
        LocationListener {

    // constants
    private static final String TAG = "Map_screen";
    private static final int LOCATION_REQUEST_CODE = 101;
    private static final float DEFAULT_ZOOM = 13;
    public static final int PATTERN_DASH_LENGTH_PX = 20;
    public static final int PATTERN_GAP_LENGTH_PX = 20;
    public static final PatternItem DOT = new Dot();
    public static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    public static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    public static final List<PatternItem> PATTERN_POLYGON_ALPHA = Arrays.asList(GAP, DASH);

    // instantiate a local map object, location, and location manager
    public GoogleMap mMap;
    private Location mLastLocation;
    private LocationManager locationManager;
    public Criteria criteria;
    public String bestProvider;

    double latitude;
    double longitude;

    double marker_latitude;
    double marker_longitude;

    LatLng marker_latlng;

    Polyline line;

    private String locationName = "";
    private String key = "";
    private String API_KEY = "AIzaSyA1xDIXPZDyoF8vFStlA89kAv0eXdbp8NQ";

    // bool to help us identify whether permissions are granted, not currently in use
    // private boolean mLocationPermissionGranted;

    // our firebase user
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String path = "users/" + currentUser.getUid() + "/items";

    // our firebase realtimedb
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = db.getReference(path);
    private DatabaseReference dbRef2 = db.getReference("users/" + currentUser.getUid());
    ChildEventListener childref;

    String mode= "";
    Button recordLocBtn;
    Button directionsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_screen);
        recordLocBtn = findViewById(R.id.record);
        directionsBtn = findViewById(R.id.directions);
        recordLocBtn.getBackground().setAlpha(128);
        directionsBtn.getBackground().setAlpha(128);

        // get a local instance of the location manager, this is critical
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // this may or may not be needed due to the same call in request permissions?
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        getMode();
        //mode = Welcome_screen.mode;

        // test function, is the provider even enabled?
        // boolean test = locationManager.isProviderEnabled(locationManager.NETWORK_PROVIDER);
        Log.i(TAG, "onCreate() " /*+ String.valueOf(test)*/);
    }

    private void getMode() {
        dbRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(mode.equals("")) {
                    mode = dataSnapshot.child("setting").getValue(String.class);
                    assert mode != null;
                    //Log.i(TAG, mode);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady()");
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Get the location
        getLocation(googleMap);

        // get hold of the settings for the map and then set them
        UiSettings mapSettings;
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);
        mapSettings.setCompassEnabled(true);
        mapSettings.setMapToolbarEnabled(true); // forward to directions

        // -----------------------------------------------------------------------------------------
        childref = dbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.exists()) {
                    double tlat = 0;
                    double tlng = 0;
                    if(dataSnapshot.child("location").child("latitude").getValue(Double.class) != null) {
                        tlat = dataSnapshot.child("location").child("latitude").getValue(Double.class);
                    }
                    if(dataSnapshot.child("location").child("longitude").getValue(Double.class) != null) {
                        tlng = dataSnapshot.child("location").child("longitude").getValue(Double.class);
                    }
                    if(tlat != 0 && tlng != 0) {
                        LatLng newLocation = new LatLng(tlat, tlng);
                        mMap.addMarker(new MarkerOptions()
                                .position(newLocation)
                                .title(dataSnapshot.child("description").getValue(String.class)));
                    }

                } else {
                    dbRef.removeEventListener(this);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
        // -----------------------------------------------------------------------------------------

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker_latitude = marker.getPosition().latitude;
                marker_longitude = marker.getPosition().longitude;

                marker_latlng = new LatLng(marker_latitude, marker_longitude);
                //Log.i(TAG,marker_latlng.toString());
                return false;
            }
        });
    }

    // find the location
    public void getLocation(GoogleMap mMap) {
        locationManager = (LocationManager)  this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

        if (mMap != null) {
            int permission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                // permission has been granted so begin setup for the map
                // mLocationPermissionGranted = true;
                mMap.setMyLocationEnabled(true);
                mLastLocation = getLastKnownLocation();
                if (mLastLocation != null) {
                    latitude = mLastLocation.getLatitude();
                    longitude = mLastLocation.getLongitude();
                    LatLng latLng = new LatLng(latitude, longitude);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,DEFAULT_ZOOM));
                } else {
                    locationManager.requestLocationUpdates(bestProvider, 1000, 0, (LocationListener) this);
                }

            } else {
                requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_REQUEST_CODE);
            }
        }

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
        getMarkerName();
        // String locProv;
        // check for permissions per the IDE even though permissions are guaranteed to be set
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        // -----------------------------------------------------------------------------------------
        // lay down marker
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Current Location"));

        // the code right here will create unique ids and then save multiple items
        key = dbRef.push().getKey();
        assert key != null;
        dbRef.child(key).child("location").setValue(latLng);
        dbRef.child(key).child("notes").setValue("");
        dbRef.child(key).child("image").setValue("");

        Log.i(TAG, "recordLocation()" + String.valueOf(lat) + " " +
                String.valueOf(lng));
    }

    private void getMarkerName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Description");

        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                locationName = input.getText().toString();
                dbRef.child(key).child("description").setValue(locationName);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbRef.child(key).removeValue();
                dialog.cancel();
            }
        });

        builder.show();
    }


    // here, at time 2, generate the directions back to the saved location
    public void generateDirections(View view) {
        if(marker_latlng == null) {
            Toast toast =
                    Toast.makeText(this,"Must have destination marker selected.",Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            return;
        }
        mLastLocation = getLastKnownLocation();

        double lat = mLastLocation.getLatitude();
        double lng = mLastLocation.getLongitude();
        LatLng latLng = new LatLng(lat, lng);

        String url = getDirectionsUrl(latLng, marker_latlng);

        DownloadTask downloadTask = new DownloadTask();

        downloadTask.execute(url);


        Log.i(TAG, "generateDirections()");
    }

    private Location getLastKnownLocation() {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
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

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
        dbRef.removeEventListener(childref);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
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

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        String str_origin = "origin="+origin.latitude+","+origin.longitude;
        String str_dest = "destination="+dest.latitude+","+dest.longitude;
        String str_mode = "mode="+mode;
        String parameters = str_origin+"&"+str_dest+"&"+str_mode;
        String output = "json";

        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters+"&key="+API_KEY;
        Log.i(TAG,url);
        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d(TAG, e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsParser parser = new DirectionsParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            ArrayList<LatLng> points = new ArrayList<LatLng>();
            PolylineOptions lineOptions = new PolylineOptions();

            for (int i = 0; i < result.size(); i++) {
                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(16);
                lineOptions.color(Color.RED);
                if(mode.equals("walking"))
                    lineOptions.pattern(PATTERN_POLYGON_ALPHA);
            }
            addLinesToMap(lineOptions);

        }
    }

    public void addLinesToMap(final PolylineOptions lineOptions) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                line = mMap.addPolyline(lineOptions);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}
