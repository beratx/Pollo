package mattoncino.pollo;

import android.content.Context;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by berat on 09.06.2017.
 */

public class ServerThreadProcessor {
    private ServerSocket serverSocket = null;
    private Context context;
    private Thread serverProcessorThread;

    public void startServerProcessorThread(Context context) {
        this.context = context;
        ServerSocketHandler serverSocketHandler = new ServerSocketHandler(serverSocket, context);
        this.serverProcessorThread = new Thread(serverSocketHandler);
        this.serverProcessorThread.start();
    }

    public void stopServerProcessorThread() {

        try {
            // make sure you close the socket upon exiting
            if (serverSocket != null)
                serverSocket.close();

            if (serverProcessorThread != null)
                serverProcessorThread.interrupt();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
