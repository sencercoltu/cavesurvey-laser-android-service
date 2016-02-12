package net.speleomaniac.mapit.sencemeterservice;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceAdapter extends ArrayAdapter<DeviceItem>
{
    public final ArrayList<DeviceItem> devices;

    public DeviceAdapter(Context context, int textViewResourceId, ArrayList<DeviceItem> devices)
    {
        super(context, textViewResourceId, devices);
        this.devices = devices;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listitem_device_select, null);
        }
        DeviceItem i = devices.get(position);

        if (i != null)
        {
            if (convertView != null)
            {
                CheckedTextView txtName = (CheckedTextView) convertView.findViewById(R.id.device_item_name);
                TextView txtAddress = (TextView) convertView.findViewById(R.id.device_item_address);

                if (txtName != null)
                {
                    txtName.setText(i.getName());
                    txtName.setChecked(i.isSelected);
                }
                if (txtAddress != null)
                {
                    txtAddress.setText(i.getAddress());
                }
            }
        }

        return convertView;

    }
}
