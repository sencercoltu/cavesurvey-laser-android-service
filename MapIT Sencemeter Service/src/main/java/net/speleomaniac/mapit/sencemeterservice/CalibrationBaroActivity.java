package net.speleomaniac.mapit.sencemeterservice;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CalibrationBaroActivity extends ActionBarActivity implements CallbackInterface, View.OnClickListener {

    private View mView;
    //private BroadcastReceiver mCalibrationReceiver;

    private float BaroCurr;
    private float BaroBase;
    private float BaroAlt;
    private float BaroTemp;

    private Button mBaroCurr;
    private EditText mBaroBase;
    private TextView mBaroAlt;
    private TextView mBaroTemp;

    // --Commented out by Inspection (19.8.2014 23:42):private boolean mIsFresh = true;

    public CalibrationBaroActivity() {
        // Required empty public constructor

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration_baro_activity);
        mView = findViewById(R.id.main_window);

        mBaroCurr = (Button) mView.findViewById(R.id.baro_curr); mBaroCurr.setOnClickListener(this);
        mBaroAlt = (TextView) mView.findViewById(R.id.baro_alt);
        mBaroTemp = (TextView) mView.findViewById(R.id.baro_temp);
        mBaroBase = (EditText) mView.findViewById(R.id.baro_base);

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
        command += "set 19=" + mBaroBase.getText() + "\r\n";

        if (command.length() == 0) {
            command += "calib save\r\n";
            Intent commandIntent = new Intent();
            commandIntent.setAction(getString(R.string.device_command));
            commandIntent.putExtra("COMMAND", command);
            sendBroadcast(commandIntent);
            Toast.makeText(CalibrationBaroActivity.this, "Values stored on device", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(CalibrationBaroActivity.this, "No value selected", Toast.LENGTH_SHORT).show();
    }

    void ReadFromDevice() {
        //default locked gelsin

        Intent commandIntent = new Intent();
        commandIntent.setAction(getString(R.string.device_command));
        commandIntent.putExtra("COMMAND", "param\r\n");
        sendBroadcast(commandIntent);

    }

    void ResetAll()
    {
        //mIsFresh = true;

        BaroCurr = 0;
        BaroBase = 101325;

        mBaroBase.setText(String.format("%.0f", BaroBase));
        mBaroBase.selectAll();

        Toast.makeText(CalibrationBaroActivity.this, "Values reset", Toast.LENGTH_SHORT).show();
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
            mBaroCurr.setText(String.format("%.0f", BaroCurr));
            mBaroAlt.setText(String.format("%.2f", BaroAlt));
            mBaroTemp.setText(String.format("%.2f", BaroTemp));
        }
    };

    private final Runnable UpdateParameterDisplay = new Runnable() {
        @Override
        public void run() {
            mBaroBase.setText(String.format("%.0f", BaroBase));
            mBaroBase.selectAll();
            Toast.makeText(CalibrationBaroActivity.this, "Values received from device", Toast.LENGTH_SHORT).show();
        }
    };



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
                if (sensorType.equals("P"))
                {
                    BaroTemp = Float.parseFloat(values[0]);
                    BaroCurr = Float.parseFloat(values[1]);
                    BaroAlt = Float.parseFloat(values[2]);
                }
            }
            //mIsFresh = false;
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
                case 19:
                    BaroBase = (int) value;
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
                case R.id.baro_curr:
                    BaroBase = BaroCurr;
                    mBaroBase.setText(String.format("%.0f", BaroBase));
                    mBaroBase.selectAll();
                    break;
            }
        }
    }

}
