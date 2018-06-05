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
import java.nio.ByteBuffer;
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

    private final byte NO_COMMAND = 000;
    private final byte SKIP_COMMAND = 1;
    private final byte ECHO_COMMAND = 11;
    private final byte CONFIG_SPEED_COMMAND = 22;
    private final byte STATUS_REQUEST_COMMAND = 33;
    private final byte STATUS_ANSWER_COMMAND = 44;
    public static final byte FAIL_COMMAND = 127;
    public static final byte SUCCESS_COMMAND = 126;


    Bundle lastLocalCommand = null;

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

        Status status = Status.make(contex);
        byte[] b = status.toBytes();


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
        else if (code.equals("config_speed")) onLocalConfigSpeed(command);
        else if (code.equals("status")) onLocalStatus(command);

        lastLocalCommand = command;
    }

    private void onRemoteCommand(byte[] data) throws InterruptedException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        if (inputStream.available() == 0) return;
        int code = inputStream.read();
        if (code == ECHO_COMMAND) onRemoteEcho(inputStream);
        if (code == CONFIG_SPEED_COMMAND) onRemoteConfigSpeed(inputStream);
        if (code == FAIL_COMMAND) onRemoteFail();
        if (code == SUCCESS_COMMAND) onRemoteSuccess();
        if (code == STATUS_REQUEST_COMMAND) onRemoteStatusRequest(inputStream);
        if (code == STATUS_ANSWER_COMMAND) onRemoteStatusAnswer(inputStream);
    }

    private void onLocalStatus(Bundle command) throws InterruptedException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(STATUS_REQUEST_COMMAND);
        TransportTask.outQueue.put(new OutRequest(outputStream.toByteArray()));

        //onRemoteStatusAnswer(new ByteArrayInputStream(Status.make(contex).toBytes()));
    }

    private void onRemoteStatusRequest(ByteArrayInputStream stream) throws IOException, InterruptedException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(STATUS_ANSWER_COMMAND);
        outputStream.write(Status.make(contex).toBytes()) ;
        TransportTask.outQueue.put(new OutRequest(outputStream.toByteArray()));
    }

    private void onRemoteStatusAnswer(ByteArrayInputStream stream) throws IOException {
        byte[] buf = new byte[stream.available()];
        stream.read(buf);
        ByteBuffer bbuf =  ByteBuffer.wrap(buf, 0, buf.length);
        Status status = new Status(bbuf);

        log("\tStatus:");
        log("\tuptime: " + status.uptime / 2 + " hours");
        log("\tpower: " + status.power);
        log("\tgsm: " + status.gsm );
        log("\tgps: " + status.location);
        log("\twifi: " + status.wifi);
        log("\tbluetooth: " + status.bluetooth);
    }

    private void onLocalConfigSpeed(Bundle command) throws InterruptedException {
        short duration = (short)Integer.parseInt(command.getString("value"));
        log("local request change call duration to " + duration );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(CONFIG_SPEED_COMMAND);
        outputStream.write(duration >> 8);
        outputStream.write((duration << 8) >> 8);
        TransportTask.outQueue.put(new OutRequest(outputStream.toByteArray()));
    }

    private void onRemoteConfigSpeed(ByteArrayInputStream stream) throws InterruptedException, IOException {
        if (stream.available() == 0) return;
        //byte[] b = new byte[2];
        int duration = stream.read();
        duration = duration << 8;
        duration += stream.read();

        log("remote request change call duration to " + duration );
        TransportTask.outQueue.put(new OutRequest("config_speed", duration));
    }

    private void onRemoteFail() throws InterruptedException {
        log("CMD FAIL");

    }

    private void onRemoteSuccess() throws InterruptedException {
        log("CMD SUCCESS");
        if (lastLocalCommand != null) {
            String code = lastLocalCommand.getString("code");
            if (code.equals("config_speed")) {
                int duration = Integer.parseInt(lastLocalCommand.getString("value"));
                TransportTask.outQueue.put(new OutRequest("config_speed", duration));
            }
        }
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


