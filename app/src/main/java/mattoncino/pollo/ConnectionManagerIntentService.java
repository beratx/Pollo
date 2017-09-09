package mattoncino.pollo;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;


public class ConnectionManagerIntentService extends IntentService {
    private static final String TAG = "ConnManagerService";
    private JmDnsManager jManager;

    public ConnectionManagerIntentService() {
        super("ConnectionManagerIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Connection Manager Service is started.");
        //Bundle data = intent.getData();
        jManager = ((MyApplication)getApplication()).getConnectionManager();
        jManager.initializeService(this);
    }
}
