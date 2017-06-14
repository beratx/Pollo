package mattoncino.pollo;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public class MyApplication extends Application {
    private ServiceConnectionManager manager;
    private String deviceId = "";
    public static Context currentContext;

    public ServiceConnectionManager getConnectionManager() {
        if (manager == null)
            manager = new ServiceConnectionManager();
        return manager;
    }

    @Override
    public void onCreate() {

        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getDeviceId() != null) {
            setDeviceId(tm.getDeviceId()); //use for mobiles
        } else {
            setDeviceId(Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID)); //use for tablets
        }

        manager = new ServiceConnectionManager();

    }

    private void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
