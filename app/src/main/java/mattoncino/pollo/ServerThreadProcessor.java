package mattoncino.pollo;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Act as the server side thread to accept and serve
 * connection requests from other devices.
 */
public class ServerThreadProcessor extends Thread{
    public static final int SERVER_PORT = 8700;
    private static String TAG = "ServerThreadProcessor";
    private ServerSocket serverSocket = null;
    private static boolean serviceUp = true;
    private Context context;


    /**
     * Constructor
     * @param context Activity's context
     */
    public ServerThreadProcessor(Context context) {
        this.context = context;
        this.serviceUp = true;
    }

    /**
     * Establish the connection and launches a ClientHandler to
     * handle the data exchange between devices.
     *
     * @see ClientHandler
     */
    public void run() {
        Log.i(TAG, "ServerThreadProcessor is launched...");

        while(serviceUp) {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);

                while (!Thread.currentThread().isInterrupted()) {

                    socket = serverSocket.accept();
                    Log.d(TAG, "A new socket connection ACCEPTED!");
                    Thread tClient = new Thread(new ClientHandler(socket, context));
                    tClient.start();
                }

            } catch (SocketException e) {
                Log.wtf(TAG, e.toString());
            } catch (IOException e) {
                Log.wtf(TAG, e.toString());
            }
        }

        Log.i(TAG, "ServerThreadProcessor is terminated successfully...");

    }

    /** Terminates the thread */
    public void terminate() {
        try {
            serviceUp = false;
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            Log.wtf(TAG, e.toString());
        }

        Log.i(TAG, "ServerThreadProcessor is getting terminated...");
    }

    /** Returns true if thread is running, false otherwise */
    public boolean serviceUp(){
        return serviceUp;
    }

}
