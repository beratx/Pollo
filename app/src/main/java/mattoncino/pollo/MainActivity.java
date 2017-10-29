package mattoncino.pollo;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import java.lang.ref.WeakReference;
import java.util.HashSet;

import mattoncino.pollo.databinding.ActivityMainBinding;


/**
 * Pollo is a simple polling application designed to
 * be used between devices in the same WLAN.
 *
 * @author  Berat
 * @version 1.0
 * @since   2017-06-01
 */


/**
 * Main Activity is the first Activity launched by launcher that displays
 * the menu to reach  App's functionality:
 *  <ul>
 * <li> CreatePollActivity : to create a new poll
 * <li> ShowPollsActivity : to list created/received polls
 * <li> WaitingPollRequestsActivity (if present, to list waiting requests)
 * <li> Show Device List : to list active devices' addresses
 * </ul>
 *<p>
 * It also initializes JmDnsManager to manage all the network related issues.
 * </p>
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private JmDnsManager jmDnsManager;
    private BroadcastReceiver wifiReceiver;
    private BroadcastReceiver countReceiver;
    private MyHandler handler;


    /**
     * Does binding of layout and sets action listeners to the buttons
     * @param savedInstanceState
     */
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


        binding.createPollActivityButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this, mattoncino.pollo.MultiOptPollActivity.class));
            }
        });

        binding.showDeviceListButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(wifiConnected()) {
                    ShowOnlineDevicesDialog dialog = new ShowOnlineDevicesDialog(MainActivity.this);
                    dialog.execute(null, null, null);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("No connection, no devices :)");
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setCancelable(true);
                    builder.show();
                }
            }
        });


        binding.activePollsActivityButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, mattoncino.pollo.ActivePollsActivity.class));
            }
        });


        binding.waitingPollsActivityButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, mattoncino.pollo.WaitingPollsActivity.class));
            }
        });

    }

    /**
     * Updates the main menu if waiting polls is updated,
     * Checks wifi connection, if it's present than initializes
     * JmDnsManager for data transfer between devices in the WLAN,
     * otherwise informs user about the connection stat.
     *
     */
    @Override
    public void onResume() {
        super.onResume();
        //Toast.makeText(MainActivity.this, "called onResume()", Toast.LENGTH_SHORT).show();
        int waitingPollsCount = WaitingPolls.getInstance().getWaitingPolls().size();
        if(waitingPollsCount > 0) {
            binding.waitingPollsActivityButton.setText("Waiting Poll Requests (" + waitingPollsCount + ")");
            binding.waitingPollsActivityButton.setVisibility(View.VISIBLE);
        }

        if(wifiConnected()) {
            connectForDataTransferring();
        } else {
            setTitle("Connecting...");
            SnackHelper.showSnackBar(MainActivity.this, binding.activityMain,
                    "No active Wifi connection. Please connect to an Access Point");
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        //Toast.makeText(this, "called onStop", Toast.LENGTH_SHORT).show();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiReceiver);
    }

    /**
     * Initializes JmDnsManager (connectionManager) by launching an IntentService.
     * Creates a handler to update UI when initialization is completed.
     */
    private void connectForDataTransferring(){
        handler = new MyHandler(this);

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

    /**
     * Registers BroadcastListeners:
     *  <ul>
     * <li> wifiReceiver : broadcast for the wifi connection stat
     * <li> countReceiver : broadcast  for the number of waiting poll requests
     * </ul>
     */
    @Override
    protected void onStart() {
        super.onStart();
        //Toast.makeText(this, "called onStart", Toast.LENGTH_SHORT).show();
        wifiReceiver = createWifiBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(wifiReceiver, new IntentFilter(Receivers.WIFI));

        countReceiver = createWaitingCountBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(countReceiver, new IntentFilter(Receivers.W_COUNT));
    }


    /**
     * When Back button is pressed user will return to Home
    */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Checks if there is a connection and if the active connection type is wifi
     *
     * @return  <code>true</code> if device is connected to a wifi.
     *          <code>true</code> otherwise
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
     * Creates a BroadcastListener to receive changes in Wifi status.
     * When receives the broadcast for the wifi stat:
     * if there is an active wifi connection then updates UI,
     * otherwise informs user about the wifi stat.
     *
     * @return BroadcastReceiver returns a BroadcastReceiver
     */
    private BroadcastReceiver createWifiBroadcastReceiver() {
        Log.v(TAG, "received wifi broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.WIFI)) {
                    boolean stat = intent.getBooleanExtra("wifi", true);
                    setTitle(stat ? "Pollo" : "Connecting...");
                    if(!stat)
                        SnackHelper.showSnackBar(MainActivity.this, binding.activityMain,
                                "No active Wifi connection. Please connect to an Access Point");
                }
            }
        };
    }

    /**
     * Creates a BroadcastListener to receive number of waiting
     * poll requests. When receives the local broadcast,
     * if there is no waiting poll request (count == 0) then
     * hides Waiting Poll Requests button,
     * otherwise updates button's text.
     *
     * @return BroadcastReceiver returns a BroadcastReceiver
     */
    private BroadcastReceiver createWaitingCountBroadcastReceiver() {
        Log.v(TAG, "received waiting count broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.W_COUNT)) {
                    int count = intent.getIntExtra(Consts.COUNT, 0);
                    if(count > 0){
                        binding.waitingPollsActivityButton.setText("Waiting Poll Requests (" + count + ")");
                        binding.waitingPollsActivityButton.setVisibility(View.VISIBLE);
                    }
                    else binding.waitingPollsActivityButton.setVisibility(View.GONE);
                }
            }
        };
    }

    /**
     * Handler class to update UI from jmDNSManager thread
     */
    private static class MyHandler extends Handler{
        private final WeakReference<MainActivity> currentActivity;

        public MyHandler(MainActivity activity){
            currentActivity = new WeakReference<MainActivity>(activity);
        }

        /**
         * Updates Activity's title with the msg from bundle
         * @param msg message string
         */
        @Override
        public void handleMessage(Message msg) {
            Bundle reply = msg.getData();
            MainActivity activity = currentActivity.get();
            if (activity!= null)
                activity.setTitle(reply.getString("result"));
        }
    }

    private class ShowOnlineDevicesDialog extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        AlertDialog.Builder builder;
        private HashSet<String> onlineDevices;

        public ShowOnlineDevicesDialog(MainActivity activity){
            dialog = new ProgressDialog(activity);
            builder = new AlertDialog.Builder(activity);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Checking online devices, please wait...");
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            onlineDevices = (HashSet<String>) jmDnsManager.getOnlineDevices(MainActivity.this);
            return null;
        }

        protected void onPostExecute(Void result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MainActivity.this,
                    android.R.layout.simple_list_item_1,
                    onlineDevices.toArray(new String[onlineDevices.size()]));

            builder.setTitle("Device list: " + "(" + onlineDevices.size() + ")");
            builder.setAdapter(arrayAdapter, null);
            builder.setCancelable(true);
            builder.show();
        }
    }
}
