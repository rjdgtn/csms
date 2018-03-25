package rjdgtn.csms;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class WorkerService extends Service {
    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (WorkerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public static void start(Context context) {
        Intent intent = new Intent(context, WorkerService.class);
        context.startService(intent);
    }
    public static void stop(Context context) {
        Intent intent = new Intent(context, WorkerService.class);
        context.stopService(intent);
    }

    private TransportTask transportTask;
    private ProcessorTask processorTask;
    private Thread transportThread;
    private Thread processorThread;

    public WorkerService() {

    }

    @Override
    public void onCreate() {
        Log.d("CSMS", "WORKER create");
        super.onCreate();

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("CSMS")
                .setContentText("Worker active");
        Notification notification = builder.build();
        startForeground(999, notification);

        transportTask = new TransportTask();
        processorTask = new ProcessorTask();

        transportThread = new Thread(transportTask);
        transportThread.start();

        processorThread = new Thread(processorTask);
        processorThread.start();

    }

    @Override
    public void onDestroy() {
        Log.d("CSMS", "WORKER destroy");
        processorThread.interrupt();
        transportThread.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //private BlockingQueue<>

//    private enum COMMANDS {
//        UNKNOWN,
//        STATUS,
//        READ,
//        SEND,
//        RESTART
//    }

}
