package rjdgtn.csms;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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
    public static void send(Context context, HashMap<String, String> add) {
        Intent intent = new Intent(context, WorkerService.class);
        for ( Map.Entry<String, String> entry : add.entrySet() ) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        context.startService(intent);
    }

    private void log(String str) {
        Log.d("MY WRKR", str);
        Intent intent = new Intent("csms_log");
        intent.putExtra("log", str);
        intent.putExtra("ch", "WRKR");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public static final long launchTime = System.currentTimeMillis();

    public static AtomicBoolean breakExec;

    private CheckerTask checkerTask = null;
    private TransportTask transportTask = null;
    private ProcessorTask processorTask = null;
    private Thread checkerThread = null;
    private Thread transportThread = null;
    private Thread processorThread = null;

    private Timer timer = null;

    public WorkerService() {

    }

    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(new TryMe());
        this.registerReceiver(logReceiver, new IntentFilter("csms_log"));

        timer = new Timer();
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.MINUTE, 30);
        int nextReboot = 2 * (calendar.get(calendar.HOUR_OF_DAY)/2 + 1);
        calendar.set(calendar.HOUR_OF_DAY, nextReboot);
        long delay = calendar.getTime().getTime() - System.currentTimeMillis();
        timer.schedule(new WorkerService.ShutdownTask(getApplicationContext()), delay);

        log("create");
        super.onCreate();

        breakExec = new AtomicBoolean(false);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("CSMS")
                .setContentText("Worker active");
        Notification notification = builder.getNotification();
        startForeground(999, notification);

        checkerTask = new CheckerTask(getApplicationContext());
        transportTask = new TransportTask(getApplicationContext());
        processorTask = new ProcessorTask(getApplicationContext());

        checkerThread = new Thread(checkerTask);
        checkerThread.start();

        transportThread = new Thread(transportTask);
        transportThread.start();

        processorThread = new Thread(processorTask);
        processorThread.start();

        MyPhoneStateListener.init(getApplicationContext());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {
            try {
                ProcessorTask.localCommands.put(intent.getExtras());
            } catch (Exception e) {
                log("crash");
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
        }
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        log("destroy");
        unregisterReceiver(logReceiver);
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
            StringWriter errors = new StringWriter();
            throwable.printStackTrace(new PrintWriter(errors));
            log("uncaughtException :\n" + throwable.getMessage() + "\n" + errors.toString());
            Log.e("CRASH", throwable.getMessage());
            Log.e("CRASH", errors.toString());
            System.exit(0);
        }
    }

    class ShutdownTask extends TimerTask {
        Context context;
        ShutdownTask(Context c) {
            context = c;
        }

        @Override
        public void run() {
            stop(context);
//            log("timeout shutdown");
//            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());
        }
    }


    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String log = intent.getStringExtra("log");
            String channel = intent.getStringExtra("ch");
            String dateStr = new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()));

            try {
                File file = new File(Environment.getExternalStorageDirectory()+ "/CSMS/log.txt");
                if (!file.exists()) {
                    File directory = new File(Environment.getExternalStorageDirectory() + "/CSMS");
                    directory.mkdirs();
                    file.createNewFile();
                }

                FileWriter out = new FileWriter(file, true);
                out.write(dateStr + " " + channel + ": " + log + "\n");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

}
