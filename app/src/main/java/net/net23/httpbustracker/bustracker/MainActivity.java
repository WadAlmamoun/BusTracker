package net.net23.httpbustracker.bustracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    TextView txtname, txtid;
    SharedPreferences Data;
    SharedPreferences.Editor editor;
    private static final int TIME_INTERVAL = 2000; // # milliseconds, desired time passed between two back presses.
    private long mBackPressed;

    //Maps Activivty Variables
    private GoogleMap mMap;
    FloatingActionButton clearMap, getMyLocation;
    private final static int MY_PERMISSION_FINE_LOCATION = 101;
    ZoomControls zoomControls;
    Double myLatitude, myLongitude;

    //objects for the google api client
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    protected static final String TAG = "MapsActivity";

    //variables for requesting buses locations
    int LOCATION_UPDATE_REQUEST_TIMEOUT = 0;
    HttpURLConnection httpURLConnection;
    StringRequest stringRequest;
    Timer requestTimer;
    String tripS, tripE;
    int busesNos = 0;
    int oldbusesNo = 0;
    Marker[] bus = new Marker[busesNos];
    int i = 0;
    boolean newloc = true;
    ProgressDialog progressDialog = null;
    ProgressDialog busProgressDialog;
    CountDownTimer NEW_LOCATION_REQUEST_TIMER;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        busProgressDialog = new ProgressDialog(MainActivity.this);
        busProgressDialog.setMessage("Acquiring buses locations");
        busProgressDialog.setIndeterminate(true);
        busProgressDialog.setCancelable(true);

        Data = this.getSharedPreferences("MYDATA", Context.MODE_PRIVATE);
        //Maps++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(15 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //Maps------------------------------------------------------------------------------------------


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ////////////////////////////////////////////////////////////////////////
        // Login Section
        txtname = (TextView) findViewById(R.id.text_view_name);
        txtid = (TextView) findViewById(R.id.text_view_id);
        String message = getIntent().getStringExtra("message");
        String id = getIntent().getStringExtra("id");
        String name = getIntent().getStringExtra("name");
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            Boolean userStatus = Data.getBoolean("logout", true);
            if (!userStatus) {
                name = Data.getString("Name", "ERROR Acquiring your name");
                id = Data.getString("UserID", "ERROR Acquiring your ID");
            } else {
                name = "Guest";
                id = "Not Logged In";
            }
        }

        txtname.setText("Name: " + name);
        txtid.setText("ID: " + id);
        ///////////////////////////////////////////////////////////////////////////

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
                myLatitude = null;
                myLongitude = null;
                super.onBackPressed();
                return;
            } else {
                Toast.makeText(getBaseContext(), "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
            }

            mBackPressed = System.currentTimeMillis();
            //    System.exit(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (id == R.id.arabi_markazi) {
                tripS = "Arabi";
                tripE = "Markazi";
                busProgressDialog.show();
                Async(tripS, tripE);
            } else if (id == R.id.markazi_arabi) {
                tripS = "Markazi";
                tripE = "Arabi";
                busProgressDialog.show();
                Async(tripS, tripE);
            } else if (id == R.id.arabi_mamora) {
                tripS = "Arabi";
                tripE = "Mamora";
                busProgressDialog.show();
                Async(tripS, tripE);
            } else if (id == R.id.mamora_arabi) {
                tripS = "Mamora";
                tripE = "Arabi";
                busProgressDialog.show();
                Async(tripS, tripE);
            } else if (id == R.id.arabi_jabra) {
                tripS = "Arabi";
                tripE = "Jabra";
                busProgressDialog.show();
                Async(tripS, tripE);
            } else if (id == R.id.jabra_arabi) {
                tripS = "Jabra";
                tripE = "Arabi";
                busProgressDialog.show();
                Async(tripS, tripE);
            }
        } else {
            Toast.makeText(MainActivity.this, "No Internet Connection!", Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);

    }

    public void Async(final String start, final String end) {
            try {
                requestTimer.cancel();
            } catch (java.lang.NullPointerException e) {
                e.printStackTrace();
            }
            for (int x = 0; x < busesNos; x++)
                if (bus[x] != null) {
                    {
                        bus[x].remove();
                    }
                }
            requestTimer = new Timer();
            requestTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        BackgroundGetBus backgroundGetBus = new BackgroundGetBus(getApplicationContext());
                        backgroundGetBus.execute(start, end);
                    }else {
                        Toast.makeText(getApplicationContext(),"Lost Internet Connection",Toast.LENGTH_LONG).show();
                    }
                }
            }, 0, 1000);
}
    //&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
    private class BackgroundGetBus extends AsyncTask<String,Void,String> {
        //    String updateC_url = "http://bustracker.net23.net/user_credit_update.php";
        String locationUrl = "http://bustrackersudan.net16.net/gps_server-user.php";

        public BackgroundGetBus asyncObject;

        public BackgroundGetBus(Context applicationContext) {
        }


        @Override
        protected void onPreExecute() {
            asyncObject = this;
//            Looper.prepare();
  //          new CountDownTimer(20000, 1000) {
    //            public void onTick(long millisUntilFinished) {}
      //          public void onFinish() {
                    // stop async task if not in progress
         //           if (asyncObject.getStatus() == AsyncTask.Status.RUNNING) {
        //                busProgressDialog.dismiss();
          //              asyncObject.cancel(true);
            //            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
              //          builder.setPositiveButton("OK", null);
                //        builder.setTitle("Error!");
                  //      builder.setMessage("Request timeout!");
                    //    builder.show();
                    //}
         //       }
           // }.start();
        }

        @Override
        protected String doInBackground(String... params) {
                try {
                    URL url = new URL(locationUrl);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);
                    OutputStream OS = httpURLConnection.getOutputStream();
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(OS, "UTF-8"));
                    String tripS = params[0];
                    String tripE = params[1];
                    String data = URLEncoder.encode("tripS", "UTF-8") + "=" + URLEncoder.encode(tripS, "UTF-8")+ "&" +
                            URLEncoder.encode("tripE", "UTF-8") + "=" + URLEncoder.encode(tripE, "UTF-8");
                    bufferedWriter.write(data);
                    bufferedWriter.flush();
                    bufferedWriter.close();
                    OS.close();
                    InputStream IS = httpURLConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(IS));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line = "";
                    while ((line = bufferedReader.readLine())!= null)
                    {
                        stringBuilder.append(line+"\n");
                    }
                    httpURLConnection.disconnect();
                    return stringBuilder.toString().trim();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            return null;
        }
        @Override
        protected void onPostExecute(String json) {
            try {
                busProgressDialog.dismiss();
                JSONObject jsonObject = new JSONObject(json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1));
                JSONArray jsonArray = jsonObject.getJSONArray("server_response");
                JSONObject JO = jsonArray.getJSONObject(0);
                String code = JO.getString("code");
                if (code.equals("success")) {
                    String busesNo = JO.getString("busesNumber");
                    busesNos = Integer.valueOf(busesNo);
                    for (int j = 0; j < oldbusesNo; j++) {
                        try{
                            bus[j].remove();
                        }catch (Exception e){e.printStackTrace();};
                    }
                    bus =new Marker [busesNos];
                    oldbusesNo = 0;
                    for (i = 0; i < busesNos; i++) {
                        JO = jsonArray.getJSONObject(i);
                        final String busId = JO.getString("idBus");
                        final String driver = JO.getString("driverName");
                        String longitude = JO.getString("longitude");
                        String latitude = JO.getString("latitude");
                        final Double lat = Double.valueOf(latitude);
                        final Double lng = Double.valueOf(longitude);
                        LatLng point = new LatLng(lat, lng);
                            bus[i] = mMap.addMarker(new MarkerOptions().position(point).title(tripS + " - " + tripE).snippet(busId + "-" + driver).icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_marker_icon)));
                        oldbusesNo = oldbusesNo + 1;
                    }
                } else {
                    try{requestTimer.cancel();
                    this.cancel(true);
                    } catch (java.lang.NullPointerException e) {e.printStackTrace();}
                    for (int x = 0; x < busesNos; x++)
                        if (bus[x] != null) {
                            {
                                bus[x].remove();
                            }
                        }
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setPositiveButton("OK", null);
                    builder.setTitle("Error!");
                    builder.setMessage("No Buses on this route at the moment");
                    builder.show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }catch (java.lang.NullPointerException e){
                Toast.makeText(getApplicationContext(), "Jajajajajaaaa",Toast.LENGTH_SHORT).show();
            }
        }

    }
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

    //Maps+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMinZoomPreference(12);
        mMap.setMaxZoomPreference(20);
        LatLng neBounds = new LatLng (15.767396, 32.713090);
        LatLng swBounds = new LatLng(15.420839, 32.300287);
        LatLngBounds bounds = new LatLngBounds(swBounds,neBounds);
        mMap.setLatLngBoundsForCameraTarget(bounds);

        clearMap = (FloatingActionButton) findViewById(R.id.fab_clear);
        getMyLocation = (FloatingActionButton) findViewById(R.id.fab_me);
        //Get my location if the user pressed this floating action button
        getMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setIndeterminate(true);
                        progressDialog.setCancelable(false);
                        progressDialog.setMessage("Acquiring Your Location");
                        progressDialog.show();
                        mMap.setMyLocationEnabled(true);
                        LatLng myLocation = new LatLng(myLatitude, myLongitude);
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17));
                    }
                    else
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_FINE_LOCATION);
                        }
                    }
                } catch (NullPointerException e) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setPositiveButton("Go To Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                        }
                    });
                    builder.setNegativeButton("No", null);
                    builder.setMessage("Application is requesting permission to turn on Location Service. \n Allow?");
                    builder.show();
                }
            }
        });

        zoomControls = (ZoomControls) findViewById(R.id.zoom_control);
        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        clearMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bus != null)
                {
                try{requestTimer.cancel();} catch (java.lang.NullPointerException e) {e.printStackTrace();}
                for (int x = 0; x < busesNos; x++)
                { for (int y = 0; y < oldbusesNo; y++) {
                    if (bus[x] != null) {
                        bus[x].remove();
                    }
                }
                }
            }
                else{
                    Toast.makeText(MainActivity.this,"Map is already cleared",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case MY_PERMISSION_FINE_LOCATION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ActivityCompat.checkSelfPermission(this,  android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    {
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"This app requires location permission to be granted",Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        requestLocationUpdates();
    }
    private void requestLocationUpdates()
    {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest,this);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient.isConnected()){
            requestLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        myLatitude = location.getLatitude();
        myLongitude = location.getLongitude();
        LatLng myLocation = new LatLng(myLatitude, myLongitude);
        if (progressDialog !=null)
        {
            progressDialog.dismiss();
            progressDialog = null;
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation));
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i(TAG,"Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.i(TAG,"Connection Failed:" + connectionResult.getErrorCode());
    }
    //----------------------------------------------------------------------------------------------------------------------------------------------


    // @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_login_regisetr) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                Boolean userStatus = Data.getBoolean("logout", true);
                if (userStatus == false)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setPositiveButton("OK", null);
                    builder.setTitle("Warning!");
                    builder.setMessage("You are already logged in");
                    builder.show();
                }
                else {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setPositiveButton("OK", null);
                builder.setTitle("Error!");
                builder.setMessage("You Cannot Login Or Register Without Internet Connection");
                builder.show();
            }
        } else if (id == R.id.nav_tickets) {
            Boolean userStatus = Data.getBoolean("logout", true);
            if (!userStatus) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    Data = getSharedPreferences("MYDATA", Context.MODE_PRIVATE);
                    String uid = Data.getString("UserID", "Error getting ID");
                    String method = "updateC";
                    String request = "background";
                    BackgroundCredit backgroundCredit = new BackgroundCredit(MainActivity.this);
                    backgroundCredit.execute(method, uid, request);
                } else {
                    Intent intent = new Intent(this, TicketsActivity.class);
                    startActivity(intent);
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setPositiveButton("OK", null);
                builder.setTitle("Error!");
                builder.setMessage("You can't access Tickets without logging in");
                builder.show();
            }
        }else if (id == R.id.nav_about) {

        } else if (id == R.id.nav_share) {
            ApplicationInfo app = getApplicationContext().getApplicationInfo();
            String filePath = app.sourceDir;

            Intent intent = new Intent(Intent.ACTION_SEND);

            // MIME of .apk is "application/vnd.android.package-archive".
            // but Bluetooth does not accept this. Let's use "*/*" instead.
            intent.setType("*/*");
            // Append file and send Intent
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
            startActivity(Intent.createChooser(intent, "Share app via"));
        } else if (id == R.id.nav_logout_exit) {
            editor = Data.edit();
            editor.putBoolean("logout", true);
            editor.commit();
            editor.putString("Message",null).commit();
            editor.putString("UserID",null).commit();
            editor.putString("Name",null).commit();
            editor.putString("Credit",null).commit();
            editor.putString("Pin",null).commit();
            myLatitude = null;
            myLongitude = null;
            finish();
            System.exit(0);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }
}