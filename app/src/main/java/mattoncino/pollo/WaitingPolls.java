package mattoncino.pollo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;


/**
 * Manages Waiting Polls Requests of a user. These are polls
 * received from others but user did not accepted or received
 * them yet.
 */
public class WaitingPolls extends Observable {
    public static final String TAG = "WaitingPolls";
    final String SHARED_PREFS_FILE = "polloSharedPrefs";
    private static final Type LIST_TYPE = new TypeToken<List<WaitingData>>() {}.getType();
    private static ArrayList<WaitingData> waiting_polls;
    private static SharedPreferences.Editor editor;
    private static SharedPreferences pref;


    private WaitingPolls(){
        this.pref = (MyApplication.getContext()).getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        this.waiting_polls = new Gson().fromJson(pref.getString(Consts.WAITING_LIST, null), LIST_TYPE);
        if(this.waiting_polls == null)  this.waiting_polls = new ArrayList<>();
    }

    private static class WaitingPollsHelper{
        private static final WaitingPolls INSTANCE = new WaitingPolls();
    }


    /** Returns WaitingPolls instance */
    public static WaitingPolls getInstance(){
        return WaitingPolls.WaitingPollsHelper.INSTANCE;
    }

    /** Returns list of WaitingPoll objects */
    public static ArrayList<WaitingData> getWaitingPolls(){
        return waiting_polls;
    }

    /**
     * Adds WaitingData object to the WaitingPolls list
     * @param wd Waiting Poll's data
     */
    public synchronized void addData(WaitingData wd){
        if (!waiting_polls.contains(wd)){
            Log.d(TAG, "waiting data added to list");
            waiting_polls.add(0, wd);
        }
    }

    /**
     * Removes the WaitingData with the given notification id from
     * the WaitingPolls list
     * @param notID
     */
    public synchronized int removeData(Integer notID){
        int pos=0;
        for (Iterator<WaitingData> i = waiting_polls.iterator(); i.hasNext(); ) {
            WaitingData wd = i.next();
            if(wd.getNotificationID() == notID){
                Poll poll = wd.getPoll();
                if(poll.hasImage())
                    removeFile(poll.getImageInfo().getPath());
                if(poll.hasRecord())
                    removeFile(poll.getRecordPath());
                i.remove();
                setChanged();
                notifyObservers();
                return pos;
            }
            pos++;
        }
        return -1;
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


    /** Saves WaitingPolls list permanently */
    public static void savetoWaitingList(){
        editor = pref.edit();
        editor.putString(Consts.WAITING_LIST, new Gson().toJson(new ArrayList<>(waiting_polls)));
        editor.commit();
    }

    /**
     * Sends a local broadcast message to remove the WaitingData with the given id
     *
     * @param context Activity's context
     * @param id notification id
     */
    public static void sendRemoveBroadcast(Context context, Integer id){
        Intent intent = new Intent(Receivers.W_REMOVE)
                .putExtra(Consts.NOTIFICATION_ID, id);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Sends a local broadcast message to update the WaitingPolls list size
     * @param context context Activity's context
     * @param count WaitingPolls list size
     */
    public static void sendUpdateBroadcast(Context context, int count) {
        Intent intent = new Intent(Receivers.W_COUNT)
                .putExtra(Consts.COUNT, count);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


}
