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

public class CalibrationGyroActivity extends ActionBarActivity implements CallbackInterface, View.OnClickListener, ToggleButton.OnCheckedChangeListener, ToggleButton.OnLongClickListener{

    private View mView;
    //private BroadcastReceiver mCalibrationReceiver;

    private final float[] GyroAvg = new float[3];
    private final float[] GyroCurr = new float[3];

    private TextView mGyroCurrX, mGyroCurrY, mGyroCurrZ;
    private ToggleButton mGyroAvgX, mGyroAvgY, mGyroAvgZ;

    private boolean mIsFresh = true;

    public CalibrationGyroActivity() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration_gyro_activity);
        mView = findViewById(R.id.main_window);

        mGyroCurrX = (TextView) mView.findViewById(R.id.gyro_curr_x);
        mGyroCurrY = (TextView) mView.findViewById(R.id.gyro_curr_y);
        mGyroCurrZ = (TextView) mView.findViewById(R.id.gyro_curr_z);

        ToggleButton butt;

        butt = (ToggleButton) mView.findViewById(R.id.gyro_avg_x); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mGyroAvgX = butt;
        butt = (ToggleButton) mView.findViewById(R.id.gyro_avg_y); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mGyroAvgY = butt;
        butt = (ToggleButton) mView.findViewById(R.id.gyro_avg_z); butt.setOnCheckedChangeListener(this); butt.setOnLongClickListener(this); mGyroAvgZ = butt;

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
        if (mGyroAvgX.isChecked()) command += "set 13=" + mGyroAvgX.getTextOn() + "\r\n";
        if (mGyroAvgY.isChecked()) command += "set 14=" + mGyroAvgY.getTextOn() + "\r\n";
        if (mGyroAvgZ.isChecked()) command += "set 15=" + mGyroAvgZ.getTextOn() + "\r\n";

        if (command.length() != 0) {
            command += "calib save\r\n";
            Intent commandIntent = new Intent();
            commandIntent.setAction(getString(R.string.device_command));
            commandIntent.putExtra("COMMAND", command);
            sendBroadcast(commandIntent);
            Toast.makeText(CalibrationGyroActivity.this, "Values stored on device", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(CalibrationGyroActivity.this, "No value selected", Toast.LENGTH_SHORT).show();
    }

    void ReadFromDevice() {
        //default locked gelsin
        mGyroAvgX.setChecked(true);
        mGyroAvgY.setChecked(true);
        mGyroAvgZ.setChecked(true);

        Intent commandIntent = new Intent();
        commandIntent.setAction(getString(R.string.device_command));
        commandIntent.putExtra("COMMAND", "param\r\n");
        sendBroadcast(commandIntent);

    }

    void ResetAll()
    {
        mIsFresh = true;

        GyroAvg[0] = GyroAvg[1] = GyroAvg[2] = 0;
        GyroCurr[0] = GyroCurr[1] = GyroCurr[2] = 0;

        mGyroAvgX.setChecked(false);
        mGyroAvgY.setChecked(false);
        mGyroAvgZ.setChecked(false);

        InvalidateToggleButtons();
        Toast.makeText(CalibrationGyroActivity.this, "Min/Max values reset", Toast.LENGTH_SHORT).show();
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

            mGyroAvgX.setTextOff(String.format("%.2f", GyroAvg[0]));
            mGyroCurrX.setText(String.format("%.0f", GyroCurr[0]));
            mGyroAvgY.setTextOff(String.format("%.2f", GyroAvg[1]));
            mGyroCurrY.setText(String.format("%.0f", GyroCurr[1]));
            mGyroAvgZ.setTextOff(String.format("%.2f", GyroAvg[2]));
            mGyroCurrZ.setText(String.format("%.0f", GyroCurr[2]));

            InvalidateToggleButtons();
        }
    };

    private final Runnable UpdateParameterDisplay = new Runnable() {
        @Override
        public void run() {

            //mMessagesReceived.setText(String.valueOf(MessagesReceived));

            mGyroAvgX.setTextOn(String.format("%.2f", GyroAvg[0]));
            mGyroAvgY.setTextOn(String.format("%.2f", GyroAvg[1]));
            mGyroAvgZ.setTextOn(String.format("%.2f", GyroAvg[2]));

            InvalidateToggleButtons();
            Toast.makeText(CalibrationGyroActivity.this, "Values received from device", Toast.LENGTH_SHORT).show();
        }
    };

    private void InvalidateToggleButtons() {
        mGyroAvgX.setChecked(mGyroAvgX.isChecked());
        mGyroAvgY.setChecked(mGyroAvgY.isChecked());
        mGyroAvgZ.setChecked(mGyroAvgZ.isChecked());
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
                if (sensorType.equals("G"))
                {
                    for (int i = 0; i<3; i++)
                    {
                        float val = Float.parseFloat(values[i]);
                        if (mIsFresh) {
                            GyroCurr[i] = val;
                            GyroAvg[i] = val;
                        }
                        else {
                            GyroCurr[i] = GyroCurr[i] * 0.9f + val * 0.1f;
                            GyroAvg[i] = GyroAvg[i] * 0.99f + GyroCurr[i] * 0.01f;
                        }
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
                case 13:
                    GyroAvg[0] = value;
                    break;
                case 14:
                    GyroAvg[1] = value;
                    break;
                case 15:
                    GyroAvg[2] = value;
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
            if (butt.isChecked())
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
                case R.id.gyro_avg_x:
                    GyroAvg[0] = 0;
                    break;
                case R.id.gyro_avg_y:
                    GyroAvg[1] = 0;
                    break;
                case R.id.gyro_avg_z:
                    GyroAvg[2] = 0;
                    break;
            }
            butt.setChecked(false);
        }
        return true;
    }

}
