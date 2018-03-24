package rjdgtn.csms;

import android.app.ActivityManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

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

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    protected void OnClick(View view) {
        rjdgtn.csms.LauncherService.start(getApplicationContext());
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            rjdgtn.csms.LauncherService.start(getApplicationContext());
        } else {
            rjdgtn.csms.LauncherService.stop(getApplicationContext());
        }
    }

}
