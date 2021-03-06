package mattoncino.pollo;

import android.app.Notification;
import android.app.PendingIntent;
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

/**
 *  <p>Handles connection and data transfer requests received
 *  from other users.
 *  </p>
 *<p>
 * There can be 5 different type of requests:
 * <ul>
 * <li> Poll Request : a user sent a new poll request
 * <li> Accept : a user accepted a poll launched by this user
 * <li> Reject : a user rejected a poll launched by this user
 * <li> Vote : a user voted for  a poll launched by this user
 * <li> Result : a user sent results for a poll accepted by this user
 * </ul>
 * </p>
 */
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


    /**
     * Handles requests respect to its type.
     */
    @Override
    public void run() {
        try {

            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            String message = dataInputStream.readUTF();

            switch(message){
                case Consts.REQUEST:
                    serveRequestMessage();
                    break;
                case Consts.ACCEPT:
                    serveAcceptMessage();
                    break;
                case Consts.REJECT:
                    serveRejectMessage();
                    break;
                case Consts.VOTE:
                    serveVoteMessage();
                    break;
                case Consts.RESULT:
                    serveResultMessage();
                    break;
            }

        } catch(EOFException e){
            //e.printStackTrace();
            Log.d(TAG, "it must be isReachable, so it's ok...");
            Log.w(TAG, e.toString());
        } catch(SocketException e) {
            Log.wtf(TAG, e.toString());
        } catch (IOException e) {
            Log.wtf(TAG, e.toString());
        } finally {
            try {
                dataOutputStream.close();
                dataInputStream.close();
            } catch(IOException e){
                Log.wtf(TAG, e.toString());
            }
        }
    }

    /**
     * Receives a new Poll request from a user and all it's data,
     * and creates a new Poll.
     * Then creates a notification to inform the user and sends a
     * local broadcast to inform other activities in order to
     * update their state.
     *
     * @throws IOException
     */
    private void serveRequestMessage() throws IOException {
        String id = dataInputStream.readUTF();
        String name = dataInputStream.readUTF();
        String question = dataInputStream.readUTF();

        int optCount = dataInputStream.readInt();
        List<String> options = new ArrayList<>();
        for (int i = 0; i < optCount; i++) {
            options.add(dataInputStream.readUTF());
        }

        final String hostAddress = socket.getInetAddress().getHostAddress();

        boolean hasRecord = dataInputStream.readBoolean();
        String recordPath = null;
        int duration = -1;

        if(hasRecord){
            duration = dataInputStream.readInt();
            recordPath = SoundRecord.createTempFile(context, "3gp");
            FileOutputStream fileOut = new FileOutputStream(recordPath);

            long fileLength = dataInputStream.readLong();

            byte[] buffer = new byte[2048];
            long sum = 0;
            int len;

            while(sum < fileLength){
                len = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, fileLength - sum));
                fileOut.write(buffer, 0, len);
                fileOut.flush();
                sum += len;
            }

            fileOut.close();

            Log.d(TAG, "Record is received and saved.");
        }

        boolean hasImage = dataInputStream.readBoolean();

        ImageInfo info = null;

        if(hasImage) {
            boolean isCamera = dataInputStream.readBoolean();

            String ext = dataInputStream.readUTF();
            File imageFile = ImagePicker.createTempFile(context, ext);

            if (imageFile != null) {

                long fileLength = dataInputStream.readLong();

                Uri imageUri = Uri.fromFile(imageFile);
                Log.d(TAG, "imageUri for received image: " + imageUri.toString());
                FileOutputStream fileOut = new FileOutputStream(imageFile);

                int len;
                long sum = 0;
                byte[] buffer = new byte[8192];

                while(sum < fileLength){
                    len = dataInputStream.read(buffer, 0, 8192);
                    fileOut.write(buffer, 0, len);
                    fileOut.flush();
                    sum += len;
                }

                fileOut.close();

                Log.d(TAG, "Image is received and saved. image length: " + imageFile.length() + " ext: " + ext);

                info = new ImageInfo(imageUri.toString(), isCamera);
            }
            else
                Log.wtf(TAG, "ImagePicker.createFile returns NULL");
        }

        poll = new Poll(id, name, question, options, hasImage, info, recordPath, duration);

        Log.d(TAG, "POLL REQUEST FROM: " + hostAddress);

        int notificationID = addNotification(poll, hostAddress);

        addToWaitingPolls(notificationID, poll, hostAddress);
    }

    /**
     * Receives an ACCEPT message for a poll launched by this user.
     * Sends a Received message to the sender device and send a
     * local broadcast to inform ActivePollsActivity in order to
     * update its UI.
     *
     * @throws IOException
     */

    private void serveAcceptMessage() throws IOException {
        String pollID = dataInputStream.readUTF();
        String hostAddress = socket.getInetAddress().getHostAddress();

        Log.d(TAG, "RECEIVED ACCEPT FROM: " + hostAddress);

        dataOutputStream.writeUTF(Consts.RECEIVED);
        dataOutputStream.flush();

        Log.d(TAG, "SENT ACCEPT RECEIVED MSG TO: " + hostAddress);

        Intent intent = new Intent(Receivers.ACCEPT);
        intent.putExtra(Consts.POLL_ID, pollID);
        intent.putExtra(Consts.ADDRESS, hostAddress);
        intent.putExtra("accepted", true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Receives a REJECT message for a poll launched by this user.
     * Sends a local broadcast to inform ActivePollsActivity in order
     * to update its UI.
     * @throws IOException
     */
    private void serveRejectMessage() throws IOException {
        final String id = dataInputStream.readUTF();
        final String hostAddress = dataInputStream.readUTF();

        Log.d(TAG, "RECEIVED REJECT FROM: " + hostAddress);

        Intent intent = new Intent(Receivers.ACCEPT);
        intent.putExtra(Consts.POLL_ID, id);
        intent.putExtra(Consts.ADDRESS, hostAddress);
        intent.putExtra("accepted", false);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Receives a VOTE message for a poll launched by this user.
     * Sends a local broadcast to inform ActivePollsActivity in
     * order to update its UI.
     * @throws IOException
     */
    private void serveVoteMessage() throws IOException {
        final String id = dataInputStream.readUTF();
        final int vote = Integer.parseInt(dataInputStream.readUTF());
        final String hostAddress = socket.getInetAddress().getHostAddress();
        Log.d(TAG, "ARRIVED VOTE " + vote + " FROM HOST " + hostAddress);

        dataOutputStream.writeUTF(Consts.RECEIVED);
        dataOutputStream.flush();

        Log.d(TAG, "SENT VOTE RECEIVED MSG TO: " + hostAddress);

        Intent intent = new Intent(Receivers.VOTE);
        intent.putExtra(Consts.POLL_ID, id);
        intent.putExtra(Consts.VOTE, vote);
        intent.putExtra(Consts.ADDRESS, hostAddress);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Receives a RESULT message for an accepted poll from another user.
     * Receives results and sends a local broadcast to inform
     * ActivePollsActivity in order to update its UI.
     *
     * @throws IOException
     */
    private void serveResultMessage() throws IOException {
        final String id = dataInputStream.readUTF();
        final int count = dataInputStream.readInt();
        int[] result = new int[5];
        for (int i = 0; i < count; i++) {
            result[i] = dataInputStream.readInt();
        }

        Intent intent = new Intent(Receivers.RESULT);
        intent.putExtra(Consts.POLL_ID, id);
        intent.putExtra(Consts.RESULT, result);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    /**
     *  Creates a notification with Poll object and the host address
     *  of the user who sent the poll, with two actions: Accept and
     *  Reject. Both of them leads user to the ActivePollsActivity.
     *
     * @param poll  Poll object created with the arrived request data
     * @param hostAddress Host address of the poll's owner user
     * @return id number of created notification
     */
    private int addNotification(Poll poll, String hostAddress){
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
                .putExtra("notificationID", NOTIFICATION_ID)
                .putExtra(Consts.ACCEPT, true);


        PendingIntent acceptedPendingIntent =  PendingIntent.getActivity(context, requestCode,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        while((requestCode = randomGenerator.nextInt()) == 0)
            ;

        Intent notificationIntent2 = new Intent(context, ActivePollsActivity.class)
                .putExtra(Consts.OWNER, Consts.OTHER)
                .putExtra(Consts.POLL, (Parcelable) poll)
                .putExtra("hostAddress", hostAddress)
                .putExtra("notificationID", NOTIFICATION_ID)
                .putExtra(Consts.ACCEPT, false);


        PendingIntent rejectedPendingIntent =  PendingIntent.getActivity(context, requestCode,
                notificationIntent2, PendingIntent.FLAG_UPDATE_CURRENT);

        while((requestCode = randomGenerator.nextInt()) == 0)
            ;

        Intent notificationIntent3 = new Intent(context, WaitingPollsActivity.class);
        PendingIntent waitingPollsPendingIntent =  PendingIntent.getActivity(context, requestCode,
                notificationIntent3, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("New Poll Request")
                        .setContentText(poll.getName())
                        .setAutoCancel(true)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setVibrate(new long[] { 0, 1000, 1000, 1000, 1000 })
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .addAction(R.mipmap.ic_launcher, "Accept", acceptedPendingIntent)
                        .addAction(R.mipmap.ic_launcher, "Reject", rejectedPendingIntent);
                        //.setLights(Color.RED, 3000, 3000);

        builder.setContentIntent(waitingPollsPendingIntent);

        // Add as notification
        android.app.NotificationManager manager = (android.app.NotificationManager)
                                            context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());

        return NOTIFICATION_ID;
    }

    /**
     * Add newly created poll to the WaitingPolls List. These polls
     * are received but not accepted or rejected yet. Then sends a
     * local broadcast to other activities in order to update their
     * UI
     *
     * @param notificationID id number of the notification
     * @param poll newly received poll
     * @param hostAddress host address of the poll's owner
     */
    private void addToWaitingPolls(Integer notificationID, Poll poll, String hostAddress){
        WaitingPolls manager = WaitingPolls.getInstance();
        manager.addData(new WaitingData(poll, notificationID, hostAddress));
        //manager.savetoWaitingList();

        Intent intent = new Intent(Receivers.W_ADD);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        //to update main activity
        int count = WaitingPolls.getInstance().getWaitingPolls().size();
        Intent intent2 = new Intent(Receivers.W_COUNT).putExtra(Consts.COUNT, count);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent2);
    }
}
