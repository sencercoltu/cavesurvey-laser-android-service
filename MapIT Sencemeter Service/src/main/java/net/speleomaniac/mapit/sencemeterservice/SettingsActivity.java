package net.speleomaniac.mapit.sencemeterservice;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
//import org.jraf.android.backport.switchwidget.SwitchPreference;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener, SwitchPreference.OnPreferenceChangeListener, CallbackInterface
{
    public static SharedPreferences SharedPrefs;
    private Preference mDeviceSelectPref;
    private SwitchPreference mEnableServicePref;
    private Preference mTestData;
    private Preference mRequestShot;
    private SwitchPreference mRequestContinous;
    private Preference mCalibrateMenu;
    private Preference mCalibrateAccel;
    private Preference mCalibrateGyro;
    private Preference mCalibrateMag;
    private Preference mCalibrateBaro;
    private Preference mCalibrateReset;
    private Preference mCalibrateReboot;
    private Preference mBoardAlignment;
    private boolean isNewLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //setProgressBarIndeterminateVisibility(false);

        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_general);
        PreferenceScreen mPrefScreen = getPreferenceScreen();

        SharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mEnableServicePref = (SwitchPreference) mPrefScreen.findPreference("prefs_enable_service");
        mEnableServicePref.setOnPreferenceChangeListener(this);

        //boolean isServiceActive = AdapterService.IsActive();
        //SharedPrefs.edit().putBoolean("prefs_enable_service", isServiceActive).apply();

        mDeviceSelectPref = mPrefScreen.findPreference("prefs_select_device");
        mDeviceSelectPref.setOnPreferenceClickListener(this);

        mTestData = mPrefScreen.findPreference("prefs_test");
        mTestData.setOnPreferenceClickListener(this);

        mRequestShot = mPrefScreen.findPreference("prefs_readsingle");
        mRequestShot.setOnPreferenceClickListener(this);

        mRequestContinous = (SwitchPreference) mPrefScreen.findPreference("prefs_continous");
        mRequestContinous.setOnPreferenceChangeListener(this);

        mCalibrateMenu = mPrefScreen.findPreference("prefs_calibrations");

        mCalibrateAccel = mPrefScreen.findPreference("prefs_calibrate_accel");
        mCalibrateAccel.setOnPreferenceClickListener(this);

        mCalibrateGyro = mPrefScreen.findPreference("prefs_calibrate_gyro");
        mCalibrateGyro.setOnPreferenceClickListener(this);

        mCalibrateMag = mPrefScreen.findPreference("prefs_calibrate_mag");
        mCalibrateMag.setOnPreferenceClickListener(this);

        mCalibrateBaro = mPrefScreen.findPreference("prefs_calibrate_baro");
        mCalibrateBaro.setOnPreferenceClickListener(this);

        mCalibrateReset = mPrefScreen.findPreference("prefs_calibrate_reset");
        mCalibrateReset.setOnPreferenceClickListener(this);

        mCalibrateReboot = mPrefScreen.findPreference("prefs_reboot");
        mCalibrateReboot.setOnPreferenceClickListener(this);

        mBoardAlignment = mPrefScreen.findPreference("prefs_alignment");
        mBoardAlignment.setOnPreferenceClickListener(this);

        SetServiceStatusPref();
    }

    @Override
    public void onResume()
    {
        AdapterService.AddReceiver(this);
        String selectedDeviceName = SharedPrefs.getString("prefs_selected_device_name", getString(R.string.unknown_device_name));
        mDeviceSelectPref.setSummary(selectedDeviceName);
        mEnableServicePref.setEnabled(!selectedDeviceName.equals(getString(R.string.unknown_device_name)));
        SetServiceStatusPref();
        super.onResume();
    }

    @Override
    protected void onPause() {
        AdapterService.RemoveReceiver(this);
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference    preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

// If the user has clicked on a preference screen, set up the action bar
        if (preference instanceof PreferenceScreen) {
            PreferenceScreen ps = (PreferenceScreen) preference;
            initializeActionBar(ps);
        }
        return false;
    }

    /** Sets up the action bar for an {@link PreferenceScreen} */
    private static void initializeActionBar(PreferenceScreen preferenceScreen) {
        final Dialog dialog = preferenceScreen.getDialog();

        if (dialog != null) {
            // Inialize the action bar

            ActionBar actionBar = dialog.getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);

            // Apply custom home button area click listener to close the PreferenceScreen because PreferenceScreens are dialogs which swallow
            // events instead of passing to the activity
            // Related Issue: https://code.google.com/p/android/issues/detail?id=4611
            View homeBtn = dialog.findViewById(android.R.id.home);

            if (homeBtn != null) {
                View.OnClickListener dismissDialogClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                };

                // Prepare yourselves for some hacky programming
                ViewParent homeBtnContainer = homeBtn.getParent();

                // The home button is an ImageView inside a FrameLayout
                if (homeBtnContainer instanceof FrameLayout) {
                    ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();

                    if (containerParent instanceof LinearLayout) {
                        // This view also contains the title text, set the whole view as clickable
                        containerParent.setOnClickListener(dismissDialogClickListener);
                    } else {
                        // Just set it on the home button
                        ((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
                    }
                } else {
                    // The 'If all else fails' default case
                    homeBtn.setOnClickListener(dismissDialogClickListener);
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {

        if (preference == null)
            return true;
        else if (preference.equals(mEnableServicePref))
        {
            boolean checked = o.equals(true); //SharedPrefs.getBoolean("prefs_enable_service", false);

            if (AdapterService.IsActive())
            {
                if (!checked) {
                    Intent serviceIntent = new Intent(this, AdapterService.class);
                    mEnableServicePref.setEnabled(false); // adapter thread start ya da sto olunca enable olacak
                    EnableConnectedMenus(false);
                    stopService(serviceIntent);
                }
            } else
            {
                if (checked) {
                    Intent serviceIntent = new Intent(this, AdapterService.class);
                    mEnableServicePref.setEnabled(false); // adapter thread start ya da sto olunca enable olacak
                    mDeviceSelectPref.setEnabled(false);
                    startService(serviceIntent);
                }
            }
            //SetServiceStatusPref();
        }
        else if (preference.equals(mRequestContinous)) {
            boolean checked = o.equals(true);
            AdapterService.setContinous(checked);
        }
        return true;
    }

    private void RequestShot() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(getString(R.string.device_command));
        broadcastIntent.putExtra("COMMAND", "cmd *11114#\r\n");
        sendBroadcast(broadcastIntent);
    }

    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        if (preference == null)
            return true;

        if (preference.equals(mDeviceSelectPref))
        {
            Intent intent = new Intent(this, DeviceSelectActivity.class);
            startActivity(intent);

        } else if (preference.equals(mTestData))
        {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(getString(R.string.device_data));


            String shotData = "";
            shotData += "1=" + String.format("%.2f", Math.random() * 10000.0) + ";";
            shotData += "2=" + String.format("%.2f", Math.random() * 359.99) + ";";
            shotData += "3=" + String.format("%.2f", Math.random() * 180.0 - 90.0) + ";";
            shotData += "5=" + String.format("%.0f", Math.random() * 35) + ";";
            shotData += "6=" + String.format("%.0f", (Math.random() * 1000) - 500 + 10135) + ";";
            broadcastIntent.putExtra("DATA", shotData);
            sendBroadcast(broadcastIntent);
        }else if (preference.equals(mRequestShot))
        {
            RequestShot();
        } else  if (preference.equals(mCalibrateAccel))
        {
            Intent intent = new Intent(this, CalibrationAccelActivity.class);
            startActivity(intent);
        } else  if (preference.equals(mCalibrateGyro))
        {
            Intent intent = new Intent(this, CalibrationGyroActivity.class);
            startActivity(intent);
        } else  if (preference.equals(mCalibrateMag))
        {
            Intent intent = new Intent(this, CalibrationMagActivity.class);
            startActivity(intent);
        } else  if (preference.equals(mCalibrateBaro))
        {
            Intent intent = new Intent(this, CalibrationBaroActivity.class);
            startActivity(intent);
        } else  if (preference.equals(mBoardAlignment))
        {
            Intent intent = new Intent(this, AlignmentActivity.class);
            startActivity(intent);
        } else  if (preference.equals(mCalibrateReset))
        {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Reset Device")
                    .setMessage("Are you sure you want to reset calibration data to factory defaults?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent commandIntent = new Intent();
                            commandIntent.setAction(getString(R.string.device_command));
                            commandIntent.putExtra("COMMAND", "reset\r\n");
                            sendBroadcast(commandIntent);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else  if (preference.equals(mCalibrateReboot))
        {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Reboot Device")
                    .setMessage("Are you sure you want to reboot the device?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent commandIntent = new Intent();
                            commandIntent.setAction(getString(R.string.device_command));
                            commandIntent.putExtra("COMMAND", "reboot\r\n");
                            sendBroadcast(commandIntent);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
        return true;
    }

    private void SetServiceStatusPref()
    {
        //SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean active = AdapterService.IsActive();
        boolean connected = AdapterService.IsConnected();
        //SharedPrefs.edit().putBoolean("prefs_enable_service", active).commit();
        mDeviceSelectPref.setEnabled(!active);
        EnableConnectedMenus(connected);
    }

    private void EnableConnectedMenus(boolean enable) {
        mCalibrateMenu.setEnabled(enable);
        mCalibrateAccel.setEnabled(enable);
        mBoardAlignment.setEnabled(enable);
        mCalibrateGyro.setEnabled(enable);
        mCalibrateMag.setEnabled(enable);
        mCalibrateBaro.setEnabled(enable);
        mCalibrateReset.setEnabled(enable);
        mRequestShot.setEnabled(enable);
    }

    @Override
    public void OnReceive(CallbackType callbackType, String data) {
        //discard
    }

    @Override
    public void OnConnect() {
        //enable disable menu items
        getWindow().getDecorView().post(
                new Runnable() {
                    @Override
                    public void run() {
                        SetServiceStatusPref();
                    }
                }
        );
    }

    @Override
    public void OnDisconnect() {
        //enable disable menu items
        getWindow().getDecorView().post(
                new Runnable() {
                    @Override
                    public void run() {
                        SetServiceStatusPref();
                    }
                }
        );
    }

    @Override
    public void OnStart() {
        getWindow().getDecorView().post(
                new Runnable() {
                    @Override
                    public void run() {
                        mEnableServicePref.setEnabled(true);
                    }
                });
    }

    @Override
    public void OnStop() {

        getWindow().getDecorView().post(
                new Runnable() {
                    @Override
                    public void run() {
                        mEnableServicePref.setEnabled(true);
                    }
                });
    }
}
