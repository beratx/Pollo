package mattoncino.pollo;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class PollManager implements Runnable {
    public static final String TAG = "PollManager";
    //private static ConcurrentHashMap<String, Poll> active_polls;
    private static ArrayList<Poll> active_polls;
    private static final Type LIST_TYPE = new TypeToken<List<Poll>>() {}.getType();
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Gson gson;
    private ServiceConnectionManager connectionManager;
    private Context context;
    private RecyclerView.Adapter adapter;
    private RecyclerView recyclerView;

    public PollManager(Context context, RecyclerView recyclerView) {
        /* new ConcurrentHashMap<String, MyClass>(8, 0.9f, 1) */
        //this.active_polls = new ConcurrentHashMap<String, Poll>(8, 0.9f, 1);
        this.active_polls = new ArrayList<Poll>();
        this.context = context;
        this.pref = context.getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        this.adapter = new PollsCardViewAdapter(active_polls);
        this.recyclerView = recyclerView;
    }

    public void addPoll(Poll poll){
        if(active_polls.contains(poll))
            Log.i(TAG, "poll is already in list");
        else {
            active_polls.add(poll);
            Log.i(TAG, "new poll is added");
        }

    }

    public void addAll(ArrayList<Poll> polls){
        this.active_polls.addAll(polls);
    }

    /*public Poll popPoll(String key){
        return active_polls.remove(key);
    }*/

    public void terminatePoll(Poll poll){

    }

    public void updatePoll(String name, int vote){

    }

    public ArrayList<Poll> getPolls(){
        return new ArrayList<Poll>(active_polls);
    }

    /*public ArrayList<Poll> getPollMap(){
        return new ArrayList<Poll>(active_polls);
    }*/

    public int getCount(){
        return active_polls.size();
    }

    @Override
    public void run() {

        ArrayList<Poll> backup = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
        if (backup != null && backup.size() != 0) {
            active_polls.addAll(backup);
        }

        editor.clear();
        editor.commit();


        recyclerView.setAdapter(adapter);
        //adapter.notifyDataSetChanged();

    }
}
