package com.siddharthkumar.android.helpfulweather;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.StringBuilderPrinter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ThreadFactory;


public class NotificationService extends Service implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {
    public GoogleApiClient mGoogleApiClient;
    public static final String TAG = "helpfulweather";
    final String API_KEY = "d9a03c069a7bf250a30a3229e82a0a9b";
    private LocationManager mLocationManager = null;
    private static final int REFRESH_TIME = 1800000;
    private static final float LOCATION_DISTANCE = 100f;
    Location mlastLocation = new Location("");
    Location lastLocation=new Location("");
    private final IBinder binder = new LocalBinder();
    int id=0;
    public NotificationService() {

    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        return Service.START_STICKY;
    }

    void updateNotification(){
        NotificationThread runnable = new NotificationThread();
        Thread th = new Thread(runnable);
        th.start();
        Log.i(TAG,"Thread started");

    }
    @Override
    public void onCreate()
    {
        lastLocation.setLatitude(40);
        lastLocation.setLongitude(-96);

        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, REFRESH_TIME, LOCATION_DISTANCE,
                    mLocationListeners[1]);
            if(mLocationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER)!=null)
                lastLocation=mLocationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, REFRESH_TIME, LOCATION_DISTANCE,
                    mLocationListeners[0]);
            if(mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER)!=null)
                lastLocation=mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }
    @Override
    public void onDestroy()
    {

        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                        mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listeners, ignore", ex);
                }
            }
        }
    }
    private void initializeLocationManager() {

        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return binder;
    }
    private class LocationListener implements android.location.LocationListener{

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mlastLocation = new Location(provider);
        }
        @Override
        public void onLocationChanged(Location location)
        {

            mlastLocation.set(location);


        }
        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }
        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }
    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    public class NotificationThread implements Runnable,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {
        public void run() {
            while(true){
                synchronized (this){
                    fetch();
                    try {
                        wait(REFRESH_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }

        public void  fetch(){
            try{

                if(mLocationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER)!=null)
                    lastLocation=mLocationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER);
                if(mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER)!=null)
                    lastLocation=mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER);

                if(ContextCompat.checkSelfPermission(getApplicationContext()  , Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED&&
                        ContextCompat.checkSelfPermission(getApplicationContext()  , Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED
                        )
                {

                    if (mGoogleApiClient == null) {
                        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                                .addConnectionCallbacks(this)
                                .addOnConnectionFailedListener(this)
                                .addApi(LocationServices.API)
                                .build();
                    }




                    if(lastLocation!=null){

                        try{
                            String jsonWeatherData = new FetchWeatherTask().execute(lastLocation.getLatitude()+" "+lastLocation.getLongitude()).get();
                            if(jsonWeatherData==null)
                                Log.e(TAG,"Error json");
                            else{

                                JSONObject jsonObject = new JSONObject(jsonWeatherData);
                                double temp = jsonObject.getJSONObject("main").getDouble("temp");

                                Calendar cal = Calendar.getInstance();
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                                Log.i(TAG,lastLocation.toString());
                                temp = (temp-273)*9.0/5+32;
                                String description = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
                                String time=sdf.format(cal.getTime());
                                StringBuilder stringBuilder = new StringBuilder(time);
                                boolean am=false;
                                if(time.charAt(0)=='0'&&time.charAt(1)=='0')
                                {
                                    am=true;
                                    stringBuilder.setCharAt(0,'1');
                                    stringBuilder.setCharAt(1,'2');
                                }

                                time=stringBuilder.toString();
                                if(am)
                                    time+=" AM";
                                else
                                    time+=" PM";
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                                        .setSmallIcon(R.drawable.ms02_example_heavy_rain_showers)
                                        .setContentTitle("Weather Update for "+jsonObject.getString("name"))
                                        .setColor(ContextCompat.getColor(getApplicationContext(),R.color.colorPrimary))
                                        .setContentText("It is "+String.format("%.0f",temp)+"°F and "+description+".")
                                        .setVisibility(Notification.VISIBILITY_PUBLIC);
                                TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                stackBuilder.addParentStack(MainActivity.class);
                                stackBuilder.addNextIntent(intent);
                                PendingIntent pendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
                                builder.setContentIntent(pendingIntent);
                                NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                                Notification notification = builder.build();
                                //  notification.flags = Notification.FLAG_ONGOING_EVENT;

                                notificationManager.notify(id,notification);
                                Log.i(TAG,"Sent notification");
                            }

                        }
                        catch (Exception e){
                            Log.e(TAG, e.getMessage());
                        }
                    }


                }


            }
            catch(Exception e){
                Log.e(TAG,e.getMessage());
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {

        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
    }
    public class LocalBinder extends Binder{
        NotificationService getService(){
            return NotificationService.this;
        }
    }
    private class FetchWeatherTask extends AsyncTask<String, Void, String> {




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