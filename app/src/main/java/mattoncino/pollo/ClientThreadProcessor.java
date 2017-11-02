package mattoncino.pollo;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Responsible to connect and send specified type of data to
 * other devices.
 * </p>
 * <p>
 * <ul>
 * <li> Poll Request : sends a user request to a host
 * <li> Accept : sends an accept message for a received poll request
 * <li> Reject : sends a receive message for a received poll request
 * <li> Vote : sends a vote for an accepted poll
 * <li> Result : sends results of a (its own) Poll to a host
 * </ul>
 * </p>
 */
public class ClientThreadProcessor implements Runnable{
    private static final String TAG = "ClientThreadProcessor";
    private static final int SERVER_PORT = 8700;
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private Context context;
    private String hostIpAddress;
    private List<String> pollInfo;
    private Poll poll;
    private String type;
    private String pollID;
    private int[] result;


    /**
     * Constructor
     *
     * @param hostIpAddress local host address of the device to connect
     * @param context Activity's context
     * @param type type of message: [REQUEST, ACCEPT, REJECT, VOTE, RESULT]
     */
    public ClientThreadProcessor(String hostIpAddress, Context context, String type){
        this.context = context;
        this.hostIpAddress = hostIpAddress;
        this.type = type;
    }

    /**
     * Constructor
     *
     * @param hostIpAddress local host address of the device to connect
     * @param context Activity's context
     * @param type type of message: [REQUEST, ACCEPT, REJECT, VOTE, RESULT]
     * @param pollInfo poll information to send
     */
    public ClientThreadProcessor(String hostIpAddress, Context context, String type, ArrayList<String> pollInfo) {
        this(hostIpAddress, context, type);
        this.pollInfo = pollInfo;
    }

    /**
     *
     * @param hostIpAddress local host address of the device to connect
     * @param context Activity's context
     * @param type type of message: [REQUEST, ACCEPT, REJECT, VOTE, RESULT]
     * @param pollID identifier of the poll
     * @param result results of the poll
     */
    public ClientThreadProcessor(String hostIpAddress, Context context, String type, String pollID, int[] result) {
        this(hostIpAddress, context, type);
        this.pollID = pollID;
        this.result = result;
    }

    /**
     *
     * @param hostIpAddress local host address of the device to connect
     * @param context Activity's context
     * @param type type of message: [REQUEST, ACCEPT, REJECT, VOTE, RESULT]
     * @param poll poll object to send
     */
    public ClientThreadProcessor(String hostIpAddress, Context context, String type, Poll poll) {
        this(hostIpAddress, context, type);
        this.poll = poll;
    }

    /**
     * Creates a new socket to comunicate with the host with hostIpaddress
     * @param hostIpAddress local host address of the device to connect
     * @return a new created socket
     * @throws IOException
     */
    private Socket getSocket(String hostIpAddress) throws IOException {
            InetAddress serverAddr = InetAddress.getByName(hostIpAddress);
            socket = new Socket(serverAddr, SERVER_PORT);

        return socket;
    }

    /**
     * Closes given socket
     * @param socket
     */
    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Establishes connection with the host with hostIpAddress
     * and realizes requested data transfer.
     */
    @Override
    public void run() {
        try {
            socket = getSocket(hostIpAddress);

            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            switch(type) {
                case Consts.REQUEST:
                    sendPollRequest();
                    break;
                case Consts.ACCEPT:
                    sendAccept();
                    break;
                case Consts.VOTE:
                    sendVote();
                    break;
                case Consts.RESULT:
                    sendResult();
                    break;
            }
        } catch (UnknownHostException e1) {
            Log.wtf(TAG, e1.toString());
        } catch (IOException e) {
            Log.wtf(TAG, e.toString());
        } finally {
            closeSocket(socket);
            try {
                dataOutputStream.close();
                dataInputStream.close();
            } catch (NullPointerException e) {
                Log.wtf(TAG, e.toString());
            } catch (IOException e) {
                Log.wtf(TAG, e.toString());
            }
        }
    }


