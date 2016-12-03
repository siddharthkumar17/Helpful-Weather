package com.siddharthkumar.android.helpfulweather;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
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
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements OnConnectionFailedListener, ConnectionCallbacks{
    TextView temp;
    GoogleApiClient mGoogleApiClient;
    Location lastLocation;
    final String API_KEY = "d9a03c069a7bf250a30a3229e82a0a9b";
    final int MY_PERMISSIONS_REQUEST_LOCATION = 17;
    final int MY_PERMISSIONS_REQUEST_INTERNET = 18;
    boolean permissionInternet = true;
    boolean permissionLocation = true;
    final String TAG = "MAIN";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        temp = (TextView)findViewById(R.id.temp);


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




                        getWeather();


                } else {

                    permissionLocation = false;


                }
                return;
            }



        }
    }
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
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
                        JSONObject jsonObject = new JSONObject(jsonWeatherData);
                        temp.setText("The weather in "+jsonObject.getString("name")+" is "+jsonObject.getJSONObject("weather").getString("main"));

                    }

                }
                catch (Exception e){
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

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
}
