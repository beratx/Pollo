package mattoncino.pollo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import mattoncino.pollo.databinding.ActivityActivePollsBinding;

public class ActivePollsActivity extends AppCompatActivity implements Observer {
    private static final String TAG = "ActivePollsActivity";
    private ActivityActivePollsBinding binding;
    private BroadcastReceiver updateReceiver;
    private BroadcastReceiver acceptReceiver;
    private BroadcastReceiver resultReceiver;
    private BroadcastReceiver removeReceiver;
    private BroadcastReceiver wifiReceiver;
    private ArrayList<PollData> active_polls;
    private RecyclerView.Adapter adapter;
    private JmDnsManager jmDnsManager;
    private PollManager manager;
    private Poll poll;
    private boolean myPollRequest = false;
    private boolean acceptedPollRequest = false;
    private boolean connected = true;
    private String xhostAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Toast.makeText(this, "called onCreate", Toast.LENGTH_LONG).show();
        setTitle("Active Polls");
        binding = DataBindingUtil.setContentView(this, R.layout.activity_active_polls);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        jmDnsManager = ((MyApplication) getApplication()).getConnectionManager();
        if(jmDnsManager == null || !jmDnsManager.initialized()){
            setTitle("Connecting...");
            connected = false;
        }

        manager = PollManager.getInstance();
        manager.addObserver(this);


