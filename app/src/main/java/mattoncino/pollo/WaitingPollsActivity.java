package mattoncino.pollo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import mattoncino.pollo.databinding.ActivityWaitingPollsBinding;


/**
 * Activity to display and interact with the Waiting Poll Requests,
 * that are Poll requests received from other devices but not accepted
 * nor rejected by the user yet.
 */
public class WaitingPollsActivity extends AppCompatActivity implements Observer {
    private static final String TAG = "ActivePollsActivity";
    private ActivityWaitingPollsBinding binding;
    private BroadcastReceiver removeReceiver;
    private BroadcastReceiver wifiReceiver;
    private WaitingPolls waitingManager;
    private RecyclerView.Adapter adapter;
    private JmDnsManager jmDnsManager;
    private boolean connected;


    /**
     *  Binds layout and initializes WaitingPolls manager
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_waiting_polls);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setTitle("Waiting Poll Requests");

        waitingManager = WaitingPolls.getInstance();
        waitingManager.addObserver(this);
    }

    /**
     * Checks the wifi connection and if not present,
     * displays a message to inform the user.
     * Then gets waiting polls list and sets the adapter
     * to display the waiting polls.
     */
    @Override
    protected void onResume() {
        super.onResume();

        jmDnsManager = ((MyApplication) getApplication()).getConnectionManager();
        if(jmDnsManager == null || !jmDnsManager.initialized()){
            setTitle("Connecting...");
            SnackHelper.showSnackBar(WaitingPollsActivity.this, binding.activityActivePolls,
                    "No active Wifi connection. Please connect to an Access Point.");
            connected = false;
        } else connected = true;


        List<WaitingData> waitingPolls = Collections.synchronizedList(WaitingPolls.getInstance().getWaitingPolls());
        adapter = new WaitingPollsAdapter(waitingPolls);
        binding.recyclerView.setAdapter(adapter);
    }

    /**
     * Notifies adapter when a change has occurred in the state of the
     * observable object.
     *
     * @param observable
     * @param o
     */
    @Override
    public void update(Observable observable, Object o) {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            Log.i(TAG, "waitingPollList is modified, adapter is notified");
        }
    }

    /**
     * Register BroadcastListeners:
     * <ul>
     * <li> wifiReceiver : broadcast for the wifi connection stat
     * <li> removeReceiver : broadcast received when the user rejects
     *                       a poll, so it must be removed from the list
     */
    @Override
    protected void onStart(){
        super.onStart();

        wifiReceiver = createWifiBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(wifiReceiver, new IntentFilter(Receivers.WIFI));

        removeReceiver = createRemoveBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(removeReceiver, new IntentFilter(Receivers.W_REMOVE));
    }

    @Override
    protected void onStop(){
        super.onStop();
        waitingManager.savetoWaitingList();
    }

    /**
     * Creates a BroadcastListener to receive a remove message for
     * a poll. When receives it, removes the poll from the list
     *
     * @return BroadcastReceiver
     */
    private BroadcastReceiver createRemoveBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.W_REMOVE)) {
                    Log.v(TAG, "received waiting remove broadcast");
                    int notId = intent.getIntExtra(Consts.NOTIFICATION_ID,0);
                    waitingManager.removeData(notId);
                    //waitingManager.savetoWaitingList();
                    adapter.notifyDataSetChanged();
                }
            }
        };
    }

    /**
     * Creates a BroadcastListener to receive changes in Wifi status.
     * When receives the broadcast for the wifi state:
     * if there is an active wifi connection then updates the UI,
     * otherwise informs user about the wifi state.
     *
     * @return BroadcastReceiver
     */
    private BroadcastReceiver createWifiBroadcastReceiver() {
        Log.v(TAG, "received wifi broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.WIFI)) {
                    boolean stat = intent.getBooleanExtra("wifi", true);
                    setTitle(stat ? "Waiting Poll Requests" : "Connecting...");
                    if(!stat)
                        SnackHelper.showSnackBar(WaitingPollsActivity.this, binding.recyclerView,
                                "No active Wifi connection. Please connect to an Access Point");
                }
            }
        };
    }

}
