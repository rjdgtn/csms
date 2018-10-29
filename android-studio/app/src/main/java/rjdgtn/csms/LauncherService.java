package rjdgtn.csms;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class LauncherService extends Service {
    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LauncherService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldAutoStart(Context context) {
        String[] lst = context.getFilesDir().list();
        return Arrays.asList(lst).contains("autostart");
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, LauncherService.class);
        intent.setAction("start_service");
        context.startService(intent);

        try{
            File file = new File(context.getFilesDir(), "autostart");
            file.createNewFile();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void stop(Context context) {
        Intent intent = new Intent(context, LauncherService.class);
        intent.setAction("stop_service");
        context.stopService(intent);

        File file = new File(context.getFilesDir(), "autostart");
        file.delete();
    }

    public LauncherService() {

    }

    public void onCreate() {
        Log.d("MY launcher", "create");
        super.onCreate();

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("CSMS")
                .setContentText("Launcher active");
        Notification notification = builder.build();
        startForeground(777, notification);


        Intent intent = new Intent(this, WorkerService.class);
        intent.setAction("start from launcher");
        PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, 5 * 60 * 1000, pIntent1);


        WorkerService.start(getApplicationContext());

    }


    public void onDestroy() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("ring");
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);
            am.cancel(pIntent1);
        }
        {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("ringup_stop");
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);
            am.cancel(pIntent1);
        }
        {
            Intent intent = new Intent(this, WorkerService.class);
            intent.setAction("start");
            PendingIntent pIntent1 = PendingIntent.getService(this, 0, intent, 0);

            am.cancel(pIntent1);

        }
        WorkerService.stop(getApplicationContext());
        super.onDestroy();
        Log.d("MY launcher", "destroy");
    }

    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}


