package rjdgtn.csms;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static android.content.Context.POWER_SERVICE;

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
        intent.putExtra("tm", System.currentTimeMillis());
        context.sendBroadcast(intent);
    }

    public void run() {
        try{
            while(true) {

                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                am.setStreamVolume(AudioManager.STREAM_RING, 0 ,0);
                am.setStreamVolume(AudioManager.STREAM_DTMF, 0 ,0);
                //am.setStreamVolume(AudioManager.STREAM_ACCESSIBILITY, 0 ,0);
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0 ,0);
                am.setStreamVolume(AudioManager.STREAM_ALARM, 0 ,0);
                am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0 ,0);
                am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0 ,0);


                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.disable();
                }

                WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                if (wifiManager != null && wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                }

                if (isInteractive() && !isRunning()) {
                    Runtime.getRuntime().exec(new String[] { "su", "-c", "/system/bin/input keyevent 26" });
                }

                Thread.sleep( 60 * 1000);
            }
        } catch (Exception e) {
            log("crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

        log("finish");
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());
    }

    public boolean isRunning() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (context.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                return true;
        }
        return false;
    }

    public boolean isInteractive() {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
                ? powerManager.isInteractive()
                : powerManager.isScreenOn();
    }
}
