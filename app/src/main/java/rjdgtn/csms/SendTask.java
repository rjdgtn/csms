package rjdgtn.csms;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Petr on 25.03.2018.
 */

public class SendTask implements Runnable {
    public LinkedBlockingQueue<String> outQueue;

    public SendTask() {
        outQueue = new LinkedBlockingQueue<String>();

        Log.d("MY SEND", "create");
    }

    public static AtomicInteger callDuration = new AtomicInteger(60);
    public static AtomicInteger spaceDuration = new AtomicInteger(50);

    public native void encodeInit(int frameSize, int callDur, int spaceDur);
    public native void encode(String str);
    public native boolean encodeStep(short[] buf);

    //JNIEXPORT void JNICALL Java_rjdgtn_csms_SendTask_encodeInit(JNIEnv *env, jobject, jint frameSize, jint callDur, jint spaceDur) {
    //JNIEXPORT void JNICALL Java_rjdgtn_csms_SendTask_encode(JNIEnv *env, jobject, jbyteArray jdata) {
    //JNIEXPORT jboolean JNICALL Java_rjdgtn_csms_SendTask_encodeStep(JNIEnv *env, jobject, jshortArray jdata) {

    public void run() {
        Log.d("MY SEND", "run");
        AudioTrack audio = null;
        try {
            int frequency = 8000;
            int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;//CHANNEL_CONFIGURATION_MONO;
            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

            int bufsize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

            audio = new AudioTrack(AudioManager.STREAM_MUSIC,
                    frequency,
                    channelConfiguration, //2 channel
                    audioEncoding, // 16-bit
                    bufsize * 5,
                    AudioTrack.MODE_STREAM);

            audio.play();

            Log.d("MY SEND", "loop");
            while (true) {
                if (outQueue.isEmpty()) {
                    Thread.sleep(100);
                    continue;
                }
                String in = outQueue.element();
                if (!in.isEmpty()) {
                    if (in.contains("sleep ")) {
                        int duration = Integer.parseInt(in.substring(6));
                        Log.v("MY SEND", "sleep "+duration);
                        Thread.sleep(duration);
                    } else if (in.contains("callDuration ")) {
                        int duration = Integer.parseInt(in.substring(13));
                        callDuration.set(duration);
                        Log.v("MY SEND", "callDuration "+ duration);
                    } else if (in.contains("spaceDuration ")) {
                        int duration = Integer.parseInt(in.substring(14));
                        spaceDuration.set(duration);
                        Log.v("MY SEND", "spaceDuration "+ duration);
                    } else {
                        short[] buffer = new short[256];

                        encodeInit(buffer.length, callDuration.get(), spaceDuration.get());
                        encode(in);
                        Log.v("MY SEND", "start " + in);
                        while (true) {
                            boolean finished = encodeStep(buffer);
                            audio.write(buffer, 0, buffer.length);
                            Thread.sleep(buffer.length / (frequency / 1000));
                            if (finished) {
                                break;
                            }
                        }
                        Log.v("MY SEND", "finish ");
                        Thread.sleep(1000);
                    }
                }
                outQueue.take();
            }

        } catch (Exception e) {
            Log.e("SEND", "crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

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
}
