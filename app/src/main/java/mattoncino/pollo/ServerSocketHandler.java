package mattoncino.pollo;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;


public class ServerSocketHandler implements  Runnable {
    public static final int SERVER_PORT = 8700;
    private static String TAG = "SERVER_SOCKET";
    private static final String POLL_REQUEST = "poll_request";
    private static final String ACCEPT = "accept";
    private ServerSocket serverSocket;
    private Context context;
    private BufferedReader inputBufferedReader;
    private PrintWriter outputPrintWriter;
    private Set<String> acceptedAddressList;



    public ServerSocketHandler(ServerSocket serverSocket, Context context) {
        this.serverSocket = serverSocket;
        this.context = context;
        this.acceptedAddressList = new HashSet<String>();
    }

    public void run() {
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            while (!Thread.currentThread().isInterrupted()) {

                //Log.v(TAG, "before socket ACCEPT");
                socket = serverSocket.accept();
                Log.v(TAG, "socket ACCEPTED");

                //ClientHandler chandler = new ClientHandler(socket, ....)
                //chandler.start()

                /*InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                inputBufferedReader = new BufferedReader(inputStreamReader);*/


                inputBufferedReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                //Log.v(TAG, "GOT INPUT STREAM");

                outputPrintWriter = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);

                //Log.v(TAG, "GOT OUTPUT STREAM");

                //processInputInputOutputBuffers();
                //process here input & output
                final String inputLine = inputBufferedReader.readLine(); //arrived messaged
                if (inputLine.equals(POLL_REQUEST)){
                    //socket.getInetAddress();
                    //Toast.makeText(context, inputLine, Toast.LENGTH_LONG).show();
                    outputPrintWriter.println(ACCEPT);
                    Log.v(TAG, "SENT ACCEPT MESSAGE");
                    Activity act = (Activity) context;
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastHelper.useShortToast(context, inputLine);
                        }
                    });

                }
                //outputPrintWriter.println("YOU TEXT ARRIVED. THANKS"); //send message*/
            }

            inputBufferedReader.close();
            outputPrintWriter.close();

            Log.v(TAG, "BUFFERS CLOSED");


        } catch (Exception ex) {
            Log.v(TAG, "server socket processor thread EXCEPTION : " + ex.toString());

        } catch (Error error){
            Log.v(TAG, "server socket processor thread ERROR : " + error.toString());
        }

    }
}
