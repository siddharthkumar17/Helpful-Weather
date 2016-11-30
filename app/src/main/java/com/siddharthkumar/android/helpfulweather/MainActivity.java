package com.siddharthkumar.android.helpfulweather;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;;import java.io.InputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements OnConnectionFailedListener, ConnectionCallbacks{
    TextView temp;
    GoogleApiClient mGoogleApiClient;
    Location lastLocation;
    final String API_KEY = "d9a03c069a7bf250a30a3229e82a0a9b";
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
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED)
        {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(lastLocation!=null)
                temp.setText(lastLocation.getLatitude()+", "+lastLocation.getLongitude());
            else
                Toast.makeText(this, "BAD", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this,"Please give location access to find accurate weather",Toast.LENGTH_LONG).show();
            lastLocation=null;
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
                    String url = "http://api.openweathermap.org/data/2.5/weather?lat=35&lon=139&appid="+API_KEY;
                   // InputStream input = new URL.openStream();

                }
                catch(Exception e){
                    Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();

                }




            }

            return null;
        }
    }
}
