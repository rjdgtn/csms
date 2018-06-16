package rjdgtn.csms;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MyPhoneStateListener extends PhoneStateListener {

    static protected MyPhoneStateListener signalListner = new MyPhoneStateListener();
    static protected MyPhoneStateListener serviceListner = new MyPhoneStateListener();
    static protected TelephonyManager telephonyManager = null;

    static void init(Context context) {
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(MyPhoneStateListener.signalListner, PhoneStateListener.LISTEN_SERVICE_STATE);
        telephonyManager.listen(MyPhoneStateListener.serviceListner, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

    }

    static private boolean inService = false;
    static private byte signalStrength = 0;
    static public byte getSignalStrength() { return inService ? signalStrength : 0; }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        MyPhoneStateListener.signalStrength = (byte) signalStrength.getGsmSignalStrength();
        Log.v("MY","signalStrength: " + MyPhoneStateListener.getSignalStrength() );
        //signalStrength.
    }

    @Override
    public void onServiceStateChanged (ServiceState serviceState) {
        super.onServiceStateChanged(serviceState);
        inService = (serviceState.getState() == ServiceState.STATE_IN_SERVICE);
        Log.v("MY","signalStrength: " + MyPhoneStateListener.getSignalStrength() );
    }

//    @Override
//    public void onSignalStrengthChanged(int asu) {
//        MyPhoneStateListener.signalStrength = (byte)asu;
//        Log.v("MY","signalStrength: " + MyPhoneStateListener.signalStrength );
//    }
}