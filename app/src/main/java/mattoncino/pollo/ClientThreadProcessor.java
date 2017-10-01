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


public class ClientThreadProcessor implements Runnable{
    private static final String TAG = "ClientThreadProcessor";
    private static final int SERVER_PORT = 8700;
    private Socket socket;
    private Context context;
    private String hostIpAddress;
    private List<String> pollInfo;
    private Poll poll;
    private String type;
    private String pollID;
    private int[] result;

    public ClientThreadProcessor(String hostIpAddress, Context context, String type){
        this.context = context;
        this.hostIpAddress = hostIpAddress;
        this.type = type;
    }

    public ClientThreadProcessor(String hostIpAddress, Context context, String type, ArrayList<String> pollInfo) {
        this(hostIpAddress, context, type);
        this.pollInfo = pollInfo;
    }

    public ClientThreadProcessor(String hostIpAddress, Context context, String type, String pollID, int[] result) {
        this(hostIpAddress, context, type);
        this.pollID = pollID;
        this.result = result;
    }

    public ClientThreadProcessor(String hostIpAddress, Context context, String type, Poll poll) {
        this(hostIpAddress, context, type);
        this.poll = poll;
    }

    private Socket getSocket(String hostIpAddress) throws IOException {
            InetAddress serverAddr = InetAddress.getByName(hostIpAddress);
            socket = new Socket(serverAddr, SERVER_PORT);

        return socket;
    }

    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serve(Socket socket, String type){
        switch(type) {
            case Consts.POLL_REQUEST:
                sendPollRequest(socket);
                break;
            case Consts.ACCEPT:
                sendAccept(socket);
                break;
            case Consts.POLL_VOTE:
                sendVote(socket);
                break;
            case Consts.RESULT:
                sendResult(socket);
                break;
        }
    }

    @Override
    public void run() {
        try {
            socket = getSocket(hostIpAddress);
            serve(socket, type);
        } catch (UnknownHostException e1) {
            Log.wtf(TAG, e1.toString());
        } catch (IOException e) {
            Log.wtf(TAG, e.toString());
        } finally {
            closeSocket(socket);
        }

    }

    public void sendPollRequest(Socket socket){

        DataOutputStream dataOutputStream = null;

        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            dataOutputStream.writeUTF(Consts.POLL_REQUEST);
            dataOutputStream.writeUTF(poll.getId()); //poll_id
            dataOutputStream.writeUTF(poll.getName()); //poll_name
            dataOutputStream.writeUTF(poll.getQuestion()); //poll_question
            //dataOutputStream.writeUTF(poll.get(3)); //device address
            dataOutputStream.writeInt(poll.getOptions().size());//#options
            for (int i = 0; i < poll.getOptions().size(); i++) {
                dataOutputStream.writeUTF(poll.getOptions().get(i));
            }
            dataOutputStream.writeBoolean(poll.hasImage());
            if(poll.hasImage()){
                dataOutputStream.writeBoolean(poll.getImageInfo().isCamera());

                Uri imageUri = Uri.parse(poll.getImageInfo().getPath());
                String mimeType = ImagePicker.getMimeType(context, imageUri);
                String ext = mimeType.substring(mimeType.lastIndexOf("/") + 1);

                if(ext == null || ext.length() == 0) ext = "bmp";
                dataOutputStream.writeUTF(ext);

                String realPath = ImagePicker.getRealPathFromUri(context, imageUri);
                File imageFile = new File(realPath);
                FileInputStream fis = new FileInputStream(imageFile);

                Log.d(TAG, "Image len: " + imageFile.length() +" Mime type: " + mimeType + " ext: " + ext);

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

        } catch (IOException e) {
            e.printStackTrace();
        }/* catch (NullPointerException e) {
            Log.d(TAG, e.toString());
            Activity act = (Activity) context;
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(context,  "Can't connect to other devices. Check your connection.", Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }*/  finally {
            if(dataOutputStream != null)
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public void sendAccept(Socket socket){
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;

        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeUTF(Consts.ACCEPT);
            dataOutputStream.writeUTF(pollInfo.get(0)); //poll_id
            dataOutputStream.writeUTF(pollInfo.get(1)); //poll_hostAddress

            dataOutputStream.flush();
            //System.out.println("hostIpaddress: " + hostIpAddress + " address in pollInfo: " + pollInfo.get(1));

            //BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            dataInputStream = new DataInputStream(socket.getInputStream());
;
            final String res = dataInputStream.readUTF();

            Log.d(TAG, "SENT ACCEPT MSG TO: " + pollInfo.get(1));

            if(res.equals(Consts.RECEIVED))
                Log.d(TAG, "DEVICE " + pollInfo.get(1) + " RECEIVED MY ACCEPT: ");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(dataOutputStream != null)
                try {
                    dataOutputStream.close();
                    dataInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public void sendVote(Socket socket){
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;

        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            dataOutputStream.writeUTF(Consts.POLL_VOTE);
            dataOutputStream.writeUTF(pollInfo.get(0)); //id
            dataOutputStream.writeUTF(pollInfo.get(1)); //vote

            dataOutputStream.flush();

            Log.d(TAG, "SENT VOTE");

            dataInputStream = new DataInputStream(socket.getInputStream());

            final String messageFromClient = dataInputStream.readUTF();

            if (messageFromClient.equals(Consts.RECEIVED)) {
                Log.d(TAG, "MY VOTE IS RECEIVED.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }  finally {
            if(dataOutputStream != null)
                try {
                    dataOutputStream.close();
                    dataInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }

    public void sendResult(Socket socket){
        DataOutputStream dataOutputStream = null;

        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeUTF(Consts.RESULT);
            dataOutputStream.writeUTF(pollID);
            dataOutputStream.writeInt(result.length);
            for (int i = 0; i < result.length; i++) {
                dataOutputStream.writeInt(result[i]);
            }

            dataOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(dataOutputStream != null)
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}
