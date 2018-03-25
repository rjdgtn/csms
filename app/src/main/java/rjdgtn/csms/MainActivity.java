package rjdgtn.csms;

import android.app.ActivityManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener  {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());

        ((Switch)findViewById(R.id.switch1)).setOnCheckedChangeListener(this);
        ((Switch)findViewById(R.id.switch1)).setChecked(LauncherService.isRunning(getApplicationContext()));

        TimerTask schedulerTask = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        onUpdate(); // this action have to be in UI thread
                    }
                });
            }
        };
        Timer scheduler = new Timer();
        scheduler.schedule(schedulerTask, 0, 1000);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    protected void OnClick(View view) {
//        TransportTask transportTask = new TransportTask();
//        ProcessorTask processorTask = new ProcessorTask();
//
//        processorTask.execute();
//        transportTask.execute();
        //rjdgtn.csms.LauncherService.start(getApplicationContext());
//        Thread thread;
//        thread.interrupt();
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            rjdgtn.csms.LauncherService.start(getApplicationContext());
        } else {
            rjdgtn.csms.LauncherService.stop(getApplicationContext());
        }
    }

    public void onUpdate() {
        ((CheckBox)findViewById(R.id.checkbox1)).setChecked(WorkerService.isRunning(getApplicationContext()));
    }
}

//class ActivityUpdater extends TimerTask {
//    Context context;
//    CheckWorkerTask(Context c) {
//        context = c;
//    }
//
//    @Override
//    public void run() {
//        if (!WorkerService.isRunning(context)) {
//            WorkerService.start(context);
//        }
//    }
//}
