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

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        status.power = (byte) Math.min(255, (batteryPct * 100));

        status.uptime = (byte) Math.min(255, (System.currentTimeMillis() - WorkerService.launchTime) / (1000 * 30 * 60));

        return status;
    }

    Status () {

    }

    Status (ByteBuffer buffer) {
        uptime = buffer.get();
        power = buffer.get();
        gsm = buffer.get();
        byte v = buffer.get();
        wifi = (v & 0b1) > 0;
        bluetooth = (v & 0b10) > 0;
        location = (v & 0b100) > 0;
        airplane = (v & 0b1000) > 0;
    }

    byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(uptime);
        buffer.put(power);
        buffer.put(gsm);
        buffer.put((byte)(0 | (wifi ? 0b1 : 0b0) | (bluetooth ? 0b10 : 0b00) | (location ? 0b100 : 0b000) | (airplane ? 0b1000 : 0b0000)));

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

    byte uptime = 0; //halfhours
    byte power = 0;
    byte gsm = 0;
    boolean wifi = false;
    boolean bluetooth = false;
    boolean location = false;
    boolean airplane = false;
}
