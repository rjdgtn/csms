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
    final static public int[] signalDurations =
            {1000, // длительность сигнала 1/4, паузы 1/10
            750,
            550,
            420,
            315,
            235,
            175,
            130,
            100,
            75,
            55};

    public class TransportPrefs {
        public byte bytesPerPack = 5;
        public short signalDuration = 0;
        public short confirmWait = 0;
        public short controlDelay = 0;
        public short controlDuration = 0;
        public short controlAwait = 0;
        public short readSignalDuration = 0;
        public short spaceDuration = 0;

        void setSignalDuration(short dur) {
            signalDuration = dur;
            controlDelay = (short)(dur * 1);
            controlDuration = (short)(dur * 3);
            controlAwait = (short)(dur * 1);
            readSignalDuration = (short)(dur * 0.2);
            spaceDuration = (short)(dur * 0.2);
            confirmWait = (short)min(2000 + (short)(dur * 10), 32767);
        }

//        TransportPrefs(int dur) {
//            setSignalDuration((short)dur);
//        }
    }

    private TransportPrefs prefs = new TransportPrefs();

    enum State {
        IDLE,
        SEND,
        SENDING,
        WAIT_FOR_CONFIRM,
        READ
    };

    private String stateToStr(State st) {
        if (st == State.IDLE) return "IDLE";
        if (st == State.SEND) return "SEND";
        if (st == State.SENDING) return "SENDING";
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

    private void setCallDuration(short duration) throws InterruptedException {
        prefs.setSignalDuration(duration);
        sendTask.outQueue.put("callDuration " + prefs.signalDuration);
        sendTask.outQueue.put("spaceDuration " + prefs.spaceDuration);
        readTask.bufferDuration.set(prefs.readSignalDuration);

    }

    private void sendControlSignal(String signal) throws InterruptedException {
        logv("send control " + signal);

        sendTask.outQueue.put("sleep " + prefs.controlDelay);
        sendTask.outQueue.put("callDuration " + prefs.controlDuration);
        sendTask.outQueue.put("spaceDuration " + prefs.spaceDuration);

        sendTask.outQueue.put(signal);

        sendTask.outQueue.put("callDuration " + prefs.signalDuration);
        sendTask.outQueue.put("spaceDuration " + prefs.spaceDuration);
        sendTask.outQueue.put("sleep " + prefs.controlAwait);
    }

    private void sendControlSignal(char signal) throws InterruptedException  {
        sendControlSignal("" + signal);
    }

    private void setState(State st) {
        //log(stateToStr(state) + " => "+ stateToStr(st));
        log(stateToStr(st));
        state = st;
    }

    private void logv(String str) {
        Log.d("MY TRPV", str);
        Intent intent = new Intent("csms_log");
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
    private void onStartSend() {
        Intent intent = new Intent("csms_transport");
        intent.putExtra("prefs.bytesPerPack", prefs.bytesPerPack);
        intent.putExtra("prefs.signalDuration", prefs.signalDuration);
        intent.putExtra("prefs.confirmWait", prefs.confirmWait);
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
        String packed = DtmfPacking.pack(new byte[]{0,1,2,3,4,5,6,7,8,9});
        byte[] unp = DtmfPacking.unpack(packed);

        String [] s = DtmfPacking.multipack(new byte[]{0,1,2,3,4,5,6,7,8,9}, prefs.bytesPerPack);

        boolean emulator = Build.FINGERPRINT.startsWith("generic");
        log("run");
        try {
            readTask = new ReadTask(contex);
            sendTask = new SendTask(contex);

            setCallDuration((short)315);

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

                    if (pattern.equals(RESTART_PATTERN)) {
                        log("detect pattern " + pattern);
                        throw new Exception();
                    } else if (ch == AWAKE_SIGNAL) {
                        if (state == State.IDLE) {
                            log("awake");
                            sendControlSignal(SUCCESS_SIGNAL);
                            setState(State.READ);
                        }
                    } else if (ch == SUCCESS_SIGNAL) {
                        if (state == State.WAIT_FOR_CONFIRM || state == State.SENDING) {
                            log("SUCCESS confirm");
                            // CONFIRM
                            resendCount = 0;
                            setState(State.SEND);
                            outMessages = Arrays.copyOfRange(outMessages, 1, outMessages.length);
                        }
                    } else if (ch == FAIL_SIGNAL) {
                        if (state == State.WAIT_FOR_CONFIRM || state == State.SENDING) {
                            log("FAIL confirm");
                            // RESEND
                            setState(State.SEND);
                        }
                    } else if (ch == '*') {
                        if (state == State.READ) {
                            inMessage = "*";
                            log("in: " + inMessage);
                        }
                    } else if (ch >= '0' && ch <= '8' || ch == 'D' || ch == 'C') {
                        if (state == State.READ) {
                            inMessage += ch;
                            log("in: " + inMessage);
                        }
                    } else if (ch == '#') {
                        if (state == State.READ) {
                            inMessage += "#";
                            log("in: " + inMessage);
                            if (inMessage.equals("*D#")) {
                                log("FINISHED");
                                byte[] res = DtmfPacking.mergeBytesQueue(msgBlocks);
                                log(res);
                                inQueue.put(res);
                                msgBlocks.clear();
                                sendControlSignal(SUCCESS_SIGNAL);

                            }else if (inMessage.equals("*C#")) {
                                log("FLUSH");
                                msgBlocks.clear();
                                sendControlSignal(SUCCESS_SIGNAL);
                            } else {
                                byte[] data = DtmfPacking.unpack(inMessage);
                                if (data != null) {
                                    msgBlocks.add(data);
                                    log("SUCCESS " + SUCCESS_SIGNAL);
                                    sendControlSignal(SUCCESS_SIGNAL);
                                } else {
                                    log("FAIL " + FAIL_SIGNAL);
                                    sendControlSignal(FAIL_SIGNAL);
                                }
                            }
                            inMessage = new String();
                        }
                    }
                }

                if (state == State.READ && !outQueue.isEmpty()) {
                    onStartSend();
                    OutRequest req = outQueue.element();
                    if (req.request != null) {
                        if (req.request == "reboot_remote") {
                            log("start send " + req.request);
                            while(!sendTask.outQueue.isEmpty()) {
                                Thread.sleep(100);
                            }
                            Thread.sleep(2000);
                            sendControlSignal(RESTART_PATTERN);
                            while(!sendTask.outQueue.isEmpty()) {
                                Thread.sleep(100);
                            }
                            Thread.sleep(1000);
                            readTask.inQueue.clear();
                            outQueue.take();
                            log("finish send " + req.request);
                        } else if (req.request == "config_speed") {
                            log("set call duration to  " + req.intParam);
                            setCallDuration((short)req.intParam);
                            outQueue.take();
                        }
                    } else if (req.data != null) {
                        outMessages = DtmfPacking.multipack(req.data, prefs.bytesPerPack);
                        log(" ");
                        log(req.data);
                        log("pack bytes " + req.data.length + " :");
                        for (String msg : outMessages) {
                            log(msg);
                        }
                        setState(State.SEND);
                    }
                }

                if (state == State.SEND) {
                    if (outMessages == null || outMessages.length == 0) {
                        logv("nothing to send");
                        resendCount = 0;
                        setState(State.READ);
                        inQueue.put(new byte[]{ProcessorTask.SUCCESS_COMMAND});
                        if (!outQueue.isEmpty()) outQueue.take();
                    } else if (resendCount >= 6) {
                        log("too much resend");
                        resendCount = 0;
                        setState(State.READ);
                        inQueue.put(new byte[]{ProcessorTask.FAIL_COMMAND});
                        if (!outQueue.isEmpty()) outQueue.take();
                    } else {
                        setState(State.SENDING);
                        logv("sleep before send " + (prefs.controlDuration + prefs.controlAwait));
                        Thread.sleep(prefs.controlDuration);
                        Thread.sleep(prefs.controlAwait);
                        String msg = outMessages[0];
                        if (resendCount == 0) log("play " + msg);
                        else log("replay ("+resendCount+") " + msg);
                        sendTask.outQueue.put(msg);
                        waitForConfimColdown = 0;
                        resendCount++;
                    }
                }

                if (state == State.SENDING) {
                    if (sendTask.outQueue.isEmpty() && waitForConfimColdown == 0) {
                        waitForConfimColdown = System.currentTimeMillis() + prefs.confirmWait;
                        logv("confirm coldown " + waitForConfimColdown);
                        setState(State.WAIT_FOR_CONFIRM);
                    }
                }
                if (state == State.WAIT_FOR_CONFIRM) {
                    if (waitForConfimColdown > 0 && waitForConfimColdown < System.currentTimeMillis()) {
                        log("coldown expire");
                        logv("expire coldown at " + System.currentTimeMillis());
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
