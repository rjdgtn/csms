package rjdgtn.csms;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewDebug;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import android.util.Log;
import java.lang.reflect.Method;


public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener  {

    static final int PICK_CONTACT_FOR_SMS_REQUEST = 1;  // The request code
    // Used to load the 'native-lib' library on application startup.
    TreeSet<String> logChannels = new TreeSet<String>();
    short curSignalDuration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        //new AudioTest().forceRouteHeadset(false);

//        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        am.setRouting();

//        Sms sms = new Sms();
//        sms.number = "number";
//        sms.text = "text";
//        try {
//            sms.toBytes();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        SmsUtils.Sms sms = SmsUtils.getMinInboxWithIndexGreater(getApplicationContext(), 151);
//        int i = 0;
        //SmsUtils.getAllSmsMessages(getApplicationContext());
        //SmsStorage.updateMaxSmsInboxId(getApplicationContext(), 123);
        //int kind, int id, long date, String number, String text
        //SmsStorage.saveSms(getApplicationContext(), 1, 1, System.currentTimeMillis(), "+71234567890", "Сообщение сообщение");
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
        if (itemName.equals("echo")) {
            showEchoEdit("echo", new StringCallback() {
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
        } else if (itemName.equals("config_speed") || itemName.equals("config_speed_local")) {
            final boolean local = itemName.equals("config_speed_local");
            PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.switch1));
            popupMenu.inflate(R.menu.speed_config);
            for (int i = 0; i < TransportTask.signalDurations.length; i++) {
                int dur = TransportTask.signalDurations[i];
                popupMenu.getMenu().add("" + dur).setCheckable(true).setChecked(dur == curSignalDuration);
            }
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(final MenuItem item) {
//                    if (logChannels.contains(item.getTitle())) logChannels.remove(item.getTitle());
//                    else logChannels.add(item.getTitle().toString());
                    WorkerService.send(getApplicationContext()
                            , new HashMap<String, String>() {{
                                put("code", (local ? "config_speed_local" : "config_speed"));
                                put("value", item.getTitle().toString());
                            }});
                    return true;
                }
            });

            popupMenu.show();
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
            Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            pickContact.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            startActivityForResult(pickContact, PICK_CONTACT_FOR_SMS_REQUEST);
        } else if (itemName.equals("get_sms")) {
            showEchoEdit(""+(SmsStorage.getMaxSmsInboxId()+1), new StringCallback() {
                @Override
                public void on(final String str) {
                    if (!str.isEmpty()) {
                        WorkerService.send(getApplicationContext()
                                , new HashMap<String, String>() {{
                                    put("code", "get_sms");
                                    put("smsId", str);
                                }});
                    }
                }
            });
        } else if (itemName.equals("wake") ||
                itemName.equals("idle") ||
                itemName.equals("check_sms_local") ||
                itemName.equals("status") ||
                itemName.equals("reboot_remote") ||
                itemName.equals("restart_remote") ||
                itemName.equals("test") ||
                itemName.equals("ringup_local_start") ||
                itemName.equals("ringup_local_stop") ||
                itemName.equals("ringup_remote_start") ||
                itemName.equals("ringup_remote_stop") ) {
            WorkerService.send(getApplicationContext(), new HashMap<String, String>() {{ put("code",itemName); }});
        }
//        else if (itemName.equals("ringup_local") || itemName.equals("ringup_remote")) {
//
//        }

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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) return;
        Uri contactData = data.getData();
        Cursor c = getContentResolver().query(contactData, null, null, null, null);
        if (c.moveToFirst()) {
            int phoneIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String num = c.getString(phoneIndex);

            showSmsEdit("sms", num, new SmsInputCallback() {
                @Override
                public void on(final String number, final String text) {
                    if (number.isEmpty()) {
                        Toast.makeText(getBaseContext(), "no number",Toast.LENGTH_SHORT).show();
                    } else if (text.isEmpty()) {
                        Toast.makeText(getBaseContext(), "no text",Toast.LENGTH_SHORT).show();
                    } else {
                        WorkerService.send(getApplicationContext()
                                , new HashMap<String, String>() {{
                                    put("code", "send_sms");
                                    put("number", number);
                                    put("text", text);
                                }});
                    }
                }
            });

            //Toast.makeText(MainActivity.this, "Number=" + num, Toast.LENGTH_LONG).show();
        }
    }

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
    protected void showEchoEdit(String txt, final StringCallback okClick ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setMinLines(3);
        input.setText(txt);
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

    interface SmsInputCallback {
        public void on(String number, String text);
    }
    protected void showSmsEdit(String title, String number, final SmsInputCallback okClick ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.sms_input, null);
        ((EditText)dialogView.findViewById(R.id.editText6)).setText(number);
        builder.setView(dialogView);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String number = ((EditText) dialogView.findViewById(R.id.editText6)).getText().toString();
                String text = ((EditText) dialogView.findViewById(R.id.editText7)).getText().toString();
                okClick.on(number, text);
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

//    public class AudioTest {
//        private final String TAG = "AudioTest";
//        // Constants copied from AudioSystem
//        private static final int DEVICE_IN_WIRED_HEADSET    = 0x400000;
//        private static final int DEVICE_OUT_EARPIECE        = 0x1;
//        private static final int DEVICE_OUT_WIRED_HEADSET   = 0x4;
//        private static final int DEVICE_STATE_UNAVAILABLE   = 0;
//        private static final int DEVICE_STATE_AVAILABLE     = 1;
//
//        /* force route function through AudioSystem */
//        private void setDeviceConnectionState(final int device, final int state, final String address) {
//            try {
//                Class<?> audioSystem = Class.forName("android.media.AudioSystem");
//                Method setDeviceConnectionState = audioSystem.getMethod(
//                        "setDeviceConnectionState", int.class, int.class, String.class);
//
//                setDeviceConnectionState.invoke(audioSystem, device, state, address);
//            } catch (Exception e) {
//                Log.e(TAG, "setDeviceConnectionState failed: " + e);
//            }
//        }
//
//        public void forceRouteHeadset(boolean enable) {
//            if (enable) {
//                Log.i(TAG, "force route to Headset");
//                setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
//                setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
//            } else {
//                Log.i(TAG, "force route to Earpirce");
//                setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
//                setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
//                setDeviceConnectionState(DEVICE_OUT_EARPIECE, DEVICE_STATE_AVAILABLE, "");
//            }
//        }
//    }
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
