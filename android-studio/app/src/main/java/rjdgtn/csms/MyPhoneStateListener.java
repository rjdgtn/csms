package rjdgtn.csms;

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MyPhoneStateListener extends PhoneStateListener {

    static protected MyPhoneStateListener signalListner = new MyPhoneStateListener();
    static protected MyPhoneStateListener serviceListner = new MyPhoneStateListener();
    static protected TelephonyManager telephonyManager = null;
    static protected Context context = null;
    static void init(Context c) {
        context = c;
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(MyPhoneStateListener.signalListner, PhoneStateListener.LISTEN_SERVICE_STATE);
        telephonyManager.listen(MyPhoneStateListener.serviceListner, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

    }

    static private boolean inService = false;
    static private byte signalStrength = 0;
    static private long lastInService = 0;
    static public long getLastInService() { return lastInService; }
    static public byte getSignalStrength() { return inService ? signalStrength : 0; }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        MyPhoneStateListener.signalStrength = (byte) signalStrength.getGsmSignalStrength();
        if (inService && MyPhoneStateListener.signalStrength > 0)
            lastInService = System.currentTimeMillis();
        log(context, "signalStrength: " + MyPhoneStateListener.getSignalStrength());
    }

    @Override
    public void onServiceStateChanged (ServiceState serviceState) {
        super.onServiceStateChanged(serviceState);
        inService = (serviceState.getState() == ServiceState.STATE_IN_SERVICE);
        if (inService && signalStrength > 0)
            lastInService = System.currentTimeMillis();
        log(context, "signalStrength: " + MyPhoneStateListener.getSignalStrength());
    }

    private static void log(Context context, String str) {
        Log.d("MY STTS", str);
        Intent intent = new Intent("csms_log");
        intent.putExtra("log", str);
        intent.putExtra("ch", "STTS");
        intent.putExtra("tm", System.currentTimeMillis());
        context.sendBroadcast(intent);
    }
}