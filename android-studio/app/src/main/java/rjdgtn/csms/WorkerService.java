package rjdgtn.csms;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

import static android.os.BatteryManager.BATTERY_PLUGGED_AC;
import static android.os.BatteryManager.BATTERY_PLUGGED_USB;

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
        intent.setAction("start_service");
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, WorkerService.class);
        intent.setAction("stop_service");
        context.stopService(intent);
    }

    public static void send(Context context, HashMap<String, String> add) {
        Intent intent = new Intent(context, WorkerService.class);
        intent.setAction("command");
        for (Map.Entry<String, String> entry : add.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        context.startService(intent);
    }

    public static void performWake(Context context, long timestamp) {
        Intent intent = new Intent(context, WorkerService.class);
        intent.setAction("wake " + (++wakeCounter));
        PendingIntent pIntent1 = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, timestamp, pIntent1);

        log(context, "perform wake at " + new SimpleDateFormat("MM-dd HH:mm:ss").format(timestamp));
    }

    private  void log(String str) {
        log(this, str);
    }
    private static  void log(Context context, String str) {
        Log.d("MY WRKR", str);
        Intent intent = new Intent("csms_log");
        intent.putExtra("log", str);
        intent.putExtra("ch", "WRKR");
        intent.putExtra("tm", System.currentTimeMillis());
        context.sendBroadcast(intent);
    }

    public static final long launchTime = System.currentTimeMillis();

    public static AtomicBoolean idleMode = new AtomicBoolean(false);

    private CheckerTask checkerTask = null;
    private TransportTask transportTask = null;
    private ProcessorTask processorTask = null;
    private Thread checkerThread = null;
    private Thread transportThread = null;
    private Thread processorThread = null;

    private static int wakeCounter = 0;
    private static int airplaneCounter = 0;

    public WorkerService() {

    }

    @Override
    public void onCreate() {
        logBattary();
        Thread.setDefaultUncaughtExceptionHandler(new TryMe());
        this.registerReceiver(logReceiver, new IntentFilter("csms_log"));

        IntentFilter chargeFilter = new IntentFilter();
        chargeFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        chargeFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        this.registerReceiver(chargeReceiver, chargeFilter);

        AirplaneMode.setFlightMode(this, true);




        {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("battery");
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);

            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+1000,60 * 60 * 1000, pIntent1);
        }

