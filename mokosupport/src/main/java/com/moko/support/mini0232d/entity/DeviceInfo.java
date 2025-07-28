package com.moko.support.mini0232d.entity;

import java.io.Serializable;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class DeviceInfo implements Serializable {
    public String name;
    public int rssi;
    public String mac;
    public String scanRecord;
    public int deviceType;
    public boolean isFirstConfig;
    public ScanResult scanResult;
}
