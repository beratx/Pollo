package mattoncino.pollo;

import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class PollManager extends Observable {
    public static final String TAG = "PollManager";
    private static final Type LIST_TYPE = new TypeToken<List<PollData>>() {}.getType();
    //private static final Type MAP_TYPE = new TypeToken<HashMap<String,Poll>>() {}.getType();
    private static ArrayList<PollData> active_polls;
    //ConcurrentHashMap active_polls;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;


    private PollManager(){
        this.context = MyApplication.getContext();
        this.pref = context.getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        this.active_polls = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
        if(this.active_polls == null)   this.active_polls = new ArrayList<PollData>();
        //this.active_polls = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), MAP_TYPE);
        //if(this.active_polls == null)   this.active_polls = new ConcurrentHashMap<String, Poll>();
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


    public void addPoll(PollData pd){
        synchronized (this) {
            if (active_polls.contains(pd))
                Log.i(TAG, "poll is already in list");
            else {
                active_polls.add(0, pd);
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


    public void addAll(ArrayList<PollData> polls){
        this.active_polls.addAll(polls);
    }

    public void addVoter(String pollID, String hostAddress){
        Log.d(TAG, "gets in addVoter...");
        synchronized (this) {
            for (PollData pd : active_polls) {
                if (pollID.equals(pd.getID())) {
                    pd.addVoter(hostAddress);
                    Log.d(TAG, "voter " + hostAddress + " added to pol: " + pd.getPollName());
                }
            }
        }
    }

    public void updatePoll(String pollID, int vote){
        Log.d(TAG, "gets in updatePoll...");
        synchronized (this) {
            for (PollData pd : active_polls) {
                if (pollID.equals(pd.getID())) {
                    pd.addVote(vote);
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
        editor.putString(Consts.POLL_LIST, new Gson().toJson(new ArrayList<PollData>(active_polls)));
        editor.commit();
    }

}
