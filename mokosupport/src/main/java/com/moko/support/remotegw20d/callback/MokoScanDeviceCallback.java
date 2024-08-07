package com.moko.support.remotegw20d.callback;

import com.moko.support.remotegw20d.entity.DeviceInfo;

public interface MokoScanDeviceCallback {
    void onStartScan();

    void onScanDevice(DeviceInfo device);

    void onStopScan();
}
