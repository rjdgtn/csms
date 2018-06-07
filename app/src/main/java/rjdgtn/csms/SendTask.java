package rjdgtn.csms;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Petr on 25.03.2018.
 */

public class SendTask implements Runnable {
    public LinkedBlockingQueue<String> outQueue;
    Context context = null;

    public SendTask(Context context) {
        this.context = context;
        outQueue = new LinkedBlockingQueue<String>();

        Log.d("MY SEND", "create");
    }


    public static AtomicInteger callDuration = new AtomicInteger(60);
    public static AtomicInteger spaceDuration = new AtomicInteger(50);
    public static AtomicBoolean idleMode = new AtomicBoolean(false);

    public native void encodeInit(int frameSize, int callDur, int spaceDur);
    public native void encode(String str);
    public native boolean encodeStep(short[] buf);

    //JNIEXPORT void JNICALL Java_rjdgtn_csms_SendTask_encodeInit(JNIEnv *env, jobject, jint frameSize, jint callDur, jint spaceDur) {
    //JNIEXPORT void JNICALL Java_rjdgtn_csms_SendTask_encode(JNIEnv *env, jobject, jbyteArray jdata) {
    //JNIEXPORT jboolean JNICALL Java_rjdgtn_csms_SendTask_encodeStep(JNIEnv *env, jobject, jshortArray jdata) {

    private void log(String str) {
        Log.d("MY SEND", str);
        Intent intent = new Intent("csms_log");
        intent.putExtra("log", str);
        intent.putExtra("ch", "SEND");
        context.sendBroadcast(intent);
    }

    public void run() {
        log("run");
        boolean emulator = Build.FINGERPRINT.startsWith("generic");
        AudioTrack audio = null;
        try {
            int frequency = 8000;
            int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;//CHANNEL_CONFIGURATION_MONO;
            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

            int bufsize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

            if (!emulator) {
                audio = new AudioTrack(AudioManager.STREAM_MUSIC,
                        frequency,
                        channelConfiguration, //2 channel
                        audioEncoding, // 16-bit
                        bufsize * 5,
                        AudioTrack.MODE_STREAM);

                audio.play();
            } else {
                log("!!!!!!fake send");
            }
            log("loop");
            while (true) {
                if (outQueue.isEmpty()) {
                    Thread.sleep(idleMode.get() ? 100 : 2500);
                    continue;
                }
                String in = outQueue.element();
                if (!in.isEmpty()) {
                    if (in.contains("sleep ")) {
                        int duration = Integer.parseInt(in.substring(6));
                        log("sleep "+duration);
                        Thread.sleep(duration);
                    } else if (in.contains("callDuration ")) {
                        int duration = Integer.parseInt(in.substring(13));
                        callDuration.set(duration);
                        log( "callDuration "+ duration);
                    } else if (in.contains("spaceDuration ")) {
                        int duration = Integer.parseInt(in.substring(14));
                        spaceDuration.set(duration);
                        log("spaceDuration "+ duration);
                    } else {
//                        String[] inSplit =in.split("(?<=\\G.{10})");
//                        for (String str : inSplit) {
                        for (int i = 0; i < in.length(); i += 10) {
                            String str = in.substring(i, Math.min(i + 10, in.length()));

                            short[] buffer = new short[25];

                            encodeInit(buffer.length, callDuration.get(), spaceDuration.get());
                            encode(str);
                            log("start " + str);
                            while (true) {
                                boolean finished = encodeStep(buffer);
                                int duration = buffer.length / (frequency / 1000);
                                new VolumeCheckerTask(context, (int)(duration * 1.2));
                                if (audio != null) audio.write(buffer, 0, buffer.length);
                                Thread.sleep(duration);
                                if (finished) {
                                    break;
                                }
                            }
                        }

                        int dur = 1000;
                        byte[] pause = new byte[dur * 8];
                        audio.write(pause, 0, pause.length);
                        log("finish ");
                        Thread.sleep(dur);
                    }
                }
                outQueue.take();
            }

        } catch (Exception e) {
            log("crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

        log("finish");
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());

//        } catch (Exception e) {
//            Log.d("MY CSMS", "send crash");
//            if (audio != null) {
//                audio.stop();
//                audio.release();
//                audio = null;
//            }
//        } finally {
//            WorkerService.breakExec.set(true);
//        }
    }

    public class VolumeCheckerTask implements Runnable {
        public int targetVolume = 0;
        AudioManager am = null;
        long workUntil = 0;
        VolumeCheckerTask(Context context, long duration) {
            am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            targetVolume = (int)(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.7);

            workUntil = System.currentTimeMillis() + duration;

            checkVolume();
        }

        public void run() {
            checkVolume();
            while (System.currentTimeMillis() <= workUntil) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                checkVolume();
            }
        }

        void checkVolume() {
            if (targetVolume != am.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume ,0);
            }
        }
    }
}
