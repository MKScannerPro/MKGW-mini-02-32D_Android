package com.moko.mkmini0232d.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkmini0232d.R;
import com.moko.support.mini0232d.entity.DeviceInfo;

public class DeviceInfoAdapter extends BaseQuickAdapter<DeviceInfo, BaseViewHolder> {
    public DeviceInfoAdapter() {
        super(R.layout.item_devices_mini02_32d);
    }

    @Override
    protected void convert(BaseViewHolder helper, DeviceInfo item) {
        helper.setText(R.id.tv_device_name, item.name);
        helper.setText(R.id.tv_device_rssi, String.valueOf(item.rssi));
    }
}
