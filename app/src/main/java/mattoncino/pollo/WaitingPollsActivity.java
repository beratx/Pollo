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

public class WaitingPollsActivity extends AppCompatActivity implements Observer {
    private static final String TAG = "ActivePollsActivity";
    private ActivityWaitingPollsBinding binding;
    private BroadcastReceiver removeReceiver;
    private BroadcastReceiver wifiReceiver;
    private WaitingPolls waitingManager;
    private RecyclerView.Adapter adapter;
    private JmDnsManager jmDnsManager;
    private boolean connected;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_waiting_polls);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setTitle("Waiting Poll Requests");

        waitingManager = WaitingPolls.getInstance();
        waitingManager.addObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        jmDnsManager = ((MyApplication) getApplication()).getConnectionManager();
        if(jmDnsManager == null || !jmDnsManager.initialized()){
            setTitle("Connecting...");
            ToastHelper.showSnackBar(WaitingPollsActivity.this, binding.activityActivePolls,
                    "No active Wifi connection. Please connect to an Access Point.");
            connected = false;
        } else connected = true;


        List<WaitingData> waitingPolls = Collections.synchronizedList(waitingManager.getWaitingPolls());
        adapter = new WaitingPollsAdapter(waitingPolls);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    public void update(Observable observable, Object o) {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            Log.i(TAG, "waitingPollList is modified, adapter is notified");
        }
    }

    @Override
    protected void onStart(){
        super.onStart();

        wifiReceiver = createWifiBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(wifiReceiver, new IntentFilter(Receivers.WIFI));

        removeReceiver = createRemoveBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(removeReceiver, new IntentFilter(Receivers.W_REMOVE));
    }


    private BroadcastReceiver createRemoveBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.W_REMOVE)) {
                    Log.v(TAG, "received waiting remove broadcast");
                    int notId = intent.getIntExtra(Consts.NOTIFICATION_ID,0);
                    waitingManager.removeData(notId);
                    waitingManager.savetoWaitingList();
                    adapter.notifyDataSetChanged();
                }
            }
        };
    }

    private BroadcastReceiver createWifiBroadcastReceiver() {
        Log.v(TAG, "received wifi broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.WIFI)) {
                    boolean stat = intent.getBooleanExtra("wifi", true);
                    setTitle(stat ? "Waiting Poll Requests" : "Connecting...");
                    if(!stat)
                        ToastHelper.showSnackBar(WaitingPollsActivity.this, binding.recyclerView,
                                "No active Wifi connection. Please connect to an Access Point");
                }
            }
        };
    }


    /*private BroadcastReceiver createAddBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.W_ADD)) {
                    Log.v(TAG, "received waiting add broadcast");
                    int notId = intent.getIntExtra(Consts.NOTIFICATION_ID, 0);
                    Poll poll = intent.getParcelableExtra(Consts.POLL);
                    String hostAddress = intent.getStringExtra(Consts.ADDRESS);
                    waitingManager.addData(new WaitingData(poll, notId, hostAddress));
                    waitingManager.savetoWaitingList();
                    adapter.notifyDataSetChanged();
                }
            }
        };
    }*/
}
