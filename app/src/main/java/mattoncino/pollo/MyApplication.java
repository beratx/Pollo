package mattoncino.pollo;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private JmDnsManager manager;
    private String deviceId = "";
    private static MyApplication instance;

    @Override
    public void onCreate() {

        instance = this;

        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getDeviceId() != null) {
            setDeviceId(tm.getDeviceId()); //use for mobiles
        } else {
            setDeviceId(Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID)); //use for tablets
        }

        manager = new JmDnsManager();

        Intent mServiceIntent = new Intent(this, StatusUpdaterService.class);
        startService(mServiceIntent);
        Log.d(TAG, "StatusUpdaterService is launched");

    }

    public JmDnsManager getConnectionManager() {
        if (manager == null)
            manager = new JmDnsManager();
        return manager;
    }

    public static MyApplication getContext(){
        return instance;
    }

    private void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

}
