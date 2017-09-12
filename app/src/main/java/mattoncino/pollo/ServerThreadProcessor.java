package mattoncino.pollo;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerThreadProcessor extends Thread{
    public static final int SERVER_PORT = 8700;
    private static String TAG = "SERVER_THREAD_PROCESSOR";
    private ServerSocket serverSocket = null;
    private static boolean serviceUp = true;
    private Context context;


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
                Log.d(TAG, e.toString());
            } catch (Exception ex) {
                Log.v(TAG, ex.toString());
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

    public boolean serviceUp(){
        return serviceUp;
    }

}
