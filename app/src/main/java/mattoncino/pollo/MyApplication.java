package mattoncino.pollo;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

/**
 * Singleton Class that initializes JmDnsManager
 * and returns its only instance through its method
 */

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private JmDnsManager manager;
    private String deviceId = "";
    private static MyApplication instance;

    /**
     * Get device id for the device and initializes
     * JmDNSManager instance
     *
     * @see JmDnsManager
     */
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
    }

    /**
     * If not created yet, creates a new JmDnsManager
     * otherwise returns the existing one
     *
     * @return JmDNsManager instance
     * @see JmDnsManager
     */
    public JmDnsManager getConnectionManager() {
        if (manager == null)
            manager = new JmDnsManager();
        return manager;
    }

    /**
     * Returns MyApplication class instance
     *
     * @return  MyApplication class instance
     */
    public static MyApplication getContext(){
        return instance;
    }

    /**
     * Sets device id of the device
     *
     * @param deviceId devices id
     */
    private void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns device id
     * @return device id
     */
    public String getDeviceId() {
        return deviceId;
    }

}
