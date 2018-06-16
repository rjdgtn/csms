package rjdgtn.csms;

import android.content.Context;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Telephony;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

public class SmsStorage {
    static void saveSms(Context context/*, int kind, int id, long date, String number, String text*/) {//} throws IOException {
//        if (kind == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX) {
//
//        }
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
                PrintWriter out = new PrintWriter(file);
                out.write(Integer.toString(id));
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
