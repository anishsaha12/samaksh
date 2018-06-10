package com.rising.dots.samaksh;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    public static boolean isAppRunning;
    ListView lv;
    private boolean all_mode= true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv = (ListView) findViewById(R.id.list);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            all_mode = false;
            String case_id = extras.getString("id");
            String latitude = extras.getString("latitude");
            String longitude = extras.getString("longitude");

            //Toast.makeText(MainActivity.this, (case_id + " " + latitude + ", " + longitude), Toast.LENGTH_SHORT).show();

            if (extras.getInt("emergency")!=0){     //an emergency
                String description = extras.getString("description");
                String remarks = extras.getString("remarks");

                HashMap<String, String> fake_case = new HashMap<>();
                fake_case.put("desc","EMERGENCY!!");
                fake_case.put("latitude",latitude);
                fake_case.put("longitude",longitude);
                fake_case.put("remarks", "Your Friend needs you!");
                fake_case.put("loc_url","Your Friend needs you!" );

                ArrayList<HashMap<String, String>> caseList = new ArrayList<>();
                caseList.add(fake_case);

                HashMap<String, String> a_case = new HashMap<>();
                a_case.put("desc",description);
                a_case.put("latitude",latitude);
                a_case.put("longitude",longitude);
                a_case.put("remarks", remarks);
                a_case.put("loc_url","http://maps.google.com/maps?z=12&t=m&q=loc:"+latitude+"+"+longitude);

                caseList.add(a_case);
                ListAdapter adapter = new CustomList(MainActivity.this, caseList);
                lv.setAdapter(adapter);

            }else{                                      //broadcast
                Log.d("ABCD", "id: "+case_id);
                new GetCases(case_id).execute();
            }
        }else {
            all_mode = true;
            new GetCases("").execute();
        }


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "1";
        String channel2 = "2";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId,
                    "Channel 1", NotificationManager.IMPORTANCE_HIGH);

            notificationChannel.setDescription("This is BNT");
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(notificationChannel);

            NotificationChannel notificationChannel2 = new NotificationChannel(channel2,
                    "Channel 2", NotificationManager.IMPORTANCE_MIN);

            notificationChannel.setDescription("This is bTV");
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(notificationChannel2);

        }

        FirebaseMessaging.getInstance().subscribeToTopic("samaksh")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Subscription Success";
                        if (!task.isSuccessful()) {
                            msg = "Subscription Fail";
                        }
                        Log.d("ABCD", msg);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isAppRunning = false;
    }


    private class GetCases extends AsyncTask<Void, Void, Void> {
        private ProgressDialog pDialog;
        ArrayList<HashMap<String, String>> caseList;
        private String case_id;

        public GetCases(String case_id) {
            this.case_id = case_id;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

            caseList = new ArrayList<>();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            String url="";
            if(all_mode==true){
                url = "http://samaksh.herokuapp.com/allCamCases";
            }else{
                url = "http://samaksh.herokuapp.com/singleCamCrime/"+case_id;
            }

            Log.d("ABCD", "url: "+url);
            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);

            Log.e("ABCD", "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {
                    if(all_mode==false) {
                        JSONObject jsonObj = new JSONObject(jsonStr);
                        String desc = jsonObj.getString("description");
                        String lat = jsonObj.getString("latitude");
                        String lon = jsonObj.getString("longitude");
                        String rem = jsonObj.getString("remarks");

                        Log.d("ABCD", lat+"+"+lon);

                        HashMap<String, String> a_case = new HashMap<>();
                        a_case.put("desc", desc);
                        a_case.put("latitude", lat);
                        a_case.put("longitude", lon);
                        a_case.put("remarks", rem);
                        a_case.put("loc_url","http://maps.google.com/maps?z=12&t=m&q=loc:"+lat+"+"+lon);
                        caseList.add(a_case);
                    }else{
                        JSONArray jsonArray = new JSONArray(jsonStr);
                        for(int i = 0; i < jsonArray.length(); i++) {
                            JSONObject cas = jsonArray.getJSONObject(i);
                            Log.d("ABCD","obj - "+cas);
                            String desc = cas.getString("description");
                            String lat = cas.getString("latitude");
                            String lon = cas.getString("longitude");
                            String rem = cas.getString("remarks");

                            HashMap<String, String> a_case = new HashMap<>();
                            a_case.put("desc", desc);
                            a_case.put("latitude", lat);
                            a_case.put("longitude", lon);
                            a_case.put("remarks", rem);
                            a_case.put("loc_url","http://maps.google.com/maps?z=12&t=m&q=loc:"+lat+"+"+lon);
                            caseList.add(a_case);
                        }
                    }

                } catch (final JSONException e) {
                    Log.e("ABCD", "Json parsing error: " + e.getMessage());
                }
            } else {
                Log.e("ABCD", "Couldn't get json from server.");


            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            ListAdapter adapter = new CustomList(MainActivity.this, caseList);
            lv.setAdapter(adapter);
        }

    }

    void storeNumber(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Number");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = input.getText().toString();
                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("mobile_number", m_Text);
                editor.commit();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

}