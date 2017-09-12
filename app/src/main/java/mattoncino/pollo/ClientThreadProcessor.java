package mattoncino.pollo;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class ClientThreadProcessor implements Runnable{
    private static final String TAG = "ClientThreadProcess";
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

    private Socket getSocket(String hostIpAddress) {
        try {
            InetAddress serverAddr = InetAddress.getByName(hostIpAddress);
            socket = new Socket(serverAddr, SERVER_PORT);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
            Log.d(TAG, "getSocket: socket IO Exception!!!");
        }
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

    @Override
    public void run() {
        try {
            socket = getSocket(hostIpAddress);

            if(type.equals(Consts.POLL_REQUEST))
                sendPollRequest(socket);
            else if(type.equals(Consts.ACCEPT))
                sendAccept(socket);
            else if(type.equals(Consts.POLL_VOTE))
                sendVote(socket);
            else if(type.equals(Consts.RESULT))
                sendResult(socket);
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
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

                Log.d(TAG, "Mime type: " + mimeType + "ext: " + ext);

                String realPath = ImagePicker.getRealPathFromUri(context, imageUri);
                File imageFile = new File(realPath);
                byte[] byteArray = new byte[(int) imageFile.length()];

                FileInputStream fis = new FileInputStream(imageFile);
                fis.read(byteArray); //read file into bytes[]
                fis.close();

                dataOutputStream.writeInt(byteArray.length);
                dataOutputStream.write(byteArray, 0, byteArray.length);

                dataOutputStream.flush();

                Log.d(TAG, "Image sent over network and stream is closed.");
            }

            Log.d(TAG, "SENT POLL_REQUEST");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.d(TAG, e.toString());
            Activity act = (Activity) context;
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(context,  "Can't connect to other devices. Check your connection.", Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        } finally {
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
