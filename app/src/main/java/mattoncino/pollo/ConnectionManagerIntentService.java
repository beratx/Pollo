package mattoncino.pollo;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Messenger;
import android.util.Log;

/**
 * An IntentService to initiliaze JmDnsManager (network connection manager)
 *
 * @see JmDnsManager
 */
public class ConnectionManagerIntentService extends IntentService {
    private static final String TAG = "ConnectionManagerIntSrv";
    private JmDnsManager jManager;
    private Messenger messenger;

    public ConnectionManagerIntentService() {
        super("ConnectionManagerIntentService");
    }

    /**
     * Initiliazes JmDns connection manager and passes messenger to it
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            messenger = (Messenger) bundle.get("messenger");
        }

        jManager = ((MyApplication)getApplication()).getConnectionManager();
        jManager.initializeService(this, messenger);
        Log.i(TAG, "Connection Manager Service is started.");
    }
}