    /**
     * Sends a poll request and all the data of
     * the Poll object
     * @throws IOException
     */
    public void sendPollRequest() throws IOException {
        dataOutputStream.writeUTF(Consts.REQUEST);
        dataOutputStream.writeUTF(poll.getId());
        dataOutputStream.writeUTF(poll.getName());
        dataOutputStream.writeUTF(poll.getQuestion());

        dataOutputStream.writeInt(poll.getOptions().size());
        for (int i = 0; i < poll.getOptions().size(); i++) {
            dataOutputStream.writeUTF(poll.getOptions().get(i));
        }

        dataOutputStream.writeBoolean(poll.hasRecord());

        if(poll.hasRecord()){
            dataOutputStream.writeInt(poll.getDuration());

            File recordFile = new File(poll.getRecordPath());
            long length = recordFile.length();
            dataOutputStream.writeLong(length);

            FileInputStream fis = new FileInputStream(recordFile);

            Log.d(TAG, "Record len: " + recordFile.length());

            int len;
            byte[] byteArray = new byte[2048];

            while (length > 0 && (len = fis.read(byteArray, 0, (int)Math.min(byteArray.length, length))) != -1)
            {
                dataOutputStream.write(byteArray, 0, len);
                dataOutputStream.flush();
                length -= len;
            }
            fis.close();
            Log.d(TAG, "Record sent over network and stream is closed.");
        }

        dataOutputStream.writeBoolean(poll.hasImage());

        if(poll.hasImage()){
            String realPath;
            String ext;

            dataOutputStream.writeBoolean(poll.getImageInfo().isCamera());

            if(poll.getImageInfo().isCamera()){
                realPath = poll.getImageInfo().getPath().substring(7);
                ext = "jpg";
            }
            else {
                Uri imageUri = Uri.parse(poll.getImageInfo().getPath());
                realPath = ImagePicker.getRealPathFromUri(context, imageUri);
                ext = ImagePicker.getImageType(context, imageUri);
                if(ext.length() == 0) ext = "bmp";
            }
            //Log.d(TAG, "imagePath: " + poll.getImageInfo().getPath());
            //Log.d(TAG, "realPath: " + realPath);

            dataOutputStream.writeUTF(ext);

            File imageFile = new File(realPath);

            long length = imageFile.length();
            dataOutputStream.writeLong(length);

            FileInputStream fis = new FileInputStream(imageFile);

            Log.d(TAG, "Image len: " + imageFile.length() + " ext: " + ext);

            int len;
            byte[] byteArray = new byte[8192];

            while( (len = fis.read(byteArray)) != -1 ){
                dataOutputStream.write(byteArray, 0, len);
                dataOutputStream.flush();
            }

            fis.close();
            Log.d(TAG, "Image sent over network and stream is closed.");
        }

        Log.d(TAG, "SENT POLL_REQUEST");
    }

    /**
     * Sends an ACCEPT message and eventually receives a RECEIVED message from host
     */
    public void sendAccept() throws IOException {
            dataOutputStream.writeUTF(Consts.ACCEPT);
            dataOutputStream.writeUTF(pollInfo.get(0)); //poll_id
            dataOutputStream.flush();

            dataInputStream = new DataInputStream(socket.getInputStream());

            final String res = dataInputStream.readUTF();

            Log.d(TAG, "SENT ACCEPT MSG TO: " + hostIpAddress);

            if(res.equals(Consts.RECEIVED))
                Log.d(TAG, "DEVICE " + hostIpAddress + " RECEIVED MY ACCEPT: ");
    }

    /**
     * sends a vote for a poll sent by the host
     * @throws IOException
     */
    public void sendVote() throws IOException {
            dataOutputStream.writeUTF(Consts.VOTE);
            dataOutputStream.writeUTF(pollInfo.get(0)); //id
            dataOutputStream.writeUTF(pollInfo.get(1)); //vote

            dataOutputStream.flush();

            Log.d(TAG, "SENT VOTE");

            dataInputStream = new DataInputStream(socket.getInputStream());

            final String messageFromClient = dataInputStream.readUTF();

            if (messageFromClient.equals(Consts.RECEIVED))
                Log.d(TAG, "MY VOTE IS RECEIVED.");
    }

    /**
     * Sends results of a terminated Poll
     * @throws IOException
     */
    public void sendResult() throws IOException {
            dataOutputStream.writeUTF(Consts.RESULT);
            dataOutputStream.writeUTF(pollID);
            dataOutputStream.writeInt(result.length);
            for (int i = 0; i < result.length; i++) {
                dataOutputStream.writeInt(result[i]);
            }

            dataOutputStream.flush();
    }
}
