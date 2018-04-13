package rjdgtn.csms;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */

public class ProcessorTask implements Runnable {
//    public BlockingQueue<Request> outQueue;
//    public BlockingQueue<Request> outQueue;

    Context contex;

    public ProcessorTask(Context contex) {
        this.contex = contex;
        Log.d("MY PRCS", "create");
    }


    @Override
    public void run() {
        Log.d("MY PRCS", "start");
        try {
//            while (true) {
//                AirplaneMode.setFlightMode(contex, !AirplaneMode.isFlightModeEnabled(contex));
//                Thread.sleep(5000);
//
//                Log.d("MY CSMS", "process");
//            }
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }

    }

}


