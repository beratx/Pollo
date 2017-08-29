package mattoncino.pollo;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
            output = new PrintWriter(new BufferedWriter(
                                        new OutputStreamWriter(socket.getOutputStream())), true);

            output.println(Consts.POLL_REQUEST);
            output.println(pollInfo.get(0)); //poll_id
            output.println(pollInfo.get(1)); //poll_name
            output.println(pollInfo.get(2)); //poll_question
            output.println(pollInfo.get(3)); //device address
            output.println(pollInfo.size()-4); //number of options
            for (int i = 4; i < pollInfo.size(); i++) {
                output.println(pollInfo.get(i));
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
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                sendPollRequest(socket);
            }
            sendPollRequest(socket);
        } finally {
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
