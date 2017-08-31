package mattoncino.pollo;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
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
    private ActivityMainBinding binding;
    public static boolean exist_active_pool = true;
    public static boolean exist_saved_pool = true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

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
                //if(wifiConnected())
                    startActivity(new Intent(MainActivity.this, mattoncino.pollo.MultiOptPollActivity.class));
            }
        });

        binding.showDeviceListButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //if(wifiConnected()) {
                    //connectionManager.unregisterService();
                    //connectionManager.registerService();
                    onShowOnlineDevicesListDialogPress();
                //}
            }
        });


        binding.activePollsActivityButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, mattoncino.pollo.ActivePollsActivity.class));
            }
        });


        binding.oldPollsActivityButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, mattoncino.pollo.OldPollsActivity.class));
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if(wifiConnected()) {
            startDataTransferring();
            Log.d(TAG, "connManagerServiceIntent is launched");
            binding.createPollActivityButton.setEnabled(true);
            binding.activePollsActivityButton.setEnabled(true);
            binding.oldPollsActivityButton.setEnabled(true);
            binding.showDeviceListButton.setEnabled(true);
        }
    }

    private void startDataTransferring(){
        //connectionManager = ((MyApplication)getApplication()).getConnectionManager();
        Intent connManagerServiceIntent = new Intent(this, ConnectionManagerIntentService.class);
        startService(connManagerServiceIntent);

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
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected() && activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.i(TAG, getString(R.string.wifi_connection));
            return true;
        } else {
            Log.i(TAG, getString(R.string.mobile_connection) + " or " + R.string.no_wifi_or_mobile);
            Toast.makeText(this, "Pollo works only under LAN. Please activate your wifi and connect to an Access Point", Toast.LENGTH_LONG).show();
            return false;
        }
    }

/* Check if online device is empty, in case we will disable features.
    public boolean checkOnlineDevices(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String deviceId = ((MyApplication)getApplication()).getDeviceId();
                final List<String> onlineDevices = connectionManager.getOnlineDevicesList(MainActivity.this, deviceId);

                return onlineDevices.size() != 0;

            }
        };
        Thread thread = new Thread(runnable);
        thread.start();

    }
*/

    public void onShowOnlineDevicesListDialogPress(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String deviceId = ((MyApplication)getApplication()).getDeviceId();
                ServiceConnectionManager connectionManager = ((MyApplication)getApplication()).getConnectionManager();
                final HashSet<String> onlineDevices = (HashSet<String>) connectionManager.getOnlineDevices(MainActivity.this);

                //Activity act = (Activity) MainActivity.this;
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
        arrayAdapter.notifyDataSetChanged();
        builderSingle.show();
    }


}
