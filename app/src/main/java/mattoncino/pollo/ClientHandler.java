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
import android.util.Log;

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
    private List<String> pollData;
    private Poll poll;
    private ActivityMainBinding mainActbinding;
    private static final Type LIST_TYPE = new TypeToken<List<Poll>>() {}.getType();


    public ClientHandler(Socket socket, Context context){
        this.socket = socket;
        this.context = context;
        this.pollData = new ArrayList<String>();
    }

    @Override
    public void run() {
        try {
            inputBufferedReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            outputPrintWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);

            //Log.v(TAG, "GOT INPUT AND OUTPUT STREAM");

            final String message = inputBufferedReader.readLine();

            if (message.equals(Consts.POLL_REQUEST)) {

                String name = inputBufferedReader.readLine(); //poll_name
                String question = inputBufferedReader.readLine(); //poll_question
                final String hostAddress = inputBufferedReader.readLine(); //poll_hostAddress
                int optSize = Integer.parseInt(inputBufferedReader.readLine());
                List<String> options = new ArrayList<>();
                for (int i = 0; i < optSize; i++) {
                    options.add(inputBufferedReader.readLine());
                }

                poll = new Poll(name, question, options, hostAddress);

                /*outputPrintWriter.println(Consts.ACCEPT);*/
                Log.d(TAG, "SENT ACCEPT MESSAGE");

                Activity act = (Activity) context;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.useShortToast(context, message + " from: " + hostAddress);
                    }
                });

                addNotification(poll);
                //how to update main menu so you can see new polls note?


            }else if(message.equals(Consts.ACCEPT)){

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


            }else if(message.equals(Consts.POLL_VOTE)){

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
