package com.moko.support.mini0232d.event;

public class DeviceDeletedEvent {

    private int id;

    public DeviceDeletedEvent(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
