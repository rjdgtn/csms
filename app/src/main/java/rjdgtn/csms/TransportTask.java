package rjdgtn.csms;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.BlockingQueue;

import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */

public class TransportTask  implements Runnable {
    private BlockingQueue<Packet> in;
    private BlockingQueue<Packet> out;

    public TransportTask() {
        Log.d("CSMS", "TRANSPORT create");
    }


    public void run() {
        Log.d("CSMS", "do transport");
        try {
            while (true) {
                Thread.sleep(1000);

                Log.d("CSMS", "transport");

            }

        } catch (InterruptedException e) {

        }
    }
}
