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
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import mattoncino.pollo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private JmDnsManager jmDnsManager;
    private BroadcastReceiver wifiReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        //setAlarm();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(wifiConnected()) {
            connectForDataTransferring();
            enableButtons(true);
        }
        else enableButtons(false);

    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiReceiver);
    }

    private void connectForDataTransferring(){
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

    /*@Override
    public void onPause(){
        super.onPause();
        Toast.makeText(this, "called onPause", Toast.LENGTH_SHORT).show();
    }*/

    @Override
    protected void onStart() {
        //Toast.makeText(this, "called onStart", Toast.LENGTH_SHORT).show();
        super.onStart();
        wifiReceiver = createWifiBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(wifiReceiver, new IntentFilter("mattoncino.pollo.receive.wifi.stat"));
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    private boolean wifiConnected() {
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
                if(jmDnsManager.initialized()) {
                    final HashSet<String> onlineDevices = (HashSet<String>) jmDnsManager.getOnlineDevices(MainActivity.this);

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDevicesInNetworkList(onlineDevices);
                        }
                    });
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();

    }

    private void showDevicesInNetworkList(Set<String> devices){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);

        builderSingle.setTitle("Device list: " + "(" + devices.size() + ")");

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
        Log.v(TAG, "received wifi broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null
                        && intent.getAction().equals("mattoncino.pollo.receive.wifi.stat")) {
                    boolean stat = intent.getBooleanExtra("wifi", false);
                    enableButtons(stat);
                    setTitle(stat ? "Pollo" : "Connecting...");
                }
            }
        };
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
