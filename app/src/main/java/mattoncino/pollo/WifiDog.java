package mattoncino.pollo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


public class WifiDog extends BroadcastReceiver {
    private static final String TAG = "WifiDog";
    final static private long THIRTY_SECONDS = 1000 * 30;
    private Intent connManagerServiceIntent;
    private JmDnsManager jmDnsManager;
    //Intent statusUpdater;
    private static boolean first_on = true;
    private static boolean first_off = true;
    private AlarmManager alarm;
    //private BroadcastReceiver alarmReceiver;
    private PendingIntent pintent;



    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null && (intent.getAction().equals("android.net.wifi.supplicant.STATE_CHANGE")
            || intent.getAction().equals("android.net.wifi.supplicant.NETWORK_STATE_CHANGED_ACTION")
                || intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE"))) {

                ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

                jmDnsManager = ((MyApplication) context.getApplicationContext()).getConnectionManager();

                /*Intent amIntent = new Intent(context, StatusUpdaterService.class);
                pintent = PendingIntent.getService(context, 0, amIntent, 0);
                alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);*/

                //statusUpdater = new Intent(context, StatusUpdaterService.class);
                //Log.d(TAG, "serviceLaunched: " + serviceLaunched);
                Log.d(TAG, "first_on: " + first_on + " first_off: " + first_off);
                if (activeInfo != null && activeInfo.isConnected() && activeInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (first_on) {
                        Log.i(TAG, "WIFI IS ON");

                        if(!jmDnsManager.initialized()) {
                            connManagerServiceIntent = new Intent(context, ConnectionManagerIntentService.class);
                            context.startService(connManagerServiceIntent);
                            Log.i(TAG, "WifiDog launches connManagerServiceIntent");
                        }

                        //setAlarm(context);

                        /*context.startService(statusUpdater);
                        Log.d(TAG, "WifiDog launches StatusUpdaterService");*/

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

                        /*if(alarm != null)
                            alarm.cancel(pintent);*/

                        /*context.stopService(statusUpdater);
                        Log.d(TAG, "WifiDog stops StatusUpdaterService");*/

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

    private void setAlarm(Context context) {
        //alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, cal.getTimeInMillis(), 30*1000, pintent);
        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(), THIRTY_SECONDS, pintent);
    }
}
