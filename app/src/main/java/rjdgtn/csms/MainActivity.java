package rjdgtn.csms;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener  {

    // Used to load the 'native-lib' library on application startup.
    TreeSet<String> logChannels = new TreeSet<String>();
    short curSignalDuration = 0;

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


        HeadsetPlugReceiver headsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(headsetPlugReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
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
            PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.switch1));
            popupMenu.inflate(R.menu.logs_menu);
            for (int i = 0; i < popupMenu.getMenu().size(); i++) {
                boolean enbld = logChannels.contains(popupMenu.getMenu().getItem(i).getTitle());
                popupMenu.getMenu().getItem(i).setChecked(enbld);
            }
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getTitle().equals("clear")) {
                        TextView tv = ((TextView) findViewById(R.id.textView1));
                        tv.setText("");
                    } else {
                        if (logChannels.contains(item.getTitle()))
                            logChannels.remove(item.getTitle());
                        else logChannels.add(item.getTitle().toString());
                    }
                    return true;
                }
            });

            popupMenu.show();
        } else if (itemName.equals("config_speed")) {
            PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.switch1));
            popupMenu.inflate(R.menu.speed_config);
            for (int i = 0; i < TransportTask.signalDurations.length; i++) {
                int dur = TransportTask.signalDurations[i];
                popupMenu.getMenu().add(""+dur).setCheckable(true).setChecked(dur == curSignalDuration);
            }
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(final MenuItem item) {
//                    if (logChannels.contains(item.getTitle())) logChannels.remove(item.getTitle());
//                    else logChannels.add(item.getTitle().toString());
                    WorkerService.send(getApplicationContext()
                            , new HashMap<String, String>() {{
                                put("code", "config_speed");
                                put("value", item.getTitle().toString());
                            }});
                    return true;
                }
            });

            popupMenu.show();
        } else if (itemName.equals("status")) {
            WorkerService.send(getApplicationContext(), new HashMap<String, String>() {{ put("code", "status"); }});
        } else if (itemName.equals("check_sms")) {
            PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.switch1));
            popupMenu.inflate(R.menu.check_duration);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    WorkerService.send(getApplicationContext()
                            , new HashMap<String, String>() {{
                                put("code", "check_sms");
                                put("duration", item.getTitle().toString());
                            }});
                    return true;
                }
            });

            popupMenu.show();
        } else if (itemName.equals("send_sms")) {
            SmsUtils.getAllSmsMessages(getApplicationContext());
//            ContentValues my_values = new ContentValues(); // hold the message details
//            my_values.put("address", "+79060331180");//sender name
//            my_values.put("body", "some text");
//            my_values.put("read", 0);
//            my_values.put("date", System.currentTimeMillis() - 1000);
//            String path="content://sms/failed";
//            if( getContentResolver().insert(Uri.parse(path), my_values)!=null){
//                Toast.makeText(getBaseContext(), "Successfully Faked!",Toast.LENGTH_SHORT).show();
//
//                getContentResolver().delete(Uri.parse("content://sms/conversations/-1"), null, null);
//            } else {
//                Toast.makeText(getBaseContext(), "Unsuccesful!",Toast.LENGTH_SHORT).show();
//            }
//            String phoneNumber = "+79060331180";
//            String message = "lalala";
//            String readState = "0";
//            String time = Long.toString(System.currentTimeMillis());
//            String folderName = "inbox";
//
//            ContentValues values = new ContentValues();
//            values.put("address", phoneNumber);
//            values.put("body", message);
//            values.put("read", readState); //"0" for have not read sms and "1" for have read sms
//            values.put("date", time);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                Uri uri = Telephony.Sms.Sent.CONTENT_URI;
//                if(folderName.equals("inbox")){
//                    uri = Telephony.Sms.Inbox.CONTENT_URI;
//                }
//                getContentResolver().insert(uri, values);
//            } else {
//                getContentResolver().insert(Uri.parse("content://sms/" + folderName), values);
//            }


        }
        else if (itemName.equals("wake")) {
            WorkerService.send(getApplicationContext(), new HashMap<String, String>() {{ put("code", "wake"); }});
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
//    private boolean isHeadphonesPlugged(){
//        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//        AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
//        for(AudioDeviceInfo deviceInfo : audioDevices){
//            if(deviceInfo.getType()==AudioDeviceInfo.TYPE_WIRED_HEADSET){
//                return true;
//            }
//        }
//        return false;
//    }

    public void onUpdate() {
        ((CheckBox)findViewById(R.id.checkbox1)).setChecked(WorkerService.isRunning(getApplicationContext()));
        //AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        ((CheckBox)findViewById(R.id.checkbox2)).setChecked(connectedMicrophone);//audioManager.isWiredHeadsetOn());
    }

    @Override
    public void onResume() {
        super.onResume();
        // This registers mMessageReceiver to receive messages.
       // LocalBroadcastManager.getInstance(this)
        this.registerReceiver(mLogReceiver, new IntentFilter("csms_log"));
        this.registerReceiver(mTransportPrefReceiver, new IntentFilter("csms_transport"));
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        unregisterReceiver(mLogReceiver);
        unregisterReceiver(mTransportPrefReceiver);
        super.onPause();
    }

    // Handling the received Intents for the "my-integer" event
    private BroadcastReceiver mLogReceiver = new BroadcastReceiver() {
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

    private BroadcastReceiver mTransportPrefReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //short bytesPerPack = intent.getShortExtra("prefs.bytesPerPack", (short)0);
            curSignalDuration = intent.getShortExtra("prefs.signalDuration", (short)0);
            //short confirmWait = intent.getShortExtra("prefs.confirmWait", (short)0);
        }
    };

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

    static boolean connectedMicrophone = false;
    public class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                return;
            }

            boolean connectedHeadphones = (intent.getIntExtra("state", 0) == 1);
            connectedMicrophone = (intent.getIntExtra("microphone", 0) == 1) && connectedHeadphones;

        }
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
