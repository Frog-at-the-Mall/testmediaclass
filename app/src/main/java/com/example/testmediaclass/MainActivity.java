package com.example.testmediaclass;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.util.LogPrinter;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;

//is activity && implements SensorListeners for compass i think

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //for something
    int PERMISSION_ID = 44;

    //init sensor
    private SensorManager SensorManage;

    // initializing FusedLocationProviderClient object
    FusedLocationProviderClient mFusedLocationClient;

    // Initializing text views
    TextView headingTextView;
    TextView latitudeTextView, longitudeTextView;
    TextView relativeBearingTextView;

    //init compass image view
    private ImageView compassImage;
    private float DegreeStart = 0f;

    //..--init music player--//
    MediaPlayer player;

    ///init dummy destination
    Location dest = getDest();


    //implementing volley
    interface Listener{
        void response(String string);
    }
    RequestQueue mQueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instance your android device sensor capabilities
        SensorManage = (SensorManager) getSystemService(SENSOR_SERVICE);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        //.----gps & compass textview instancing -----..///
        headingTextView = (TextView) findViewById(R.id.headingTextView);
        latitudeTextView =(TextView) findViewById(R.id.latTextView);
        longitudeTextView =(TextView)findViewById(R.id.lonTextView);

        relativeBearingTextView = (TextView) findViewById(R.id.relativeBearingTextView);

        compassImage = (ImageView) findViewById(R.id.image);

        //..---instance music guide  + start playing music ----.////
        player = MediaPlayer.create(this, R.raw.the_waxen_pith);
        player.start();


        //volley buttons + other stuff
        final Button btn1 = (Button) findViewById(R.id.btn1);
        final Button btn2 = (Button) findViewById(R.id.btn2);
        final TextView textView = (TextView) findViewById(R.id.textView);

        mQueue = Volley.newRequestQueue(this);


        //click button start request
        btn1.setOnClickListener(view -> {
            btn1.setEnabled(false);
            String secret = getString(R.string.getName);
            Log.d(TAG, "onClick: clicky clicky");


            addRequest(secret, new Listener() {
                @Override
                public void response(String response) {
                    btn1.setEnabled(true);
                    Log.d(TAG, "retrived response");
                    textView.setText(response);
                    btn1.setEnabled(true);
                }
            });
        });

    }


    //request method for button press
    private void addRequest(String url, final Listener listener) {
        final StringRequest stringRequest2 = new StringRequest(Request.Method.GET,url,new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response != null) {
                    listener.response(response);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: errors at add request " + error);
            }
        });
        mQueue.add(stringRequest2);
    }



////////////////////////////////////////////////////////////////////////////
    //compass methods
    @Override
    protected void onPause() {
        super.onPause();
        // to stop the listener and save battery
        SensorManage.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }

        // code for system's orientation sensor registered listeners
        SensorManage.registerListener(this, SensorManage.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        // get angle around the z-axis rotated
        float degree = Math.round(event.values[0]);

        headingTextView.setText("Heading: " + Float.toString(degree) + " degrees" );


        ///get the bearing with Location.bearingTo(Loc dest)

        //connecting media player to degrees from north
        //setting volume
        float leftVol = 0;
        float rightVol= 0;

        ///still has bugs facing forward
        //facing forward
        if(degree > 315 && degree < 45){
            leftVol = 1;
            rightVol = 1;
        }
        //facing left
        if(degree < 315 && degree > 225){
            leftVol = 0;
            rightVol = 1;
        }
        //facing backwards
        if(degree < 225 && degree > 135){
            leftVol = 0;
            rightVol = 0;
        }
        //facing right
        if(degree < 135 && degree > 45){
            leftVol = 1;
            rightVol =0;
        }

        player.setVolume(leftVol,rightVol);
        Log.d(TAG, "onSensorChanged: volume" );
        Log.d(TAG, String.valueOf(dest.getBearing()));





        //.----add updates in here)---..///
        //.----dynamic update location textview when sensor change (big deal method placement)---..///
        getLastLocation();

        // rotation animation - reverse turn degree degrees
        RotateAnimation ra = new RotateAnimation(
                DegreeStart,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        // set the compass animation after the end of the reservation status
        ra.setFillAfter(true);
        // set how long the animation for the compass image will take place
        ra.setDuration(210);
        // Start animation of compass image
        compassImage.startAnimation(ra);
        DegreeStart = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    ///gps methods :0
    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {

            // check if location is enabled
            if (isLocationEnabled()) {

                // getting last location from FusedLocationClient object
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            latitudeTextView.setText(location.getLatitude() + "");
                            longitudeTextView.setText(location.getLongitude() + "");

                            
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        // Initializing LocationRequest object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            latitudeTextView.setText("Latitude: " + mLastLocation.getLatitude() + "");
            longitudeTextView.setText("Longitude: " + mLastLocation.getLongitude() + "");
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location on Android 10.0 and higher, use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }


////helper funks///////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ///create dummy destination that sits in for server call for dest
    //return destination
    private Location getDest(){

        Location destination = new Location("");

//west of me => 44.484869171461725, -73.23584437166608
        destination.setLatitude(44.484869171461725);
        destination.setLongitude(-73.23584437166608);
        return destination;
    }

    //get distance from dest
    private float getDistance(Location current, Location destination){

        float distance = current.distanceTo(destination);
        return distance;

    }


}


