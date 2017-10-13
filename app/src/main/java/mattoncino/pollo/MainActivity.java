package mattoncino.pollo;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import java.util.HashSet;
import java.util.Set;

import mattoncino.pollo.databinding.ActivityMainBinding;


/**
 * Pollo is a simple polling application designed to
 * be used between devices in the same LAN.
 *
 * @author  Berat
 * @version 1.0
 * @since   2017-06-01
 */

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private JmDnsManager jmDnsManager;
    private BroadcastReceiver wifiReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Toast.makeText(MainActivity.this, "called onCreate()", Toast.LENGTH_SHORT).show();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setTitle("Pollo");

        Bundle data = getIntent().getExtras();
        if (data != null) {
            int notfID = data.getInt("notificationID");
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notfID);
            getIntent().removeExtra("notificationID");
        }

        //wifiReceiver = createWifiBroadcastReceiver();
        //LocalBroadcastManager.getInstance(this).registerReceiver(wifiReceiver, new IntentFilter("mattoncino.pollo.receive.wifi.stat"));


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

        enableButtons(true);

    }

    @Override
    public void onResume() {
        super.onResume();
        //Toast.makeText(MainActivity.this, "called onResume()", Toast.LENGTH_SHORT).show();
        if(wifiConnected()) {
            connectForDataTransferring();
            //enableButtons(true);
        } else {
            setTitle("Connecting...");
            ToastHelper.showSnackBar(MainActivity.this, binding.activityMain,
                    "No active Wifi connection. Please connect to an Access Point");
            //enableButtons(false);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        //Toast.makeText(this, "called onStop", Toast.LENGTH_SHORT).show();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiReceiver);
    }

    private void connectForDataTransferring(){
        //TODO make it static!!!!
         Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle reply = msg.getData();
                setTitle(reply.getString("result"));
            }
        };

        jmDnsManager = ((MyApplication)getApplication()).getConnectionManager();

        if(!jmDnsManager.initialized()) {
            setTitle("Connecting...");
            Log.i(TAG, "initializing jmdnsManager...");
            Intent connManagerSrvInt = new Intent(this, ConnectionManagerIntentService.class);
            connManagerSrvInt.putExtra("messenger", new Messenger(handler));
            startService(connManagerSrvInt);
            //Log.i(TAG, "connManagerServiceIntent is launched");
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        //Toast.makeText(this, "called onPause", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Toast.makeText(this, "called onStart", Toast.LENGTH_SHORT).show();
        wifiReceiver = createWifiBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(wifiReceiver, new IntentFilter(Receivers.WIFI));
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     *
     * @return true if device is connected to a wifi.
     */

    private boolean wifiConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected() && activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.i(TAG, getString(R.string.wifi_connection));
            return true;
        }

        Log.i(TAG, getString(R.string.mobile_connection) + " or " + R.string.no_wifi_or_mobile);
        return false;
    }


    /**
     * Gets online devices list and show them as
     */
    public void onShowOnlineDevicesListDialogPress() {
        if(jmDnsManager != null && jmDnsManager.initialized()){
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
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("No connection, no devices :)");
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setCancelable(true);
            builder.show();
        }
    }


    private void showDevicesInNetworkList(Set<String> devices){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Device list: " + "(" + devices.size() + ")");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MainActivity.this,
                                                    android.R.layout.simple_list_item_1,
                                                    devices.toArray(new String[devices.size()]));

        //APILEVEL REQ 11!!!
        //arrayAdapter.addAll(devices);
        builder.setAdapter(arrayAdapter, null);
        builder.setCancelable(true);
        builder.show();
    }

    /**
     * Enables/disables buttons
     * @param b This is the boolean value
     */
    private void enableButtons(boolean b){
        binding.createPollActivityButton.setEnabled(b);
        binding.activePollsActivityButton.setEnabled(b);
        binding.showDeviceListButton.setEnabled(b);
    }

    /**
     * Creates a Broadcast Listener for changes in Wifi status
     * @return BroadcastReceiver This returns a Broadcast Receiver
     */
    private BroadcastReceiver createWifiBroadcastReceiver() {
        Log.v(TAG, "received wifi broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.WIFI)) {
                    boolean stat = intent.getBooleanExtra("wifi", true);
                    //enableButtons(stat);
                    setTitle(stat ? "Pollo" : "Connecting...");
                    if(!stat)
                        ToastHelper.showSnackBar(MainActivity.this, binding.activityMain,
                                "No active Wifi connection. Please connect to an Access Point");
                }
            }
        };
    }
}
