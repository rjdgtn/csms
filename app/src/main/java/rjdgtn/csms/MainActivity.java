package rjdgtn.csms;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
        logChannels.add("TRPT");

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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String itemName = item.getTitle().toString();
        if (itemName.equals("reboot_remote") || itemName.equals("test")) {
            WorkerService.send(getApplicationContext(), new HashMap<String, String>() {{ put("code", itemName); }});
        } else if (itemName.equals("echo")) {
            showEdit("echo", new StringCallback() {
                @Override
                public void on(final String str) {
                    if (!str.isEmpty()) {
                        WorkerService.send(getApplicationContext()
                                , new HashMap<String, String>() {{
                                    put("code", "echo");
                                    put("msg", str);
                                }});
                    }
                }
            });
        } else if (itemName.equals("logs")) {
            PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.workerStatus));
            popupMenu.inflate(R.menu.logs_menu);
            for (int i = 0; i < popupMenu.getMenu().size(); i++) {
                boolean enbld = logChannels.contains(popupMenu.getMenu().getItem(i).getTitle());
                popupMenu.getMenu().getItem(i).setChecked(enbld);
            }
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (logChannels.contains(item.getTitle())) logChannels.remove(item.getTitle());
                    else logChannels.add(item.getTitle().toString());
                    return true;
                }
            });

            popupMenu.show();
        } else if (itemName.equals("clear")) {
            TextView tv = ((TextView) findViewById(R.id.textView1));
            tv.setText("");
        }
        return true;
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
       // LocalBroadcastManager.getInstance(this)
        this.registerReceiver(mMessageReceiver, new IntentFilter("csms_log"));
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
                //tv.setText(channel + ": " + log + "\n" + tv.getText());
                String text = tv.getText().toString();

                ArrayList<String> lines = new ArrayList<String>(Arrays.asList(text.split("\r\n|\r|\n")));
                while(lines.size() > 100) lines.remove(lines.size()-1);

                lines.add(0, channel + ": " + log);

                String listString = "";
                for (String s : lines) {
                    listString += s + "\n";
                }
                tv.setText(listString);
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
    interface StringCallback {
        public void on(String str);
    }
    protected void showEdit(String title, final StringCallback okClick ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        final EditText input = new EditText(this);
        input.setMinLines(3);
       // input.setHeight(200);

        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                okClick.on(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
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
