package mattoncino.pollo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {
    private ServiceConnectionManager manager;
    private String deviceId = "";
    //public static Context currentContext;
    private static final Type LIST_TYPE = new TypeToken<List<Poll>>() {}.getType();
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    public static ArrayList active_polls;
    private static MyApplication instance;

    public ServiceConnectionManager getConnectionManager() {
        if (manager == null)
            manager = new ServiceConnectionManager();
        return manager;
    }

    @Override
    public void onCreate() {

        instance = this;

        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getDeviceId() != null) {
            setDeviceId(tm.getDeviceId()); //use for mobiles
        } else {
            setDeviceId(Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID)); //use for tablets
        }

        manager = new ServiceConnectionManager();


        pref = getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        active_polls = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);

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

    public ArrayList<Poll> getActivePolls(){
        return active_polls;
    }
}
