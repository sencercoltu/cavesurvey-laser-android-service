package net.speleomaniac.mapit.sencemeterservice;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class CalibrationMagActivity extends ActionBarActivity implements CallbackInterface, View.OnClickListener, ToggleButton.OnCheckedChangeListener, ToggleButton.OnLongClickListener{

    private View mView;
    //private BroadcastReceiver mCalibrationReceiver;

    private final float[] MagMin = new float[3];
    private final float[] MagMax = new float[3];
    private final float[] MagCurr = new float[3];

    private TextView mMagCurrX, mMagCurrY, mMagCurrZ;
    private ToggleButton mMagMinX, mMagMaxX, mMagMinY, mMagMaxY, mMagMinZ, mMagMaxZ;

    private boolean mIsFresh = true;

    public CalibrationMagActivity() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration_mag_activity);
        mView = findViewById(R.id.main_window);

        mMagCurrX = (TextView) mView.findViewById(R.id.mag_curr_x);
        mMagCurrY = (TextView) mView.findViewById(R.id.mag_curr_y);
        mMagCurrZ = (TextView) mView.findViewById(R.id.mag_curr_z);

        ToggleButton butt;

        butt = (ToggleButton) mView.findViewById(R.id.mag_min_x); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mMagMinX = butt;
        butt = (ToggleButton) mView.findViewById(R.id.mag_max_x); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mMagMaxX = butt;
        butt = (ToggleButton) mView.findViewById(R.id.mag_min_y); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mMagMinY = butt;
        butt = (ToggleButton) mView.findViewById(R.id.mag_max_y); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mMagMaxY = butt;
        butt = (ToggleButton) mView.findViewById(R.id.mag_min_z); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mMagMinZ = butt;
        butt = (ToggleButton) mView.findViewById(R.id.mag_max_z); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mMagMaxZ = butt;

        Button butt2;
        butt2 = (Button) mView.findViewById(R.id.reset); butt2.setOnClickListener(this);
        butt2 = (Button) mView.findViewById(R.id.reload); butt2.setOnClickListener(this);
        butt2 = (Button) mView.findViewById(R.id.save); butt2.setOnClickListener(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //ResetAll();
        //mView.post(UpdateCalibrationDisplay);
        ReadFromDevice();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void SaveToDevice() {
        String command = "";

        //sadece locked olanları gönder
        if (mMagMinX.isChecked()) command += "set 7=" + mMagMinX.getTextOn() + "\r\n";
        if (mMagMaxX.isChecked()) command += "set 8=" + mMagMaxX.getTextOn() + "\r\n";

        if (mMagMinY.isChecked()) command += "set 9=" + mMagMinY.getTextOn() + "\r\n";
        if (mMagMaxY.isChecked()) command += "set 10=" + mMagMaxY.getTextOn() + "\r\n";

        if (mMagMinZ.isChecked()) command += "set 11=" + mMagMinZ.getTextOn() + "\r\n";
        if (mMagMaxZ.isChecked()) command += "set 12=" + mMagMaxZ.getTextOn() + "\r\n";

        if (command.length() != 0) {
            command += "calib save\r\n";
            Intent commandIntent = new Intent();
            commandIntent.setAction(getString(R.string.device_command));
            commandIntent.putExtra("COMMAND", command);
            sendBroadcast(commandIntent);
            Toast.makeText(CalibrationMagActivity.this, "Values stored on device", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(CalibrationMagActivity.this, "No value selected", Toast.LENGTH_SHORT).show();
    }

    void ReadFromDevice() {
        //default locked gelsin
        mMagMinX.setChecked(true); mMagMaxX.setChecked(true);
        mMagMinY.setChecked(true); mMagMaxY.setChecked(true);
        mMagMinZ.setChecked(true); mMagMaxZ.setChecked(true);

        Intent commandIntent = new Intent();
        commandIntent.setAction(getString(R.string.device_command));
        commandIntent.putExtra("COMMAND", "param\r\n");
        sendBroadcast(commandIntent);
    }

    void ResetAll()
    {
        mIsFresh = true;

        MagMin[0] = MagMin[1] = MagMin[2] = 9999;
        MagMax[0] = MagMax[1] = MagMax[2] = -9999;
        MagCurr[0] = MagCurr[1] = MagCurr[2] = 0;

        mMagMinX.setChecked(false); mMagMaxX.setChecked(false);
        mMagMinY.setChecked(false); mMagMaxY.setChecked(false);
        mMagMinZ.setChecked(false); mMagMaxZ.setChecked(false);

        InvalidateToggleButtons();

        Toast.makeText(CalibrationMagActivity.this, "Min/Max values reset", Toast.LENGTH_SHORT).show();
    }

    private void StartCalibration() {
        Intent commandIntent = new Intent();
        commandIntent.setAction(getString(R.string.device_command));
        commandIntent.putExtra("COMMAND", "calib on\r\n");
        sendBroadcast(commandIntent);

        ReadFromDevice();
    }

    private void StopCalibration() {
        Intent commandIntent = new Intent();
        commandIntent.setAction(getString(R.string.device_command));
        commandIntent.putExtra("COMMAND", "calib off\r\n");
        sendBroadcast(commandIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        AdapterService.AddReceiver(this);
        StartCalibration();
        mView.setKeepScreenOn(true);
    }

    @Override
    public void onPause() {
        mView.setKeepScreenOn(false);
        AdapterService.RemoveReceiver(null);
        StopCalibration();
        super.onPause();
    }

    private final Runnable UpdateCalibrationDisplay = new Runnable() {
        @Override
        public void run() {

            //mMessagesReceived.setText(String.valueOf(MessagesReceived));

            mMagMinX.setTextOff(String.format("%.2f", MagMin[0]));
            mMagCurrX.setText(String.format("%.0f", MagCurr[0]));
            mMagMaxX.setTextOff(String.format("%.2f", MagMax[0]));

            mMagMinY.setTextOff(String.format("%.2f", MagMin[1]));
            mMagCurrY.setText(String.format("%.0f", MagCurr[1]));
            mMagMaxY.setTextOff(String.format("%.2f", MagMax[1]));

            mMagMinZ.setTextOff(String.format("%.2f", MagMin[2]));
            mMagCurrZ.setText(String.format("%.0f", MagCurr[2]));
            mMagMaxZ.setTextOff(String.format("%.2f", MagMax[2]));

            InvalidateToggleButtons();
        }
    };

    private final Runnable UpdateParameterDisplay = new Runnable() {
        @Override
        public void run() {

            //mMessagesReceived.setText(String.valueOf(MessagesReceived));

            mMagMinX.setTextOn(String.format("%.2f", MagMin[0]));
            mMagMaxX.setTextOn(String.format("%.2f", MagMax[0]));

            mMagMinY.setTextOn(String.format("%.2f", MagMin[1]));
            mMagMaxY.setTextOn(String.format("%.2f", MagMax[1]));

            mMagMinZ.setTextOn(String.format("%.2f", MagMin[2]));
            mMagMaxZ.setTextOn(String.format("%.2f", MagMax[2]));

            InvalidateToggleButtons();
            Toast.makeText(CalibrationMagActivity.this, "Values received from device", Toast.LENGTH_SHORT).show();
        }
    };

    private void InvalidateToggleButtons() {
        mMagMinX.setChecked(mMagMinX.isChecked()); mMagMaxX.setChecked(mMagMaxX.isChecked());
        mMagMinY.setChecked(mMagMinY.isChecked()); mMagMaxY.setChecked(mMagMaxY.isChecked());
        mMagMinZ.setChecked(mMagMinZ.isChecked()); mMagMaxZ.setChecked(mMagMaxZ.isChecked());
    }

    private void ProcessCalibration(String data)
    {
        try
        {
            //MessagesReceived++;
            String[] sensors = data.split(" ");
            String sensorType;
            for(String sensor: sensors)
            {
                sensorType = sensor.substring(0, 1);
                String[] values = sensor.substring(2).split(";");
                if (values.length != 3) continue;
                if (sensorType.equals("M"))
                {
                    for (int i = 0; i<3; i++)
                    {
                        float val = Float.parseFloat(values[i]);
                        if (mIsFresh)
                            MagCurr[i] = val;
                        else
                            MagCurr[i] = MagCurr[i] * 0.9f + val * 0.1f;
                        if (MagCurr[i] > MagMax[i]) MagMax[i] = MagCurr[i];
                        if (MagCurr[i] < MagMin[i]) MagMin[i] = MagCurr[i];
                    }
                }            }
            mIsFresh = false;
            //if (bUpdate)
            runOnUiThread(UpdateCalibrationDisplay);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void ProcessParameter(String data)
    {
        try
        {
            String[] fields = data.split("=");
            int parameterIndex = Integer.parseInt(fields[0]);
            float value = Float.parseFloat(fields[1]);
            switch(parameterIndex)
            {
                case 7:
                    MagMin[0] = value;
                    break;
                case 8:
                    MagMax[0] = value;
                    break;
                case 9:
                    MagMin[1] = value;
                    break;
                case 10:
                    MagMax[1] = value;
                    break;
                case 11:
                    MagMin[2] = value;
                    break;
                case 12:
                    MagMax[2] = value;
                    runOnUiThread(UpdateParameterDisplay);
                    break;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void OnReceive(CallbackType callbackType, String data) {
        switch(callbackType)
        {
            case CALLBACK_TYPE_CALIBRATION:
                ProcessCalibration(data);
                break;
            case CALLBACK_TYPE_PARAMETER:
                ProcessParameter(data);
                break;
        }
    }

    @Override
    public void OnConnect() {

    }

    @Override
    public void OnDisconnect() {

    }

    @Override
    public void OnStart() {

    }

    @Override
    public void OnStop() {

    }

    @Override
    public void onClick(View view) {
        if (view instanceof Button) {
            Button butt = (Button) view;
            switch(butt.getId()) {
                case R.id.reset:
                    ResetAll();
                    break;
                case R.id.reload:
                    ReadFromDevice();
                    break;
                case R.id.save:
                    SaveToDevice();
                    break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton instanceof ToggleButton) {
            ToggleButton butt = (ToggleButton) compoundButton;
            butt.setTextOn(butt.getTextOff());
            InvalidateToggleButtons();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view instanceof ToggleButton) {
            ToggleButton butt = (ToggleButton) view;
            switch(butt.getId())
            {
                case R.id.mag_min_x:
                    MagMin[0] = 9999;
                    break;
                case R.id.mag_max_x:
                    MagMax[0] = -9999;
                    break;
                case R.id.mag_min_y:
                    MagMin[1] = 9999;
                    break;
                case R.id.mag_max_y:
                    MagMax[1] = -9999;
                    break;
                case R.id.mag_min_z:
                    MagMin[2] = 9999;
                    break;
                case R.id.mag_max_z:
                    MagMax[2] = -9999;
                    break;
            }
            butt.setChecked(false);
        }
        return true;
    }

}
