package mattoncino.pollo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


public class WifiDog extends BroadcastReceiver {
    private static final String TAG = "WIFI_DOG";
    private Intent connManagerServiceIntent;
    private JmDnsManager jmDnsManager;
    private static boolean serviceLaunched = false;
    private static boolean first_on = true;
    private static boolean first_off = true;


    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null && (intent.getAction().equals("android.net.wifi.supplicant.STATE_CHANGE")
        || intent.getAction().equals("android.net.wifi.supplicant.NETWORK_STATE_CHANGED_ACTION")
                || intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE"))) {
                //WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                //if(wifiManager.isWifiEnabled()){
                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

                jmDnsManager = ((MyApplication) context.getApplicationContext()).getConnectionManager();
                //Log.d(TAG, "serviceLaunched: " + serviceLaunched);
                Log.d(TAG, "first_on: " + first_on + " first_off: " + first_off);
                if (activeInfo != null && activeInfo.isConnected() && activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (first_on) {
                    //if (!serviceLaunched) {
                        Log.d(TAG, "WIFI IS ON");

                        Intent connManagerServiceIntent = new Intent(context, ConnectionManagerIntentService.class);
                        context.startService(connManagerServiceIntent);
                        Log.d(TAG, "connManagerServiceIntent is launched");
                        //jmDnsManager.registerService();


                        /*connManagerServiceIntent = new Intent(context, ConnectionManagerIntentService.class);
                        context.startService(connManagerServiceIntent);
                        Log.d(TAG, "WifiDog launches connManagerServiceIntent");*/

                        /*Intent serviceIntent = new Intent(context, StatusUpdaterService.class);
                        context.startService(serviceIntent);
                        Log.d(TAG, "WifiDog launches StatusUpdaterService");*/
                        updateWifiStat(context, true);
                        //serviceLaunched = true;
                        first_on = false;
                        first_off = true;
                    }
                } else {
                    if(first_off) {
                        //if (serviceLaunched) {
                        jmDnsManager.unregisterService();
                        Log.d(TAG, "WIFI IS OFF");

                        /*context.stopService(connManagerServiceIntent);
                        Log.d(TAG, "WifiDog stops connManagerServiceIntent");*/

                        /*Intent statusService = new Intent(context, StatusUpdaterService.class);
                        context.stopService(statusService);
                        Log.d(TAG, "WifiDog stops StatusUpdaterService");*/
                        updateWifiStat(context, false);
                        //serviceLaunched = false;
                        first_off = false;
                        first_on = true;
                    }
                  }
                //}
            }
    }


    private void updateWifiStat(Context context, boolean stat) {
        Intent intent = new Intent("mattoncino.pollo.receive.wifi.stat");
        intent.putExtra("wifi", stat);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
