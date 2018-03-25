package rjdgtn.csms;

import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Petr on 25.03.2018.
 */

public class SendTask implements Runnable {
    public BlockingQueue<String> outQueue;

    public SendTask() {
        outQueue = new LinkedBlockingQueue<String>();

        Log.d("CSMS", "SEND create");
    }

    boolean active = false;

    public void run() {
        try {
            while (true) {
                Thread.sleep(1000);

                Log.d("CSMS", "send");
            }

        } catch (InterruptedException e) {

        }
    }
}
