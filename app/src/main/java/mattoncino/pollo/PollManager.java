package mattoncino.pollo;

import android.util.Log;

import java.util.ArrayList;


public class PollManager {
    public static final String TAG = "PollManager";
    //private static ConcurrentHashMap<String, Poll> active_polls;
    private static ArrayList<Poll> active_polls;

    public PollManager() {
        /* new ConcurrentHashMap<String, MyClass>(8, 0.9f, 1) */
        //this.active_polls = new ConcurrentHashMap<String, Poll>(8, 0.9f, 1);
        this.active_polls = new ArrayList<Poll>();
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
}
