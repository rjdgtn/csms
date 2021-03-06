package rjdgtn.csms;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.telephony.TelephonyManager.*;

public class Status implements Serializable {
    static Status make(Context context) {
        Status status = new Status();

        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        status.wifi = wifiManager.isWifiEnabled();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) status.bluetooth = bluetoothAdapter.isEnabled();

        LocationManager locationManager = (LocationManager)context.getSystemService( Context.LOCATION_SERVICE );
        if (locationManager != null)  status.location = locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );

        if (Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0) {
            status.gsm = 0;
        } else {
            status.gsm = MyPhoneStateListener.getSignalStrength();
        }

        status.airplane = AirplaneMode.isFlightModeEnabled(context);

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        status.sim = (byte)(telephonyManager != null ? telephonyManager.getSimState() : 0);

        status.inbox = (short)SmsUtils.getMaxInbox(context);

        status.lastInService =  (byte) Math.min(255, (System.currentTimeMillis() - MyPhoneStateListener.getLastInService()) / (1000 * 30 * 60));

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        status.power = (byte) Math.min(255, (batteryPct * 100));

        int voltage = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) : -1;

        status.voltage = voltage/10;

        status.uptime = (byte) Math.min(255, (System.currentTimeMillis() - WorkerService.launchTime) / (1000 * 30 * 60));

        status.charging = Power.isConnected(context);

        return status;
    }

    Status () {

    }

    Status (ByteBuffer buffer) {
        uptime = buffer.get();
        power = buffer.get();
        byte voltageByte = buffer.get();
        voltage = ((voltageByte) & 0xFF) * 2;
        gsm = buffer.get();
        sim = buffer.get();
        lastInService = buffer.get();
        inbox = buffer.getShort();
        byte v = buffer.get();
        wifi = (v & 0b1) > 0;
        bluetooth = (v & 0b10) > 0;
        location = (v & 0b100) > 0;
        airplane = (v & 0b1000) > 0;
        charging = (v & 0b10000) > 0;
    }

    byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(uptime);
        buffer.put(power);
        buffer.put((byte)(voltage/2));
        buffer.put(gsm);
        buffer.put(sim);
        buffer.put(lastInService);
        buffer.putShort(inbox);
        buffer.put((byte)(0 | (wifi ? 0b1 : 0b0)
                            | (bluetooth ? 0b10 : 0b00)
                            | (location ? 0b100 : 0b000)
                            | (airplane ? 0b1000 : 0b0000)
                            | (charging ? 0b10000 : 0b00000)));

        return buffer.array();
    }

    String getGsmLevelStrign() {
        String gsmLevel = "";
        int asu = gsm;
        if (asu <= 2 || asu == 99) gsmLevel = "none/unknown";
        else if (asu >= 12) gsmLevel = "great";
        else if (asu >= 8)  gsmLevel = "good";
        else if (asu >= 5)  gsmLevel = "moderate";
        else gsmLevel = "poor";
        return gsmLevel;
    }

    String getSimStrign() {
        if (sim == SIM_STATE_UNKNOWN) return "UNKNOWN";
        if (sim == SIM_STATE_ABSENT) return "ABSENT";
        if (sim == SIM_STATE_PIN_REQUIRED) return "PIN_REQUIRED";
        if (sim == SIM_STATE_PUK_REQUIRED) return "PUK_REQUIRED";
        if (sim == SIM_STATE_NETWORK_LOCKED) return "NETWORK_LOCKED";
        if (sim == SIM_STATE_READY) return "READY";
        if (sim == SIM_STATE_NOT_READY) return "NOT_READY";
        if (sim == SIM_STATE_PERM_DISABLED) return "READY";
        if (sim == SIM_STATE_READY) return "PERM_DISABLED";
        if (sim == SIM_STATE_CARD_IO_ERROR) return "CARD_IO_ERROR";
        else return "invalid value";
    }

    List<String> description() {
        List<String> list = new ArrayList<String>();

        list.add("\tStatus:");
        list.add("\tuptime: " + (uptime & 0xFF) / 2.0 + " hours");
        list.add("\tpower: " + power);
        list.add("\tvoltage: " + voltage);
        list.add("\tgsm: " + gsm + " " + getGsmLevelStrign());
        list.add("\tsim: " + sim + " " + getSimStrign());
        list.add("\tin service: " + (lastInService & 0xFF) / 2.0 + " hours ago");
        list.add("\tinbox: " + inbox + " " + (inbox > SmsStorage.getMaxSmsInboxId() ? "NEW" : "readed"));
        list.add("\tgps: " + location);
        list.add("\twifi: " + wifi);
        list.add("\tcharging: " + charging);
        list.add("\tbluetooth: " + bluetooth);

        Collections.reverse(list);

        return list;
    }

    byte uptime = 0; // halfhours
    byte power = 0;
    int voltage = 0;
    byte gsm = 0;
    byte sim = 0;
    byte lastInService = 0; // halfhours
    short inbox = 0;
    boolean wifi = false;
    boolean bluetooth = false;
    boolean location = false;
    boolean airplane = false;
    boolean charging = false;



    public static class Power {
        public static boolean isConnected(Context context) {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        }
    }
}
