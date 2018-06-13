package rjdgtn.csms;

import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

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

                Thread.sleep(10 * 1000);
            }
        } catch (Exception e) {
            log("crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

        log("finish");
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());
    }
}
