package mattoncino.pollo;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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

        String deviceId = ((MyApplication)getApplication()).getDeviceId();
        final List<String> onlineDevices  = ((MyApplication)getApplication()).getConnectionManager().getOnlineDevicesList(this, deviceId);

        //if (intent != null) {
            //get your stuff from intent
            //final String action = intent.getAction();
            //intent.setExtrasClassLoader(getClass().getClassLoader());
            //ArrayList<PollData> activePolls = intent.getParcelableArrayListExtra("polls");


            while(true){
                try {
                    Thread.sleep(10000);
                    if(!activePolls.isEmpty()){
                        System.out.println("----------------Device Status Update starts--------------------");
                        System.out.println("Online Devices: " + onlineDevices.toString());
                        System.out.println("---------Active Poll Count: " + activePolls.size() + "-----------");
                        for (PollData pd : activePolls) {
                            System.out.println("pollName: " + pd.getPollName());
                            System.out.println("deviceCount: " + pd.getDeviceCount());
                            System.out.println("responseCount: " + pd.getResponseCount());
                            System.out.println("acceptedDevices: " + Arrays.toString(pd.getAcceptedDevices().toArray()));
                            System.out.println("votedDevices: " + Arrays.toString(pd.getVotedDevices().toArray()));
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
