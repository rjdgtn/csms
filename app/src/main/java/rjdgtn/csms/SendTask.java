package rjdgtn.csms;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Petr on 25.03.2018.
 */

public class SendTask implements Runnable {
    public LinkedBlockingQueue<String> inQueue;

    public SendTask() {
        inQueue = new LinkedBlockingQueue<String>();

        Log.d("CSMS", "SEND create");
    }

    public static AtomicInteger callDuration = new AtomicInteger(75);
    public static AtomicInteger spaceDuration = new AtomicInteger(25);

    public native void encodeInit(int frameSize, int callDur, int spaceDur);
    public native void encode(String str);
    public native boolean encodeStep(short[] buf);

    //JNIEXPORT void JNICALL Java_rjdgtn_csms_SendTask_encodeInit(JNIEnv *env, jobject, jint frameSize, jint callDur, jint spaceDur) {
    //JNIEXPORT void JNICALL Java_rjdgtn_csms_SendTask_encode(JNIEnv *env, jobject, jbyteArray jdata) {
    //JNIEXPORT jboolean JNICALL Java_rjdgtn_csms_SendTask_encodeStep(JNIEnv *env, jobject, jshortArray jdata) {

    public void run() {
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

            while (true) {
                if (inQueue.isEmpty()) {
                    Thread.sleep(1000);
                    continue;
                }
                String in = inQueue.element();
                if (!in.isEmpty()) {
                    //in += "    ";
                    short[] buffer = new short[100];

                    encodeInit(buffer.length, callDuration.get(), spaceDuration.get());
                    encode(in);
                    while (true) {
                        boolean finished = encodeStep(buffer);
                        audio.write(buffer, 0, buffer.length);
                        Thread.sleep(buffer.length / (frequency / 1000));
                        if (finished) {
                            break;
                        }
                    }
                    Thread.sleep(1000);
                }
                inQueue.take();
            }

        } catch (Exception e) {
            Log.d("CSMS", "transport crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());

//        } catch (Exception e) {
//            Log.d("CSMS", "send crash");
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
