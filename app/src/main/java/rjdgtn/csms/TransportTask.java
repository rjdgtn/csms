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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Math.ceil;
import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */

public class TransportTask  implements Runnable {
    private int[] signalLength =
            // длительность сигнала 1/5, паузы 1/10
                    {1000,
                     750,
                     550,
                     420,
                     315,
                     235,
                     175,
                     130,
                     100,
                     75,
                     55,
                     42,
                     32};

    enum State {
        IDLE,
        SEND,
        WAIT_FOR_CONFIRM,
        READ
    };

    private State state = State.IDLE;

    private ReadTask readTask = null;
    private SendTask sendTask = null;
    private Thread readThread = null;
    private Thread sendThread = null;

    public BlockingQueue<Request> inQueue;
    public BlockingQueue<Request> outQueue;

    Context contex;

    private char[] intToSymbol = {'0', '1', '2', '3', '4', '5', '6', '7', '8'};
    private int symbolToInt(char ch) {
        switch (ch) {
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            default: return -1;
        }
    }



    public TransportTask(Context contex) {
        this.contex = contex;

        inQueue = new LinkedBlockingQueue<Request>();
        outQueue = new LinkedBlockingQueue<Request>();

        Log.d("CSMS", "TRANSPORT create");
    }

    public String pack(byte[] data) {
        String res = new String("*");
        //String origRes = new String();
        BitSet bitset = BitSet.valueOf(data);

        int checksum = 0;
        int prevVal = 8;
        int symbolsNum = (int)ceil(data.length * 8 / 3.0);
        for (int i = 0; i < symbolsNum; i++) {
            //0-7 - 3 bits
            int val = 0;
            if (bitset.get(i*3))
                val += 1;
            val = val << 1;
            if (bitset.get(i*3+1))
                val += 1;
            val = val << 1;
            if (bitset.get(i*3+2))
                val += 1;

            checksum = checksum ^ val;

            //origRes += intToSymbol[val];

            if (val >= prevVal) val++;

            res += intToSymbol[val];
            prevVal = val;
        }
        res += intToSymbol[checksum];
        res += '#';

//        Log.d("CSMS:", origRes);

        return res;
    }

    public byte[] unpack(String msg) {
        int overhead = 3;
        if (msg.length() < 3 + overhead) return null;
        if (msg.charAt(0) != '*') return null;
        if (msg.charAt(msg.length()-1) != '#') return null;

        int checkSum = symbolToInt(msg.charAt(msg.length()-2));
        int meanBitsNum = ((msg.length() - overhead) * 3 / 8) * 8;

        BitSet res = new BitSet(meanBitsNum);
        int i = 0;
        int prevSym = 8;
        for (int j = 1; j < msg.length()-2; j++) {
            int sym = symbolToInt(msg.charAt(j));
            if (sym == prevSym) {
                return null;
            } else if (sym > prevSym) {
                prevSym = sym;
                --sym;
            } else {
                prevSym = sym;
            }
            checkSum = checkSum ^ sym;
            res.set(i++, (sym & 0b100) > 0);
            if (i >= meanBitsNum) break;
            res.set(i++, (sym & 0b010) > 0);
            if (i >= meanBitsNum) break;
            res.set(i++, (sym & 0b001) > 0);
            if (i >= meanBitsNum) break;
        }

        if (checkSum != 0) return null;

        return res.toByteArray();
    }
    public void sendData(byte[] data) {

    }

    public void run() {

//        Random rnd = new Random(System.currentTimeMillis());
//        int size = 150 + rnd.nextInt(50 + 1);
//
//        byte[] data = new byte[size];
//        for (int i = 0; i < size; i++) {
//            byte min = -128;
//            byte max = 127;
//            int elem = min + rnd.nextInt(max - min + 1);
//            data[i] = (byte)elem;
//        }
//        //byte[] data = {1, 2, 3, 4, 5};
//        String encd = encode(data);
//        byte[] dedata = decode(encd);
//
//        if (!Arrays.equals(data, dedata)) {
//            System.exit(0);
//        }

//        Log.d("CSMS", encd);
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

//                sleep(2000);
//                sendTask.inQueue.put("1234567890");
//                sleep(2000);
//
                Character symbol = readTask.outQueue.take();
                Log.d("CSMS:", symbol.toString());
                Intent intent = new Intent("my-integer");
                // Adding some data
                intent.putExtra("message", symbol);
                LocalBroadcastManager.getInstance(contex).sendBroadcast(intent);

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
