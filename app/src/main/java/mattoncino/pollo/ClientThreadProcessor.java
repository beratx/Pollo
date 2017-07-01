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
    private String serverIpAddress;
    private static final String TAG = "ClientThreadProcess";
    private List messages;
    private String type;
    private int vote;

    public ClientThreadProcessor(String serverIpAddress, Context context, String type, ArrayList<String> messages) {
        this.context = context;
        this.serverIpAddress = serverIpAddress;
        this.messages = messages;
        this.type = type;
    }

    /*public ClientThreadProcessor(String serverIpAddress, Context context, String type, int vote) {
        this.context = context;
        this.serverIpAddress = serverIpAddress;
        this.type = type;
        this.vote = vote;
    }*/

    private Socket getSocket(String serverIpAddress) {
        try {
            InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
            socket = new Socket(serverAddr, ServerSocketHandler.SERVER_PORT);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
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
            socket = getSocket(serverIpAddress);

            if(type.equals(Consts.POLL_REQUEST))
                sendPollRequest(socket);
            else if(type.equals(Consts.ACCEPT))
                sendAccept(socket);
            else if(type.equals(Consts.POLL_VOTE))
                sendVote(socket, vote);

                //updateVoterList(messages[0], messages[1]);

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
            output.println(messages.get(0)); //poll_name
            output.println(messages.get(1)); //poll_question
            output.println(messages.get(2)); //device address
            output.println(messages.size()-3); //number of options
            for (int i = 3; i < messages.size(); i++) {
                output.println(messages.get(i));
            }

            if (output.checkError()) {
                Log.d(TAG, "PRINTWRITER ENCOUNTERED AN ERROR");
                return;
            }

            //Toast.makeText(context, "i send poll from host: " + messages[4], Toast.LENGTH_LONG).show();

            Log.d(TAG, "SENT POLL_REQUEST");

            /*BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final String messageFromClient = input.readLine();

            if (messageFromClient.equals(Consts.ACCEPT)) {
                //qui devo aggiungere il client alla lista
                //Log.d(TAG, "arrived message: " + messageFromClient);
                Activity act = (Activity) context;
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
            output.println(messages.get(0)); //poll_name
            output.println(messages.get(1)); //poll_hostAddress
            //output.println(vote);
            //output.println(socket.getInetAddress().getHostAddress()); //device address

            if (output.checkError())
                Log.d(TAG, "PRINTWRITER ENCOUNTERED AN ERROR");

            Log.d(TAG, "SENT ACCEPT MSG TO: " + messages.get(1));

            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final String res = input.readLine();

            if(res.equals(Consts.RECEIVED))
                Log.d(TAG, "DEVICE RECEIVED MY ACCEPT: " + messages.get(1));



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendVote(Socket socket, int vote){
        PrintWriter output = null;

        try {
            output = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);

            output.println(Consts.POLL_VOTE);
            output.println(messages.get(0)); //name
            output.println(messages.get(1)); //vote
            output.println(messages.get(2)); //hostAddress
            //output.println(socket.getInetAddress().getHostAddress()); //device address

            if (output.checkError())
                Log.d(TAG, "PRINTWRITER ENCOUNTERED AN ERROR");


            Log.d(TAG, "SENT VOTE");

            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final String messageFromClient = input.readLine();

            if (messageFromClient.equals(Consts.RECEIVED)) {
                Log.d(TAG, "MY VOTE IS RECEIVED: ");
                //qui devo aggiungere il client alla lista
                //Log.d(TAG, "vote is received");
                /*Activity act = (Activity) context;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.useShortToast(context, "vote is received" );
                    }
                });*/
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            output.close();
        }

    }

    public void updateVoterList(String pollName, String hostAddress){
        Toast.makeText(context, "Gonna update voter list for " + pollName,  Toast.LENGTH_LONG).show();
    }


    public void sendSimpleMessageToOtherDevice(String message) {
        try {
            socket = getSocket(serverIpAddress);

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
