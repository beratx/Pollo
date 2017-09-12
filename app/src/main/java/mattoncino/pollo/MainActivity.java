package mattoncino.pollo;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import mattoncino.pollo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "Pollo Main Activity";
    final static private long THIRTY_SECONDS = 1000 * 30;
    private ActivityMainBinding binding;
    private JmDnsManager jmDnsManager;
    private BroadcastReceiver wifiReceiver;
    private AlarmManager alarm;
    //private BroadcastReceiver alarmReceiver;
    private PendingIntent pintent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        Bundle data = getIntent().getExtras();
        if (data != null) {
            int notfID = data.getInt("notificationID");
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notfID);
            getIntent().removeExtra("notificationID");
        }

        wifiReceiver = createWifiBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(wifiReceiver, new IntentFilter("mattoncino.pollo.receive.wifi.stat"));


        binding.createPollActivityButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, mattoncino.pollo.MultiOptPollActivity.class));
            }
        });

        binding.showDeviceListButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                    onShowOnlineDevicesListDialogPress();
            }
        });


        binding.activePollsActivityButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, mattoncino.pollo.ActivePollsActivity.class));
            }
        });

        setAlarm();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(wifiConnected()) {
            startDataTransferring();
            enableButtons(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiReceiver);
    }

    private void startDataTransferring(){
        jmDnsManager = ((MyApplication)getApplication()).getConnectionManager();
        if(!jmDnsManager.initialized()) {
            Log.d(TAG, "initializing jmdnsManager...");
            Intent connManagerServiceIntent = new Intent(this, ConnectionManagerIntentService.class);
            startService(connManagerServiceIntent);
            Log.d(TAG, "connManagerServiceIntent is launched");
        }
    }

    @Override
    protected void onStart() {
        //Toast.makeText(this, "called onRestart", Toast.LENGTH_LONG).show();
        super.onStart();
        if(wifiReceiver == null)
            wifiReceiver = createWifiBroadcastReceiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(wifiReceiver,
                                new IntentFilter("mattoncino.pollo.receive.wifi.stat"));
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private boolean wifiConnected() {
        // BEGIN_INCLUDE(connect)
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected() && activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.i(TAG, getString(R.string.wifi_connection));
            return true;
        } else {
            Log.i(TAG, getString(R.string.mobile_connection) + " or " + R.string.no_wifi_or_mobile);
            Toast.makeText(this, "Pollo works only under LAN. Please activate your wifi and connect to an Access Point",
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }


    public void onShowOnlineDevicesListDialogPress(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final HashSet<String> onlineDevices = (HashSet<String>) jmDnsManager.getOnlineDevices(MainActivity.this);

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDevicesInNetworkList(onlineDevices);
                    }
                });
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();

    }

    private void showDevicesInNetworkList(Set<String> devices){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);

        builderSingle.setTitle("Devices list");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                MainActivity.this,
                android.R.layout.simple_list_item_1);

        arrayAdapter.addAll(devices);
        builderSingle.setAdapter(arrayAdapter, null);
        builderSingle.show();
    }

    private void enableButtons(boolean b){
        binding.createPollActivityButton.setEnabled(b);
        binding.activePollsActivityButton.setEnabled(b);
        binding.showDeviceListButton.setEnabled(b);
    }

    private BroadcastReceiver createWifiBroadcastReceiver() {
        Log.d(TAG, "received wifi broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null
                        && intent.getAction().equals("mattoncino.pollo.receive.wifi")) {
                    boolean stat = intent.getBooleanExtra("wifi", false);
                    enableButtons(stat);
                }
            }
        };
    }

    private void setAlarm() {

        Intent intent = new Intent(MainActivity.this, StatusUpdaterService.class);
        pintent = PendingIntent.getService(MainActivity.this, 0, intent, 0);
        alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        //alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, cal.getTimeInMillis(), 30*1000, pintent);
        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(), THIRTY_SECONDS, pintent);
    }

    @Override
    protected void onDestroy() {
        alarm.cancel(pintent);
        //unregisterReceiver(alarmReceiver);
        super.onDestroy();
    }
}
