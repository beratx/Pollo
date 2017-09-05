package mattoncino.pollo;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
    private Socket socket;
    private Context context;
    private String hostIpAddress;
    private static final String TAG = "ClientThreadProcess";
    private List<String> pollInfo;
    private Poll poll;
    private String type;
    private String pollID;
    private int[] result;

    public ClientThreadProcessor(String hostIpAddress, Context context, String type, ArrayList<String> pollInfo) {
        this.context = context;
        this.hostIpAddress = hostIpAddress;
        this.pollInfo = pollInfo;
        this.type = type;
    }

    public ClientThreadProcessor(String hostIpAddress, Context context, String type, String pollID, int[] result) {
        this.context = context;
        this.hostIpAddress = hostIpAddress;
        this.type = type;
        this.pollID = pollID;
        this.result = result;
    }

    public ClientThreadProcessor(String hostIpAddress, Context context, String type, Poll poll) {
        this.context = context;
        this.hostIpAddress = hostIpAddress;
        this.poll = poll;
        this.type = type;
    }



    /*public ClientThreadProcessor(String hostIpAddress, Context context, String type, int vote) {
        this.context = context;
        this.hostIpAddress = hostIpAddress;
        this.type = type;
        this.vote = vote;
    }*/

    private Socket getSocket(String hostIpAddress) {
        try {
            InetAddress serverAddr = InetAddress.getByName(hostIpAddress);
            socket = new Socket(serverAddr, ServerSocketHandler.SERVER_PORT);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            //e1.printStackTrace();
            Log.d(TAG, "cant connect to other devices. check your connetion");
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
        PrintWriter output = null;

        try {
            OutputStream outputStream = socket.getOutputStream();


            output = new PrintWriter(new BufferedWriter(
                                        new OutputStreamWriter(outputStream)), true);


            //DataOutputStream dout = new DataOutputStream(bout);

            //TODO:CHANGE IT WITH POLL
            output.println(Consts.POLL_REQUEST);
            output.println(poll.getId()); //poll_id
            output.println(poll.getName()); //poll_name
            output.println(poll.getQuestion()); //poll_question
            //output.println(poll.get(3)); //device address
            output.println(poll.getOptions().size());//#options
            for (int i = 0; i < poll.getOptions().size(); i++) {
                output.println(poll.getOptions().get(i));
            }
            output.println(poll.hasImage());
            if(poll.hasImage()){
                output.println(poll.getImageInfo().isCamera());
                BufferedOutputStream bout = new BufferedOutputStream(outputStream);
                Log.d(TAG, "imagePath: " + poll.getImageInfo().getPath());
                String realPath = ImagePicker.getRealPathFromUri(context, Uri.parse(poll.getImageInfo().getPath()));
                if(realPath != null)  Log.d(TAG, "real path: " + realPath);
                InputStream input = new FileInputStream(realPath);

                byte[] buffer = new byte[1024];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    //bout.write(buffer, 0, len);
                    bout.write(buffer);
                }
                input.close();
                bout.close();
                Log.d(TAG, "Image sent over network and stream is closed.");
            }

            if (output.checkError()) {
                Log.d(TAG, "PRINTWRITER ENCOUNTERED AN ERROR");
                return;
            }

            //Toast.makeText(context, "i send poll from host: " + pollInfo[4], Toast.LENGTH_LONG).show();

            Log.d(TAG, "SENT POLL_REQUEST");

            /*Activity act = (Activity) context;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.useShortToast(context, messageFromClient);
                    }
                });

                //Toast.makeText(context, "Poll request is accepted!", Toast.LENGTH_LONG).show();
            }*/
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.d(TAG, e.toString());
            Activity act = (Activity) context;
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastHelper.useLongToast(context, "Can't connect to other devices. Check your connection.");
                }
            });
        } finally {
            if(output != null)
            output.close();
        }
    }

    public void sendAccept(Socket socket){
        PrintWriter output = null;

        try {
            output = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);
            output.println(Consts.ACCEPT);
            output.println(pollInfo.get(0)); //poll_id
            output.println(pollInfo.get(1)); //poll_hostAddress
            //System.out.println("hostIpaddress: " + hostIpAddress + " address in pollInfo: " + pollInfo.get(1));

            if (output.checkError())
                Log.d(TAG, "PRINTWRITER ENCOUNTERED AN ERROR");

            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final String res = input.readLine();

            Log.d(TAG, "SENT ACCEPT MSG TO: " + pollInfo.get(1));

            if(res.equals(Consts.RECEIVED))
                Log.d(TAG, "DEVICE " + pollInfo.get(1) + " RECEIVED MY ACCEPT: ");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendVote(Socket socket){
        PrintWriter output = null;

        try {
            output = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);

            output.println(Consts.POLL_VOTE);
            output.println(pollInfo.get(0)); //id
            output.println(pollInfo.get(1)); //vote
            //output.println(pollInfo.get(2)); //own hostAddress
            //output.println(socket.getInetAddress().getHostAddress()); //device address

            if (output.checkError())
                Log.d(TAG, "PRINTWRITER ENCOUNTERED AN ERROR");

            Log.d(TAG, "SENT VOTE");

            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final String messageFromClient = input.readLine();

            if (messageFromClient.equals(Consts.RECEIVED)) {
                Log.d(TAG, "MY VOTE IS RECEIVED: ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            output.close();
        }

    }

    public void sendResult(Socket socket){
        PrintWriter output = null;

        try {
            output = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);
            output.println(Consts.RESULT);
            output.println(pollID);
            output.println(result.length);
            for (int i = 0; i < result.length; i++) {
                output.println(result[i]);
            }

            if (output.checkError())
                Log.d(TAG, "PRINTWRITER ENCOUNTERED AN ERROR");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            output.close();
        }

    }

    public void sendSimpleMessageToOtherDevice(String message) {
        try {
            socket = getSocket(hostIpAddress);

            PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            output.println(message);

            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String messageFromClient = input.readLine();

            if(messageFromClient.equals(Consts.ACCEPT))
                Log.d(TAG, "arrived message: ACCEPT");
                Toast.makeText(context, "Poll request is accepted!",  Toast.LENGTH_LONG).show();

            output.close();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();

        } finally {
            closeSocket(socket);
        }
    }




}
