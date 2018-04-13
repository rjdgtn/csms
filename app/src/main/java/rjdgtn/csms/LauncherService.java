package rjdgtn.csms;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class LauncherService extends Service {
    private Timer timer;

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

        timer = new Timer();
        timer.schedule(new CheckWorkerTask(getApplicationContext()), 1000, 10000);
    }


    public void onDestroy() {
        timer.cancel();
        WorkerService.stop(getApplicationContext());
        super.onDestroy();
        Log.d("MY launcher", "destroy");
    }

    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    class CheckWorkerTask extends TimerTask {
        Context context;
        CheckWorkerTask(Context c) {
            context = c;
        }

        @Override
        public void run() {
           // Log.v("MY CSMS", "check worker");
            //if (!WorkerService.isRunning(context)) {
                Log.v("MY LCHR", "start worker");
                WorkerService.start(context);
            //}
        }
    }
}


