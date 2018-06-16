package rjdgtn.csms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.klinker.android.send_message.SentReceiver;

public class MySentReceiver extends SentReceiver {
    @Override
    public void onMessageStatusUpdated(Context context, Intent intent, int i) {
        Uri uri = Uri.parse(intent.getStringExtra("message_uri"));
       // int p = 0;
    }
}
