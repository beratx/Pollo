package mattoncino.pollo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


/**
 * WifiDog is a BroadcastReceiver which monitors the wifi connection state
 * and start or terminates connection manager respect to this state.
 */
public class WifiDog extends BroadcastReceiver {
    private static final String TAG = "WifiDog";
    private Intent connManagerServiceIntent;
    private JmDnsManager jmDnsManager;
    private static boolean first_on = true;
    private static boolean first_off = true;


    /**
     * If a wifi connection is established then starts a JmDnsManager,
     * Otherwise terminates JmDnsManager.
     * And sends a local  broadcast message to update UI.
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null && (intent.getAction().equals("android.net.wifi.supplicant.STATE_CHANGE")
            || intent.getAction().equals("android.net.wifi.supplicant.NETWORK_STATE_CHANGED_ACTION")
                || intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE"))) {

                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

                jmDnsManager = ((MyApplication) context.getApplicationContext()).getConnectionManager();

                if (activeInfo != null && activeInfo.isConnected()
                        && activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (first_on) {
                        Log.i(TAG, "WIFI IS ON");

                        if(!jmDnsManager.initialized()) {
                            connManagerServiceIntent = new Intent(context, ConnectionManagerIntentService.class);
                            context.startService(connManagerServiceIntent);
                            Log.i(TAG, "WifiDog launches connManagerServiceIntent");
                        }

                        updateWifiStat(context, true);

                        first_on = false;
                        first_off = true;
                    }
                } else {
                    if(first_off) {
                        Log.i(TAG, "WIFI IS OFF");

                        updateWifiStat(context, false);

                        if(jmDnsManager.initialized()) {
                            Log.i(TAG, "WifiDog stops jmDNS service");
                            jmDnsManager.unregisterService();
                        }

                        first_off = false;
                        first_on = true;
                    }
                  }
            }
    }

    /**
     * Sends a broadcast message about the wifi connection state
     * to the other activities in order to update their UI
     * @param context
     * @param stat
     */
    private void updateWifiStat(Context context, boolean stat) {
        Intent intent = new Intent(Receivers.WIFI);
        intent.putExtra(Consts.WIFI, stat);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
