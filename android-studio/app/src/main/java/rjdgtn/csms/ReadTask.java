package rjdgtn.csms;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.min;

/**
 * Created by Petr on 25.03.2018.
 */

public class ReadTask implements Runnable {

    char prevSymbol = '\0';
    char prevMeanSymbol = '\0';
    public BlockingQueue<Character> inQueue;;

    public native char decode(short[] data);
    Context contex;

    public ReadTask(Context contex) {
        this.contex = contex;
        inQueue = new LinkedBlockingQueue<Character>();

        log("create");
    }

    private void log(String str) {
        Log.d("MY READ", str);
        Intent intent = new Intent("csms_log");
        intent.putExtra("log", str);
        intent.putExtra("ch", "READ");
        intent.putExtra("tm", System.currentTimeMillis());
        contex.sendBroadcast(intent);
    }

    public static AtomicInteger bufferDuration = new AtomicInteger(350);

//    public static int SHORT_little_endian_TO_big_endian(int i){
//        return (((i>>8)&0xff)+((i << 8)&0xff00));
//    }
    public void run() {
        log("run");
        PowerManager.WakeLock wakeLock = null;
        boolean led = false;
        try {
            PowerManager powerManager = (PowerManager) contex.getSystemService(contex.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "READ_WAKE_LOCK");

            while (true) {
                wakeLock.acquire();
                readLoop();

                Calendar calendar = Calendar.getInstance();
                int curMin = calendar.get(calendar.MINUTE);
                int nextTenMinutes = (int)Math.ceil((curMin+0.1)/10.0)*10;
                calendar.set(calendar.MINUTE, nextTenMinutes);
                calendar.set(calendar.SECOND, 0);
                calendar.set(calendar.MILLISECOND, 0);
                long wakeTime = calendar.getTime().getTime();

                log("next wake at " + new SimpleDateFormat("MM-dd HH:mm:ss").format(calendar.getTime()));

                WorkerService.performWake(contex, wakeTime);

                wakeLock.release();

                while (WorkerService.idleMode.get() && wakeTime > System.currentTimeMillis()) {
                    led = !led;
                    if (led) Runtime.getRuntime().exec(new String[] { "su", "-c", "echo 5 > /sys/class/leds/led_b/brightness" });
                    else Runtime.getRuntime().exec(new String[] { "su", "-c", "echo 0 > /sys/class/leds/led_b/brightness" });

                    long sleepDuration = Math.min(1000, wakeTime - System.currentTimeMillis());
                    log("sleep for " + sleepDuration);
                    Thread.sleep(sleepDuration);
                }
            }
        } catch (Exception e) {
            log("crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        } finally {
            wakeLock.release();
        }

        log("finish");
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());
    }

    void readLoop() throws Exception {
        int frequency = 8000;
        int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;//CHANNEL_CONFIGURATION_MONO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

        int bufferBytes = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        int bufferShorts = bufferBytes / 2;
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferBytes * 5);

        short[] buffer = new short[bufferShorts * 2];
        audioRecord.startRecording();

        int dupCounter = 0;
        int wpos = 0;
        short[] decodeBuffer = null;
        String pattern = "XxXxXx";

        log("loop");
        long startLoopTime = System.currentTimeMillis();
        while (true) {
            int rpos = 0;
            int rsz = audioRecord.read(buffer, 0, buffer.length);

            if (rsz > 0) {
                int bufsz = bufferDuration.get() * 8;
                if (decodeBuffer == null || decodeBuffer.length != bufsz) {
                    decodeBuffer = new short[bufsz];
                    wpos = 0;
                }

                while (true) {
                    int sz = min(bufsz - wpos, rsz);
                    if (sz > 0) {
                        System.arraycopy(buffer, rpos, decodeBuffer, wpos, sz);
                        rpos += sz;
                        wpos += sz;
                        rsz -= sz;
                    }

                    if (wpos < bufsz) {
                        break;
                    }
                    wpos = 0;
                    char symbol = decode(decodeBuffer);
                    //if (symbol != '\0') Log.v("MY READ", "detect "+symbol);

                    if (prevSymbol != symbol) {
                        if (prevSymbol != '\0' && prevSymbol != prevMeanSymbol) {
                            log("put " + prevSymbol + " dup " + dupCounter);
                            inQueue.put(new Character(prevSymbol));
                            prevMeanSymbol = prevSymbol;

                            pattern = pattern.substring(1) + prevSymbol;
                            if (pattern.equals(TransportTask.RESTART_PATTERN)) {
                                log("detect pattern " + pattern);
                                throw new Exception();
                            }
                        }
                        dupCounter = 1;
                        prevSymbol = symbol;
                    } else {
                        dupCounter++;
                    }
                }

            }

            if (WorkerService.idleMode.get() && System.currentTimeMillis() - startLoopTime > 10 * 1000) {
                break;
            }
        }

        audioRecord.stop();
        log("stop");
    }
}
