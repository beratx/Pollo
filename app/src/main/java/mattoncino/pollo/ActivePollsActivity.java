package mattoncino.pollo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

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
    private String xhostAddress;
    private String ownAddress;



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

        /*if(savedInstanceState != null){
            active_polls = savedInstanceState.getParcelableArrayList("pollList");
            if(active_polls != null) {
                //restoredBeforeMe = true;
                Log.d(TAG, "in onCreate() restored from savedInstanceState");
            }
        }
        else {*/

        Bundle data = getIntent().getExtras();
        if(data != null) {
            int type = data.getInt(Consts.OWNER);
            poll = data.getParcelable(Consts.POLL);
            int notfID = data.getInt("notificationID");

            if(poll != null) {
                if (type == Consts.OWN) {
                    myPollRequest = true;
                    ownAddress = ((MyApplication) getApplication()).getConnectionManager().getHostAddress();
                    manager.addPoll(new PollData(poll,ownAddress,type));
                } else if (type == Consts.OTHER) {
                    acceptedPollRequest = true;
                    xhostAddress = data.getString("hostAddress");
                    manager.addPoll(new PollData(poll,xhostAddress,type));
                }

            }

            removeNotification(notfID);
            getIntent().removeExtra(Consts.POLL);
            getIntent().removeExtra(Consts.OWNER);
            getIntent().removeExtra("notificationID");
            if (type == Consts.OTHER) getIntent().removeExtra("hostAddress");
        }
    }

    private BroadcastReceiver createUpdateBroadcastReceiver() {
        Log.d(TAG, "received create broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String pollID = intent.getStringExtra("pollID");
                int vote = intent.getIntExtra("vote", -1);
                //String hostAddress = intent.getStringExtra("hostAddress");
                if(vote == -1)
                    Log.d(TAG, "corrupted vote");
                else
                    manager.updatePoll(pollID, vote);
                intent.removeExtra("pollID");
                intent.removeExtra("vote");
                intent.removeExtra("hostAddress");
                //adapter.notifyDataSetChanged();
            }
        };
    }

    private BroadcastReceiver createAcceptBroadcastReceiver() {
        Log.d(TAG, "received accept broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String pollID = intent.getStringExtra("pollID");
                xhostAddress = intent.getStringExtra("hostAddress");

                manager.addVoter(pollID, xhostAddress);
                intent.removeExtra("pollID");
                intent.removeExtra("hostAddress");
                //adapter.notifyDataSetChanged();
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

        if(myPollRequest) {
            new Thread(new Runnable(){
                @Override
                public void run() {
                    connectionManager = ((MyApplication) getApplication()).getConnectionManager();
                    if (connectionManager == null) {
                        Log.d(TAG, "connectionManager is null!!!");
                        return;
                    }

                    ArrayList<String> pollInfo = new ArrayList<String>();
                    pollInfo.add(poll.getId());
                    pollInfo.add(poll.getName());
                    pollInfo.add(poll.getQuestion());
                    pollInfo.add(ownAddress);
                    pollInfo.addAll(poll.getOptions());

                    try {
                        connectionManager.sendMessageToAllDevicesInNetwork(ActivePollsActivity.this, Consts.POLL_REQUEST, pollInfo);
                    } catch (NullPointerException e) {
                        Log.d(TAG, "connectionManager.sendMessageToAllDevices nullPointerException!!!");
                        return;
                    }

                }
            }).start();

            myPollRequest = false;

        }
        else if(acceptedPollRequest){
            Log.d(TAG, "onResume(): acceptedPollReq");
            acceptedPollRequest = false;
            ArrayList<String> pollInfo = new ArrayList<>();
            pollInfo.add(poll.getId());
            pollInfo.add(xhostAddress);
            ClientThreadProcessor clientProcessor = new ClientThreadProcessor(xhostAddress, ActivePollsActivity.this, Consts.ACCEPT, pollInfo);
            Thread t = new Thread(clientProcessor);
            t.start();
            //Toast.makeText(this, "ARRIVED FROM: " + poll.getHostAddress(), Toast.LENGTH_LONG).show();
        }

    }

    //called when a change has occurred in the state of the observable
    @Override
    public void update(Observable observable, Object o) {
        if(adapter != null) {
            adapter.notifyDataSetChanged();
            Log.d(TAG, "activePollList is modified, adapter is notified");
        }
    }

    @Override
    protected void onRestart() {
        //Toast.makeText(this, "called onRestart", Toast.LENGTH_LONG).show();
        super.onRestart();
        //active_polls = PollManager.getActivePolls();
        manager = PollManager.getInstance();
        manager.addObserver(this);

        updateReceiver = createUpdateBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, new IntentFilter("mattoncino.pollo.receive.poll.vote"));

        acceptReceiver = createAcceptBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(acceptReceiver, new IntentFilter("mattoncino.pollo.receive.poll.accept"));

        Bundle data = getIntent().getExtras();
        if(data != null) {
            int type = data.getInt(Consts.OWNER);
            poll = data.getParcelable(Consts.POLL);
            int notfID = data.getInt("notificationID");

            if(poll != null) {
                if (type == Consts.OWN) {
                    myPollRequest = true;
                    ownAddress = ((MyApplication) getApplication()).getConnectionManager().getHostAddress();
                    manager.addPoll(new PollData(poll,ownAddress, type));
                } else if (type == Consts.OTHER) {
                    acceptedPollRequest = true;
                    xhostAddress = data.getString("hostAddress");
                    manager.addPoll(new PollData(poll,xhostAddress, type));
                }

            }

            removeNotification(notfID);
            getIntent().removeExtra(Consts.POLL);
            getIntent().removeExtra(Consts.OWNER);
            if (type == Consts.OTHER) getIntent().removeExtra("hostAddress");
        }

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
            if(savedInstanceState != null) {
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
        manager.savePollsPermanently();

    }

    @Override
    protected void onStop() {
        //Toast.makeText(this, "called onStop", Toast.LENGTH_LONG).show();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //Toast.makeText(this, "called Destroy", Toast.LENGTH_LONG).show();
        if (updateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
        }
        super.onDestroy();

    }

    @Override
    public void onBackPressed()
    {
        startActivity(new Intent(ActivePollsActivity.this, mattoncino.pollo.MainActivity.class));
    }

    private void removeNotification(int notificationID){
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationID);
    }



}


