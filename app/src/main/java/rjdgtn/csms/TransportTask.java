package rjdgtn.csms;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */

public class TransportTask  implements Runnable {
    private int[][] signalLength =
            // длительность сигнала 1/5, паузы 1/10
                    {{1000, 2000},
                    {750, 1500},
                    {550, 1100},
                    {420, 840},
                    {315, 630},
                    {235, 470},
                    {175, 350},
                    {130, 260},
                    {100, 200},
                    {75, 150},
                    {55, 110},
                    {42, 88},
                    {32, 64}};

    private ReadTask readTask = null;
    private SendTask sendTask = null;
    private Thread readThread = null;
    private Thread sendThread = null;

    public BlockingQueue<Request> inQueue;
    public BlockingQueue<Request> outQueue;

    Context contex;

    public TransportTask(Context contex) {
        this.contex = contex;

        inQueue = new LinkedBlockingQueue<Request>();
        outQueue = new LinkedBlockingQueue<Request>();

        Log.d("CSMS", "TRANSPORT create");
    }

//    public native void encode(short[] data);
//    public native void destroy();

    public void run() {
        Log.d("CSMS", "do transport");
        try {

            //decodee(new short[100]);
            //Thread.sleep(2500);
            //int o = 100/0;

            readTask = new ReadTask();
            sendTask = new SendTask();

            readThread = new Thread(readTask);
            //readThread.setDaemon(true);
            readThread.start();

            sendThread = new Thread(sendTask);
            //sendThread.setDaemon(true);
            sendThread.start();

            //short[] superBuff = new short[25000];
            while (true) {
                //Thread.sleep(1000);

                sleep(2000);
                sendTask.inQueue.put("1234567890");
                sleep(2000);

//                Character symbol = readTask.outQueue.take();
//                Log.d("CSMS:", symbol.toString());
//                Intent intent = new Intent("my-integer");
//                // Adding some data
//                intent.putExtra("message", symbol);
//                LocalBroadcastManager.getInstance(contex).sendBroadcast(intent);

//                short[] inBytes = new short[320];
//                Log.d("CSMS", "outQueue " + readTask.outQueue.size() + " inQueue " + sendTask.inQueue.size());
//                encode(inBytes);


//                if (i + inBytes.length > superBuff.length) break;
//
//                System.arraycopy(inBytes, 0, superBuff, i, inBytes.length);
//                i += inBytes.length;
//                Log.d("CSMS", ""+i);

               // Log.d("CSMS", "readTask.outQueue: "+readTask.outQueue.size());
//                String decode = decode(inBytes);
//                if (!decode.isEmpty())
//                    Log.d("CSMS", "transport: "+decode);



            }
//            Log.d("CSMS", "PLAY");
//            Thread.sleep(1000);
//            int frequency = 8000;
//            int channelConfiguration = AudioFormat.CHANNEL_OUT_MONO;//CHANNEL_CONFIGURATION_MONO;
//            int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
//
//            int bufsize = AudioTrack.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
//
//            AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC,
//                    frequency,
//                    channelConfiguration, //2 channel
//                    audioEncoding, // 16-bit
//                    superBuff.length * 2,
//                    AudioTrack.MODE_STATIC);
//
//            audio.write(superBuff, 0, superBuff.length);
//
//            audio.play();
//
//            i++;
////            String decode = decode(superBuff);
////            if (!decode.isEmpty())
////                Log.d("CSMS", "transport: "+decode);
//
//            i = 0;
//            while (true) {
//                short[] localBuf = new short[160];
//                if (i + localBuf.length > superBuff.length) break;
//                System.arraycopy(superBuff, i, localBuf, 0, localBuf.length);
//                i += localBuf.length;
//
////                Log.d("CSMS", ""+i+" queue: " + sendTask.inQueue.size());
////                sendTask.inQueue.put(localBuf);
//
//                String decode = decode(localBuf);
//                if (!decode.isEmpty())
//                    Log.d("CSMS", "transport: "+decode);
//                //Thread.sleep(900 * localBuf.length / 8000);
//            }
//
////            Thread.sleep(5000);
////            int o = 100/0;

//            while (true) {
//                Thread.sleep(1000);
//            }

        } catch (Exception e) {
            Log.d("CSMS", "transport crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());

//        finally {
//            //destroy();
//            if (sendThread != null) sendThread.interrupt();
//            if (readThread != null) readThread.interrupt();
//            WorkerService.breakExec.set(true);
//            //System.exit(0);
//        }
    }
}
