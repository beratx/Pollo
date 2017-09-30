package mattoncino.pollo;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;


public class ConnectionManagerIntentService extends IntentService {
    private static final String TAG = "ConnectionManagerIntSrv";
    private JmDnsManager jManager;

    public ConnectionManagerIntentService() {
        super("ConnectionManagerIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Connection Manager Service is started.");
        jManager = ((MyApplication)getApplication()).getConnectionManager();
        jManager.initializeService(this);
    }
}
