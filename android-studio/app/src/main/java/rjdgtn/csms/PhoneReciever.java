package rjdgtn.csms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import java.lang.reflect.Method;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class PhoneReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Create object of Telephony Manager class.
        TelephonyManager telephony = (TelephonyManager)  context.getSystemService(Context.TELEPHONY_SERVICE);
        //Assign a phone state listener.
        CustomPhoneStateListener customPhoneListener = new CustomPhoneStateListener (context);
        telephony.listen(customPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }



    public class CustomPhoneStateListener extends PhoneStateListener {
        Context context;

        public CustomPhoneStateListener(Context context) {
            super();
            this.context = context;
        }

        @Override
        public void onCallStateChanged(int state, String callingNumber)
        {
            if (WorkerService.isRunning(context)) {
                super.onCallStateChanged(state, callingNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        //handle out going call
                        endCallIfBlocked(callingNumber);
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        //handle in coming call
                        endCallIfBlocked(callingNumber);
                        break;
                    default:
                        break;
                }
            }
        }

        private void endCallIfBlocked(String callingNumber) {
            try {

                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                Method m1 = tm.getClass().getDeclaredMethod("getITelephony");
                m1.setAccessible(true);
                Object iTelephony = m1.invoke(tm);

                Method m2 = iTelephony.getClass().getDeclaredMethod("silenceRinger");
                Method m3 = iTelephony.getClass().getDeclaredMethod("endCall");

                m2.invoke(iTelephony);
                m3.invoke(iTelephony);

                // Java reflection to gain access to TelephonyManager's
                // ITelephony getter
//                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//                c = Class.forName(tm.getClass().getName());
//                Method m = c.getDeclaredMethod("getITelephony");
//                m.setAccessible(true);
//                com.android.internal.telephony.ITelephony telephonyService = (ITelephony) m.invoke(tm);
//                telephonyService = (ITelephony) m.invoke(tm);
//
//                telephonyService.silenceRinger();
//                telephonyService.endCall();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
