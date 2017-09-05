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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import mattoncino.pollo.databinding.ActivityActivePollsBinding;

public class ActivePollsActivity extends AppCompatActivity implements Observer {
    private static final String TAG = "ActivePollsActivity";

    //private static final String SP_POLL_LIST = "pollList1";
    //private static final Type LIST_TYPE = new TypeToken<List<Poll>>() {}.getType();
    private ActivityActivePollsBinding binding;
    private RecyclerView.Adapter adapter;
    private ArrayList<PollData> active_polls;
    private Poll poll;
    private ServiceConnectionManager connectionManager;
    private boolean myPollRequest = false;
    private boolean acceptedPollRequest = false;
    private PollManager manager;
    private BroadcastReceiver updateReceiver;
    private BroadcastReceiver acceptReceiver;
    private BroadcastReceiver resultReceiver;
    private String xhostAddress;
    private String ownAddress;
    private boolean completedPoll = false;
    private PollData completedPD;
    private static boolean receivedAcceptBroadcast = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Toast.makeText(this, "called onCreate", Toast.LENGTH_LONG).show();
        setTitle("Active Polls");
        binding = DataBindingUtil.setContentView(
                this, R.layout.activity_active_polls);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        manager = PollManager.getInstance();
        manager.addObserver(this);

        updateReceiver = createUpdateBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, new IntentFilter("mattoncino.pollo.receive.poll.vote"));

        acceptReceiver = createAcceptBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(acceptReceiver, new IntentFilter("mattoncino.pollo.receive.poll.accept"));

        resultReceiver = createResultBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(resultReceiver, new IntentFilter("mattoncino.pollo.receive.poll.result"));

        /*if(savedInstanceState != null){
            active_polls = savedInstanceState.getParcelableArrayList("pollList");
            if(active_polls != null) {
                //restoredBeforeMe = true;
                Log.d(TAG, "in onCreate() restored from savedInstanceState");
            }
        }
        else {
        }*/

        Bundle data = getIntent().getExtras();
        if (data != null) {
            int type = data.getInt(Consts.OWNER);
            poll = data.getParcelable(Consts.POLL);
            int notfID = data.getInt("notificationID");
            ownAddress = ((MyApplication) getApplication()).getConnectionManager().getHostAddress();

            if (poll != null) {
                if (type == Consts.OWN) {
                    myPollRequest = true;
                    manager.addPoll(new PollData(poll, ownAddress, type));
                } else if (type == Consts.OTHER) {
                    acceptedPollRequest = true;
                    xhostAddress = data.getString("hostAddress");
                    //System.out.println("accepted poll from: " + xhostAddress);
                    manager.addPoll(new PollData(poll, xhostAddress, type));
                    /*if(poll.hasImage()){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                File image = File.create
                                imageFile.getParentFile().mkdirs();
                                return imageFile;
                                //IF has an image SHOULD SAVE PERMAMNENTLY
                                File imageFile = ImagePicker.getTempFile(ActivePollsActivity.this);
                                //Uri imageUri = Uri.fromFile(imageFile);
                                File permanentPath = ImagePicker.getPictureStorageDir(ActivePollsActivity.this, "poll_pictures");

                                FileInputStream input = null;
                                FileOutputStream output = null;
                                try {
                                    input = new FileInputStream(imageFile);
                                    output = new FileOutputStream(permanentPath);

                                    byte[] buffer = new byte[1024];
                                    int len;

                                    while((len = input.read(buffer)) != -1){
                                        output.write(buffer);
                                        output.flush();
                                    }
                                    poll.getImageInfo().setPath(permanentPath.toString());
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    try {
                                        input.close();
                                        output.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).start();
                    }*/
                }
            }

            removeNotification(notfID);
            /*getIntent().removeExtra(Consts.POLL);
            getIntent().removeExtra(Consts.OWNER);
            getIntent().removeExtra("notificationID");
            if (type == Consts.OTHER) getIntent().removeExtra("hostAddress");*/
        }

    }

    private BroadcastReceiver createUpdateBroadcastReceiver() {
        Log.d(TAG, "received update broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals("mattoncino.pollo.receive.poll.vote")) {
                    String pollID = intent.getStringExtra("pollID");
                    boolean isMyVote = intent.getBooleanExtra("myVote", false);
                    if(!isMyVote) {
                        int vote = intent.getIntExtra("vote", -1);
                        if (vote != -1) {
                            String hostAddress = intent.getStringExtra("hostAddress");
                            manager.updatePoll(pollID, hostAddress, vote);
                        }
                    }
                    if (manager.isCompleted(pollID)) {
                        completedPoll = true;
                        completedPD = manager.getPollData(pollID);
                        Log.d(TAG, "poll " + pollID + " is completed! Will send results...");
                    }
                    intent.removeExtra("pollID");
                    intent.removeExtra("vote");
                    intent.removeExtra("hostAddress");
                }
            }
        };
    }

    private BroadcastReceiver createAcceptBroadcastReceiver() {
        Log.d(TAG, "received accept broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null
                   && intent.getAction().equals("mattoncino.pollo.receive.poll.accept")) {
                    //TODO: E' MEGLIO LANCIARE UN SERVICE?
                    String pollID = intent.getStringExtra("pollID");
                    xhostAddress = intent.getStringExtra("hostAddress");
                    boolean accepted = intent.getBooleanExtra("accepted", false);

                    manager.updateAcceptedDeviceList(pollID, xhostAddress, accepted);
                    receivedAcceptBroadcast = true;

                    //intent.removeExtra("pollID");
                    //intent.removeExtra("hostAddress");
                    //intent.removeExtra("accepted");
                    //adapter.notifyDataSetChanged();
                }
            }
        };
    }

    private BroadcastReceiver createResultBroadcastReceiver() {
        Log.d(TAG, "received result broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null && intent.getAction().equals("mattoncino.pollo.receive.poll.result")) {
                    String pollID = intent.getStringExtra("pollID");
                    int[] result = (int[]) intent.getSerializableExtra(Consts.RESULT);

                    //inside sets also terminated flag
                    manager.setVotes(pollID, result);

                    intent.removeExtra("pollID");
                    intent.removeExtra(Consts.RESULT);
                }
            }
        };
    }


    @Override
    protected void onResume() {
        super.onResume();
        //Toast.makeText(this, "called onResume", Toast.LENGTH_LONG).show();
        //Log.d(TAG, "get in onResume()");

        adapter = new PollsCardViewAdapter(manager.getActivePolls());
        binding.recyclerView.setAdapter(adapter);

        //Intent mServiceIntent = new Intent(this, StatusUpdaterService.class);
        //mServiceIntent.putParcelableArrayListExtra("polls", manager.getActivePolls());
        //mServiceIntent.setData(Uri.parse(dataUrl));
        //startService(mServiceIntent);

        if (myPollRequest) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectionManager = ((MyApplication) getApplication()).getConnectionManager();
                    if (connectionManager == null) {
                        Log.d(TAG, "connectionManager is null!!!");
                        return;
                    }

                    /*ArrayList<String> pollInfo = new ArrayList<String>();
                    pollInfo.add(poll.getId());
                    pollInfo.add(poll.getName());
                    pollInfo.add(poll.getQuestion());
                    pollInfo.add(ownAddress);
                    pollInfo.add(String.valueOf(poll.hasImage()));
                    pollInfo.addAll(poll.getOptions());
                    if(poll.hasImage()){
                        pollInfo.add(poll.getImageInfo().getUri().toString());
                        pollInfo.add(String.valueOf(poll.getImageInfo().isCamera()));
                    }*/
                    try {
                        //HashSet<String> contactedDevices = (HashSet<String>) connectionManager.sendMessageToAllDevicesInNetwork(ActivePollsActivity.this, Consts.POLL_REQUEST, pollInfo);
                        HashSet<String> contactedDevices = (HashSet<String>) connectionManager.sendMessageToAllDevicesInNetwork(ActivePollsActivity.this, Consts.POLL_REQUEST, poll);
                        if(contactedDevices != null)
                            manager.setContactedDevices(poll.getId(), contactedDevices);
                        else
                            Log.d(TAG, "connectionManager.sendMessageToAllDevices contactedDevices set is null!!!");
                    } catch (NullPointerException e) {
                        Log.d(TAG, "connectionManager.sendMessageToAllDevices nullPointerException!!!");
                        return;
                    }

                }
            }).start();

            myPollRequest = false;

        } else if (acceptedPollRequest) {
            Log.d(TAG, "onResume(): acceptedPollReq");
            acceptedPollRequest = false;
            ArrayList<String> pollInfo = new ArrayList<>();
            pollInfo.add(poll.getId());
            pollInfo.add(ownAddress);
            //System.out.println("poll from: " + xhostAddress + " is accepted.");
            ClientThreadProcessor clientProcessor = new ClientThreadProcessor(xhostAddress, ActivePollsActivity.this, Consts.ACCEPT, pollInfo);
            Thread t = new Thread(clientProcessor);
            t.start();
            //Toast.makeText(this, "ARRIVED FROM: " + poll.getHostAddress(), Toast.LENGTH_LONG).show();
        } else if (completedPoll) {
            Log.d(TAG, "onResume(): completedPoll");
            completedPoll = false;
            //pollID
            int[] result = manager.getVotes(completedPD.getID());
            Set<String> devices = completedPD.getAcceptedDevices();
            try {
                //here maybe add a handler to comunciate back to the thread
                //manager.setResult(completedPD.getID(), result);
                Log.d(TAG, completedPD.getPollName() + " is finished, sending results to other devices...");
                connectionManager.sendResultToAllDevices(ActivePollsActivity.this, devices, completedPD.getID(), result);
            } catch (NullPointerException e) {
                Log.d(TAG, "connectionManager.sendMessageToAllDevices nullPointerException!!!");
                return;
            }
            //Quando finisce di mandare i messaggi
            //1)deve togliere poll dall active_polls
            //2)deve aggiungere poll nell db -> old polls
        }
    }

    //called when a change has occurred in the state of the observable
    @Override
    public void update(Observable observable, Object o) {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            Log.d(TAG, "activePollList is modified, adapter is notified");
        }
    }

    @Override
    protected void onRestart() {
        //Toast.makeText(this, "called onRestart", Toast.LENGTH_LONG).show();
        super.onRestart();
        //active_polls = PollManager.getActivePolls();
        /*manager = PollManager.getInstance();
        manager.addObserver(this);

        Bundle data = getIntent().getExtras();
        if (data != null) {
            int type = data.getInt(Consts.OWNER);
            poll = data.getParcelable(Consts.POLL);
            int notfID = data.getInt("notificationID");

            if (poll != null) {
                if (type == Consts.OWN) {
                    myPollRequest = true;
                    ownAddress = ((MyApplication) getApplication()).getConnectionManager().getHostAddress();
                    manager.addPoll(new PollData(poll, ownAddress, type));
                } else if (type == Consts.OTHER) {
                    acceptedPollRequest = true;
                    xhostAddress = data.getString("hostAddress");
                    manager.addPoll(new PollData(poll, xhostAddress, type));
                }
            }

            removeNotification(notfID);
            getIntent().removeExtra(Consts.POLL);
            getIntent().removeExtra(Consts.OWNER);
            getIntent().removeExtra("notificationID");
            if (type == Consts.OTHER) getIntent().removeExtra("hostAddress");
        }*/

    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        //Toast.makeText(this, "called onSaveInstanceState", Toast.LENGTH_LONG).show();
        Log.d(TAG, "called onSaveInstanceState");
        outState.putParcelableArrayList("pollList", active_polls);


        /*int cardCount = adapter.getItemCount();
        for (int i = 0; i < cardCount; i++) {
            long id = adapter.getItemId(i);
            int type = adapter.getItemViewType(i);

        }*/
        //final LinearLayout rLayout = findViewById(R..listItemLayout;

            /*int mViewsCount = 0;
            for(View view : mViews)
            {
                savedInstanceState.putInt("mViewId_" + mViewsCount, view.getId());
                mViewsCount++;
            }

            savedInstanceState.putInt("mViewsCount", mViewsCount);*/
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        Log.d(TAG, "called onRestoreInstanceState");
        //Toast.makeText(this, "called onRestoreInstanceState", Toast.LENGTH_LONG).show();
        //if(!restoredBeforeMe) {
        if (savedInstanceState != null) {
            active_polls = savedInstanceState.getParcelableArrayList("pollList");
            /*int mViewsCount = savedInstanceState.getInt("mViewsCount");

            for (i = 0; i <= mViewsCount) {
                View view = mViews.get(i);
                int viewId = savedInstanceState.getInt("mViewId_" + i);
                view.setId(viewId);
                mViewsCount++;
            }*/

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Toast.makeText(this, "called onPause", Toast.LENGTH_LONG).show();
        receivedAcceptBroadcast = false;
        manager.savePollsPermanently();

    }

    @Override
    protected void onStop() {
        //Toast.makeText(this, "called onStop", Toast.LENGTH_LONG).show();
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this, "called Destroy", Toast.LENGTH_LONG).show();
        if (updateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        }
        if (acceptReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(acceptReceiver);
        }
        if (resultReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(resultReceiver);
        }
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


