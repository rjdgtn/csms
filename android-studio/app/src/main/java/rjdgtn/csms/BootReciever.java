package rjdgtn.csms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import rjdgtn.csms.LauncherService;

/**
 * Created by Petr on 24.03.2018.
 */

public class BootReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            if(LauncherService.shouldAutoStart(context)) {
                LauncherService.start(context);
            }
        }
    }
}
