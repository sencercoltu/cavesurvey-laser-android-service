package net.speleomaniac.mapit.sencemeterservice;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Sencer Coltu on 8.9.2014.
 */
public class AlignmentActivity extends ActionBarActivity implements CallbackInterface, View.OnClickListener {
    private View mView;
    private final int[] AccDev = new int[3];
    private final int[] MagDev = new int[3];

    private final float[] MagCurr = new float[3];
    private final float[] AccelCurr = new float[3];

    private EditText axEdit;
    private EditText ayEdit;
    private EditText azEdit;

    private EditText mxEdit;
    private EditText myEdit;
    private EditText mzEdit;

    private TextView SensorDisplay;

    private OrientationRenderer mRenderer;
    private GLSurfaceView mGLView;
    private TextView mYaw, mPitch, mRoll;

    private float yaw = 0, pitch = 0, roll = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alignment_activity);
        mView = findViewById(R.id.main_window);


        axEdit = (EditText) findViewById(R.id.acc_dev_x);
        ayEdit = (EditText) findViewById(R.id.acc_dev_y);
        azEdit = (EditText) findViewById(R.id.acc_dev_z);

        mxEdit = (EditText) findViewById(R.id.mag_dev_x);
        myEdit = (EditText) findViewById(R.id.mag_dev_y);
        mzEdit = (EditText) findViewById(R.id.mag_dev_z);

        SensorDisplay = (TextView) findViewById(R.id.sensor_display);

        Button button;
        button = (Button) mView.findViewById(R.id.reset); button.setOnClickListener(this);
        button = (Button) mView.findViewById(R.id.reload); button.setOnClickListener(this);
        button = (Button) mView.findViewById(R.id.save); button.setOnClickListener(this);

        mYaw = (TextView) mView.findViewById(R.id.orient_yaw);
        mPitch = (TextView) mView.findViewById(R.id.orient_pitch);
        mRoll = (TextView) mView.findViewById(R.id.orient_roll);

        runOnUiThread(mOrientationRunnable);

        LinearLayout v = (LinearLayout) mView.findViewById(R.id.calib_render_area);
        mGLView = new GLSurfaceView(this);
        mRenderer = new OrientationRenderer(this, mGLView);

        mGLView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mGLView.setBackgroundResource(R.drawable.cave);
        mGLView.setZOrderOnTop(true);
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        v.addView(mGLView, 0);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //ResetAll();
        //mView.post(UpdateCalibrationDisplay);
        ReadFromDevice();
    }

    void ReadFromDevice() {
        //default locked gelsin

        Intent commandIntent = new Intent();
        commandIntent.setAction(getString(R.string.device_command));
        commandIntent.putExtra("COMMAND", "param\r\n");
        sendBroadcast(commandIntent);
    }

    void SaveToDevice() {
        String command = "";

        command += "set 20=" + axEdit.getText().toString() + "\r\n";
        command += "set 21=" + ayEdit.getText().toString() + "\r\n";
        command += "set 22=" + azEdit.getText().toString() + "\r\n";
        command += "set 23=" + mxEdit.getText().toString() + "\r\n";
        command += "set 24=" + myEdit.getText().toString() + "\r\n";
        command += "set 25=" + mzEdit.getText().toString() + "\r\n";

        if (command.length() != 0) {
            command += "calib save\r\n";
            Intent commandIntent = new Intent();
            commandIntent.setAction(getString(R.string.device_command));
            commandIntent.putExtra("COMMAND", command);
            sendBroadcast(commandIntent);
            Toast.makeText(AlignmentActivity.this, "Values stored on device", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(AlignmentActivity.this, "No value selected", Toast.LENGTH_SHORT).show();
    }

    private void startAlignment() {
        Intent commandIntent = new Intent();
        commandIntent.setAction(getString(R.string.device_command));
        commandIntent.putExtra("COMMAND", "cont on\r\ncalib on\r\n");
        sendBroadcast(commandIntent);
    }

    private void stopAlignment() {
        Intent commandIntent = new Intent();
        commandIntent.setAction(getString(R.string.device_command));
        commandIntent.putExtra("COMMAND", "cont off\r\ncalib off\r\n");
        sendBroadcast(commandIntent);
    }

    void ResetAll()
    {
        axEdit.setText("0"); ayEdit.setText("0"); azEdit.setText("0");
        mxEdit.setText("0"); myEdit.setText("0"); mzEdit.setText("0");

        Toast.makeText(AlignmentActivity.this, "Min/Max values reset", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        AdapterService.AddReceiver(this);
        super.onResume();
        mGLView.onResume();
        startAlignment();
        mView.setKeepScreenOn(true);
    }

    @Override
    public void onPause() {
        mView.setKeepScreenOn(false);
        AdapterService.RemoveReceiver(this);
        stopAlignment();
        super.onPause();
        mGLView.onPause();
    }

    private final Runnable mParameterRunnable = new Runnable() {
        @Override
        public void run() {
            axEdit.setText(Integer.toString(AccDev[0]));
            ayEdit.setText(Integer.toString(AccDev[1]));
            azEdit.setText(Integer.toString(AccDev[2]));

            mxEdit.setText(Integer.toString(MagDev[0]));
            myEdit.setText(Integer.toString(MagDev[1]));
            mzEdit.setText(Integer.toString(MagDev[2]));

            Toast.makeText(AlignmentActivity.this, "Values received from device", Toast.LENGTH_SHORT).show();
        }
    };

    private final Runnable mSensorRunnable = new Runnable() {
        @Override
        public void run() {
            String val = "Acc:(" + AccelCurr[0] + ";" + AccelCurr[1] + ";" + AccelCurr[2] + ") " +
                         "Mag:(" + MagCurr[0] + ";" + MagCurr[1] + ";" + MagCurr[2] + ")";
            SensorDisplay.setText(val);
        }
    };

    private boolean ProcessOrientation(String data) {
        try
        {
            String[] fields = data.split(";");
            for(String field: fields)
            {
                String[] parts = field.split("=");
                if (parts.length != 2) continue;
                if (parts[1].equals("nan"))
                    return false;
                int tag = Integer.parseInt(parts[0]);
                float value = Float.parseFloat(parts[1]);
                switch(tag)
                {
                    case 1: //distance
                        break;
                    case 2: //compass
                        yaw = value;
                        break;
                    case 3: //inclination
                        pitch = value;
                        break;
                    case 4: //roll
                        roll = value;
                        break;
                }
            }

            if (mRenderer != null)
            {
                mRenderer.setOrientation(yaw, pitch, roll);
            }
            runOnUiThread(mOrientationRunnable);
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
    private final Runnable mOrientationRunnable = new Runnable() {
        @Override
        public void run() {
            mYaw.setText("Comp: " + String.format("%.2f", yaw));
            mPitch.setText("Inc: " + String.format("%.2f", pitch));
            mRoll.setText("Roll: " + String.format("%.2f", roll));
        }
    };

//    private String getValue(String data, String index) {
//        String[] values = data.split(";");
//        for (int i=0; i<values.length; i++) {
//            String[] parts = values[i].split("=");
//            if (parts[0].equals(index))
//                return parts[1];
//        }
//        return "0";
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ProcessParameter(String data) {
        try
        {
            String[] fields = data.split("=");
            int parameterIndex = Integer.parseInt(fields[0]);
            float value = Float.parseFloat(fields[1]);
            switch(parameterIndex)
            {
                case 20:
                    AccDev[0] = (int) value;
                    break;
                case 21:
                    AccDev[1] = (int) value;
                    break;
                case 22:
                    AccDev[2] = (int) value;
                    break;
                case 23:
                    MagDev[0] = (int) value;
                    break;
                case 24:
                    MagDev[1] = (int) value;
                    break;
                case 25:
                    MagDev[2] = (int) value;
                    runOnUiThread(mParameterRunnable);
                    break;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void ProcessCalibration(String data) {
        try
        {
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
                        AccelCurr[i] = val;
                    }
                } else if (sensorType.equals("M")) {
                    for (int i = 0; i < 3; i++) {
                        float val = Float.parseFloat(values[i]);
                        MagCurr[i] = val;
                    }
                }
            }
            runOnUiThread(mSensorRunnable);
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
            case CALLBACK_TYPE_ORIENTATION:
                if (!ProcessOrientation(data))
                {
                    //reboot device
                    Intent commandIntent = new Intent();
                    commandIntent.setAction(getString(R.string.device_command));
                    commandIntent.putExtra("COMMAND", "reboot\r\n");
                    sendBroadcast(commandIntent);
                }
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

}