        final Bundle data = getIntent().getExtras();
        if (data != null) {
            final int type = data.getInt(Consts.OWNER);
            poll = data.getParcelable(Consts.POLL);
            int notfID = data.getInt("notificationID");

            switch (type) {
                case Consts.OWN:
                    String ownAddress = null;
                    try {
                        ownAddress = jmDnsManager.getHostAddress();
                    } catch(NullPointerException e){
                        Log.e(TAG, e.toString() + " jmdnsManager is not initialized");
                        ToastHelper.showSnackBar(ActivePollsActivity.this, binding.activityActivePolls,
                                "No active Wifi connection. Failed to send poll.");
                    }
                    manager.addPoll(new PollData(poll, ownAddress, type));
                    myPollRequest = true;
                    break;
                case Consts.OTHER:
                    //TODO: dont remove the notification if there is no connection
                    boolean accepted = data.getBoolean(Consts.ACCEPT, true);
                    if(!connected) {
                        ToastHelper.showSnackBar(ActivePollsActivity.this, binding.activityActivePolls,
                                "No active Wifi connection. Failed to receive poll.");
                    } else if (accepted) {
                        xhostAddress = data.getString("hostAddress");
                        manager.addPoll(new PollData(poll, xhostAddress, type));
                        acceptedPollRequest = true;
                        if (poll.hasImage())
                            saveImagePermanently();
                    } else { //not accepted
                        if (poll.hasImage())
                            removeImageFromCache();
                        data.remove(Consts.POLL);
                    }

                    break;
            }
            removeNotification(notfID);
        }
        else if(!connected)
            ToastHelper.showSnackBar(ActivePollsActivity.this, binding.activityActivePolls,
                    "No active Wifi connection. Please connect to an Access Point.");
    }

    private void removeImageFromCache(){
        String imagePath = poll.getImageInfo().getPath();
        File image = new File(imagePath);
        boolean r = image.delete();
        if (r) Log.d(TAG, "image in cache is deleted.");
    }

    private void saveImagePermanently(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                File temp = new File(poll.getImageInfo().getPath());
                Uri tempUri = Uri.fromFile(temp);
                String ext = ImagePicker.getImageType(ActivePollsActivity.this, tempUri);

                File perm = ImagePicker.createFile(ActivePollsActivity.this,
                        ImagePicker.isExternalStorageWritable(), ext);
                try {
                    ImagePicker.savePermanently(temp, perm);
                    Uri permUri = Uri.fromFile(perm);
                    poll.getImageInfo().setPath(permUri.toString());
                } catch (IOException e) {
                    Log.d(TAG, "ImagePicker.savePermanently(): " + e.toString());
                    e.printStackTrace();
                }

            }
        }).start();
    }

    @Override
    protected void onStart(){
        super.onStart();
        //Toast.makeText(this, "called onStart", Toast.LENGTH_LONG).show();
        wifiReceiver = createWifiBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(wifiReceiver, new IntentFilter(Receivers.WIFI));

        acceptReceiver = createAcceptBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(acceptReceiver, new IntentFilter(Receivers.ACCEPT));

        updateReceiver = createUpdateBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, new IntentFilter(Receivers.VOTE));

        resultReceiver = createResultBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(resultReceiver, new IntentFilter(Receivers.RESULT));

        removeReceiver = createRemoveBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(removeReceiver, new IntentFilter(Receivers.REMOVE));

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Toast.makeText(this, "called onResume", Toast.LENGTH_LONG).show();

        List<PollData> activePolls = Collections.synchronizedList(PollManager.getInstance().getActivePolls());

        //adapter = new PollsCardViewAdapter(manager.getActivePolls());
        adapter = new PollsCardViewAdapter(activePolls);
        binding.recyclerView.setAdapter(adapter);


        if (myPollRequest) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        HashSet<String> contactedDevices =
                                (HashSet<String>) jmDnsManager.sendMessageToAllDevicesInNetwork(
                                                    ActivePollsActivity.this, Consts.REQUEST, poll);

                        manager.setContactedDevices(poll.getId(), contactedDevices);

                        Log.d(TAG, "jmDnsManager.sendMessageToAllDevices: " + contactedDevices.size() + " device(s) to contact!!!");
                    } catch (NullPointerException e) {
                        Log.wtf(TAG, "jmDnsManager.sendMessageToAllDevicesInNetwork: " + e.toString());
                    }

                    myPollRequest = false;

                }
            }).start();

        } else if (acceptedPollRequest) {
            ArrayList<String> pollInfo = new ArrayList<>();
            pollInfo.add(poll.getId());

            ClientThreadProcessor clientProcessor = new ClientThreadProcessor(xhostAddress,
                    ActivePollsActivity.this, Consts.ACCEPT, pollInfo);
            Thread t = new Thread(clientProcessor);
            t.start();

            acceptedPollRequest = false;
        }
    }

    private BroadcastReceiver createWifiBroadcastReceiver() {
        Log.v(TAG, "received wifi broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.WIFI)) {
                    boolean connected = intent.getBooleanExtra("wifi", false);
                    if(!connected){
                        setTitle("Connecting...");
                        ToastHelper.showSnackBar(ActivePollsActivity.this, binding.activityActivePolls,
                                "No active Wifi connection. Please connect to an Access Point.");
                    }
                    else setTitle("Active Polls");
                }
            }
        };
    }


    private BroadcastReceiver createUpdateBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.VOTE)) {
                    Log.v(TAG, "received update broadcast");
                    String pollID = intent.getStringExtra("pollID");
                    boolean isMyVote = intent.getBooleanExtra("myVote", false);
                    if(!isMyVote) {
                        int vote = intent.getIntExtra("vote", -1);
                        if (vote != -1) {
                            String hostAddress = intent.getStringExtra("hostAddress");
                            manager.updatePoll(pollID, hostAddress, vote);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    intent.removeExtra("pollID");
                    intent.removeExtra("vote");
                    intent.removeExtra("hostAddress");
                }
            }
        };
    }

    private BroadcastReceiver createAcceptBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.ACCEPT)) {
                    Log.d(TAG, "received accept broadcast");
                    String pollID = intent.getStringExtra("pollID");
                    String hostAddress = intent.getStringExtra("hostAddress");
                    boolean accepted = intent.getBooleanExtra("accepted", false);

                    manager.updateAcceptedDeviceList(pollID, hostAddress, accepted);

                    intent.removeExtra("pollID");
                    intent.removeExtra("hostAddress");
                    intent.removeExtra("accepted");
                    adapter.notifyDataSetChanged();
                }
            }
        };
    }

    private BroadcastReceiver createResultBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.RESULT)) {
                    Log.v(TAG, "received result broadcast");
                    String pollID = intent.getStringExtra("pollID");
                    int[] result = (int[]) intent.getSerializableExtra(Consts.RESULT);
                    //inside sets also terminated flag
                    manager.setVotes(pollID, result);
                    adapter.notifyDataSetChanged();

                    intent.removeExtra("pollID");
                    intent.removeExtra(Consts.RESULT);
                }
            }
        };
    }

    private BroadcastReceiver createRemoveBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null &&
                        intent.getAction().equals("mattoncino.pollo.receive.poll.remove")) {
                    Log.v(TAG, "received result broadcast");
                    String pollID = intent.getStringExtra("pollID");
                    manager.removePoll(pollID);
                    intent.removeExtra("pollID");
                }
            }
        };
    }



    //called when a change has occurred in the state of the observable
    @Override
    public void update(Observable observable, Object o) {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            Log.i(TAG, "activePollList is modified, adapter is notified");
        }
    }

    @Override
    protected void onRestart() {
        //Toast.makeText(this, "called onRestart", Toast.LENGTH_LONG).show();
        super.onRestart();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        //Toast.makeText(this, "called onSaveInstanceState", Toast.LENGTH_LONG).show();
        Log.v(TAG, "called onSaveInstanceState");
        outState.putParcelableArrayList("pollList", active_polls);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        Log.v(TAG, "called onRestoreInstanceState");
        //Toast.makeText(this, "called onRestoreInstanceState", Toast.LENGTH_LONG).show();
        if (savedInstanceState != null) {
            active_polls = savedInstanceState.getParcelableArrayList("pollList");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Toast.makeText(this, "called onPause", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        //Toast.makeText(this, "called onStop", Toast.LENGTH_LONG).show();
        super.onStop();
        manager.savePollsPermanently();

        /*if (updateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        }
        if (acceptReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(acceptReceiver);
        }
        if (resultReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(resultReceiver);
        }
        if (removeReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(resultReceiver);
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this, "called Destroy", Toast.LENGTH_LONG).show();


    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ActivePollsActivity.this, MainActivity.class));
    }

    private void removeNotification(int notificationID) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationID);
    }



}


