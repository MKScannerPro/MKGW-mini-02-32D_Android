package com.moko.support.remotegw20d.entity;

public class MsgConfigReq<T> {
    public int msg_id;
    public MsgDeviceInfo device_info;
    public T data;
}
