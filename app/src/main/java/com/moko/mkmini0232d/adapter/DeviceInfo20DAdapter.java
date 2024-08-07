package com.moko.mkmini0232d.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkmini0232d.R;
import com.moko.support.remotegw20d.entity.DeviceInfo;

public class DeviceInfo20DAdapter extends BaseQuickAdapter<DeviceInfo, BaseViewHolder> {
    public DeviceInfo20DAdapter() {
        super(R.layout.item_devices);
    }

    @Override
    protected void convert(BaseViewHolder helper, DeviceInfo item) {
        helper.setText(R.id.tv_device_name, item.name);
        helper.setText(R.id.tv_device_rssi, String.valueOf(item.rssi));
    }
}
