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
    private ServerSocket serverSocket = null;
    private Context context;
    private boolean serviceUp = true;
    //private Thread serverProcessorThread;

    public ServerThreadProcessor(Context context) {
        this.context = context;
    }

    public void run() {
        while(serviceUp) {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);

                while (!Thread.currentThread().isInterrupted()) {

                    socket = serverSocket.accept();
                    Log.v(TAG, "socket ACCEPTED");
                    Thread tClient = new Thread(new ClientHandler(socket, context));
                    tClient.start();
                }

            } catch (IOException e) {
                e.printStackTrace();

            } catch (Exception ex) {
                Log.v(TAG, "thread EXCEPTION : " + ex.toString());
            }
        }
    }

    public void terminate() {
        try {
            serviceUp = false;
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
