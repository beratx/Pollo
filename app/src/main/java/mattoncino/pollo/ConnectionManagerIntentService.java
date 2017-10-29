package mattoncino.pollo;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Messenger;
import android.util.Log;

/**
 * An IntentService to launch JmDnsManager (network connection manager)
 */
public class ConnectionManagerIntentService extends IntentService {
    private static final String TAG = "ConnectionManagerIntSrv";
    private JmDnsManager jManager;
    private Messenger messenger;

    public ConnectionManagerIntentService() {
        super("ConnectionManagerIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            messenger = (Messenger) bundle.get("messenger");
        }

        Log.i(TAG, "Connection Manager Service is started.");
        jManager = ((MyApplication)getApplication()).getConnectionManager();
        jManager.initializeService(this, messenger);



    }
}
