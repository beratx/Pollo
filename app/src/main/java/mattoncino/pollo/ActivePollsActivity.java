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

/**
 * Activity to display and interact with created/received Polls.
 * User can:
 * <ul>
 * <li> vote for Polls
 * <li> terminate Polls created by him/her
 * <li> remove Polls from list
 * <li> receive results for accepted Polls
 * </ul>
 *
 */
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

    /**
     * Firstly  checks connection and initializes PollManager.
     * If there is a new created/accepted Poll, adds it to the list
     * and saves properly its media.
     *
     * @param savedInstanceState
     */
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
                        SnackHelper.showSnackBar(ActivePollsActivity.this, binding.activityActivePolls,
                                "No active Wifi connection. Failed to send poll.");
                    }
                    manager.addPoll(new PollData(poll, ownAddress, type));
                    myPollRequest = true;
                    break;
                case Consts.OTHER:
                    boolean accepted = data.getBoolean(Consts.ACCEPT, true);
                    if(!connected) {
                        SnackHelper.showSnackBar(ActivePollsActivity.this, binding.activityActivePolls,
                                "No active Wifi connection. Failed to receive poll.");
                    } else if (accepted) {
                        xhostAddress = data.getString("hostAddress");
                        manager.addPoll(new PollData(poll, xhostAddress, type));
                        acceptedPollRequest = true;
                        if (poll.hasImage())
                            saveImagePermanently();
                        if(poll.hasRecord())
                            saveRecordPermanently();
                    } else { //not accepted
                        if (poll.hasImage())
                            removeFromCache(poll.getImageInfo().getPath().substring(7));
                        if(poll.hasRecord())
                            removeFromCache(poll.getRecordPath());
                        data.remove(Consts.POLL);
                    }
                    break;
                case Consts.WAITED:
                    if(!connected) {
                        SnackHelper.showSnackBar(ActivePollsActivity.this, binding.activityActivePolls,
                                "No active Wifi connection. Failed to receive poll.");
                    } else {
                        xhostAddress = data.getString("hostAddress");
                        manager.addPoll(new PollData(poll, xhostAddress, Consts.OTHER));
                        acceptedPollRequest = true;
                        if (poll.hasImage())
                            saveImagePermanently();
                        if (poll.hasRecord())
                            saveRecordPermanently();
                    }
                    break;
            }
            WaitingPolls.sendRemoveBroadcast(ActivePollsActivity.this, notfID);
            removeNotification(notfID);
        }
        else if(!connected)
            SnackHelper.showSnackBar(ActivePollsActivity.this, binding.activityActivePolls,
                    "No active Wifi connection. Please connect to an Access Point.");
    }


    /**
     * Removes the file in the given path from the app's cache
     * @param path path of a file as a string
     */
    private void removeFromCache(String path){
        File file = new File(path);
        boolean r = file.delete();
        if (r) Log.d(TAG, "image in cache is deleted.");
    }


    /**
     * Save poll's sound record file permanently,
     * in another directory other than cache directory
     */
    private void saveRecordPermanently(){
        File temp = new File(poll.getRecordPath());
        Log.d(TAG, "temp.path: " + temp.getPath());
        File perm = new File(SoundRecord.createFile2(ActivePollsActivity.this, "3gp"));
        Log.d(TAG, "perm.path: " + perm.getPath());

        boolean r = temp.renameTo(perm);

        if(r) poll.setRecordPath(perm.getPath());
        else Log.d(TAG, "Can't rename record file!");
    }

    /**
     * Save poll's image file permanently,
     * in another directory other than cache directory
     */
    private void saveImagePermanently(){
        String tempPath = poll.getImageInfo().getPath().substring(7);
        File temp = new File(tempPath);
        Uri tempUri = Uri.fromFile(temp);
        String ext = ImagePicker.getImageType(ActivePollsActivity.this, tempUri);
        Log.d(TAG, "temp.path: " + temp.getPath());

        try {
            File perm = ImagePicker.createFile(ActivePollsActivity.this, ext);
            Log.d(TAG, "perm.path: " + perm.getPath());

            boolean r = temp.renameTo(perm);

            if(r) poll.getImageInfo().setPath(Uri.fromFile(perm).toString());
            else Log.wtf(TAG, "can't rename image file!");
        } catch (IOException e) {
            Log.d(TAG, "ImagePicker.savePermanently(): " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Register BroadcastListeners:
     * <ul>
     * <li> wifiReceiver : broadcast for the wifi connection stat
     * <li> acceptReceiver : broadcast received when another user accepts a poll
     *                       request sent from this user
     * <li> updateReceiver : broadcast received when another user votes for a poll
     *                       sent from this user
     * <li> resultReceiver : broadcast received when an accepted poll receives the
     *                       result
     * <li> removeReceiver : broadcast received when user removes a poll from list
     * </ul>
     */
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

    /**
     * Creates an adapter to display Poll list. Polls are displayed as separate cards.
     * If user has created a new poll, then sends a request to all active devices.
     * If user has accepted a poll request, sends an accept message to the owner of poll.
     * Otherwise just displays the Poll list.
     */
    @Override
    protected void onResume() {
        super.onResume();
        //Toast.makeText(this, "called onResume", Toast.LENGTH_LONG).show();

        List<PollData> activePolls = Collections.synchronizedList(PollManager.getInstance().getActivePolls());

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
                    boolean connected = intent.getBooleanExtra("wifi", false);
                    if(!connected){
                        setTitle("Connecting...");
                        SnackHelper.showSnackBar(ActivePollsActivity.this, binding.activityActivePolls,
                                "No active Wifi connection. Please connect to an Access Point.");
                    }
                    else setTitle("Active Polls");
                }
            }
        };
    }


    /**
     * Creates a BroadcastListener to receive an update for a poll.
     * When receives the broadcast:
     * if the vote is from another user, then update the voted poll
     * and notifies adapter for data change
     * otherwise just notifies adapter for data change
     *
     * @return BroadcastReceiver
     */
    private BroadcastReceiver createUpdateBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals(Receivers.VOTE)) {
                    Log.v(TAG, "received update broadcast");
                    String pollID = intent.getStringExtra("pollID");
                    int vote = intent.getIntExtra("vote", -1);
                    if (vote != -1) {
                        String hostAddress = intent.getStringExtra("hostAddress");
                        manager.updatePoll(pollID, hostAddress, vote);
                    }
                    adapter.notifyDataSetChanged();
                    intent.removeExtra("pollID");
                    intent.removeExtra("vote");
                    intent.removeExtra("hostAddress");
                }
            }
        };
    }

    /**
     * Creates a BroadcastListener to receive an accept message for
     * a launched poll.
     * When receives the broadcast:
     * Updates accepted devices list for the given poll
     *
     * @return BroadcastReceiver
     */
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

                    adapter.notifyDataSetChanged();
                    intent.removeExtra("pollID");
                    intent.removeExtra("hostAddress");
                    intent.removeExtra("accepted");

                }
            }
        };
    }

    /**
     * Creates a BroadcastListener to receive a result message for
     * an accepted poll.
     * When receives the broadcast:
     * Set votes for the given poll to display its results
     *
     * @return BroadcastReceiver
     */
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

    /**
     * Creates a BroadcastListener to receive a remove message for
     * a poll.
     * When receives the broadcast:
     * Removes the poll from the list
     *
     * @return BroadcastReceiver
     */
    private BroadcastReceiver createRemoveBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null &&
                        intent.getAction().equals("mattoncino.pollo.receive.poll.remove")) {
                    Log.v(TAG, "received remove broadcast");
                    String pollID = intent.getStringExtra("pollID");
                    manager.removePoll(pollID);
                    //adapter.notifyDataSetChanged();
                    intent.removeExtra("pollID");
                }
            }
        };
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
            Log.i(TAG, "activePollList is modified, adapter is notified");
        }
    }

    @Override
    protected void onRestart() {
        //Toast.makeText(this, "called onRestart", Toast.LENGTH_LONG).show();
        super.onRestart();
    }

    /**
     * Saves the state of the activity
     * @param outState
     * @param outPersistentState
     */
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        //Toast.makeText(this, "called onSaveInstanceState", Toast.LENGTH_LONG).show();
        Log.v(TAG, "called onSaveInstanceState");
        outState.putParcelableArrayList("pollList", active_polls);
    }

    /**
     * Restores the state of the activity
     * @param savedInstanceState
     * @param persistentState
     */
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


    protected void stopAudioPlaying(){
        if (PollsCardViewAdapter.record != null){
            if(PollsCardViewAdapter.record.isPlaying()){
                PollsCardViewAdapter.record.stopPlaying();
                PollsCardViewAdapter.record.flipPlay();
            }
            PollsCardViewAdapter.record = null;
        }

    }

    /**
     * Saves Poll List permanently
     */
    @Override
    protected void onStop() {
        //Toast.makeText(this, "called onStop", Toast.LENGTH_LONG).show();
        super.onStop();
        stopAudioPlaying();
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

    /**
     * When user presses the back button, returns to the Main Activity
     */
    @Override
    public void onBackPressed() {
        startActivity(new Intent(ActivePollsActivity.this, MainActivity.class));
    }

    /**
     * Removes the notification with the id notificationID from the status bar.
     * @param notificationID
     */
    private void removeNotification(int notificationID) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationID);
    }
}


