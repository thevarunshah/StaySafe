package com.thevarunshah.staysafe;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HomeScreen extends AppCompatActivity {

    private TextView tv = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);

        tv = (TextView) findViewById(R.id.location_text);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30000, 15, locationListener);
        }
        catch(SecurityException se){

        }
    }

    /*---------- Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            Toast.makeText(getBaseContext(), "Location changed: Lat: " + loc.getLatitude()
                    + " Long: " + loc.getLongitude(), Toast.LENGTH_SHORT).show();

            try{

                HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://56c58ce9.ngrok.io/check?lat=" +
                        loc.getLatitude() + "&lon=" + loc.getLongitude()).openConnection();
                InputStream is = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));

                String line = "";
                String response = "";
                while ((line = rd.readLine()) != null){
                    response += line;
                }

                if(response.equals("true")){
                    boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
                    if(connected) {
                        sendPebble("Stay Safe", "Danger ahead!");
                    }
                    else{
                        sendNotif("Stay Safe", "Danger ahead!");
                    }
                }

            } catch(Exception e){

                Log.i("exception: ", e.toString());
            }
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }

    public void sendPebble(String title, String body) {
        final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

        final Map<String, String> data = new HashMap<String, String>();
        data.put("title", title);
        data.put("body", body);

        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();
        i.putExtra("messageType", "PEBBLE_ALERT");
        i.putExtra("sender", "Stay Safe");
        i.putExtra("notificationData", notificationData);

        sendBroadcast(i);
    }

    public void sendNotif(String title, String body){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this).setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle(title).setContentText(body);

        Intent resultIntent = new Intent(this, HomeScreen.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(HomeScreen.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }
}
