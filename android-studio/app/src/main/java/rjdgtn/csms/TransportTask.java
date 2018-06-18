package rjdgtn.csms;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.SyncFailedException;
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
            55,
            40,
            30};

    public class TransportPrefs {
        public byte bytesPerPack = 8;
        public short signalDuration = 0;
        public short confirmWait = 0;
        public short controlDelay = 0;
        public short controlDuration = 0;
        public short controlAwait = 0;
        public short readSignalDuration = 0;
        public short spaceDuration = 0;

        void setSignalDuration(short dur) {
            signalDuration = dur;
            controlDelay = (short)(dur * 2);
            controlDuration = (short)(dur * 3);
            controlAwait = (short)(dur * 2);
            readSignalDuration = (short)(dur * 0.2);
            spaceDuration = (short)min((short)20, (short)(dur * 0.2));
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
    private char FAIL_SIGNAL = 'D';
    private char AWAKE_SIGNAL = '9';
    public static String RESTART_PATTERN = "BCBCBC";


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

    private void waitForSilence(int ms)  throws InterruptedException {
        long lastRead = System.currentTimeMillis();
        do {
            while (!readTask.inQueue.isEmpty()) {
                readTask.inQueue.take();
                lastRead = System.currentTimeMillis();
            }
            sleep(10);
        } while (System.currentTimeMillis() - lastRead < ms);
    }

    private void sendControlSignal(String signal) throws InterruptedException {
        if (signal.length() == 1) {
            if (signal.charAt(0) != '9') signal = signal + "9";
            else signal = signal + "1";
        }
        logv("send control " + signal);

        waitForSilence(prefs.controlDelay);

        //sendTask.outQueue.put("sleep " + prefs.controlDelay);
        sendTask.outQueue.put("callDuration " + prefs.controlDuration);
        sendTask.outQueue.put("spaceDuration " + prefs.spaceDuration);

        sendTask.outQueue.put(signal);

        sendTask.outQueue.put("callDuration " + prefs.signalDuration);
        sendTask.outQueue.put("spaceDuration " + prefs.spaceDuration);

        sleep(prefs.controlDuration);

        waitForSilence(prefs.controlAwait);
        //waitForSilence(prefs.controlAwait);
        //sendTask.outQueue.put("sleep " + prefs.controlAwait);
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
//        String packed = DtmfPacking.packWithCheck(new byte[]{1, 0,1,2,3,4,5,6,7,8,9});
//        byte[] unp = DtmfPacking.unpackWithCheck(packed);
//
//        String [] s = DtmfPacking.multipack(new byte[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14}, prefs.bytesPerPack);
//
//        byte[] pack1 = DtmfPacking.unpackWithCheck(s[1]);
//        byte[] pack2 = DtmfPacking.unpackWithCheck(s[2]);
       // DtmfPacking.mergeBytesQueue(pack1, pack2);

        Queue<byte[]> q = new LinkedList<byte[]>();
        q.add(new byte[]{1,0,1,2,  3,4,5});
        q.add(new byte[]{6,7,8,9,-34,0,0});
        byte[] d = DtmfPacking.multiunpack(q);

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

            String inMessage = new String();
            String[] outMessages = null;
            long waitForConfimColdown = 0;
            long resendCount = 0;
            long lastEventTime = System.currentTimeMillis();;

            while (true) {
                //Log.v("MY ", " - " + state + " " + lastEventTime + " " + System.currentTimeMillis());
                if (state == State.READ) {
                    Thread.sleep(100);
                } else if (state == State.IDLE) {
                    Thread.sleep(2000);
                }
                if (!readTask.inQueue.isEmpty() || !sendTask.outQueue.isEmpty() || !outQueue.isEmpty()) {
                    lastEventTime = System.currentTimeMillis();
                }

                while (!readTask.inQueue.isEmpty()) {
                    Character ch = readTask.inQueue.take();

                    logv("take '" + ch + "'");

                    if (ch == AWAKE_SIGNAL) {
                        if (state == State.IDLE) {
                            log("awake");
                            sendControlSignal(SUCCESS_SIGNAL);
                            sendTask.idleMode.set(false);
                            readTask.idleMode.set(false);
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
                    } else if (ch >= '0' && ch <= '8' || ch == '#' || ch == 'D' || ch == 'C' || ch == 'B' || ch == 'A') {
                        if (state == State.READ) {
                            inMessage += ch;
                            log("in: " + inMessage);
                        }
                    } else if (ch == '*') {
                        if (state == State.READ) {
                            inMessage += "*";
                            log("in: " + inMessage);
                            if (inMessage.equals("#C*")) {
                                byte[] res = DtmfPacking.multiunpack(msgBlocks);
                                if (res != null) {
                                    log("FINISHED");
                                    log("SUCCESS " + SUCCESS_SIGNAL);
                                    sendControlSignal(SUCCESS_SIGNAL);
                                    log(res);
                                    inQueue.put(res);
                                    msgBlocks.clear();
                                    sleep(1000);
                                } else {
                                    log("FAIL FINISH " + FAIL_SIGNAL);
                                    sendControlSignal(FAIL_SIGNAL);
                                }

                            }else if (inMessage.equals("#B*")) {
                                log("FLUSH");
                                msgBlocks.clear();
                                log("SUCCESS " + SUCCESS_SIGNAL);
                                sendControlSignal(SUCCESS_SIGNAL);
                            } else {
                                byte[] data = DtmfPacking.unpackWithCheck(inMessage);
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
                            waitForSilence(2000);
                            while (!sendTask.outQueue.isEmpty()) {
                                Thread.sleep(100);
                            }
                            Thread.sleep(2000);
                            sendControlSignal(RESTART_PATTERN);
                            while (!sendTask.outQueue.isEmpty()) {
                                Thread.sleep(100);
                            }
                            Thread.sleep(1000);
                            readTask.inQueue.clear();
                            outQueue.take();
                            log("finish send " + req.request);
                        } else if (req.request == "wake") {
                            log("start send " + req.request);
                            while (!sendTask.outQueue.isEmpty()) {
                                Thread.sleep(100);
                            }
                            sendTask.outQueue.put("callDuration " + 4000);
                            sendTask.outQueue.put(""+AWAKE_SIGNAL+"1");
                            sendTask.outQueue.put("callDuration " + prefs.signalDuration);

                            while (!sendTask.outQueue.isEmpty()) {
                                Thread.sleep(100);
                            }
                            Thread.sleep(2000);
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
                        waitForSilence(prefs.controlDuration + prefs.controlAwait);
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

                if (state == State.READ && lastEventTime + 5 * 60 * 1000 < System.currentTimeMillis()) {
                    sendTask.idleMode.set(true);
                    readTask.idleMode.set(true);
                    setState(State.IDLE);
                }
                if (state == State.IDLE && !outQueue.isEmpty()) {
                    sendTask.idleMode.set(false);
                    readTask.idleMode.set(false);
                    setState(State.READ);
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
