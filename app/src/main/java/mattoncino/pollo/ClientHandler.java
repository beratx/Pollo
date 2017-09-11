package mattoncino.pollo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.images.ImageManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

            InputStream input = socket.getInputStream();
            inputBufferedReader = new BufferedReader(
                                    new InputStreamReader(input));

            outputPrintWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);

            ImageInfo info = null;

            final String message = inputBufferedReader.readLine();

            if (message.equals(Consts.POLL_REQUEST)) {
                String id = inputBufferedReader.readLine();//poll_id
                String name = inputBufferedReader.readLine(); //poll_name
                String question = inputBufferedReader.readLine(); //poll_question
                int optCount = Integer.parseInt(inputBufferedReader.readLine());
                List<String> options = new ArrayList<>();
                for (int i = 0; i < optCount; i++) {
                    options.add(inputBufferedReader.readLine());
                }
                final String hostAddress = socket.getInetAddress().getHostAddress();
                boolean hasImage = Boolean.valueOf(inputBufferedReader.readLine());
                boolean isCamera = false;
                if(hasImage) {
                    isCamera = Boolean.valueOf(inputBufferedReader.readLine());
                    File imageFile = ImagePicker.createFile(context, ImagePicker.isExternalStorageWritable());
                    // Continue only if the File was successfully created
                    if (imageFile != null) {
                        /*Uri imageUri = FileProvider.getUriForFile(context,
                                "mattoncino.pollo.android.fileprovider",
                                imageFile);*/
                        Uri imageUri = Uri.fromFile(imageFile);
                        Log.d(TAG, "imageUri for received image: " + imageUri.toString());

                        BufferedInputStream bufin = null;
                        FileOutputStream output = null;

                        //File imageFile = ImagePicker.getTempFile(context);
                        //Uri imageUri = Uri.fromFile(imageFile);
                        try{
                            bufin = new BufferedInputStream(input);
                            output = new FileOutputStream(imageFile);

                            byte[] buffer = new byte[1024];
                            int len;
                            while((len = bufin.read(buffer)) != -1){
                                output.write(buffer);
                                output.flush();
                            }
                        } catch(Exception e){
                            e.printStackTrace();
                        } finally {
                            output.close();
                            bufin.close();
                        }

                        Log.d(TAG, "Image is received and saved. File length: " + imageFile.length());

                        info = new ImageInfo(imageUri.toString(), isCamera);
                    }
                    else
                        Log.d(TAG, "ImagePicker.createFile returns NULL");
                }

                //poll = new Poll(id, name, question, options, isCamera, info);
                poll = new Poll(id, name, question, options, hasImage, info);

                Log.d(TAG, "POLL REQUEST FROM: " + hostAddress);

                addNotification(poll, hostAddress);
                //how to update main menu so you can see new polls note?
                //with a handler!


            } else if (message.equals(Consts.ACCEPT)) {
                String pollID = inputBufferedReader.readLine();
                String hostAddress = inputBufferedReader.readLine();

                Log.d(TAG, "RECEIVED ACCEPT FROM: " + hostAddress);

                outputPrintWriter.println(Consts.RECEIVED);

                Log.d(TAG, "SENT ACCEPT RECEIVED MSG TO: " + hostAddress);

                Intent intent = new Intent("mattoncino.pollo.receive.poll.accept");
                intent.putExtra("pollID", pollID);
                intent.putExtra("hostAddress", hostAddress);
                intent.putExtra("accepted", true);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            } else if (message.equals(Consts.REJECT)){
                final String id = inputBufferedReader.readLine();
                final String hostAddress = inputBufferedReader.readLine();

                Intent intent = new Intent("mattoncino.pollo.receive.poll.accept");
                intent.putExtra("pollID", id);
                intent.putExtra("hostAddress", hostAddress);
                intent.putExtra("accepted", false);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            } else if(message.equals(Consts.POLL_VOTE)){
                final String id = inputBufferedReader.readLine();
                final int vote = Integer.parseInt(inputBufferedReader.readLine());
                final String hostAddress = socket.getInetAddress().getHostAddress();
                Log.d(TAG, "ARRIVED VOTE " + vote + " FROM HOST " + hostAddress);

                outputPrintWriter.println(Consts.RECEIVED);

                Log.d(TAG, "SENT VOTE RECEIVED MSG TO: " + hostAddress);

                Intent intent = new Intent("mattoncino.pollo.receive.poll.vote");
                intent.putExtra("pollID", id);
                intent.putExtra("myVote", false);
                intent.putExtra("vote", vote);
                intent.putExtra("hostAddress", hostAddress);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
            else if(message.equals(Consts.RESULT)){
                final String id = inputBufferedReader.readLine();
                final int count = Integer.parseInt(inputBufferedReader.readLine());
                int[] result = new int[5];
                for (int i = 0; i < count; i++) {
                    result[i] = Integer.parseInt(inputBufferedReader.readLine());
                }

                Intent intent = new Intent("mattoncino.pollo.receive.poll.result");
                intent.putExtra("pollID", id);
                intent.putExtra(Consts.RESULT, result);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputBufferedReader.close();
                outputPrintWriter.close();
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private void addNotification(Poll poll, String hostAddress){
        Random randomGenerator = new Random();
        int NOTIFICATION_ID;
        while((NOTIFICATION_ID = randomGenerator.nextInt()) == 0)
            ;

        Intent notificationIntent = new Intent(context, ActivePollsActivity.class)
                .putExtra(Consts.OWNER, Consts.OTHER)
                .putExtra(Consts.POLL, (Parcelable) poll)
                .putExtra("hostAddress", hostAddress)
                .putExtra("notificationID", NOTIFICATION_ID);


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(ActivePollsActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent acceptedPendingIntent =  PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startMain = new Intent(context, MainActivity.class)
                .putExtra("notificationID", NOTIFICATION_ID);

        PendingIntent rejectedPendingIntent = PendingIntent.getActivity(context, 0,
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
