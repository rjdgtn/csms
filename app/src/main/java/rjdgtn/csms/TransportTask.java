package rjdgtn.csms;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Math.min;
import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */

public class TransportTask  implements Runnable {
    private int[] signalReadLength =
            {1000, // длительность сигнала 1/5, паузы 1/10
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

    public class TransportPrefs {
        public byte bytesPerPack = 30;
        public short signalDuration = 315;
        public short confirmWait = 5000;
        public short controlDelay = (short)(signalDuration * 2);
        public short controlDuration = (short)(signalDuration * 4);
        public short controlAwait = (short)(signalDuration * 2);
       // public short controlSignalLength = responseDuration;
        public float readSignalDurationMult = 0.2f;
        public float spaceMult = 0.1f;
        //public short controlExtraSignalLength = 315;
    }

    private TransportPrefs prefs = new TransportPrefs();

    enum State {
        IDLE,
        SEND,
        WAIT_FOR_CONFIRM,
        READ
    };

    private String stateToStr(State st) {
        if (st == State.IDLE) return "IDLE";
        if (st == State.SEND) return "SEND";
        if (st == State.WAIT_FOR_CONFIRM) return "WAIT_FOR_CONFIRM";
        if (st == State.READ) return "READ";
        else return "UNKNOWN";
    }

    private State state = State.READ;

    //private char resetSignal = 'C';
    private char SUCCESS_SIGNAL = 'A';
    private char FAIL_SIGNAL = 'B';
    private char AWAKE_SIGNAL = '9';
    private String RESTART_PATTERN = "DCDCDC";


    private ReadTask readTask = null;
    private SendTask sendTask = null;
    private Thread readThread = null;
    private Thread sendThread = null;

    //public BlockingQueue<String> commands = new LinkedBlockingQueue<String>();
    private Queue<byte[]> msgBlocks = new LinkedList<byte[]>();
    public static BlockingQueue<byte[]> inQueue = new LinkedBlockingQueue<byte[]>();
    public static BlockingQueue<OutRequest> outQueue = new LinkedBlockingQueue<OutRequest>();

    Context contex;

    public TransportTask(Context contex) {
        this.contex = contex;
        log("create");
    }

    private void sendControlSignal(String signal) throws InterruptedException {
        log("push control " + signal);

        sendTask.outQueue.put("sleep " + prefs.controlDelay);
        sendTask.outQueue.put("callDuration " + prefs.controlDuration);
        sendTask.outQueue.put("spaceDuration " + (int)(prefs.controlDuration * prefs.spaceMult));

        sendTask.outQueue.put(signal);

        sendTask.outQueue.put("callDuration " + prefs.signalDuration);
        sendTask.outQueue.put("spaceDuration " + (int)(prefs.signalDuration * prefs.spaceMult));
        sendTask.outQueue.put("sleep " + prefs.controlAwait);
    }

    private void sendControlSignal(char signal) throws InterruptedException  {
        sendControlSignal("" + signal);
    }

    private void setState(State st) {
        log(stateToStr(state) + " => "+ stateToStr(st));
        state = st;
    }

    private void logv(String str) {
        Log.d("MY TRPV", str);
        Intent intent = new Intent("transport_log");
        intent.putExtra("log", str);
        intent.putExtra("ch", "TRPV");
        contex.sendBroadcast(intent);

    }
    private void log(String str) {
        Log.d("MY TRPT", str);
        Intent intent = new Intent("csms_log");
        intent.putExtra("log", str);
        intent.putExtra("ch", "TRPT");
        contex.sendBroadcast(intent);
    }

    private void log(byte[] data) {
        String res = new String();
        for (byte b : data) {
            res += b + " ";
        }
        log(res);
    }

    public void run() {
        boolean emulator = Build.FINGERPRINT.startsWith("generic");
        log("run");
        try {
            readTask = new ReadTask(contex);
            sendTask = new SendTask(contex);

            readThread = new Thread(readTask);
            //readThread.setDaemon(true);
            if (!emulator) readThread.start();

            sendThread = new Thread(sendTask);
            //sendThread.setDaemon(true);
            sendThread.start();

            String pattern = "XxXxXx";
            String inMessage = new String();
            String[] outMessages = null;
            long waitForConfimColdown = 0;
            long resendCount = 0;

            while (true) {
                if (state == State.READ) {
                    Thread.sleep(100);
                }
                while (!readTask.inQueue.isEmpty()) {
                    Character ch = readTask.inQueue.take();
                    pattern = pattern.substring(1) + ch;

                    logv("take '" + ch + "'");

                    if (pattern == RESTART_PATTERN) {
                        log("detect pattern " + pattern);
                        throw new Exception();
                    } else if (ch == AWAKE_SIGNAL) {
                        if (state == State.IDLE) {
                            log("awake");
                            sendControlSignal(SUCCESS_SIGNAL);
                            setState(State.READ);
                        }
                    } else if (ch == SUCCESS_SIGNAL) {
                        if (state == State.WAIT_FOR_CONFIRM) {
                            log("succes confirm");
                            // CONFIRM
                            resendCount = 0;
                            setState(State.SEND);
                            outMessages = Arrays.copyOfRange(outMessages, 1, outMessages.length);
                        }
                    } else if (ch == FAIL_SIGNAL) {
                        if (state == State.WAIT_FOR_CONFIRM) {
                            log("fail confirm");
                            // RESEND
                            setState(State.SEND);
                        }
                    } else if (ch == '*') {
                        if (state == State.READ) {
                            inMessage = "*";
                            log("in message: " + inMessage);
                        }
                    } else if (ch >= '0' && ch <= '8' || ch == 'D') {
                        if (state == State.READ) {
                            inMessage += ch;
                            logv("in message: " + inMessage);
                        }
                    } else if (ch == '#') {
                        if (state == State.READ) {
                            inMessage += "#";
                            log("readed: " + inMessage);
                            if (inMessage.equals("*D#")) {
                                log("receive sequence finished");
                                byte[] res = DtmfPacking.mergeBytesQueue(msgBlocks);
                                log(res);
                                inQueue.put(res);
                                msgBlocks.clear();
                                sendControlSignal(SUCCESS_SIGNAL);
                            } else {
                                byte[] data = DtmfPacking.unpack(inMessage);
                                if (data != null) {
                                    msgBlocks.add(data);
                                    log("unpack success");
                                    sendControlSignal(SUCCESS_SIGNAL);
                                } else {
                                    log("unpack fail");
                                    sendControlSignal(FAIL_SIGNAL);
                                }
                            }
                            inMessage = new String();
                        }
                    }
                }

                if (state == State.READ && !outQueue.isEmpty()) {
                    OutRequest req = outQueue.element();
                    if (req.request != null) {
                        if (req.request == "RESTART_REMOTE_DEVICE") {
                            log("start send " + req.request);
                            while(!sendTask.outQueue.isEmpty()) {
                                Thread.sleep(100);
                            }
                            Thread.sleep(2000);
                            sendTask.outQueue.put(RESTART_PATTERN);
                            while(!sendTask.outQueue.isEmpty()) {
                                Thread.sleep(100);
                            }
                            Thread.sleep(1000);
                            readTask.inQueue.clear();
                            outQueue.take();
                            log("finish send " + req.request);
                        }
                    } else if (req.data != null) {
                        outMessages = DtmfPacking.multipack(req.data, prefs.bytesPerPack);
                        log("pack bytes " + req.data.length + " to send : ");
                        for (String msg : outMessages) {
                            log(msg);
                        }
                        setState(State.SEND);
                    }
                }

                if (state == State.SEND) {
                    if (outMessages == null || outMessages.length == 0) {
                        log("nothing to send");
                        setState(State.READ);
                        if (!outQueue.isEmpty()) outQueue.take();
                    } else {
                        log("sleep before send " + (prefs.controlDuration + prefs.controlAwait));
                        Thread.sleep(prefs.controlDuration);
                        Thread.sleep(prefs.controlAwait);
                        String msg = outMessages[0];
                        if (resendCount == 0) log("send " + msg);
                        else log("resend ("+resendCount+") " + msg);
                        sendTask.outQueue.put(msg);
                        setState(State.WAIT_FOR_CONFIRM);
                        waitForConfimColdown = 0;
                        resendCount++;
                    }
                }

                if (state == State.WAIT_FOR_CONFIRM) {
                    if (sendTask.outQueue.isEmpty() && waitForConfimColdown == 0) {
                        waitForConfimColdown = System.currentTimeMillis() + prefs.confirmWait;
                        log("confirm coldown " + waitForConfimColdown);
                    }
                    if (waitForConfimColdown > 0 && waitForConfimColdown < System.currentTimeMillis()) {
                        log("expire coldown at " + System.currentTimeMillis());
                        setState(State.SEND);
                    }
                }
            }

        } catch (Exception e) {
            log("crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }

        log("finish");
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception());
    }
}
