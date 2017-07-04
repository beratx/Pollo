package mattoncino.pollo;

import android.app.IntentService;
import android.content.Intent;


public class ConnectionManagerService extends IntentService {
    private ServiceConnectionManager connectionManager;

    public ConnectionManagerService() {
        super("ConnectionManagerService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        //Bundle data = intent.getData();
        connectionManager = ((MyApplication)getApplication()).getConnectionManager();
        connectionManager.initializeService(this);
    }
}
