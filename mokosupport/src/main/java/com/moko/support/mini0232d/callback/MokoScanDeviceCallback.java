package com.moko.support.mini0232d.callback;

import com.moko.support.mini0232d.entity.DeviceInfo;

public interface MokoScanDeviceCallback {
    void onStartScan();

    void onScanDevice(DeviceInfo device);

    void onStopScan();
}
