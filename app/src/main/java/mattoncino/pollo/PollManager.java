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
import java.util.Set;


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

    public PollData getPollData(String pollID){
        for (PollData pd : active_polls) {
            if (pollID.equals(pd.getID()))
                return pd;
        }
        return null;
    }

    /*public List<Double> getResult(String pollID){
        List<Double> resos = new ArrayList<Double>();
        for (PollData pd : active_polls) {
            if (pollID.equals(pd.getID())) {
                for (int i = 1; i < pd.getPoll().getOptions().size(); i++) {
                    resos.add(pd.getResultFor(i));
                }
                return resos;
            }
        }
        return null;
    }*/

    public int[] getVotes(String pollID){
        List<Double> resos = new ArrayList<Double>();
        for (PollData pd : active_polls) {
            if (pollID.equals(pd.getID())) {
                return pd.getVotes();
            }
        }
        return null;
    }

    /*public void setResult(String pollID, int[] result){
        for (PollData pd : active_polls) {
            if (pollID.equals(pd.getID())) {
                pd.setResult(result);
                pd.setTerminated(true);
                Log.d(TAG, "poll: " +  pd.getID() + "result: "  + pd.getResult());
            }
        }
    }*/

    public void setVotes(String pollID, int[] votes){
        for (PollData pd : active_polls) {
            if (pollID.equals(pd.getID())) {
                pd.setVotes(votes);
                pd.setTerminated(true);
                Log.d(TAG, "poll: " +  pd.getID() + "result: "  + pd.getVotes());
            }
        }
    }

    public void setDeviceCount(String pollID, int count){
        for (PollData pd : active_polls) {
            if (pollID.equals(pd.getID()))
                pd.setDeviceCount(count);
        }
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



    public void updateAcceptedDeviceList(String pollID, String hostAddress, boolean accepted){
        Log.d(TAG, "gets in updateAcceptedDeviceList... to add hostAddress: " + hostAddress);
        synchronized (this) {
            for (PollData pd : active_polls) {
                if (pollID.equals(pd.getID())) {
                    if(accepted)
                        pd.addAcceptedDevice(hostAddress);
                    pd.incrementResponseCount();
                    Log.d(TAG, hostAddress + " poll: " + pd.getPollName() + " accepted? " + accepted);
                    Log.d(TAG, "current deviceCount: " + pd.getDeviceCount());
                    Log.d(TAG, "current responseCount: " + pd.getResponseCount());
                    Log.d(TAG, "current votedDevices.size(): " + pd.getVotedDevices().size());
                }
            }
        }
    }


    public void updatePoll(String pollID, String hostAddress, int vote){
        Log.d(TAG, "gets in updatePoll... to add hostAddress: " + hostAddress);
        synchronized (this) {
            for (PollData pd : active_polls) {
                if (pollID.equals(pd.getID())) {
                    //NEED TO SYNCHRONIZE!!!
                    pd.addVote(vote);
                    pd.addVotedDevice(hostAddress);
                    //pd.removeDevice(hostAddress); //from accepted devices
                    Log.d(TAG, "poll " + pd.getID() + " updated with vote: " + vote);
                    setChanged();
                    notifyObservers();
                    Log.d(TAG, "called setChanged -> notifyObservers");
                    Log.d(TAG, "current deviceCount: " + pd.getDeviceCount());
                    Log.d(TAG, "current responseCount: " + pd.getResponseCount());
                    Log.d(TAG, "current votedDevices.size(): " + pd.getVotedDevices().size());
                }
            }
        }
    }


    //TODO:should check also if owner has voted
    public boolean isCompleted(String pollID){
        for (PollData pd : active_polls) {
            if (pollID.equals(pd.getID()))
                return (pd.getDeviceCount() == pd.getResponseCount()) &&
                        (pd.getVotedDevices().size() == pd.getAcceptedDevices().size()) &&
                        (pd.getMyVote() > 0);
        }
        return false;
    }



    public Set<String> getVotedDevices(String pollID){
        for (PollData pd : active_polls) {
            if (pollID.equals(pd.getID()))
                return pd.getVotedDevices();
        }
        return null;
    }

    // Passes the object specified in the parameter
    // list to the notify() method of the observer.

    public void savePollsPermanently(){
        editor = pref.edit();
        editor.putString(Consts.POLL_LIST, new Gson().toJson(new ArrayList<PollData>(active_polls)));
        editor.commit();
    }

}
