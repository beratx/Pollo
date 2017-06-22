package mattoncino.pollo;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mattoncino.pollo.databinding.ActivityMainBinding;


public class ClientHandler implements Runnable {
    private static String TAG = "CLIENT_HANDLER";
    private Socket socket;
    private Context context;
    private BufferedReader inputBufferedReader;
    private PrintWriter outputPrintWriter;
    private String pollElements[];
    private Poll poll;
    private ActivityMainBinding mainActbinding;
    private static final Type LIST_TYPE = new TypeToken<List<Poll>>() {}.getType();


    public ClientHandler(Socket socket, Context context){
        this.socket = socket;
        this.context = context;
        this.pollElements = new String[5];
    }

    @Override
    public void run() {
        try {
            inputBufferedReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            outputPrintWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);

            //Log.v(TAG, "GOT INPUT AND OUTPUT STREAM");

            final String response = inputBufferedReader.readLine(); //poll_request

            if (response.equals(Consts.POLL_REQUEST)) {

                pollElements[0] = inputBufferedReader.readLine(); //poll_name
                pollElements[1] = inputBufferedReader.readLine(); //poll_question
                pollElements[2] = inputBufferedReader.readLine(); //poll_firstOpt
                pollElements[3] = inputBufferedReader.readLine(); //poll_secondOpt
                pollElements[4] = inputBufferedReader.readLine(); //poll_hostAddress

                poll = new Poll(pollElements[0], pollElements[1], pollElements[2], pollElements[3], pollElements[4]);

                /*outputPrintWriter.println(Consts.ACCEPT);*/
                //Log.v(TAG, "SENT ACCEPT MESSAGE");

                Activity act = (Activity) context;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.useShortToast(context, response + " from: " + pollElements[4]);
                    }
                });

                addNotification(poll);
                //how to update main menu so you can see new polls note?


            }else if(response.equals(Consts.ACCEPT)){

                String pollName = inputBufferedReader.readLine();
                String hostAddress = inputBufferedReader.readLine();
                //aggiornare voter list

                Activity act = (Activity) context;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.useShortToast(context, "GONNA UPDATE VOTER LIST");
                    }
                });


            }else if(response.equals(Consts.POLL_VOTE)){

                String pollName = inputBufferedReader.readLine();
                final int vote = Integer.parseInt(inputBufferedReader.readLine());
                String hostAddress = inputBufferedReader.readLine();

                outputPrintWriter.println(Consts.RECEIVED);

                Activity act = (Activity) context;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.useShortToast(context, "VOTED: " + vote);
                    }
                });

                SharedPreferences pref = context.getSharedPreferences(Consts.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                Gson gson = new Gson();


                ArrayList<Poll> activePolls = new Gson().fromJson(pref.getString(Consts.POLL_LIST, null), LIST_TYPE);
                if (activePolls != null && activePolls.size() != 0) {
                   for(Poll p : activePolls){
                       if(pollName.equals(p.getName()))
                           p.addVote(vote);
                   }
                }

                editor.putString(Consts.POLL_LIST, new Gson().toJson(new ArrayList<Poll>(activePolls)));
                editor.commit();
                //QUI DEVO AGGIORNARE IL POLL IN QUESTIONE


            }


            inputBufferedReader.close();
            outputPrintWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addNotification(Poll poll){
        Random randomGenerator = new Random();
        final int NOTIFICATION_ID = randomGenerator.nextInt();

        Intent notificationIntent = new Intent(context, ActivePollsActivity.class);
        notificationIntent.putExtra(Consts.POLL_OTHER, (Parcelable) poll);
        //notificationIntent.putExtra(Consts.HOST_ADDR, hostAddress);
        //notificationIntent.putExtra()

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(ActivePollsActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent acceptedPendingIntent =  PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent rejectedPendingIntent = PendingIntent.getActivity(context, 1,
                startMain, PendingIntent.FLAG_UPDATE_CURRENT);


        /*stackBuilder.getPendingIntent(
         0, PendingIntent.FLAG_UPDATE_CURRENT);*/

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("New Poll Request")
                        .setContentText("New Poll: " + poll.getName())
                        .setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_MAX)
                        .addAction(R.mipmap.ic_launcher, "Accept", acceptedPendingIntent)
                        .addAction(R.mipmap.ic_launcher, "Reject", rejectedPendingIntent);

        builder.setContentIntent(acceptedPendingIntent);

        // Add as notification
        android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }





}
