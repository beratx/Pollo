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
    //Intent statusUpdater;
    private static boolean first_on = true;
    private static boolean first_off = true;


    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null && (intent.getAction().equals("android.net.wifi.supplicant.STATE_CHANGE")
        || intent.getAction().equals("android.net.wifi.supplicant.NETWORK_STATE_CHANGED_ACTION")
                || intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE"))) {

                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

                jmDnsManager = ((MyApplication) context.getApplicationContext()).getConnectionManager();

                //statusUpdater = new Intent(context, StatusUpdaterService.class);
                //Log.d(TAG, "serviceLaunched: " + serviceLaunched);
                Log.d(TAG, "first_on: " + first_on + " first_off: " + first_off);
                if (activeInfo != null && activeInfo.isConnected() && activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (first_on) {
                        Log.d(TAG, "WIFI IS ON");

                        connManagerServiceIntent = new Intent(context, ConnectionManagerIntentService.class);
                        context.startService(connManagerServiceIntent);
                        Log.d(TAG, "WifiDog launches connManagerServiceIntent");

                        /*context.startService(statusUpdater);
                        Log.d(TAG, "WifiDog launches StatusUpdaterService");*/

                        updateWifiStat(context, true);

                        first_on = false;
                        first_off = true;
                    }
                } else {
                    if(first_off) {
                        Log.d(TAG, "WIFI IS OFF");
                        if(jmDnsManager != null) {
                            jmDnsManager.unregisterService();
                            Log.d(TAG, "WifiDog stops jmDNS service");
                        }

                        /*context.stopService(statusUpdater);
                        Log.d(TAG, "WifiDog stops StatusUpdaterService");*/

                        updateWifiStat(context, false);

                        first_off = false;
                        first_on = true;
                    }
                  }
            }
    }

    private void updateWifiStat(Context context, boolean stat) {
        Intent intent = new Intent("mattoncino.pollo.receive.wifi.stat");
        intent.putExtra("wifi", stat);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
