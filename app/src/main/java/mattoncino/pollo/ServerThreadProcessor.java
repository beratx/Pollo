package mattoncino.pollo;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public class ServerThreadProcessor extends Thread{
    public static final int SERVER_PORT = 8700;
    private static String TAG = "ServerThreadProcessor";
    private ServerSocket serverSocket = null;
    private static boolean serviceUp = true;
    private Context context;


    public ServerThreadProcessor(Context context) {
        this.context = context;
        this.serviceUp = true;
    }

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

    public boolean serviceUp(){
        return serviceUp;
    }

}
