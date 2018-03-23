package rjdgtn.csms;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class LauncherService extends Service {
    public static void start(Context context) {
        Intent intent = new Intent(context, LauncherService.class);
        context.startService(intent);
    }

    public LauncherService() {

    }

    public void onCreate() {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("tatata")
                .setContentText("lalala");
        Notification notification = builder.build();
        startForeground(777, notification);

    }
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
