package mattoncino.pollo;

import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;


public class PollManager {
    public static final String TAG = "PollManager";
    private static ConcurrentHashMap<String, Poll> active_polls;

    public PollManager() {
        /* new ConcurrentHashMap<String, MyClass>(8, 0.9f, 1) */
        this.active_polls = new ConcurrentHashMap<String, Poll>(8, 0.9f, 1);
    }

    public void addPoll(String key, Poll poll){
        if(active_polls.put(key, poll) == null)
            Log.i(TAG, "new poll is added");
        else
            Log.i(TAG, "existing poll is overwritten");
    }

    public Poll popPoll(String key){
        return active_polls.remove(key);
    }

    public void terminatePoll(Poll poll){

    }

    public void updatePoll(){

    }
}
