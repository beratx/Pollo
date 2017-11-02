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

/**
 * Manages polls of a user; all the basic operations like adding
 * removing, updating polls, saving and restoring user's Poll
 * list are realized by this class. Its a singleton class.
 *
 * @see @link PollData
 * @see @link Poll
 *
 */
public class PollManager extends Observable {
    private static final String TAG = "PollManager";
    private static final String SHARED_PREFS_FILE = "polloSharedPrefs";
    private static final Type LIST_TYPE = new TypeToken<List<PollData>>() {}.getType();
    private static ArrayList<PollData> active_polls;
    private SharedPreferences.Editor editor;
    private SharedPreferences pref;


    private PollManager(){
        this.pref = (MyApplication.getContext()).getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        this.active_polls = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
        if(this.active_polls == null)   this.active_polls = new ArrayList<>();
     }

    /** Inner helper class to create only one instance of the PollManager */
    private static class PollManagerHelper{
        private static final PollManager INSTANCE = new PollManager();
    }


    /**
     * Returns unique PollManager instance
     * @return PollManager instance
     */
    public static PollManager getInstance(){
        return PollManagerHelper.INSTANCE;
    }


    /**
     * Returns list of PollData for the polls that are created/received by the user
     *
     * @return a list of PollData
     * @see PollData
     */
    public static ArrayList<PollData> getActivePolls(){
        return active_polls;
    }


    /**
     * Sets votes for the Poll with the given pollID
     *
     * @param pollID identifier of Poll
     * @param votes  array of votes
     */
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


    /**
     * Sets contacted devices list - devices that are contacted to send poll request
     * @param pollID identifier of Poll
     * @param devices list of contacted devices
     */
    public synchronized void setContactedDevices(String pollID, HashSet<String> devices){
        for (Iterator<PollData> i = active_polls.iterator(); i.hasNext(); ) {
            PollData pd = i.next();
            if (pd.getID().equals(pollID)) {
                pd.setContactedDevices(devices);
                break;
            }
        }
    }


    /**
     * Adds the PollData to the PollData list
     * @param pd PollData object
     */
    public synchronized void addPoll(PollData pd){
            if (!active_polls.contains(pd)){
                active_polls.add(0, pd);
                setChanged();
                notifyObservers();
            }
    }


    /**
     * Removes the PollData for the Poll with the given pollID
     *
     * @param pollID identifier of poll
     */
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

    /**
     * Removes the file in the given path
     * @param path
     */
    private void removeFile(String path){
        File file = new File(path);
        boolean r = file.delete();
        if(r) Log.d(TAG, path + " is deleted successfully");
    }


    /**
     * If flag accepted is true then adds the device host address
     * to the list of the accepted devices - devices which sent an accept
     * message for the poll with the given pollID -
     *
     * @param pollID poll identifier
     * @param hostAddress host address of a device
     * @param accepted flag to indicate if poll request is accepted or rejected
     */
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


    /**
     * Updates votes list of the Poll with the given pollID and adds
     * device host address to the list of voted device for this Poll
     *
     * @param pollID poll identifier
     * @param hostAddress host address of a device
     * @param vote vote sent by the device of host address
     */
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


    /**
     * Saves active_polls list permanently with shared preferences
     */
    public synchronized void savePollsPermanently(){
        editor = pref.edit();
        editor.putString(Consts.POLL_LIST, new Gson().toJson(new ArrayList<>(active_polls)));
        editor.commit();
    }

}
