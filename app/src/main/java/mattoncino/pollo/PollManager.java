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
import java.util.Set;


public class PollManager extends Observable {
    public static final String TAG = "PollManager";
    private static final Type LIST_TYPE = new TypeToken<List<PollData>>() {}.getType();
    private static ArrayList<PollData> active_polls;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;


    private PollManager(){
        this.context = MyApplication.getContext();
        this.pref = context.getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        this.active_polls = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
        if(this.active_polls == null)   this.active_polls = new ArrayList<PollData>();
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

    /*public synchronized PollData getPollData(String pollID){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID))
                return pd;
        }
        return null;
    }*/


    /*public synchronized int[] getVotes(String pollID){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                return pd.getVotes();
            }
        }
        return new int[5];
    }*/


    public synchronized void setVotes(String pollID, int[] votes){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                pd.setVotes(votes);
                pd.setTerminated(true);
                setChanged();
                notifyObservers();
                Log.d(TAG, "poll: " +  pd.getID() + "result: "  + pd.getVotes());
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
                //Log.d(TAG, "called addPoll-> setChanged -> notifyObservers");
            }
    }


    public synchronized void removePoll(String pollID){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                if(pd.hasImage()) {
                    String imagePath = pd.getImageInfo().getPath();
                    File image = new File(imagePath);
                    boolean r = image.delete();
                    Log.d(TAG, imagePath + " is deleted successfully");
                }
                i.remove();
                setChanged();
                notifyObservers();
                break;
            }
        }
    }



    public synchronized void updateAcceptedDeviceList(String pollID, String hostAddress, boolean accepted){
        //Log.d(TAG, "gets in updateAcceptedDeviceList... to add hostAddress: " + hostAddress);
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                if(accepted)
                    pd.addAcceptedDevice(hostAddress);
                pd.incrementResponseCount();
                Log.d(TAG, hostAddress + " poll: " + pd.getPollName() + " accepted? " + accepted);
                Log.d(TAG, "current deviceCount: " + pd.getContactedDevices().size());
                Log.d(TAG, "current responseCount: " + pd.getResponseCount());
                Log.d(TAG, "current votedDevices.size(): " + pd.getVotedDevices().size());
                break;
            }
        }
    }


    public synchronized void updatePoll(String pollID, String hostAddress, int vote){
        //Log.d(TAG, "gets in updatePoll... to add hostAddress: " + hostAddress);
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                pd.addVote(vote);
                pd.addVotedDevice(hostAddress);
                Log.d(TAG, "poll " + pd.getID() + " updated with vote: " + vote);
                setChanged();
                notifyObservers();
                Log.d(TAG, "current deviceCount: " + pd.getContactedDevices().size());
                Log.d(TAG, "current responseCount: " + pd.getResponseCount());
                Log.d(TAG, "current votedDevices.size(): " + pd.getVotedDevices().size());
                break;
            }
        }
    }


    public Set<String> getVotedDevices(String pollID){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID))
                return pd.getVotedDevices();
        }
        return null;
    }


    public synchronized void savePollsPermanently(){
        editor = pref.edit();
        editor.putString(Consts.POLL_LIST, new Gson().toJson(new ArrayList<PollData>(active_polls)));
        editor.commit();
    }

}
