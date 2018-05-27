package rjdgtn.csms;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

public class CheckerTask implements Runnable {
    Context context = null;

    public CheckerTask(Context context) {
        this.context = context;

        Log.d("MY CHCK", "create");
    }

    private void log(String str) {
        Log.d("MY CHCK", str);
        Intent intent = new Intent("csms_log");
        intent.putExtra("log", str);
        intent.putExtra("ch", "CHCK");
        context.sendBroadcast(intent);
    }

    public void run() {
        try{
            while(true) {

            }
        } catch (Exception e) {
            log("crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

        log("finish");
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());
    }
}
