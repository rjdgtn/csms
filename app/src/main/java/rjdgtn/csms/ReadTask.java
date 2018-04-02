package rjdgtn.csms;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.min;

/**
 * Created by Petr on 25.03.2018.
 */

public class ReadTask implements Runnable {

    char prevSymbol = '\0';
    public BlockingQueue<Character> outQueue;;

    public native char decode(short[] data);

    public ReadTask() {
        outQueue = new LinkedBlockingQueue<Character>();

        Log.d("CSMS", "READ create");
    }

    public static AtomicInteger bufferSize = new AtomicInteger(350);

//    public static int SHORT_little_endian_TO_big_endian(int i){
//        return (((i>>8)&0xff)+((i << 8)&0xff00));
//    }
    public void run() {
        AudioRecord audioRecord = null;
        try {
            int frequency = 8000;
            int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;//CHANNEL_CONFIGURATION_MONO;
            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

            int bufferBytes = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
            int bufferShorts = bufferBytes / 2;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, bufferBytes * 100);

            short[] buffer = new short[bufferShorts * 2];
            audioRecord.startRecording();

            int wpos = 0;
            short[] decodeBuffer = null;

            while (true) {
                int rpos = 0;
                int rsz = audioRecord.read(buffer, 0, buffer.length);

                if (rsz > 0) {
                    int bufsz = bufferSize.get();
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
                        if (prevSymbol != symbol && symbol != '\0') {
                           // prevSymbol = symbol;
                            outQueue.put(new Character(symbol));
                        }
                    }

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
