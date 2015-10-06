package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class SettingsActivity extends Activity {
    private EditText mWheelCircumference;
    private EditText mTextMaxPowerSlider;
    private Button mSetButtonMaxPowerSlider;
    private Button mSetButton;
    private TextView mTextViewVersion;

    //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        mWheelCircumference = (EditText) findViewById(R.id.edit_Wheel_Circumference);
        mSetButton = (Button) findViewById(R.id.button_Set_Wheel_Circumference);
        mSetButtonMaxPowerSlider = (Button) findViewById(R.id.button_SetMaxPowerSlider);
        mTextMaxPowerSlider = (EditText) findViewById(R.id.edit_MaxPowerSlider);
        mTextViewVersion = (TextView) findViewById(R.id.textView_Version);

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            mTextViewVersion.setText("Version: " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //mTextMaxPowerSlider.setText(DeviceControlActivity.MaxPowerSlider.toString(), TextView.BufferType.EDITABLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);


        return true;
    }

    /**
     * Called after {@link #onRestoreInstanceState}, {@link #onRestart}, or
     * {@link #onPause}, for your activity to start interacting with the user.
     * This is a good place to begin animations, open exclusive-access devices
     * (such as the camera), etc.
     * <p/>
     * <p>Keep in mind that onResume is not the best indicator that your activity
     * is visible to the user; a system window such as the keyguard may be in
     * front.  Use {@link #onWindowFocusChanged} to know for certain that your
     * activity is visible to the user (for example, to resume a game).
     * <p/>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onRestoreInstanceState
     * @see #onRestart
     * @see #onPostResume
     * @see #onPause
     */
    @Override
    protected void onResume() {
        super.onResume();
        mTextMaxPowerSlider.setText(DeviceControlActivity.MaxPowerSlider.toString(), TextView.BufferType.EDITABLE);
        mWheelCircumference.setText(Integer.toString(BluetoothLeService.WheelCircumference), TextView.BufferType.EDITABLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void SetWheelCircumferenceMethod(View view) {
        String Circ;
        Circ = mWheelCircumference.getText().toString();
        BluetoothLeService.WheelCircumference = Integer.parseInt(Circ);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        finish();
    }

    public void SetPowerSliderMaxMethod(View view) {
        String Value;
        Value = mTextMaxPowerSlider.getText().toString();
        DeviceControlActivity.MaxPowerSlider = Integer.parseInt(Value);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        finish();

    }
}
