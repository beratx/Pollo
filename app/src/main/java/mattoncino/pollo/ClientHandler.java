package mattoncino.pollo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class ClientHandler implements Runnable{
    private static String TAG = "CLIENT_HANDLER";
    private Socket socket;
    private Context context;
    private BufferedReader inputBufferedReader;
    private PrintWriter outputPrintWriter;
    private Poll poll;


    public ClientHandler(Socket socket, Context context){
        this.socket = socket;
        this.context = context;
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

                String id = inputBufferedReader.readLine();//poll_id
                String name = inputBufferedReader.readLine(); //poll_name
                String question = inputBufferedReader.readLine(); //poll_question
                final String hostAddress = inputBufferedReader.readLine(); //poll_hostAddress
                int optCount = Integer.parseInt(inputBufferedReader.readLine());
                List<String> options = new ArrayList<>();
                for (int i = 0; i < optCount; i++) {
                    options.add(inputBufferedReader.readLine());
                }

                poll = new Poll(id, name, question, options);

                /*outputPrintWriter.println(Consts.ACCEPT);*/
                Log.d(TAG, "POLL REQUEST FROM: " + hostAddress);

                /*Activity act = (Activity) context;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.useShortToast(context, message + " from: " + hostAddress);
                    }
                });*/

                addNotification(poll, hostAddress);
                //how to update main menu so you can see new polls note?
                //with a handler!


            }else if(message.equals(Consts.ACCEPT)){

                String pollID = inputBufferedReader.readLine();
                String hostAddress = inputBufferedReader.readLine();

                Log.d(TAG, "RECEIVED ACCEPT FROM: " + hostAddress);

                outputPrintWriter.println(Consts.RECEIVED);

                Log.d(TAG, "SENT ACCEPT RECEIVED MSG TO: " + hostAddress);

                Intent intent = new Intent("mattoncino.pollo.receive.poll.accept");
                intent.putExtra("pollID", pollID);
                intent.putExtra("hostAddress", hostAddress);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                //aggiornare voter list
                //WRONG YOU SHOULD GET THE POLL FROM NAME THEN ADD IT!!
                //USE A THREAD TO DO IT!!!
                //poll.addParticipant(hostAddress);

            }else if(message.equals(Consts.POLL_VOTE)){

                final String id = inputBufferedReader.readLine();
                final int vote = Integer.parseInt(inputBufferedReader.readLine());
                final String hostAddress = inputBufferedReader.readLine();
                Log.d(TAG, "arrived vote: " + vote);

                outputPrintWriter.println(Consts.RECEIVED);

                Log.d(TAG, "SENT VOTE RECEIVED MSG TO: " + hostAddress);

                Intent intent = new Intent("mattoncino.pollo.receive.poll.vote");
                intent.putExtra("pollID", id);
                intent.putExtra("vote", vote);
                intent.putExtra("hostAddress", hostAddress);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                Log.d(TAG, "manager.updatePoll is called for host: " + hostAddress);

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

        Intent notificationIntent = new Intent(context, ActivePollsActivity.class)
                .putExtra(Consts.OWNER, Consts.OTHER)
                .putExtra(Consts.POLL, (Parcelable) poll)
                .putExtra("hostAddress", hostAddress)
                .putExtra("notificationID", NOTIFICATION_ID);

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
                        .setContentText(poll.getName())
                        .setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_MAX)
                        .addAction(R.mipmap.ic_launcher, "Accept", acceptedPendingIntent)
                        .addAction(R.mipmap.ic_launcher, "Reject", rejectedPendingIntent);

        builder.setContentIntent(acceptedPendingIntent);

        // Add as notification
        android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

}
