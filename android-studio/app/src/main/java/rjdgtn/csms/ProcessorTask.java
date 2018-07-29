package rjdgtn.csms;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.getAllStackTraces;
import static java.lang.Thread.sleep;

/**
 * Created by Petr on 25.03.2018.
 */

public class ProcessorTask implements Runnable {
//    public BlockingQueue<Request> outQueue;

    Context contex;
    //CommandProcessor curProcessor = null;
    public static BlockingQueue<Bundle> localCommands = new LinkedBlockingQueue<Bundle>();

    private final String ENCODING = "UTF-8";

    private final byte NO_COMMAND = 000;
    private final byte SKIP_COMMAND = 1;
    private final byte ECHO_COMMAND = 11;
    private final byte CONFIG_SPEED_COMMAND = 22;
    private final byte STATUS_REQUEST_COMMAND = 33;
    private final byte STATUS_ANSWER_COMMAND = 44;
    private final byte CHECK_SMS_REQUEST_COMMAND = 55;
    //private final byte CHECK_SMS_ANSWER_COMMAND = 66;
    private final byte GET_SMS_REQUEST_COMMAND = 77;
    private final byte GET_SMS_ANSWER_COMMAND = 88;
    private final byte SEND_SMS_REQUEST_COMMAND = 99;
    private final byte SEND_SMS_ANSWER_COMMAND = 111;
    private final byte REBOOT_COMMAND = 122;


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
        intent.putExtra("tm", System.currentTimeMillis());
        contex.sendBroadcast(intent);
    }

    @Override
    public void run() {
        log("start");
        try {
            SmsUtils.activateReceiver(contex, true);
            while (true) {
                if (!TransportTask.inQueue.isEmpty()) {
                    onRemoteCommand(TransportTask.inQueue.take());
                } else if (!localCommands.isEmpty()) {
                    onLocalCommand(localCommands.take());
                }
                SmsUtils.update(contex);

                if (WorkerService.idleMode.get()) Thread.sleep(5000);
                else Thread.sleep(500);
            }
        } catch (Exception e) {
            log("crash");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        } finally {
            SmsUtils.activateReceiver(contex, false);
        }

    }

    private void sendToRemotePhone(byte[] bytes) throws InterruptedException, IOException {
        if (true) {
            TransportTask.outQueue.put(new OutRequest(bytes));
        } else {
            onRemoteCommand(bytes);
        }
    }

    private void onLocalCommand(Bundle command) throws InterruptedException, IOException {
        String code = command.getString("code");
        log("command " + code);
        if (code.equals("reboot_remote")) onLocalRebootRemote(command);
        else if (code.equals("restart_remote")) onLocalRestartRemote(command);
        else if (code.equals("test")) onLocalTest(command);
        else if (code.equals("echo")) onLocalEcho(command);
        else if (code.equals("config_speed")) onLocalConfigSpeed(command);
        else if (code.equals("status")) onLocalStatus(command);
        else if (code.equals("wake")) onLocalWake(command);
        else if (code.equals("check_sms")) onLocalCheckSms(command);
        else if (code.equals("send_sms")) onLocalSendSms(command);
        else if (code.equals("get_sms")) onLocalGetSms(command);
        else if (code.equals("new_sms")) onLocalNewSms(command);
        else if (code.equals("idle")) onLocalIdle(command);
        else if (code.equals("check_sms_local")) onLocalCheckSmsLocal(command);

        lastLocalCommand = command;
    }

    private void onRemoteCommand(byte[] data) throws InterruptedException, IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        if (inputStream.available() == 0) return;
        byte code = (byte)inputStream.read();
        if (code == ECHO_COMMAND) onRemoteEcho(inputStream);
        if (code == CONFIG_SPEED_COMMAND) onRemoteConfigSpeed(inputStream);
        if (code == FAIL_COMMAND) onRemoteFail();
        if (code == SUCCESS_COMMAND) onRemoteSuccess();
        if (code == STATUS_REQUEST_COMMAND) onRemoteStatusRequest(inputStream);
        if (code == STATUS_ANSWER_COMMAND) onRemoteStatusAnswer(inputStream);
        if (code == CHECK_SMS_REQUEST_COMMAND) onRemoteCheckSms(inputStream);
        if (code == SEND_SMS_REQUEST_COMMAND) onRemoteSendSms(inputStream);
        if (code == SEND_SMS_ANSWER_COMMAND) onRemoteSendSmsAnswer(inputStream);
        if (code == GET_SMS_REQUEST_COMMAND) onRemoteGetSms(inputStream);
        if (code == GET_SMS_ANSWER_COMMAND) onRemoteGetSmsAnswer(inputStream);
        if (code == REBOOT_COMMAND) onRemoteReboot(inputStream);
    }

    private void onLocalCheckSmsLocal(Bundle command) throws InterruptedException {
        SmsUtils.disableAirplaneForSeconds(contex, 5 * 60);
    }

    private void onLocalNewSms(Bundle command) throws InterruptedException {
        TransportTask.outQueue.put(new OutRequest("new_sms"));
    }

    private void onLocalWake(Bundle command) throws InterruptedException {
        TransportTask.outQueue.put(new OutRequest("wake"));
    }

    private void onLocalIdle(Bundle command) throws InterruptedException {
        TransportTask.outQueue.put(new OutRequest("idle"));
    }

    private void onRemoteGetSmsAnswer(ByteArrayInputStream stream) throws InterruptedException, IOException {
        if (stream.available() == 0) {
            log("no new sms");
        } else {
            byte[] buf = new byte[stream.available()];
            stream.read(buf);
            ByteBuffer bbuf =  ByteBuffer.wrap(buf, 0, buf.length);
            Sms sms = new Sms((byte)Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX, bbuf);
            SmsStorage.save(contex, sms);
            
            String str = new String();
            str += ("new sms\n");
            str += (Integer.toString(sms.id) + "\n");
            str += (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date((long)sms.date*1000)) + "\n");
            str += (sms.number + "\n");
            str += (sms.text + "\n");
            log(str);
        }
    }

    private void onRemoteGetSms(ByteArrayInputStream stream) throws InterruptedException, IOException {
        if (stream.available() == 0) return;
        short smsId = getShort(stream);
        Sms sms = SmsUtils.getMinInboxWithIndexGreater(contex, smsId);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(GET_SMS_ANSWER_COMMAND);
        if (sms != null) {
            outputStream.write(sms.toBytes());
        }
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onLocalGetSms(Bundle command) throws InterruptedException, IOException {
        String smsId = command.getString("smsId");
        Short smsIdShort = Short.parseShort(smsId);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(GET_SMS_REQUEST_COMMAND);
        putShort(outputStream, smsIdShort);
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onRemoteSendSmsAnswer(ByteArrayInputStream stream) throws InterruptedException, IOException {
        if (stream.available() == 0) return;
        ByteBuffer buf;
        short smsId = getShort(stream)  ;
        if (smsId > 0) {
            log("sms sended: " + smsId);
            SmsStorage.writeToStorage("sms sended:" + smsId + "\n");
        } else {
            log("sms was not send ");
            SmsStorage.writeToStorage("sms was not send" + "\n");
        }
    }

    private void onLocalSendSms(Bundle command) throws InterruptedException, IOException {
        String number = command.getString("number");
        String text = command.getString("text");

        SmsStorage.save(contex, (short)0, (byte)Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT, (int)(System.currentTimeMillis()/1000), number, text);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(SEND_SMS_REQUEST_COMMAND);
        outputStream.write(number.length());
        outputStream.write(number.getBytes(ENCODING));
        outputStream.write(text.getBytes(ENCODING));
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onRemoteSendSms(ByteArrayInputStream stream) throws InterruptedException, IOException {
        if (stream.available() == 0) return;
        byte numberLength = (byte)stream.read();
        String number = readString(stream, numberLength);
        String message = readString(stream, 0);

        log("sms sending");
        log("number: " + number);
        log("message: " + message);

        short smsId = SmsUtils.send(contex, number, message);
        if (smsId > 0) log("sms sended");
        else log("sms not sended");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(SEND_SMS_ANSWER_COMMAND);
        putShort(outputStream, smsId);
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onLocalCheckSms(Bundle command) throws InterruptedException, IOException {
        byte duration = (byte)Integer.parseInt(command.getString("duration"));
        log("local request check sms in " + duration + " minutes");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(CHECK_SMS_REQUEST_COMMAND);
        outputStream.write(duration);
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onRemoteCheckSms(ByteArrayInputStream stream) throws IOException, InterruptedException {
        byte duration = (byte)stream.read();
        SmsUtils.disableAirplaneForSeconds(contex, duration * 60);
    }

    private void onLocalStatus(Bundle command) throws InterruptedException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(STATUS_REQUEST_COMMAND);
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onRemoteStatusRequest(ByteArrayInputStream stream) throws IOException, InterruptedException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(STATUS_ANSWER_COMMAND);
        outputStream.write(Status.make(contex).toBytes()) ;
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onRemoteStatusAnswer(ByteArrayInputStream stream) throws IOException {
        byte[] buf = new byte[stream.available()];
        stream.read(buf);
        ByteBuffer bbuf =  ByteBuffer.wrap(buf, 0, buf.length);
        Status status = new Status(bbuf);

        log("\tStatus:");
        log("\tuptime: " + status.uptime / 2 + " hours");
        log("\tpower: " + status.power);
        log("\tgsm: " + status.gsm + " " + status.getGsmLevelStrign());
        log("\tgps: " + status.location);
        log("\twifi: " + status.wifi);
        log("\tcharging: " + status.charging);
        log("\tbluetooth: " + status.bluetooth);
    }

    private void onLocalConfigSpeed(Bundle command) throws InterruptedException, IOException {
        short duration = (short)Integer.parseInt(command.getString("value"));
        log("local request change call duration to " + duration );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(CONFIG_SPEED_COMMAND);
        putShort(outputStream, duration);
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onRemoteConfigSpeed(ByteArrayInputStream stream) throws InterruptedException, IOException {
        if (stream.available() == 0) return;
        short duration = getShort(stream);
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

    private void onLocalRestartRemote(Bundle command) throws InterruptedException {
        TransportTask.outQueue.put(new OutRequest("reboot_remote"));
    }

    private void onLocalRebootRemote(Bundle command) throws InterruptedException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(REBOOT_COMMAND);
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onLocalTest(Bundle command) throws InterruptedException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(SKIP_COMMAND);
        outputStream.write(new byte[] {0,1,2,3,4,5,6,7,8,9});
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onLocalEcho(Bundle command) throws InterruptedException, IOException {
        String msg = command.getString("msg");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(ECHO_COMMAND);
        outputStream.write(10);
        outputStream.write(msg.getBytes(ENCODING));
        sendToRemotePhone(outputStream.toByteArray());
    }

    private void onRemoteEcho(ByteArrayInputStream stream) throws InterruptedException, IOException {
        if (stream.available() == 0) return;
        byte echoStep = (byte)stream.read();
        String msg = readString(stream, 0);
        log("echo: " + msg);
        if (echoStep > 0) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(ECHO_COMMAND);
            outputStream.write(echoStep-1);
            outputStream.write(msg.getBytes(ENCODING));
            sendToRemotePhone(outputStream.toByteArray());
        }
    }

    private void onRemoteReboot(ByteArrayInputStream stream) throws IOException, InterruptedException {
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
        } catch (Exception e) {

        }
    }

    private String readString(ByteArrayInputStream stream, int length) throws IOException {
        if (length == 0) length = stream.available();
        byte[] stringBuf = new byte[length];
        stream.read(stringBuf);
        return new String(stringBuf, ENCODING);
    }

    private void putShort(ByteArrayOutputStream stream, short s) throws IOException {
        stream.write(ByteBuffer.allocate(2).putShort(s).array());
    }

    private short getShort(ByteArrayInputStream stream) throws IOException {
        byte[] b = new byte[2];
        stream.read(b);
        return ByteBuffer.wrap(b).getShort();
    }
}


