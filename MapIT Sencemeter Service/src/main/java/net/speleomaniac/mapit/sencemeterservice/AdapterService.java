package net.speleomaniac.mapit.sencemeterservice;

import android.app.Notification;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
//import android.support.annotation.NonNull;
//import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AdapterService extends Service
{
    //private static AdapterService Instance = null;
    private Thread mThread = null;
    private static boolean mRunning = false;
    private BluetoothSocket mSocket = null;
    private static boolean mSocketConnected = false;
    //private static final String TAG = "MapITSencemeter";
    private NotificationCompat.Builder mNotBuilder;
    private NotificationManagerCompat mNotifier;
    private SharedPreferences mSharedPrefs;
    private BroadcastReceiver mCommandReceiver;

    private static boolean shotContinous = false;

    public static void setContinous(boolean state) {
        shotContinous = state;
    }

    private OutputStream outStream = null;

    private static final List<CallbackInterface> callbackInterfaces = new ArrayList<>();

    public static boolean IsActive() {
        return mRunning;
    }

    public static boolean IsConnected() {
        return mSocketConnected;
    }

    public static void AddReceiver(CallbackInterface iface) {
        if (!callbackInterfaces.contains(iface))
            callbackInterfaces.add(iface);
    }

    public static void RemoveReceiver(CallbackInterface iface) {
        if (callbackInterfaces.contains(iface))
            callbackInterfaces.remove(iface);
    }

    @Override
    public void onCreate() {
//        Instance = this;
        super.onCreate();

        if (mCommandReceiver == null)
        {
            mCommandReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    Bundle extras = intent.getExtras();
                    if (extras != null && extras.containsKey("COMMAND"))
                        SendDeviceCommand(extras.getString("COMMAND"));

                }
            };
            IntentFilter intentFilter = new IntentFilter("net.speleomaniac.mapit.sencemeterservice.COMMAND");
            registerReceiver(mCommandReceiver, intentFilter);
        }

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mNotifier = NotificationManagerCompat.from(this);//getSystemService(Context.NOTIFICATION_SERVICE);
        mNotBuilder = new NotificationCompat.Builder(getApplicationContext());
        mNotBuilder.setSmallIcon(R.drawable.ic_launcher);
        mNotBuilder.setContentTitle(getString(R.string.app_name));
        mNotBuilder.setOngoing(true);
    }

    void SendDeviceCommand(String command) {
        try {
            if (outStream != null)
                outStream.write(command.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mThread == null)
        {
            mRunning = true;
            mThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    ThreadRunner();
                }
            });
            mThread.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void ThreadRunner() {
        long messagesReceived = 0;
        byte[] byteBuffer = new byte[2048];

        mSharedPrefs.edit().putBoolean("prefs_enable_service", true).apply();

        for (CallbackInterface cb: callbackInterfaces)
            cb.OnStart();

        BluetoothAdapter mBtAdapter;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter != null)
        {
            if (!mBtAdapter.isEnabled())
                mBtAdapter.enable();

            String deviceAddress = mSharedPrefs.getString("prefs_selected_device_address", getString(R.string.no_device_address));
            if (deviceAddress.length() == 0 || deviceAddress.equals(getString(R.string.no_device_address)))
            {
                stopSelf();
                return;
            }

            Intent dataIntent = new Intent();
            dataIntent.setAction(getString(R.string.device_data));

            //Intent calibIntent = new Intent();
            //calibIntent.setAction("net.speleomaniac.mapit.CALIB");

            mNotBuilder.setContentText(getString(R.string.service_starting));
            Intent intent = new Intent(this, SettingsActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            mNotBuilder.setContentIntent(pendingIntent);
            Notification mNotification = mNotBuilder.build();
            //mNotification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            mNotifier.notify(1, mNotification);

            if (mBtAdapter.isDiscovering())
                mBtAdapter.cancelDiscovery();

            BluetoothDevice device = mBtAdapter.getRemoteDevice(deviceAddress);
            InputStream inStream = null;


            //byte[] inBuffer = new byte[1];
            //long currTime = System.currentTimeMillis();
            //long lastTime = currTime;

            String receiveBuffer = "";
            boolean btState, prevState = mBtAdapter.isEnabled();
            while (mRunning)
            {
                //eğer bluetooth disable olursa servisi stop et
                try
                {
                    btState = mBtAdapter.isEnabled();
                    if (!btState)
                    {
                        if (prevState)
                        {
                            mNotBuilder.setContentText(getString(R.string.bluetooth_is_disabled));
                            mNotification = mNotBuilder.build();
                            //mNotification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
                            mNotifier.notify(1, mNotification);
                            prevState = false;
                        }
                        Thread.sleep(500);
                        continue;
                    }
                    else
                    {
                        if (!prevState)
                        {
                            mNotBuilder.setContentText(getString(R.string.bluetooth_is_enabled));
                            mNotBuilder.setSmallIcon(R.drawable.ic_launcher);
                            mNotification = mNotBuilder.build();
                            //mNotification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
                            mNotifier.notify(1, mNotification);
                            prevState = true;
                        }
                    }

                    if (mSocket == null)
                    {
                        Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                        mSocket = (BluetoothSocket) m.invoke(device, 1);
                        mSocketConnected = false;
                        Thread.sleep(1000);
                        continue;
                    }

                    if (!mSocketConnected)
                    {
                        mSocket.connect();
                        mSocketConnected = true;
                        inStream = mSocket.getInputStream();
                        outStream = mSocket.getOutputStream();
                        mNotBuilder.setContentText(getString(R.string.device_is_connected));
                        mNotBuilder.setSmallIcon(R.drawable.ic_connected);
                        mNotification = mNotBuilder.build();
                        //mNotification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
                        mNotifier.notify(1, mNotification);
                        receiveBuffer = "";

                        if (mSharedPrefs.getBoolean("prefs_beep_on_connectevent", false))
                            RingtoneManager.getRingtone(this, Uri.parse("android.resource://" + getPackageName() + "/raw/connect")).play();

                        for(CallbackInterface cb: callbackInterfaces)
                            cb.OnConnect();

                        continue;
                    }

                    if (inStream != null) {
                        int bytesAvailable = inStream.available();
                        if (bytesAvailable > 0) {
                            if (bytesAvailable > byteBuffer.length)
                                bytesAvailable = byteBuffer.length;
                            bytesAvailable = inStream.read(byteBuffer, 0, bytesAvailable);
//                            int byteReceived = inStream.read();
//                            char theByte = (char) byteReceived;
                            if (bytesAvailable > 0) {
                                receiveBuffer += new String(byteBuffer, 0, bytesAvailable, "US-ASCII");

                                int idx = receiveBuffer.indexOf("\r\n");
                                while (idx != -1) {
                                    String incomingData = receiveBuffer.substring(0, idx);
                                    receiveBuffer = receiveBuffer.substring(idx + 2);
                                    idx = receiveBuffer.indexOf("\r\n");

                                    if (incomingData.length() == 0)
                                        continue;
                                    //Log.d("BTDATA", incomingData);

                                    if (incomingData.startsWith("[R]")) {
                                        dataIntent.removeExtra("DATA");
                                        dataIntent.putExtra("DATA", incomingData.substring(3));

                                        sendBroadcast(dataIntent);
                                        messagesReceived++;

                                        mNotBuilder.setSubText(getString(R.string.num_messages_received) + String.valueOf(messagesReceived));
                                        mNotification = mNotBuilder.build();
                                        //mNotification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
                                        mNotifier.notify(1, mNotification);

                                        if (mSharedPrefs.getBoolean("prefs_beep_on_message", false))
                                            RingtoneManager.getRingtone(this, Uri.parse("android.resource://" + getPackageName() + "/raw/message_received")).play();

                                        for (CallbackInterface cb : callbackInterfaces)
                                            cb.OnReceive(CallbackType.CALLBACK_TYPE_SHOT, incomingData.substring(3));

                                        if (shotContinous)
                                            SendDeviceCommand("read\r\n");
                                    } else if (incomingData.startsWith("[C]")) {
                                        for (CallbackInterface cb : callbackInterfaces)
                                            cb.OnReceive(CallbackType.CALLBACK_TYPE_CALIBRATION, incomingData.substring(3));
                                    } else if (incomingData.startsWith("[O]")) {
                                        for (CallbackInterface cb : callbackInterfaces)
                                            cb.OnReceive(CallbackType.CALLBACK_TYPE_ORIENTATION, incomingData.substring(3));
                                    } else if (incomingData.equals("[H]")) {
                                        if (mSharedPrefs.getBoolean("prefs_beep_on_heartbeat", false))
                                            RingtoneManager.getRingtone(this, Uri.parse("android.resource://" + getPackageName() + "/raw/heartbeat")).play();
                                        if (shotContinous)
                                            SendDeviceCommand("read\r\n");
                                    } else if (incomingData.startsWith("[P]")) {
                                        for (CallbackInterface cb : callbackInterfaces)
                                            cb.OnReceive(CallbackType.CALLBACK_TYPE_PARAMETER, incomingData.substring(3));
                                    } else if (incomingData.startsWith("[S]OK")) {
                                        if (mSharedPrefs.getBoolean("prefs_beep_on_startup", false))
                                            RingtoneManager.getRingtone(this, Uri.parse("android.resource://" + getPackageName() + "/raw/startup_finished")).play();
                                    } else if (incomingData.startsWith("[D]End ")) {
                                        if (mSharedPrefs.getBoolean("prefs_beep_on_endcalib", false))
                                            RingtoneManager.getRingtone(this, Uri.parse("android.resource://" + getPackageName() + "/raw/calib_finished")).play();
                                    }
                                }
                            }
                        }
                        else
                            Thread.sleep(1);
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                    break;
                }
                catch (InvocationTargetException e)
                {
                    e.printStackTrace();
                    break;
                }
                catch (NoSuchMethodException e)
                {
                    e.printStackTrace();
                    break;
                }
                catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                    break;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    try
                    {
                        //if (inStream != null)
                        //    inStream.close();
                        //if (outStream != null)
                        //    outStream.close();
                        if (mSocket != null)
                            mSocket.close();
                    }
                    catch (IOException e1)
                    {
                        e1.printStackTrace();
                    }
                    mSocket = null;
                    inStream = null;
                    outStream = null;
                    mSocketConnected = false;


                    if (mRunning && mSharedPrefs.getBoolean("prefs_beep_on_connectevent", false))
                        RingtoneManager.getRingtone(this, Uri.parse("android.resource://" + getPackageName() + "/raw/disconnect")).play();

                    for(CallbackInterface cb: callbackInterfaces)
                        cb.OnDisconnect();

                    mNotBuilder.setContentText(getString(R.string.device_is_disconnected));
                    mNotBuilder.setSmallIcon(R.drawable.ic_launcher);
                    mNotification = mNotBuilder.build();
                    mNotifier.notify(1, mNotification);
                }
            }

            if (mSocket != null)
            {
                try
                {
                    mSocket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                mSocket = null;
                mSocketConnected = false;
                if (mRunning && mSharedPrefs.getBoolean("prefs_beep_on_connectevent", false))
                    RingtoneManager.getRingtone(this, Uri.parse("android.resource://" + getPackageName() + "/raw/disconnect")).play();
                for(CallbackInterface cb: callbackInterfaces)
                    cb.OnDisconnect();
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Log.v("SENCER", "Canceling notification");
        mNotifier.cancelAll();

        //Log.v("SENCER", "Thread finished");

        for (CallbackInterface cb: callbackInterfaces)
            cb.OnStop();

        mSharedPrefs.edit().putBoolean("prefs_enable_service", false).apply();
    }

    @Override
    public void onDestroy()
    {
        if (mCommandReceiver != null)
        {
            unregisterReceiver(mCommandReceiver);
            mCommandReceiver = null;
        }

        if (mThread != null)
        {
            mRunning = false;
            try
            {
                mThread.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        super.onDestroy();
        //callbackInterfaces.clear();
    }
}
