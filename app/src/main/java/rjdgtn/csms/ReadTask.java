package rjdgtn.csms;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Petr on 25.03.2018.
 */

public class ReadTask implements Runnable {

    public BlockingQueue<short[]> outQueue;;

    public ReadTask() {
        outQueue = new LinkedBlockingQueue<short[]>();

        Log.d("CSMS", "READ create");
    }

    boolean active = false;

    public static int SHORT_little_endian_TO_big_endian(int i){
        return (((i>>8)&0xff)+((i << 8)&0xff00));
    }
    public void run() {
        AudioRecord audioRecord = null;
        try {
            int frequency = 8000;
            int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;//CHANNEL_CONFIGURATION_MONO;
            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

            int bufferBytes = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
            int bufferShorts = bufferBytes / 2;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferBytes);


            short[] buffer = new short[bufferShorts];
            audioRecord.startRecording();

            while (true) {
                int readShorts = audioRecord.read(buffer, 0, bufferShorts);

                if (readShorts > 0) {
                    short[] outBuffer = new short[readShorts];
//                    for(int i = 0; i < readShorts; i++) {
//                        outBuffer[i] = (short)SHORT_little_endian_TO_big_endian(buffer[i]);
//                        //pcm8Buffer[i] = (byte)(buffer[i] >> 8);
//                    }
                    System.arraycopy(buffer, 0, outBuffer, 0, outBuffer.length);
                    outQueue.put(outBuffer);
                }

                //Thread.sleep(1000);

                //Log.d("CSMS", "read");
            }

        } catch (Exception e) {
            Log.d("CSMS", "transport crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());

//        }  catch (Exception e) {
//            Log.d("CSMS", "read crash");
//            if (audioRecord != null) {
//                audioRecord.stop();
//                audioRecord.release();
//                audioRecord = null;
//            }
//        } finally {
//            WorkerService.breakExec.set(true);
//        }
    }
}
