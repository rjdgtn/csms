package rjdgtn.csms;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */

public class TransportTask  implements Runnable {

    private ReadTask readTask;
    private SendTask sendTask;
    private Thread readThread;
    private Thread sendThread;

    public BlockingQueue<Request> inQueue;
    public BlockingQueue<Request> outQueue;

    public TransportTask() {
        inQueue = new LinkedBlockingQueue<Request>();
        outQueue = new LinkedBlockingQueue<Request>();

        Log.d("CSMS", "TRANSPORT create");
    }

    boolean active = false;

    public void run() {
        Log.d("CSMS", "do transport");
        try {
            readTask = new ReadTask();
            sendTask = new SendTask();

            readThread = new Thread(readTask);
            //readThread.setDaemon(true);
            readThread.start();

            sendThread = new Thread(sendTask);
            //sendThread.setDaemon(true);
            sendThread.start();

            while (true) {
                Thread.sleep(1000);

                short[] inBytes = readTask.outQueue.take();


                Log.d("CSMS", "transport");

            }

        } catch (InterruptedException e) {

        } finally {
            sendThread.interrupt();
            readThread.interrupt();
        }
    }
}
