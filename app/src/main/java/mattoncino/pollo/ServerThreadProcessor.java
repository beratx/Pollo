package mattoncino.pollo;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by berat on 09.06.2017.
 */

public class ServerThreadProcessor extends Thread{
    public static final int SERVER_PORT = 8700;
    private static String TAG = "SERVER_THREAD_PROCESSOR";
    private static final String POLL_REQUEST = "poll_request";
    private static final String ACCEPT = "accept";
    private ServerSocket serverSocket = null;
    private Context context;
    //private Thread serverProcessorThread;

    public ServerThreadProcessor(Context context) {
        this.context = context;
        //this.serverSocket = serverSocket;
        //ServerSocketHandler serverSocketHandler = new ServerSocketHandler(serverSocket, context);
        //this.serverProcessorThread = new Thread(serverSocketHandler);
        //this.serverProcessorThread.start();
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

                socket = serverSocket.accept();
                Log.v(TAG, "socket ACCEPTED");
                Thread tClient = new Thread(new ClientHandler(socket, context));
                tClient.start();
                //Log.v(TAG, "CLIENT HANDLER THREAD LAUNCHED");
            }

        } catch (Exception ex) {
            Log.v(TAG, "thread EXCEPTION : " + ex.toString());

        }
    }

    public void stopServerProcessorThread() {

        try {
            // make sure you close the socket upon exiting
            if (serverSocket != null)
                serverSocket.close();

            //if (serverProcessorThread != null)
            //    serverProcessorThread.interrupt();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
