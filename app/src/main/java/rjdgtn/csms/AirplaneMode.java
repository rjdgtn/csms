package rjdgtn.csms;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;

public class AirplaneMode {
    //https://stackoverflow.com/questions/25674655/how-to-turn-on-off-airplane-mode-even-on-new-android-versions-and-even-with-ro?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
    //To toggle Airplane / Flight mode on and off on an Android rooted device (phone, tablet, note), you can do the following:

    private static void executeCommandWithoutWait(Context context, String option, String command) {
        boolean success = false;
        String su = "su";
        for (int i=0; i < 3; i++) {
            // "su" command executed successfully.
            if (success) {
                // Stop executing alternative su commands below.
                break;
            }
            if (i == 1) {
                su = "/system/xbin/su";
            } else if (i == 2) {
                su = "/system/bin/su";
            }
            try {
                // execute command
                Runtime.getRuntime().exec(new String[]{su, option, command});
            } catch (IOException e) {
                Log.e("CSMS", "su command has failed due to: " + e.fillInStackTrace());
            }
        }
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static boolean isFlightModeEnabled(Context context) {
        boolean mode = false;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            // API 17 onwards
            mode = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        } else {
            // API 16 and earlier.
            mode = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
        }
        return mode;
    }

    private static final String COMMAND_FLIGHT_MODE_1 = "settings put global airplane_mode_on";
    private static final String COMMAND_FLIGHT_MODE_2 = "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state";

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void setFlightMode(Context context, boolean on) {
        int enable = isFlightModeEnabled(context) ? 0 : 1;
        if (on == (enable == 0)) {
            return;
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            String command = COMMAND_FLIGHT_MODE_1 + " " + enable;
            executeCommandWithoutWait(context, "-c", command);
            command = COMMAND_FLIGHT_MODE_2 + " " + enable;
            executeCommandWithoutWait(context, "-c", command);
        } else {
            // API 16 and earlier.
            Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, enable );
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", enable == 1);
            context.sendBroadcast(intent);
        }
    }
}
