package net.speleomaniac.mapit.sencemeterservice;


//import android.app.Activity;
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


public class CalibrationAccelActivity extends ActionBarActivity implements CallbackInterface, View.OnClickListener, ToggleButton.OnCheckedChangeListener, ToggleButton.OnLongClickListener{

    private View mView;
    //private BroadcastReceiver mCalibrationReceiver;

    private final float[] AccelMin = new float[3];
    private final float[] AccelMax = new float[3];
    private final float[] AccelCurr = new float[3];

    private TextView mAccCurrX, mAccCurrY, mAccCurrZ;
    private ToggleButton mAccMinX, mAccMaxX, mAccMinY, mAccMaxY, mAccMinZ, mAccMaxZ;

    private boolean mIsFresh = true;

    public CalibrationAccelActivity() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration_accel_activity);
        mView = findViewById(R.id.main_window);

        mAccCurrX = (TextView) mView.findViewById(R.id.acc_curr_x);
        mAccCurrY = (TextView) mView.findViewById(R.id.acc_curr_y);
        mAccCurrZ = (TextView) mView.findViewById(R.id.acc_curr_z);

        ToggleButton butt;

        butt = (ToggleButton) mView.findViewById(R.id.acc_min_x); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mAccMinX = butt;
        butt = (ToggleButton) mView.findViewById(R.id.acc_max_x); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mAccMaxX = butt;
        butt = (ToggleButton) mView.findViewById(R.id.acc_min_y); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mAccMinY = butt;
        butt = (ToggleButton) mView.findViewById(R.id.acc_max_y); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mAccMaxY = butt;
        butt = (ToggleButton) mView.findViewById(R.id.acc_min_z); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mAccMinZ = butt;
        butt = (ToggleButton) mView.findViewById(R.id.acc_max_z); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mAccMaxZ = butt;

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
        if (mAccMinX.isChecked())
            command += "set 1=" + mAccMinX.getTextOn() + "\r\n";
        if (mAccMaxX.isChecked())
            command += "set 2=" + mAccMaxX.getTextOn() + "\r\n";

        if (mAccMinY.isChecked())
            command += "set 3=" + mAccMinY.getTextOn() + "\r\n";
        if (mAccMaxY.isChecked())
            command += "set 4=" + mAccMaxY.getTextOn() + "\r\n";

        if (mAccMinZ.isChecked())
            command += "set 5=" + mAccMinZ.getTextOn() + "\r\n";
        if (mAccMaxZ.isChecked())
            command += "set 6=" + mAccMaxZ.getTextOn() + "\r\n";

        if (command.length() != 0) {
            command += "calib save\r\n";
            Intent commandIntent = new Intent();
            commandIntent.setAction(getString(R.string.device_command));
            commandIntent.putExtra("COMMAND", command);
            sendBroadcast(commandIntent);
            Toast.makeText(CalibrationAccelActivity.this, "Values stored on device", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(CalibrationAccelActivity.this, "No value selected", Toast.LENGTH_SHORT).show();
    }

    void ReadFromDevice() {
        //default locked gelsin
        mAccMinX.setChecked(true); mAccMaxX.setChecked(true);
        mAccMinY.setChecked(true); mAccMaxY.setChecked(true);
        mAccMinZ.setChecked(true); mAccMaxZ.setChecked(true);

        Intent commandIntent = new Intent();
        commandIntent.setAction(getString(R.string.device_command));
        commandIntent.putExtra("COMMAND", "param\r\n");
        sendBroadcast(commandIntent);
    }

    void ResetAll() {
        mIsFresh = true;

        AccelMin[0] = AccelMin[1] = AccelMin[2] = 9999;
        AccelMax[0] = AccelMax[1] = AccelMax[2] = -9999;
        AccelCurr[0] = AccelCurr[1] = AccelCurr[2] = 0;

        mAccMinX.setChecked(false); mAccMaxX.setChecked(false);
        mAccMinY.setChecked(false); mAccMaxY.setChecked(false);
        mAccMinZ.setChecked(false); mAccMaxZ.setChecked(false);

        InvalidateToggleButtons();
        Toast.makeText(CalibrationAccelActivity.this, "Min/Max values reset", Toast.LENGTH_SHORT).show();
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

            mAccMinX.setTextOff(String.format("%.2f", AccelMin[0]));
            mAccCurrX.setText(String.format("%.0f", AccelCurr[0]));
            mAccMaxX.setTextOff(String.format("%.2f", AccelMax[0]));

            mAccMinY.setTextOff(String.format("%.2f", AccelMin[1]));
            mAccCurrY.setText(String.format("%.0f", AccelCurr[1]));
            mAccMaxY.setTextOff(String.format("%.2f", AccelMax[1]));

            mAccMinZ.setTextOff(String.format("%.2f", AccelMin[2]));
            mAccCurrZ.setText(String.format("%.0f", AccelCurr[2]));
            mAccMaxZ.setTextOff(String.format("%.2f", AccelMax[2]));

            InvalidateToggleButtons();
        }
    };

    private final Runnable UpdateParameterDisplay = new Runnable() {
        @Override
        public void run() {

            //mMessagesReceived.setText(String.valueOf(MessagesReceived));

            mAccMinX.setTextOn(String.format("%.2f", AccelMin[0]));
            mAccMaxX.setTextOn(String.format("%.2f", AccelMax[0]));

            mAccMinY.setTextOn(String.format("%.2f", AccelMin[1]));
            mAccMaxY.setTextOn(String.format("%.2f", AccelMax[1]));

            mAccMinZ.setTextOn(String.format("%.2f", AccelMin[2]));
            mAccMaxZ.setTextOn(String.format("%.2f", AccelMax[2]));

            InvalidateToggleButtons();
            Toast.makeText(CalibrationAccelActivity.this, "Values received from device", Toast.LENGTH_SHORT).show();
        }
    };

    private void InvalidateToggleButtons() {
        mAccMinX.setChecked(mAccMinX.isChecked()); mAccMaxX.setChecked(mAccMaxX.isChecked());
        mAccMinY.setChecked(mAccMinY.isChecked()); mAccMaxY.setChecked(mAccMaxY.isChecked());
        mAccMinZ.setChecked(mAccMinZ.isChecked()); mAccMaxZ.setChecked(mAccMaxZ.isChecked());
    }

    private void ProcessCalibration(String data) {
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
                if (sensorType.equals("A"))
                {
                    for (int i = 0; i<3; i++)
                    {
                        float val = Float.parseFloat(values[i]);
                        if (mIsFresh)
                            AccelCurr[i] = val;
                        else
                            AccelCurr[i] = AccelCurr[i] * 0.9f + val * 0.1f;

                        if (AccelCurr[i] > AccelMax[i]) AccelMax[i] = AccelCurr[i];
                        if (AccelCurr[i] < AccelMin[i]) AccelMin[i] = AccelCurr[i];
                    }
                }
            }
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
                case 1:
                    AccelMin[0] = value;
                    break;
                case 2:
                    AccelMax[0] = value;
                    break;
                case 3:
                    AccelMin[1] = value;
                    break;
                case 4:
                    AccelMax[1] = value;
                    break;
                case 5:
                    AccelMin[2] = value;
                    break;
                case 6:
                    AccelMax[2] = value;
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
                case R.id.acc_min_x:
                    AccelMin[0] = 9999;
                    break;
                case R.id.acc_max_x:
                    AccelMax[0] = -9999;
                    break;
                case R.id.acc_min_y:
                    AccelMin[1] = 9999;
                    break;
                case R.id.acc_max_y:
                    AccelMax[1] = -9999;
                    break;
                case R.id.acc_min_z:
                    AccelMin[2] = 9999;
                    break;
                case R.id.acc_max_z:
                    AccelMax[2] = -9999;
                    break;
            }
            butt.setChecked(false);
        }
        return true;
    }
}
