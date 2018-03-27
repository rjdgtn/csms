package rjdgtn.csms;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */


public class TransportTask  implements Runnable {

    private ReadTask readTask;
    private SendTask sendTask;
    private Thread readThread;
    private Thread sendThread;

    public BlockingQueue<Request> inQueue;
    public BlockingQueue<Request> outQueue;

    public TransportTask() {
        inQueue = new LinkedBlockingQueue<Request>();
        outQueue = new LinkedBlockingQueue<Request>();

        Log.d("CSMS", "TRANSPORT create");
    }

    public native String decode(short[] data);
    public native void encode(short[] data);
    public native void destroy();

    public void run() {
        Log.d("CSMS", "do transport");
        try {
            readTask = new ReadTask();
            sendTask = new SendTask();

            readThread = new Thread(readTask);
            //readThread.setDaemon(true);
            readThread.start();

            sendThread = new Thread(sendTask);
            //sendThread.setDaemon(true);
            sendThread.start();

            short[] superBuff = new short[25000];
            int i = 0;
            while (true) {
                //Thread.sleep(1000);

                short[] inBytes = readTask.outQueue.take();

                if (i + inBytes.length > superBuff.length) break;

                System.arraycopy(inBytes, 0, superBuff, i, inBytes.length);
                i += inBytes.length;
                Log.d("CSMS", ""+i);

               // Log.d("CSMS", "readTask.outQueue: "+readTask.outQueue.size());
//                String decode = decode(inBytes);
//                if (!decode.isEmpty())
//                    Log.d("CSMS", "transport: "+decode);

//                short[] outShorts = new short[320];
//                Log.d("CSMS", "outQueue " + readTask.outQueue.size() + " inQueue " + sendTask.inQueue.size());
//                encode(outShorts);
//                Thread.sleep(1000 * outShorts.length / 8000);
//
//                sendTask.inQueue.put(inBytes);
//                Thread.sleep(500 * inBytes.length / 8000);

            }
            Log.d("CSMS", "PLAY");
            Thread.sleep(5000);
            int frequency = 8000;
            int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;//CHANNEL_CONFIGURATION_MONO;
            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

            int bufsize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);

            AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC,
                    frequency,
                    channelConfiguration, //2 channel
                    audioEncoding, // 16-bit
                    superBuff.length * 2,
                    AudioTrack.MODE_STATIC);

            audio.write(superBuff, 0, superBuff.length);

            audio.play();

            String decode = decode(superBuff);
            if (!decode.isEmpty())
                Log.d("CSMS", "transport: "+decode);

//            i = 0;
//            while (true) {
//                short[] localBuf = new short[160];
//                if (i + localBuf.length > superBuff.length) break;
//                System.arraycopy(superBuff, i, localBuf, 0, localBuf.length);
//                i += localBuf.length;
//                Log.d("CSMS", ""+i+" queue: " + sendTask.inQueue.size());
//                sendTask.inQueue.put(localBuf);
//                String decode = decode(localBuf);
//                if (!decode.isEmpty())
//                    Log.d("CSMS", "transport: "+decode);
//                Thread.sleep(900 * localBuf.length / 8000);
//            }

            while (true) {
                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {

        } finally {
            destroy();
            sendThread.interrupt();
            readThread.interrupt();
        }
    }
}
