package com.moko.mkmini0232d.entity;

import java.io.Serializable;
import java.util.ArrayList;

public class AdvChannelInfo implements Serializable {
    public String mac;
    public int result_code;
    public ArrayList<AdvChannel> adv_param;
}
