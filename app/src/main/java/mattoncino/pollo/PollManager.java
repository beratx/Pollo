package mattoncino.pollo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;


public class PollManager extends Observable {
    public static final String TAG = "PollManager";
    private static ArrayList<Poll> active_polls;
    private static final Type LIST_TYPE = new TypeToken<List<Poll>>() {}.getType();
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;


    private PollManager(){
        this.context = MyApplication.getContext();
        this.pref = context.getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        this.active_polls = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
        if(this.active_polls == null)   this.active_polls = new ArrayList<Poll>();
    }

    private static class PollManagerHelper{
        private static final PollManager INSTANCE = new PollManager();
    }

    public static PollManager getInstance(){
        return PollManagerHelper.INSTANCE;
    }

    public static ArrayList getActivePolls(){
        return active_polls;
    }


    public void addPoll(Poll poll){
        synchronized (this) {
            if (active_polls.contains(poll))
                Log.i(TAG, "poll is already in list");
            else {
                active_polls.add(0, poll);
                setChanged();
                notifyObservers();
                Log.d(TAG, "called addPoll-> setChanged -> notifyObservers");
            }
        }
    }

    public void removePoll(Poll poll){
        active_polls.remove(poll);
        setChanged();
        notifyObservers();
        Log.d(TAG, "called removePoll-> setChanged -> notifyObservers");
    }


    public void addAll(ArrayList<Poll> polls){
        this.active_polls.addAll(polls);
    }


    public void updatePoll(String name, int vote){
        Log.d(TAG, "gets in updatePoll...");
        synchronized (this) {
            for (Poll p : active_polls) {
                if (name.equals(p.getName())) {
                    p.addVote(vote);
                    setChanged();
                    notifyObservers();
                    Log.d(TAG, "poll will be updated with vote: " + vote);
                    Log.d(TAG, "called updatePoll-> setChanged -> notifyObservers");
                }
            }
        }
    }

    // Passes the object specified in the parameter
    // list to the notify() method of the observer.

    public void savePollsPermanently(){
        editor = pref.edit();
        editor.putString(Consts.POLL_LIST, new Gson().toJson(new ArrayList<Poll>(active_polls)));
        editor.commit();
    }


}
