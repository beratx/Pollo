package mattoncino.pollo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;


public class PollManager extends Observable {
    public static final String TAG = "PollManager";
    private static final String SHARED_PREFS_FILE = "polloSharedPrefs";
    private static final Type LIST_TYPE = new TypeToken<List<PollData>>() {}.getType();
    private static ArrayList<PollData> active_polls;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private PollManager(){
        this.pref = (MyApplication.getContext()).getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        this.active_polls = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
        if(this.active_polls == null)   this.active_polls = new ArrayList<>();
     }

    private static class PollManagerHelper{
        private static final PollManager INSTANCE = new PollManager();
    }


    public static PollManager getInstance(){
        return PollManagerHelper.INSTANCE;
    }


    public static ArrayList<PollData> getActivePolls(){
        return active_polls;
    }


    public synchronized void setVotes(String pollID, int[] votes){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                pd.setVotes(votes);
                pd.setTerminated(true);
                setChanged();
                notifyObservers();
                break;
            }
        }
    }


    public synchronized void setContactedDevices(String pollID, HashSet<String> devices){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                pd.setContactedDevices(devices);
                break;
            }
        }
    }


    public synchronized void addPoll(PollData pd){
            if (!active_polls.contains(pd)){
                active_polls.add(0, pd);
                setChanged();
                notifyObservers();
            }
    }


    public synchronized void removePoll(String pollID){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                if(pd.hasImage())
                    removeFile(pd.getImageInfo().getPath());
                if(pd.hasRecord())
                    removeFile(pd.getRecordPath());
                i.remove();
                setChanged();
                notifyObservers();
                break;
            }
        }
    }

    private void removeFile(String path){
        File file = new File(path);
        boolean r = file.delete();
        if(r) Log.d(TAG, path + " is deleted successfully");
    }



    public synchronized void updateAcceptedDeviceList(String pollID, String hostAddress, boolean accepted){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                if(accepted)
                    pd.addAcceptedDevice(hostAddress);
                pd.incrementResponseCount();
                break;
            }
        }
    }


    public synchronized void updatePoll(String pollID, String hostAddress, int vote){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                pd.addVote(vote);
                pd.addVotedDevice(hostAddress);
                setChanged();
                notifyObservers();
                break;
            }
        }
    }


    /*public Set<String> getVotedDevices(String pollID){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID))
                return pd.getVotedDevices();
        }
        return null;
    }*/


    public synchronized void savePollsPermanently(){
        editor = pref.edit();
        editor.putString(Consts.POLL_LIST, new Gson().toJson(new ArrayList<>(active_polls)));
        editor.commit();
    }

}
