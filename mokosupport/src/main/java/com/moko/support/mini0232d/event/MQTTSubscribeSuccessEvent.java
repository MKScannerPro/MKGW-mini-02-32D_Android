package com.moko.support.mini0232d.event;

public class MQTTSubscribeSuccessEvent {
    private String topic;

    public MQTTSubscribeSuccessEvent(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }
}
