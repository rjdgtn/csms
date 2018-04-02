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
import java.util.concurrent.atomic.AtomicBoolean;

public class WorkerService extends Service {

    static {
        System.loadLibrary("native-lib");
    }

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

    public static AtomicBoolean breakExec;

    private TransportTask transportTask = null;
    private ProcessorTask processorTask = null;
    private Thread transportThread = null;
    private Thread processorThread = null;

    public WorkerService() {

    }

    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(new TryMe());


        Log.d("CSMS", "WORKER create");
        super.onCreate();

        breakExec = new AtomicBoolean(false);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("CSMS")
                .setContentText("Worker active");
        Notification notification = builder.build();
        startForeground(999, notification);

        transportTask = new TransportTask(getApplicationContext());
        processorTask = new ProcessorTask(transportTask.inQueue, transportTask.outQueue);

        transportThread = new Thread(transportTask);
        transportThread.start();

        processorThread = new Thread(processorTask);
        processorThread.start();
    }

    @Override
    public void onDestroy() {
        Log.d("CSMS", "WORKER destroy");
        System.exit(0);
//        processorThread.interrupt();
//        transportThread.interrupt();
//        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public class TryMe implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            Log.d("CSMS", "uncaughtException");
            System.exit(0);
        }
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
