package rjdgtn.csms;

import android.content.Context;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Telephony;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SmsStorage {
    static void save(Context context, Sms sms) {
        save(context, sms.id, sms.kind, sms.date, sms.number, sms.text);
    }

    static void save(Context context, short id, byte kind, int date, String number, String text) {//} throws IOException {
        boolean isInbox = (kind == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);

        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date((long)date*1000));
        String str = new String();
        str += ("\n");
        str += ((isInbox ? "in " : "out ") + Integer.toString(id) + "\n");
        str += (dateStr + "\n");
        str += (number + "\n");
        str += (text + "\n");

        writeToStorage(str);

        if (isInbox) {
            updateMaxSmsInboxId(context, id);
        }
    }

    static void writeToStorage(String str) {
        try {
            File file = new File(Environment.getExternalStorageDirectory()+ "/CSMS/history.txt");
            if (!file.exists()) {
                File directory = new File(Environment.getExternalStorageDirectory() + "/CSMS");
                directory.mkdirs();
                file.createNewFile();
            }

            FileWriter out = new FileWriter(file, true);
            out.write(str);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static int getMaxSmsInboxId() {
        int maxId = 0;
        BufferedReader br = null;
        try {
            try {
                br = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory()+ "/CSMS/maxInboxId.txt"));
                maxId = Integer.parseInt(br.readLine());
            } finally {
                if (br != null) br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return maxId;
    }

    static void updateMaxSmsInboxId(Context context, int id) {
        int maxId = getMaxSmsInboxId();
        if (id > maxId) {
            try {
                File file = new File(Environment.getExternalStorageDirectory()+ "/CSMS/maxInboxId.txt");
                if (!file.exists()) {
                    File directory = new File(Environment.getExternalStorageDirectory() + "/CSMS");
                    directory.mkdirs();
                    file.createNewFile();
                }
                FileWriter out = new FileWriter(file);
                out.write(Integer.toString(id));
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
