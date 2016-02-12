package net.speleomaniac.mapit.sencemeterservice;

import android.bluetooth.BluetoothDevice;

public class DeviceItem
{
    private final BluetoothDevice device;
    public boolean isSelected = false;

    public DeviceItem(BluetoothDevice device)
    {
        this.device = device;
    }

    public String getName()
    {
        return device.getName();
    }

    public String getAddress()
    {
        return device.getAddress();
    }

}
