package net.speleomaniac.mapit.sencemeterservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class DeviceSelectActivity extends ActionBarActivity implements ListView.OnItemClickListener
{
    private MenuItem mScanMenuItem;

    private BluetoothAdapter mBtAdapter;
    private BroadcastReceiver mBroadcastReceiver;
    private DeviceAdapter mDeviceAdapter;
    private String selectedDeviceAddress;
    private String selectedDeviceName;
    private View mView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(progressBar);

        setContentView(R.layout.deviceselect_activity);
        mView = findViewById(R.id.main_window);
        selectedDeviceAddress = getString(R.string.no_device_address);
        selectedDeviceName = getString(R.string.unknown_device_name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBroadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent == null)
                    return;
                String action = intent.getAction();
                if (action == null)
                    return;

                Bundle extras = intent.getExtras();

                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        if (extras != null) {
                            int state = extras.getInt(BluetoothAdapter.EXTRA_STATE);
                            if (state == BluetoothAdapter.STATE_ON) {
                                mDeviceAdapter.clear();
                                PopulateDevices();
                            } else if (state == BluetoothAdapter.STATE_OFF) {
                                mDeviceAdapter.clear();
                            }
                        }
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null)
                            AddBluetoothDevice(device);
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        setSupportProgressBarIndeterminateVisibility(true);
                        setSupportProgressBarVisibility(true);
                        mScanMenuItem.setTitle(getString(R.string.stop_scan));
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        setSupportProgressBarIndeterminateVisibility(false);
                        setSupportProgressBarVisibility(false);
                        mScanMenuItem.setTitle(getString(R.string.start_scan));
                        break;
                }
            }
        };

        IntentFilter broadcastIntentFilter = new IntentFilter();
        broadcastIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        broadcastIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        broadcastIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        broadcastIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);

        registerReceiver(mBroadcastReceiver, broadcastIntentFilter);
        mView.post(mDiscoveryRunnable);
    }

    @Override
    public void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        getSupportActionBar().getCustomView().setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_device_select, menu);
        mScanMenuItem = menu.findItem(R.id.scan_devices);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
    {
        case android.R.id.home:
            onBackPressed();
            return true;
        case R.id.scan_devices:
            if (mBtAdapter.isDiscovering())
                mBtAdapter.cancelDiscovery();
            else
                mBtAdapter.startDiscovery();
            return true;
        default:
            return super.onOptionsItemSelected(item);
    }
    }

    private void PopulateDevices()
    {
        Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
        if (bondedDevices != null)
            for (BluetoothDevice device : bondedDevices)
                AddBluetoothDevice(device);
        mBtAdapter.startDiscovery();

    }

    private void AddBluetoothDevice(BluetoothDevice device)
    {
        //eger bu device varsa sil önceden
        for (DeviceItem dev : mDeviceAdapter.devices)
        {
            if (dev.getAddress().equals(device.getAddress()))
            {
                mDeviceAdapter.devices.remove(dev);
                break;
            }
        }

        DeviceItem item = new DeviceItem(device);
        if (device.getAddress().equals(selectedDeviceAddress))
        {
            selectedDeviceName = item.getName();
            item.isSelected = true;
        }

        mDeviceAdapter.add(item);
        mDeviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        selectedDeviceAddress = SettingsActivity.SharedPrefs.getString("prefs_selected_device_address", getString(R.string.no_device_address));
        selectedDeviceName = SettingsActivity.SharedPrefs.getString("prefs_selected_device_name", getString(R.string.unknown_device_name));
        super.onResume();
    }

    private final Runnable mDiscoveryRunnable = new Runnable() {
        @Override
        public void run() {
            if (mView == null) return;

            // get a reference to the progress bar
            ListView mDeviceList = (ListView) mView.findViewById(R.id.device_list);
            mDeviceAdapter = new DeviceAdapter(DeviceSelectActivity.this, R.id.device_item_name, new ArrayList<DeviceItem>());
            mDeviceList.setAdapter(mDeviceAdapter);
            mDeviceList.setOnItemClickListener(DeviceSelectActivity.this);

            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBtAdapter == null)
            {
                Toast.makeText(DeviceSelectActivity.this, getString(R.string.adapter_not_found), Toast.LENGTH_SHORT).show();
                mScanMenuItem.setVisible(false);
                return;
            }

            if (!mBtAdapter.isEnabled())
                mBtAdapter.enable();
            else
            {
                if (mBtAdapter.isDiscovering())
                {
                    setSupportProgressBarIndeterminateVisibility(true);
                    //setSupportProgressBarVisibility(true);
                    mScanMenuItem.setTitle(getString(R.string.stop_scan));
                }
                PopulateDevices();
            }

            if (mBtAdapter == null)
                mScanMenuItem.setEnabled(false);
        }
    };

    @Override
    public void onPause()
    {
        setSupportProgressBarIndeterminateVisibility(false);
        //setSupportProgressBarVisibility(false);
        SettingsActivity.SharedPrefs.edit().putString("prefs_selected_device_address", selectedDeviceAddress).commit();
        SettingsActivity.SharedPrefs.edit().putString("prefs_selected_device_name", selectedDeviceName).commit();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mBroadcastReceiver != null)
            unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        for (DeviceItem dev : mDeviceAdapter.devices)
            dev.isSelected = false;
        DeviceItem dev = mDeviceAdapter.devices.get(position);
        dev.isSelected = true;
        selectedDeviceAddress = dev.getAddress();
        selectedDeviceName = dev.getName();
        mDeviceAdapter.notifyDataSetChanged();
    }
}
