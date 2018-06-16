package rjdgtn.csms;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.klinker.android.send_message.DeliveredReceiver;

public class MyDeliveredReceiver extends DeliveredReceiver {
    public MyDeliveredReceiver() {
    }
    @Override
    public void onMessageStatusUpdated (Context context, Intent intent, int i) {

        Uri uri = Uri.parse(intent.getStringExtra("message_uri"));
        int k = 0;
    }
}
