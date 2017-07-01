package mattoncino.pollo;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import mattoncino.pollo.databinding.ActivityActivePollsBinding;

public class ActivePollsActivity extends AppCompatActivity {
    private static final String TAG = "ActivePollsActivity";

    //private static final String SP_POLL_LIST = "pollList1";

    private static final Type LIST_TYPE = new TypeToken<List<Poll>>() {}.getType();
    private ActivityActivePollsBinding binding;
    private RecyclerView.Adapter adapter;
    private ArrayList<Poll> active_polls;
    private Poll poll;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private ServiceConnectionManager connectionManager;
    private boolean myPollRequest = false;
    private boolean acceptedPollRequest = false;
    private boolean calledBeforeMe = false; /* to save state onPause vs onSaveInstanceState */
    private boolean restoredBeforeMe = false; /* to restore state onPause vs onSaveInstanceState */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "called onCreate", Toast.LENGTH_LONG).show();
        setTitle("Active Polls");
        binding = DataBindingUtil.setContentView(
                this, R.layout.activity_active_polls);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref = getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        editor = pref.edit();

        if(active_polls == null)
            active_polls = new ArrayList<Poll>();

        /*if(savedInstanceState != null){
            active_polls = savedInstanceState.getParcelableArrayList("pollList");
            if(active_polls != null) {
                //restoredBeforeMe = true;
                Log.d(TAG, "in onCreate() restored from savedInstanceState");
            }
        }
        else {*/
            //new Thread(new Runnable(){
             //   @Override
             //   public void run() {
                    //pref = getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);

                    ArrayList<Poll> backup = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
                    if (backup != null && backup.size() != 0) {
                        //active_polls.addAll(backup);
                        active_polls = backup;
                        Log.d(TAG, "backuped poll added to active polls");
                    }
              //  }
            //}).start();
        //}

        Bundle data = getIntent().getExtras();
        if(data != null) {
            int type = data.getInt(Consts.OWNER);
            poll = (Poll) data.getParcelable(Consts.POLL);

            if(type == Consts.OWN)
                myPollRequest = true;
            else if(type == Consts.OTHER)
                acceptedPollRequest = true;

            //active_polls.add(poll);

            getIntent().removeExtra(Consts.POLL);
            getIntent().removeExtra(Consts.OWNER);

            //Log.d(TAG, "OnCreate: new poll is added to the active_polls");
        }

        //Log.d(TAG, "at the end of onCreate(): restoredBeforeMe: " + restoredBeforeMe);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "called onResume", Toast.LENGTH_LONG).show();
        //Log.d(TAG, "get in onResume()");

        //removeNotification(0);

        //Log.d(TAG, "beginning of onResume(): restoredBeforeMe: " + restoredBeforeMe);

        if(poll != null)
            active_polls.add(0, poll);

        adapter = new PollsCardViewAdapter(active_polls);
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

    @Override
    protected void onRestart() {
        Toast.makeText(this, "called onRestart", Toast.LENGTH_LONG).show();
        super.onRestart();
        if(active_polls == null)
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
        }).start();
        //active_polls.add(poll);
        //adapter = new PollsRecyclerViewListAdapter(active_polls);
        //binding.recyclerView.setAdapter(adapter);
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

                restoredBeforeMe = true;
            }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "called onPause", Toast.LENGTH_LONG).show();
            //new Thread(new Runnable() {
            //    @Override
            //    public void run() {
                    if (pref == null)
                        pref = getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
                    if (editor == null)
                        editor = pref.edit();
                    //editor.clear();
                    //editor.commit();
                    editor.putString(Consts.POLL_LIST, new Gson().toJson(new ArrayList<Poll>(active_polls)));
                    editor.commit();
              //  }
            //}).start();

    }

    @Override
    protected void onStop() {
        Toast.makeText(this, "called onStop", Toast.LENGTH_LONG).show();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Toast.makeText(this, "called Destroy", Toast.LENGTH_LONG).show();
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


