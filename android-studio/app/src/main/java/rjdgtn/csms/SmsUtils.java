package rjdgtn.csms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import com.klinker.android.send_message.*;

public class SmsUtils  {


    private static long enableAirplaneModeTime = 0;

    public static void disableAirplaneForTime(Context context, int duration) {
        AirplaneMode.setFlightMode(context, false);
        enableAirplaneModeTime = System.currentTimeMillis() + duration * 60 * 1000;
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
    }

    public static void getAllSmsMessages(Context context) {
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/sent"), null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    //int threadId = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.));
                    String[] str = cursor.getColumnNames();
                    String person = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                    int threadId = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.THREAD_ID));
                    String messageId = cursor.getString(cursor.getColumnIndex(Telephony.Sms._ID));
                    String message = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                    int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                    int i = 0;
                    //App.getDataBaseManager().saveMessage(new SmsMmsMessage(threadId, messageId,
                     //       message, type, date, null, null));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
    }

    public static void getSmsMessages() {

    }

    public static int send(Context context, String number, String message) throws InterruptedException {
        disableAirplaneForTime(context, 55 * 1000 + 55 * 1000 + 5000 * 10);

        for (int i = 0; i < 10; i++) {
            Thread.sleep((i+1)*1000);
            if (MyPhoneStateListener.getSignalStrength() > 0) break;
        }

        long sendTime = System.currentTimeMillis();

        int smsId = 0;
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
                        smsId = Math.max(smsId, cursor.getInt(cursor.getColumnIndex(Telephony.Sms._ID)));
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
}
