package mattoncino.pollo;

import android.app.Activity;
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


public class ClientThreadProcessor implements Runnable{
    private Socket socket;
    private Context context;
    private String serverIpAddress;
    private static final String POLL_REQUEST = "poll_request";
    private static final String ACCEPT = "accept";
    private static final String TAG = "ClientThreadProcess";
    private String[] messages;

    public ClientThreadProcessor(String serverIpAddress, Context context, String[] messages) {
        this.context = context;
        this.serverIpAddress = serverIpAddress;
        this.messages = messages;
    }

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

            PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            output.println(POLL_REQUEST);
            output.println(messages[0]); //poll_name
            output.println(messages[1]); //poll_question
            output.println(messages[2]); //poll_firstOpt
            output.println(messages[3]); //poll_secondOpt
            output.println(socket.getInetAddress().getHostAddress()); //device address

            if(output.checkError())
                Log.d(TAG, "PRINTWRITER ENCOUNTERED AN ERROR");

            Log.d(TAG, "sent POLL_REQUEST");

            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final String messageFromClient = input.readLine();

            if(messageFromClient.equals(ACCEPT)) {
                Log.d(TAG, "arrived message: " + messageFromClient);
                Activity act = (Activity) context;
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastHelper.useShortToast(context, messageFromClient);
                    }
                });

                //Toast.makeText(context, "Poll request is accepted!", Toast.LENGTH_LONG).show();
            }

            output.close();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();

        } finally {
            closeSocket(socket);
        }

    }


    public void sendSimpleMessageToOtherDevice(String message) {
        try {
            socket = getSocket(serverIpAddress);

            PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            output.println(message);

            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String messageFromClient = input.readLine();

            if(messageFromClient.equals(ACCEPT))
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
