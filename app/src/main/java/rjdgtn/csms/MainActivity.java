package rjdgtn.csms;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener  {

    // Used to load the 'native-lib' library on application startup.
    TreeSet<String> logChannels = new TreeSet<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DtmfPacking.pack(new byte[]{1,2,3,4,5});
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logChannels.add("PRCR");

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText();
//        stringFromJNI();
        ((TextView) findViewById(R.id.textView1)).setMovementMethod(new ScrollingMovementMethod());
        ((Switch)findViewById(R.id.switch1)).setOnCheckedChangeListener(this);
        ((Switch)findViewById(R.id.switch1)).setChecked(LauncherService.isRunning(getApplicationContext()));

//        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.floatingActionButton);
//        registerForContextMenu(button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.your_context_menu, menu);
        return true;
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals("test")) {
            try {
                OutRequest r = new OutRequest();
                r.data = new byte[]{1,2,3,4,5};
                TransportTask.outQueue.put(r);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            if (logChannels.contains(item.getTitle())) logChannels.remove(item.getTitle());
            else logChannels.add(item.getTitle().toString());
            return true;
        }
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

    @Override
    public void onResume() {
        super.onResume();
        // This registers mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mMessageReceiver,
                        new IntentFilter("csms_log"));
    }

    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String log = intent.getStringExtra("log");
            String channel = intent.getStringExtra("ch");

            if (logChannels.contains(channel)) {
                TextView tv = ((TextView) findViewById(R.id.textView1));
                tv.setText(channel + ": " + log + "\n" + tv.getText());
            }

        }
    };

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mMessageReceiver);
        super.onPause();
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
