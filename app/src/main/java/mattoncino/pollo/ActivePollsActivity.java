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
    private Gson gson;

    private ServiceConnectionManager connectionManager;
    private boolean recovered = false;
    private boolean myPollRequest = false;
    private boolean acceptedPollRequest = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Toast.makeText(this, "called onCreate", Toast.LENGTH_LONG).show();
        setTitle("Active Polls");
        binding = DataBindingUtil.setContentView(
                this, R.layout.activity_active_polls);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref = getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        editor = pref.edit();
        gson = new Gson();


        //pollManager = new PollManager();
        if(active_polls == null)
            active_polls = new ArrayList<Poll>();

        Bundle data = getIntent().getExtras();
        if(data != null) {
            poll = (Poll) data.getParcelable(Consts.POLL_MINE);
            if(poll == null) {
                poll = (Poll) data.getParcelable(Consts.POLL_OTHER);
                acceptedPollRequest = true;
            }
            else myPollRequest = true;
            data.clear();
            active_polls.add(poll);

            Log.d(TAG, "OnCreate: new poll is added to the active_polls");
        }


        /*if(savedInstanceState != null){
            /*if(active_polls == null)
                active_polls = new ArrayList<Poll>();*/
            //ConcurrentHashMap<String, Poll> backup = (ConcurrentHashMap<String, Poll>) savedInstanceState.getSerializable("pollMap");
            //pollManager.addAll(backup);
            //pollManager.addAll((ArrayList<Poll>) savedInstanceState.getSerializable(POLL_LIST));

         /*   try {
                active_polls.addAll((ArrayList<Poll>) savedInstanceState.getSerializable(B_POLL_LIST));
                Log.d(TAG, "Backuped polls recovered from savedInstanceState");
                //Toast.makeText(this, "Backuped Polls are added back", Toast.LENGTH_LONG).show();
            }catch(NullPointerException e){
                //Toast.makeText(this, "Saved Instance is null", Toast.LENGTH_LONG).show();
                Log.d(TAG, "returned null pointer from savedInstanceState");
            }

        }*/


        /*if(active_polls.size() != 0) {
            adapter = new PollsRecyclerViewListAdapter(active_polls);
            binding.recyclerView.setAdapter(adapter);
        }*/


        //change adapter to SimpleAdapter -> ArrayList<Map>
        //binding.recyclerView.setAdapter(new PollsRecyclerViewCursorAdapter(this, cursor));
    }

    @Override
    protected void onResume() {
        //Toast.makeText(this, "called onResume", Toast.LENGTH_LONG).show();
        Log.d(TAG, "get in onResume()");
        super.onResume();
        removeNotification(0);

        Log.d(TAG, "onResume(): removed notification");

        if(!recovered) {
            if (pref == null)
                pref = getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);

            ArrayList<Poll> backup = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
            if (backup != null && backup.size() != 0) {
                active_polls.addAll(backup);
            }

            editor.clear();
            editor.commit();

            adapter = new PollsCardViewAdapter(active_polls);
            binding.recyclerView.setAdapter(adapter);
            //adapter.notifyDataSetChanged();

            recovered = true;
        }


        if(myPollRequest) {
            connectionManager = ((MyApplication) getApplication()).getConnectionManager();
            if (connectionManager == null) {
                Log.d(TAG, "connectionManager is null!!!");
                return;
            }
            //adapter.notifyDataSetChanged();

            ArrayList<String> pollData = new ArrayList<String>();
            pollData.add(poll.getName());
            pollData.add(poll.getQuestion());
            pollData.add(poll.getHostAddress());
            pollData.addAll(poll.getOptions());


            //Toast.makeText(this, "MY HOST ADDRESS: " + poll.getHostAddress(), Toast.LENGTH_LONG).show();

            try {
                connectionManager.sendMessageToAllDevicesInNetwork(ActivePollsActivity.this, Consts.POLL_REQUEST, pollData);
            } catch (NullPointerException e) {
                Log.d(TAG, "connectionManager.sendMessageToAllDevices nullPointerException!!!");
                return;
            }

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


        //adapter.notifyDataSetChanged();


            /*if(polls == null)
                Toast.makeText(this, "poll list from sharedpref is null", Toast.LENGTH_LONG).show();
            else {
                active_polls.addAll((ArrayList<Poll>) polls);
                Toast.makeText(this, "poll list size: " + active_polls.size() , Toast.LENGTH_LONG).show();
            }*/


        //pollManager.addPoll(poll);
        //active_polls.add(poll);
    }

    @Override
    protected void onRestart() {
        //Toast.makeText(this, "called onRestart", Toast.LENGTH_LONG).show();

        super.onRestart();

        //if(gson == null)    gson = new Gson();

        /*try {
            List<Poll> polls = (ArrayList<Poll>) ObjectSerializer.deserialize(
                    pref.getString(SP_POLL_LIST, ObjectSerializer.serialize(new ArrayList<Poll>())));
            if(polls == null)
                Toast.makeText(this, "poll list from sharedpref is null", Toast.LENGTH_LONG).show();
            else {
                active_polls.addAll((ArrayList<Poll>) polls);
                Toast.makeText(this, "poll list size: " + active_polls.size() , Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        //active_polls.add(poll);

        adapter = new PollsRecyclerViewListAdapter(active_polls);
        binding.recyclerView.setAdapter(adapter);


    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putParcelableArrayList("pollList", active_polls);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);

        if(savedInstanceState != null){
            try {
                active_polls.addAll((ArrayList<Poll>) savedInstanceState.getSerializable(Consts.B_POLL_LIST));
                Log.d(TAG, "Backuped polls recovered from savedInstanceState");
            }catch(NullPointerException e){
                Log.d(TAG, "Returned null pointer from savedInstanceState");
            }
            recovered = true;
        }

    }

    @Override
    protected void onPause() {
        //Toast.makeText(this, "called onPause", Toast.LENGTH_LONG).show();

        super.onPause();

        //String json = gson.toJson(active_polls);

        editor.putString(Consts.POLL_LIST, new Gson().toJson(new ArrayList<Poll>(active_polls)));
        editor.commit();
    }

    @Override
    protected void onStop() {
        //Toast.makeText(this, "called onStop", Toast.LENGTH_LONG).show();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //Toast.makeText(this, "called Destroy", Toast.LENGTH_LONG).show();
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


