package com.rising.dots.samaksh;

/**
 * Created by anish on 09-06-2018.
 */

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;
/**
 * Created by anish on 09-06-2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String ADMIN_CHANNEL_ID ="admin_channel";
    private NotificationManager notificationManager;
    protected LocationManager locationManager;
    protected LocationListener locationListener;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //Setting up Notification channels for android O and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setupChannels();
        }

        if(remoteMessage.getData().get("latitude")==null || remoteMessage.getData().get("longitude")==null){
            return;
        }
        float lat1 = Float.parseFloat(remoteMessage.getData().get("latitude"));
        float lon1 = Float.parseFloat(remoteMessage.getData().get("longitude"));
        String loc_url = "https://www.google.com/maps/?q="+lat1+","+lon1;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Location location = getLastBestLocation();
        float lat2 = (float) location.getLatitude();
        float lon2 = (float) location.getLongitude();

        float dist = (float) (Math.acos(Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(lon1) - Math.toRadians(lon2))) * 6371);
        Log.d("ABCD","dist = "+dist);
        Log.d("ABCD","loc_url = "+loc_url);

        sendNotification(remoteMessage, dist);
    }

    private void sendNotification(RemoteMessage remoteMessage, Float distance){

        String emer = remoteMessage.getData().get("emergency");
        if(distance<=2.0 || emer!=null){
            int notificationId = new Random().nextInt(60000);

            Intent intent = new Intent(this , MainActivity.class);
            intent.putExtra("id", remoteMessage.getData().get("id"));
            intent.putExtra("latitude", remoteMessage.getData().get("latitude"));
            intent.putExtra("longitude", remoteMessage.getData().get("longitude"));


            if(emer!=null) {
                Log.d("ABCD", "Personal Emergency " + emer);
                String numbers = remoteMessage.getData().get("closeContacts");
                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
                String mynum =pref.getString("mobile_number", "");
                Log.d("ABCD",mynum+", "+numbers);
                if(!numbers.contains(mynum)){
                    return;
                }
                intent.putExtra("emergency",1);
                String desc = remoteMessage.getData().get("description");
                if(desc!=null){
                    intent.putExtra("description",desc);
                }else intent.putExtra("description","No Description Available");

                String remarks = remoteMessage.getData().get("remarks");
                if(remarks!=null){
                    intent.putExtra("remarks",remarks);
                    Log.d("ABCD",remarks);
                }else intent.putExtra("remarks","No remarks");
            }
            else{
                Log.d("ABCD","Broadcast");
                intent.putExtra("emergency",0);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ADMIN_CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon)//.ic_notification_small)  //a resource for your custom small icon
                    .setContentTitle("Someone is in Trouble near you!!") //the "title" value you sent in your notification
                    .setContentText("Click on me to see details") //ditto
                    .setAutoCancel(true)  //dismisses the notification on click
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(notificationId , notificationBuilder.build());

        }

    }

    ///_______________________________________________

    /*----Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }


    private Location getLastBestLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.v("ABCD", "not possible");
            return new Location("");
            //return "";
        }
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) {
            Log.v("ABCD", "only gps loc");
            GPSLocationTime = locationGPS.getTime();
        }else{
            Log.v("ABCD", "no gps loc");
        }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }else{
            Log.v("ABCD", "no net loc");
        }

        if ( 0 < GPSLocationTime - NetLocationTime ) {
            Log.v("ABCD", "postitive");
            return locationGPS;
            //return "";
        }
        else {
            Log.v("ABCD", "only gps loc"+locationNet.toString());
            return locationNet;
        }
    }

    void clickedMe() {
        Boolean flag = displayGpsStatus();

        if (flag) {

            //This part is not required --start
            Log.d("", "Please!! move your device to see the changes in coordinates.\nWait..");

            locationListener = new MyLocationListener();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d("Gps Status!!", "Your GPS is: OFF");
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);

            //This part is not required --end

            Location location = getLastBestLocation();

            Log.v("ABCD", "onClick over"+location.toString());

        } else {
            Log.d("Gps Status!!", "Your GPS is: OFF");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupChannels(){
        CharSequence adminChannelName = "Global channel";
        String adminChannelDescription = "Notifications sent from the app admin";

        NotificationChannel adminChannel;
        adminChannel = new NotificationChannel(ADMIN_CHANNEL_ID, adminChannelName, NotificationManager.IMPORTANCE_LOW);
        adminChannel.setDescription(adminChannelDescription);
        adminChannel.enableLights(true);
        adminChannel.setLightColor(Color.RED);
        adminChannel.enableVibration(true);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(adminChannel);
        }
    }
}