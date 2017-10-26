package mattoncino.pollo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;



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


    public static WaitingPolls getInstance(){
        return WaitingPolls.WaitingPollsHelper.INSTANCE;
    }

    public static ArrayList<WaitingData> getWaitingPolls(){
        return waiting_polls;
    }


    public synchronized void addData(WaitingData wd){
        if (!waiting_polls.contains(wd)){
            Log.d(TAG, "waiting data added to list");
            waiting_polls.add(0, wd);
            //setChanged();
            //notifyObservers();
        }
    }


    public synchronized void removeData(Integer notID){
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
                break;
            }
        }
    }

    private void removeFile(String path){
        File file = new File(path);
        boolean r = file.delete();
        if(r) Log.d(TAG, path + " is deleted successfully");
    }


    public static void savetoWaitingList(){
        editor = pref.edit();
        editor.putString(Consts.WAITING_LIST, new Gson().toJson(new ArrayList<>(waiting_polls)));
        editor.commit();
    }


}
