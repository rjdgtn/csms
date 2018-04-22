package rjdgtn.csms;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */

public class ProcessorTask implements Runnable {
//    public BlockingQueue<Request> outQueue;

    Context contex;
    //CommandProcessor curProcessor = null;
    public static BlockingQueue<Bundle> localCommands = new LinkedBlockingQueue<Bundle>();

    private final String ENCODING = "US-ASCII";

    private final byte SKIP_COMMAND = 000;
    private final byte ECHO_COMMAND = 011;

    public ProcessorTask(Context contex) {
        this.contex = contex;
        Log.d("MY PRCS", "create");
    }

    private void log(String str) {
        Log.d("MY PRCR", str);
        Intent intent = new Intent("csms_log");
        intent.putExtra("log", str);
        intent.putExtra("ch", "PRCR");
        contex.sendBroadcast(intent);
    }

    @Override
    public void run() {
        log("start");
        try {
            while (true) {
                if (!TransportTask.inQueue.isEmpty()) {
                    onRemoteCommand(TransportTask.inQueue.take());
                } else if (!localCommands.isEmpty()) {
                    onLocalCommand(localCommands.take());
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            log("crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

    }

    private void onLocalCommand(Bundle command) throws InterruptedException, IOException {
        String code = command.getString("code");
        log("command " + code);
        if (code.equals("reboot_remote")) onLocalRebootRemote(command);
        else if (code.equals("test")) onLocalTest(command);
        else if (code.equals("echo")) onLocalEcho(command);
    }

    private void onRemoteCommand(byte[] data) throws InterruptedException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        if (inputStream.available() == 0) return;
        int code = inputStream.read();
        if (code == ECHO_COMMAND) onRemoteEcho(inputStream);
    }

    private void onLocalRebootRemote(Bundle command) throws InterruptedException {
        TransportTask.outQueue.put(new OutRequest("reboot_remote"));
    }
    private void onLocalTest(Bundle command) throws InterruptedException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(SKIP_COMMAND);
        outputStream.write(new byte[] {0,1,2,3,4,5,6,7,8,9});
        TransportTask.outQueue.put(new OutRequest(outputStream.toByteArray()));
    }
    private void onLocalEcho(Bundle command) throws InterruptedException, IOException {
        String msg = command.getString("msg");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(ECHO_COMMAND);
        outputStream.write(10);
        outputStream.write(msg.getBytes("US-ASCII"));
        TransportTask.outQueue.put(new OutRequest(outputStream.toByteArray()));
    }

    private void onRemoteEcho(ByteArrayInputStream stream) throws InterruptedException, IOException {
        if (stream.available() == 0) return;
        int echoStep = stream.read();
        byte[] stringBuf = new byte[stream.available()];
        stream.read(stringBuf);

        String msg = new String(stringBuf, ENCODING);
        log("echo: " + msg);
        if (echoStep > 0) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(ECHO_COMMAND);
            outputStream.write(echoStep-1);
            outputStream.write(msg.getBytes(ENCODING));
            TransportTask.outQueue.put(new OutRequest(outputStream.toByteArray()));
        }
    }
//    abstract class CommandProcessor {
//
//        abstract boolean isFinished();
//
//        void initResponder(byte[] data) {
//
//        }
//        void initRequester(Bundle data) {
//
//        }
//        void onRequestSend() {
//
//        }
//        boolean onAnswer(byte[] data) {
//            return true;
//        }
//
//
////        public void on(String str) {
////
////        }
//    }
    //private


}


