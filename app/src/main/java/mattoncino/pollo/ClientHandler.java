package mattoncino.pollo;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;


public class ClientHandler implements Runnable {
    private static String TAG = "CLIENT_HANDLER";
    private static final String POLL_REQUEST = "poll_request";
    private static final String ACCEPT = "accept";
    private Socket socket;
    private Context context;
    private BufferedReader inputBufferedReader;
    private PrintWriter outputPrintWriter;
    private String pollElements[];
    private Poll poll;


    public ClientHandler(Socket socket, Context context){
        this.socket = socket;
        this.context = context;
        this.pollElements = new String[4];
    }

    @Override
    public void run() {
        try {
            inputBufferedReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            outputPrintWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);

            Log.v(TAG, "GOT INPUT AND OUTPUT STREAM");

            final String response = inputBufferedReader.readLine(); //poll_request

            pollElements[0] = inputBufferedReader.readLine(); //poll_name
            pollElements[1] = inputBufferedReader.readLine(); //poll_question
            pollElements[2] = inputBufferedReader.readLine(); //poll_firstOpt
            pollElements[3] = inputBufferedReader.readLine(); //poll_secondOpt

            final String hostAddress = inputBufferedReader.readLine();

            poll = new Poll(pollElements[0],pollElements[1],pollElements[2],pollElements[3], Consts.OTHER);

            if (response.equals(POLL_REQUEST)){
                outputPrintWriter.println(ACCEPT);
                //Log.v(TAG, "SENT ACCEPT MESSAGE");

                Activity act = (Activity) context;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.useShortToast(context, response);
                    }
                });



                addNotification(poll, hostAddress);
            }

            inputBufferedReader.close();
            outputPrintWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addNotification(Poll poll, String hostAddress){
        Random randomGenerator = new Random();
        final int NOTIFICATION_ID = randomGenerator.nextInt();

        Intent notificationIntent = new Intent(context, ActivePollsActivity.class);
        notificationIntent.putExtra(Consts.POLL_OTHER, (Parcelable) poll);
        notificationIntent.putExtra(Consts.HOST_ADDR, hostAddress);
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
                        .setContentText("Poll name will be here")
                        .setAutoCancel(true)
                        .addAction(R.mipmap.ic_launcher, "Accept", acceptedPendingIntent)
                        .addAction(R.mipmap.ic_launcher, "Reject", rejectedPendingIntent);

        builder.setContentIntent(acceptedPendingIntent);

        // Add as notification
        android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    private void removeNotification(int notificationID){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationID);
    }



}