//        {
//            Intent intent = new Intent(this, WorkerService.class);
//            intent.setAction("reboot");
//            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);
//
//            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
//            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+1000 * 60, pIntent1);
//        }

        {
            //timer = new Timer();
            Calendar calendar = Calendar.getInstance();
            calendar.set(calendar.MINUTE, 35);
            calendar.set(calendar.SECOND, 0);
            calendar.set(calendar.MILLISECOND, 0);
            int nextReboot = 2 * (calendar.get(calendar.HOUR_OF_DAY) / 4 + 1);
            calendar.set(calendar.HOUR_OF_DAY, nextReboot);
            log("perform restart at " + new SimpleDateFormat("MM-dd HH:mm:ss").format(calendar.getTime()));

            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("stop");
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);

            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, calendar.getTime().getTime(), pIntent1);

        }

        {
            int[] hours = {9, 12, 16, 19, 23};
            Calendar calendar = Calendar.getInstance();
            int curHour = calendar.get(Calendar.HOUR_OF_DAY);
            int curDay = calendar.get(Calendar.DAY_OF_YEAR);
            calendar.set(Calendar.MINUTE, 30);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            for (int h : hours) {
                Intent intent = new Intent(this, WorkerService.class);
                intent.setAction("airplane " + (++airplaneCounter));
                PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);

                calendar.set(Calendar.DAY_OF_YEAR, curDay);
                calendar.set(Calendar.HOUR_OF_DAY, h);
                if ( calendar.getTime().getTime() < System.currentTimeMillis()) calendar.set(Calendar.DAY_OF_YEAR, curDay+1);
                log("perform airplane disable at " + new SimpleDateFormat("MM-dd HH:mm:ss").format(calendar.getTime()));

                am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTime().getTime(), 24 * 60 * 60 * 1000, pIntent1);
            }
        }

        log("create");
        super.onCreate();


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
        if (intent != null && intent.getExtras() != null && intent.getAction() != null) {
            try {
                log("command " + intent.getAction());
                if (intent.getAction().contains("wake")) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (intent.getAction().equals("battery")) {
                    logBattary();
                } else if (intent.getAction().equals("stop")) {
                    stop(this);
                } else if (intent.getAction().contains("airplane")) {
                    Bundle b = new Bundle();
                    b.putString("code", "check_sms_local");
                    ProcessorTask.localCommands.put(b);
                } else if (intent.getAction().contains("reboot")) {
                    try {
                        Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
                        proc.waitFor();
                    } catch (Exception e) {

                    }
                } else if (intent.getAction().equals("command") && intent.getExtras().getString("code") != null) {
                    ProcessorTask.localCommands.put(intent.getExtras());
                }
            } catch (Exception e) {
                log("crash");
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
        }
        super.onStartCommand(intent, flags, startId);

        return START_NOT_STICKY;
    }

    protected void unperform() {
        log("unperform");
        unregisterReceiver(logReceiver);
        unregisterReceiver(chargeReceiver);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

        for (int i = 0; i <= wakeCounter; i++) {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("wake " + i);
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);
            am.cancel(pIntent1);
        }

        {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("battery");
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);
            am.cancel(pIntent1);
        }

        {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("stop");
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);
            am.cancel(pIntent1);
        }

        {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("command");
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);
            am.cancel(pIntent1);
        }

        {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("reboot");
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);
            am.cancel(pIntent1);
        }

        for (int i = 0; i <= airplaneCounter; i++) {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("airplane " + (i));
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);
            am.cancel(pIntent1);
        }
    }

    @Override
    public void onDestroy() {
        unperform();
        log("destroy");

        System.exit(0);
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
            unperform();
            System.exit(0);
        }
    }

//    class ShutdownTask extends TimerTask {
//        Context context;
//        ShutdownTask(Context c) {
//            context = c;
//        }
//
//        @Override
//        public void run() {
//            stop(context);
////            log("timeout shutdown");
////            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());
//        }
//    }


    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent

//            StringBuilder wakeLocks = new StringBuilder();
//            try {
//                // Run the command
//                Process process = Runtime.getRuntime().exec(new String[] { "su", "-c", "dumpsys power | grep WAKE_LOCK" });
//                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//
//                // Grab the results
//                String line;
//                while ((line = bufferedReader.readLine()) != null) {
//                    wakeLocks.append(line.split("[']+")[1] + "\n");
//                }
//
//            } catch (IOException e) { }

            String log = intent.getStringExtra("log");
            String channel = intent.getStringExtra("ch");
            long timestamp = intent.getLongExtra("tm", 0);
            if (timestamp == 0) timestamp = System.currentTimeMillis();
            String dateStr = new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date(timestamp));

            try {
                File file = new File(Environment.getExternalStorageDirectory() + "/CSMS/log.txt");
                if (!file.exists()) {
                    File directory = new File(Environment.getExternalStorageDirectory() + "/CSMS");
                    directory.mkdirs();
                    file.createNewFile();
                }

                FileWriter out = new FileWriter(file, true);
//                out.write(wakeLocks.toString());

                out.write(dateStr + " " + channel + ": " + log + "\n");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private BroadcastReceiver chargeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            logBattary();
        }
    };

    void logBattary() {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        boolean screen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
                ? powerManager.isInteractive()
                : powerManager.isScreenOn();

        log("battery:" + batteryPct + (isCharging ? " charging" : "") + (screen ? " screen" : "" ));
    }


}
