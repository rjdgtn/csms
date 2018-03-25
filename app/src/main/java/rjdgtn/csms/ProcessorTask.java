package rjdgtn.csms;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.BlockingQueue;

import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */

public class ProcessorTask implements Runnable {
    private BlockingQueue<Packet> in;
    private BlockingQueue<Packet> out;

    public ProcessorTask() {
        Log.d("CSMS", "PROCESSOR create");
    }


    @Override
    public void run() {
        Log.d("CSMS", "do process");
        try {
            while (true) {
                Thread.sleep(1000);

                Log.d("CSMS", "process");

            }

        } catch (InterruptedException e) {

        }

    }
}
