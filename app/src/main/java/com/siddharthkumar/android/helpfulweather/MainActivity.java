package com.siddharthkumar.android.helpfulweather;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.widget.*;
import android.content.*;
import android.view.*;
import android.widget.Toast;
import org.json.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

import com.siddharthkumar.android.helpfulweather.NotificationService.LocalBinder;
import com.siddharthkumar.android.helpfulweather.NotificationService.NotificationThread;

public class MainActivity extends AppCompatActivity implements OnConnectionFailedListener, ConnectionCallbacks{
    TextView temp;
    TextView json;
    SwipeRefreshLayout swipeRefreshLayout;
    GoogleApiClient mGoogleApiClient;
    Location lastLocation;
    final String API_KEY = "d9a03c069a7bf250a30a3229e82a0a9b";
    final int MY_PERMISSIONS_REQUEST_LOCATION = 17;
    final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 19;
    final int MY_PERMISSIONS_REQUEST_INTERNET = 18;
    boolean permissionInternet = true;
    boolean permissionLocation = true;
    boolean permissionFineLocation = true;
    public static final String TAG = "helpfulweather";
    int id=0;
    NotificationService notificationService;
    boolean isBound;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        temp = (TextView)findViewById(R.id.temp);
        json = (TextView)findViewById(R.id.json);
        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.activity_main);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override public void run() {
                        getWeather();

                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 1000);
            }
        });

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }







    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    permissionLocation = true;




                      //  getWeather();


                } else {

                    permissionLocation = false;


                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    permissionFineLocation = true;




                    //  getWeather();


                } else {

                    permissionFineLocation = false;


                }
                return;
            }



        }
    }
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();

        Intent i = new Intent(this, NotificationService.class);

        bindService(i,serviceConnection,Context.BIND_AUTO_CREATE);


    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed!" , Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)== PackageManager.PERMISSION_GRANTED)
        {
            getWeather();

        }
        else{


            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && permissionLocation) {




                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION);



            }

        }

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
        {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && permissionFineLocation) {
                 ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
        }

    }

    public void getWeather(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED)
        {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(lastLocation!=null){
                try{
                    String jsonWeatherData = new FetchWeatherTask().execute(lastLocation.getLatitude()+" "+lastLocation.getLongitude()).get();
                    if(jsonWeatherData==null)
                        Log.e(TAG,"Error json");
                    else{
                       // temp.setText(jsonWeatherData);


                        Calendar cal = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                        JSONObject jsonObject = new JSONObject(jsonWeatherData);
                        double tempe = jsonObject.getJSONObject("main").getDouble("temp");
                        temp.setText("It is "+jsonObject.getJSONArray("weather").getJSONObject(0).getString("description")+" at "+sdf.format(cal.getTime())+". " +
                                "It is "+String.format("%.2f",((tempe-273)*(9/5))+32)+" degrees outside.");
                        setTitle("Weather in "+jsonObject.getString("name"));
                        //json.setText(jsonWeatherData);


                    }

                }
                catch (Exception e){
                    Log.e(TAG, e.getMessage());
                }
            }

        }

    }

    @Override
    protected void onDestroy() {


        super.onDestroy();

        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    private class FetchWeatherTask extends AsyncTask<String, Void, String>{




        @Override
        protected String doInBackground(String... strings) {

            for(String s: strings){

                try{

                    Scanner sc = new Scanner(s);
                    String latitude=sc.next();
                    String longitude=sc.next();
                    String url = "http://api.openweathermap.org/data/2.5/weather?lat="+latitude+"&lon="+longitude+"&appid="+API_KEY;
                    InputStream input = new URL(url).openStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    String line = reader.readLine();
                    return line;
                }
                catch(Exception e){
                    Log.e(TAG, e.getMessage());

                }





            }

            return null;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LocalBinder binder = (LocalBinder)iBinder;
            notificationService = binder.getService();
            isBound = true;

            onServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };

    void onServiceBound(){

        notificationService.updateNotification();


    }
}
