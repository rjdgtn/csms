package rjdgtn.csms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import com.klinker.android.send_message.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SmsUtils  {
//    public static class Sms {
//        int id = 0;
//        int kind = 0;
//        long date = 0;
//        String number = null;
//        String text = null;
//    }

    private static long enableAirplaneModeTime = 0;

    public static void disableAirplaneForSeconds(Context context, int duration) {
        AirplaneMode.setFlightMode(context, false);
        enableAirplaneModeTime = Math.max(enableAirplaneModeTime, System.currentTimeMillis() + duration * 1000);
    }

    public static void enableAirplane(Context context) {
        AirplaneMode.setFlightMode(context, true);
        enableAirplaneModeTime = 0;
    }

    public static void update(Context context) {
        if (enableAirplaneModeTime > 0 && enableAirplaneModeTime < System.currentTimeMillis()) {
            enableAirplaneModeTime = 0;
            AirplaneMode.setFlightMode(context, true);
        }

        Calendar calendar = Calendar.getInstance();
        int minutes =  calendar.get(calendar.MINUTE);
        int hours =  calendar.get(calendar.HOUR_OF_DAY);

        if ((hours == 9 || hours == 12 || hours == 16 || hours == 19 || hours == 23) && minutes < 5 && AirplaneMode.isFlightModeEnabled(context)) {
            disableAirplaneForSeconds(context, 10 * 60);
        }
    }

    public static Sms getMinInboxWithIndexGreater(Context context, int id) {
        String where = String.format("%s >= %d", Telephony.Sms._ID, id);
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, where, null, Telephony.Sms._ID);
        Sms res = null;
        try {
            if (cursor.moveToFirst()) {
                do {
                    res = new Sms();
                    res.id = (short)cursor.getInt(cursor.getColumnIndex(Telephony.Sms._ID));
                    res.kind = (byte)cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                    res.date = (int)(cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE))/1000);
                    res.number = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                    res.text = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                    break;
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return res;
    }

    public static short send(Context context, String number, String message) throws InterruptedException {
        disableAirplaneForSeconds(context, 55 + 55 + 5 * 10);

        for (int i = 0; i < 10; i++) {
            Thread.sleep((i+1)*1000);
            if (MyPhoneStateListener.getSignalStrength() > 0) break;
        }

        long sendTime = System.currentTimeMillis();

        short smsId = 0;
        for (int i = 0; i < 10; i++) {
            Settings settings = new Settings();
           // settings.setUseSystemSending(true);
            Transaction sendTransaction = new Transaction(context, settings);
            Message messageObj = new Message(message, number);
            sendTransaction.sendNewMessage(messageObj, Thread.currentThread().getId());

            Thread.sleep(5000 + (i+1)*1000);
            String where = String.format("%s=\"%s\" AND %s=\"%s\" AND %s>%d"
                    , Telephony.Sms.BODY, message
                    , Telephony.Sms.ADDRESS, number
                    , Telephony.Sms.DATE, sendTime);
            //String where = String.format("%s=\"%s\"", Telephony.Sms.BODY, message, Telephony.Sms.ADDRESS, number);
            String[] columns = {Telephony.Sms._ID};
            Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms"), null, where, null, Telephony.Sms._ID);

            try {
                if (cursor.moveToFirst()) {
                    do {
                        smsId = (short)Math.max(smsId, cursor.getInt(cursor.getColumnIndex(Telephony.Sms._ID)));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
            if (smsId > 0) {
                enableAirplane(context);
                break;
            }
        }

        return smsId;
    }

    static void activateReceiver(Context context, boolean activate) {
        if (smsMonitor == null) smsMonitor = new SMSMonitor();
        if (activate) context.registerReceiver(smsMonitor, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        else context.unregisterReceiver(smsMonitor);
    }

    static private SMSMonitor smsMonitor = null;

    static public class SMSMonitor extends BroadcastReceiver {
        private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent intent2 = new Intent(context, WorkerService.class);
            intent2.putExtra("code", "new_sms");
            context.startService(intent2);
        }
    }
}
