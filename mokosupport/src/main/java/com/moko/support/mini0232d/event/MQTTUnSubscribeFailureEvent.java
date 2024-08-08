package com.moko.support.mini0232d.event;

public class MQTTUnSubscribeFailureEvent {
    private String topic;

    public MQTTUnSubscribeFailureEvent(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }
}
