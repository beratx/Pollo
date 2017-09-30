package mattoncino.pollo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class ClientHandler implements Runnable{
    private static String TAG = "ClientHandler";
    private Socket socket;
    private Context context;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private Poll poll;


    public ClientHandler(Socket socket, Context context){
        this.socket = socket;
        this.context = context;
    }

    public void serveRequestMessage() throws IOException {
        ImageInfo info = null;

        String id = dataInputStream.readUTF();//poll_id
        String name = dataInputStream.readUTF(); //poll_name
        String question = dataInputStream.readUTF(); //poll_question

        int optCount = dataInputStream.readInt();
        List<String> options = new ArrayList<>();
        for (int i = 0; i < optCount; i++) {
            options.add(dataInputStream.readUTF());
        }

        final String hostAddress = socket.getInetAddress().getHostAddress();

        boolean hasImage = dataInputStream.readBoolean();
        boolean isCamera = false;
        if(hasImage) {
            isCamera = dataInputStream.readBoolean();
            String imageType = dataInputStream.readUTF();
            File imageFile = ImagePicker.createFile(context, ImagePicker.isExternalStorageWritable(), imageType);
            if (imageFile != null) {
                Uri imageUri = Uri.fromFile(imageFile);
                Log.d(TAG, "imageUri for received image: " + imageUri.toString());
                FileOutputStream fileOut = new FileOutputStream(imageFile);

                int len;
                byte[] buffer = new byte[8192];

                while((len = dataInputStream.read(buffer, 0, 8192)) != -1){
                    fileOut.write(buffer, 0, len);
                    fileOut.flush();
                }

                fileOut.close();

                Log.d(TAG, "Image is received and saved. image length: " + imageFile.length() + " ext: " + imageType);

                info = new ImageInfo(imageUri.toString(), isCamera);
            }
            else
                Log.d(TAG, "ImagePicker.createFile returns NULL");
        }

        poll = new Poll(id, name, question, options, hasImage, info);

        Log.d(TAG, "POLL REQUEST FROM: " + hostAddress);

        addNotification(poll, hostAddress);
        //how to update main menu so you can see new polls note?
        //with a handler!
    }

    public void serveAcceptMessage() throws IOException {
        String pollID = dataInputStream.readUTF();
        String hostAddress = dataInputStream.readUTF();

        Log.d(TAG, "RECEIVED ACCEPT FROM: " + hostAddress);

        dataOutputStream.writeUTF(Consts.RECEIVED);
        dataOutputStream.flush();

        Log.d(TAG, "SENT ACCEPT RECEIVED MSG TO: " + hostAddress);

        Intent intent = new Intent("mattoncino.pollo.receive.poll.accept");
        intent.putExtra("pollID", pollID);
        intent.putExtra("hostAddress", hostAddress);
        intent.putExtra("accepted", true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void serveRejectMessage() throws IOException {
        final String id = dataInputStream.readUTF();
        final String hostAddress = dataInputStream.readUTF();

        Intent intent = new Intent("mattoncino.pollo.receive.poll.accept");
        intent.putExtra("pollID", id);
        intent.putExtra("hostAddress", hostAddress);
        intent.putExtra("accepted", false);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void serveVoteMessage() throws IOException {
        final String id = dataInputStream.readUTF();
        final int vote = Integer.parseInt(dataInputStream.readUTF());
        final String hostAddress = socket.getInetAddress().getHostAddress();
        Log.d(TAG, "ARRIVED VOTE " + vote + " FROM HOST " + hostAddress);

        dataOutputStream.writeUTF(Consts.RECEIVED);
        dataOutputStream.flush();

        Log.d(TAG, "SENT VOTE RECEIVED MSG TO: " + hostAddress);

        Intent intent = new Intent("mattoncino.pollo.receive.poll.vote");
        intent.putExtra("pollID", id);
        intent.putExtra("myVote", false);
        intent.putExtra("vote", vote);
        intent.putExtra("hostAddress", hostAddress);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void serveResultMessage() throws IOException {
        final String id = dataInputStream.readUTF();
        final int count = dataInputStream.readInt();
        int[] result = new int[5];
        for (int i = 0; i < count; i++) {
            result[i] = dataInputStream.readInt();
        }

        Intent intent = new Intent("mattoncino.pollo.receive.poll.result");
        intent.putExtra("pollID", id);
        intent.putExtra(Consts.RESULT, result);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    @Override
    public void run() {
        try {

            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            final String message = dataInputStream.readUTF();

            switch(message){
                case Consts.POLL_REQUEST:
                    serveRequestMessage();
                    break;
                case Consts.ACCEPT:
                    serveAcceptMessage();
                    break;
                case Consts.REJECT:
                    serveRejectMessage();
                    break;
                case Consts.POLL_VOTE:
                    serveVoteMessage();
                    break;
                case Consts.RESULT:
                    serveResultMessage();
                    break;
            }

        } catch(EOFException e){
            Log.d(TAG, "it should be isReachable socket connection, so it's ok...");
            Log.d(TAG, e.toString());
        } catch(SocketException e) {
            Log.d(TAG, e.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dataOutputStream.close();
                dataInputStream.close();
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private void addNotification(Poll poll, String hostAddress){
        Random randomGenerator = new Random();
        int NOTIFICATION_ID;
        int requestCode;

        while((NOTIFICATION_ID = randomGenerator.nextInt()) == 0)
            ;

        while((requestCode = randomGenerator.nextInt()) == 0)
            ;


        Intent notificationIntent = new Intent(context, ActivePollsActivity.class)
                .putExtra(Consts.OWNER, Consts.OTHER)
                .putExtra(Consts.POLL, (Parcelable) poll)
                .putExtra("hostAddress", hostAddress)
                .putExtra("notificationID", NOTIFICATION_ID);


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(ActivePollsActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent acceptedPendingIntent =  PendingIntent.getActivity(context, requestCode,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent startMain = new Intent(context, MainActivity.class)
                .putExtra("notificationID", NOTIFICATION_ID);

        PendingIntent rejectedPendingIntent = PendingIntent.getActivity(context, requestCode,
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
