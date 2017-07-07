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
    private ArrayList<Poll> active_polls;
    private Poll poll;
    private ServiceConnectionManager connectionManager;
    private boolean myPollRequest = false;
    private boolean acceptedPollRequest = false;
    private PollManager manager;
    private BroadcastReceiver updateReceiver;


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

        updateReceiver = createBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, new IntentFilter("mattoncino.pollo.receive.poll.vote"));


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
            poll = (Poll) data.getParcelable(Consts.POLL);

            if(type == Consts.OWN)
                myPollRequest = true;
            else if(type == Consts.OTHER)
                acceptedPollRequest = true;

            if(poll != null)
                manager.addPoll(poll);
                //active_polls.add(0, poll);

            getIntent().removeExtra(Consts.POLL);
            getIntent().removeExtra(Consts.OWNER);
        }
    }

    private BroadcastReceiver createBroadcastReceiver() {
        Log.d(TAG, "received broadcast");
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String name = intent.getStringExtra("pollName");
                int vote = intent.getIntExtra("vote", -1);
                String hostAddress = intent.getStringExtra("hostAddress");
                if(vote == -1)
                    Log.d(TAG, "invalid vote");
                else
                    manager.updatePoll(name, vote);
                intent.removeExtra("pollName");
                intent.removeExtra("vote");
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

        //removeNotification(0);

        //adapter = new PollsCardViewAdapter(active_polls);
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

                    ArrayList<String> pollData = new ArrayList<String>();
                    pollData.add(poll.getName());
                    pollData.add(poll.getQuestion());
                    pollData.add(poll.getHostAddress());
                    pollData.addAll(poll.getOptions());

                    try {
                        connectionManager.sendMessageToAllDevicesInNetwork(ActivePollsActivity.this, Consts.POLL_REQUEST, pollData);
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
            ArrayList<String> pollData = new ArrayList<>();
            pollData.add(poll.getName());
            pollData.add(poll.getHostAddress());
            ClientThreadProcessor clientProcessor = new ClientThreadProcessor(poll.getHostAddress(), ActivePollsActivity.this, Consts.ACCEPT, pollData);
            Thread t = new Thread(clientProcessor);
            t.start();
            //active_polls.add(poll);
            //adapter.notifyDataSetChanged();
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
        /*if(active_polls == null)
            active_polls = new ArrayList<Poll>();

        //if(!restoredBeforeMe) {
        new Thread(new Runnable(){
            @Override
            public void run() {
                pref = getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);

                ArrayList<Poll> backup = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
                if (backup != null && backup.size() != 0) {
                    //active_polls.addAll(backup);
                    active_polls = backup;
                    Log.d(TAG, "backuped poll added to active polls");
                }
            }
        }).start();*/
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


