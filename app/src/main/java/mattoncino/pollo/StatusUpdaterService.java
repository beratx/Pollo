package mattoncino.pollo;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;


public class StatusUpdaterService extends IntentService {
    private static final String TAG = "StatusUpdaterService";


    public StatusUpdaterService() {
        super(StatusUpdaterService.class.getName());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Service is handling the intent...");

        ArrayList<PollData> activePolls = PollManager.getInstance().getActivePolls();

        //String deviceId = ((MyApplication)getApplication()).getDeviceId();


        //if (intent != null) {
            //get your stuff from intent
            //final String action = intent.getAction();
            //intent.setExtrasClassLoader(getClass().getClassLoader());
            //ArrayList<PollData> activePolls = intent.getParcelableArrayListExtra("polls");

            while(true){
                try {
                    Thread.sleep(30000);

                    if(!activePolls.isEmpty()){

                        HashSet<String> onlineDevices = (HashSet<String>) ((MyApplication)getApplication()).getConnectionManager().getOnlineDevices(this);

                        for (PollData pd : activePolls) {
                            if(!pd.isTerminated()) {

                                HashSet<String> contactedDevices = (HashSet<String>) pd.getContactedDevices();
                                HashSet<String> acceptedDevices = (HashSet<String>) pd.getAcceptedDevices();
                                HashSet<String> votedDevices = (HashSet<String>) pd.getVotedDevices();

                                for (Iterator<String> i = contactedDevices.iterator(); i.hasNext();) {
                                    String dev = i.next();
                                    if (!onlineDevices.contains(dev)) {
                                        if (votedDevices.contains(dev))
                                            ; //do nothing
                                        else if (acceptedDevices.contains(dev)){
                                            acceptedDevices.remove(dev);
                                            i.remove();
                                            pd.decrementResponseCount();
                                        }
                                        else
                                            i.remove();
                                    }
                                }
                            }
                        }

                        System.out.println("----------------Device Status Update starts--------------------");
                        System.out.println("Online Devices: " + onlineDevices.toString());
                        System.out.println("---------Active Poll Count: " + activePolls.size() + "-----------");
                        for (PollData pd : activePolls) {
                            System.out.println("pollName: " + pd.getPollName());
                            System.out.println("deviceCount: " + pd.getContactedDevices().size());
                            System.out.println("contactedDevices: " + Arrays.toString(pd.getContactedDevices().toArray()));
                            System.out.println("responseCount: " + pd.getResponseCount());
                            System.out.println("acceptedDevices: " + Arrays.toString(pd.getAcceptedDevices().toArray()));
                            System.out.println("votedDevices: " + Arrays.toString(pd.getVotedDevices().toArray()));
                            System.out.println("myVote: " + pd.getMyVote());
                            System.out.println("---------------------------------------------------------------------");
                        }
                        System.out.println("----------------Device Status Update finishes--------------------");
                    }
                    else
                        System.out.println("-------------------No Active Polls-----------------------");


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    //}
}
