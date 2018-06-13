package rjdgtn.csms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

public class SmsUtils {
    public static void getAllSmsMessages(Context context) {
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/sent"), null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    int threadId = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.THREAD_ID));
                    String messageId = cursor.getString(cursor.getColumnIndex(Telephony.Sms._ID));
                    String message = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                    int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                    //App.getDataBaseManager().saveMessage(new SmsMmsMessage(threadId, messageId,
                     //       message, type, date, null, null));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
    }
}
