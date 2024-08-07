package com.moko.mkmini0232d.activity.set;

import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkmini0232d.AppConstants;
import com.moko.mkmini0232d.R;
import com.moko.mkmini0232d.base.BaseActivity;
import com.moko.mkmini0232d.databinding.ActivityModifyWifiSettings20dBinding;
import com.moko.mkmini0232d.dialog.Bottom20DDialog;
import com.moko.mkmini0232d.entity.MQTTConfig;
import com.moko.mkmini0232d.entity.MokoDevice;
import com.moko.mkmini0232d.utils.SPUtiles;
import com.moko.mkmini0232d.utils.ToastUtils;
import com.moko.support.remotegw20d.MQTTConstants;
import com.moko.support.remotegw20d.MQTTSupport;
import com.moko.support.remotegw20d.entity.MsgConfigResult;
import com.moko.support.remotegw20d.entity.MsgReadResult;
import com.moko.support.remotegw20d.event.DeviceOnlineEvent;
import com.moko.support.remotegw20d.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModifyWifiSettings20DActivity extends BaseActivity<ActivityModifyWifiSettings20dBinding> {
    private final String FILTER_ASCII = "[ -~]*";

    private final String[] countryBrand = {"Argentina、Mexico"
            , "Australia、New Zealand"
            , "Bahrain、Egypt、Israel、India"
            , "Bolivia、Chile、China、El Salvador"
            , "Canada"
            , "Europe"
            , "Indonesia"
            , "Japan"
            , "Jordan"
            , "Korea、US"
            , "Latin America-1"
            , "Latin America-2"
            , "Latin America-3"
            , "Lebanon"
            , "Malaysia"
            , "Qatar"
            , "Russia"
            , "Singapore"
            , "Taiwan"
            , "Tunisia"
            , "Venezuela"
            , "Worldwide"
    };

    private int countrySelected;
    private Pattern pattern;
    private boolean wifiDhcpEnable;
    private String wifiIp;
    private String wifiMask;
    private String wifiGateway;
    private String wifiDns;
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;
    public Handler mHandler;

    @Override
    protected void onCreate() {
        String IP_REGEX = "((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))*";
        pattern = Pattern.compile(IP_REGEX);
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etSsid.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        mBind.etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});


        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDevice.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getWifiSettings();
        mBind.tvCountryBrand.setOnClickListener(v -> onSelectCountry());
        mBind.imgDhcp.setOnClickListener(v -> {
            wifiDhcpEnable = !wifiDhcpEnable;
            setDhcpEnable(wifiDhcpEnable);
        });
    }

    @Override
    protected ActivityModifyWifiSettings20dBinding getViewBinding() {
        return ActivityModifyWifiSettings20dBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message)) return;
        int msg_id;
        try {
            JsonObject object = new Gson().fromJson(message, JsonObject.class);
            JsonElement element = object.get("msg_id");
            msg_id = element.getAsInt();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (msg_id == MQTTConstants.READ_MSG_ID_WIFI_SETTINGS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            mBind.etSsid.setText(result.data.get("ssid").getAsString());
            mBind.etPassword.setText(result.data.get("passwd").getAsString());
            countrySelected = result.data.get("wifi_channel").getAsInt();
            mBind.tvCountryBrand.setText(countryBrand[countrySelected]);
            getNetworkSettings();
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_NETWORK_SETTINGS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            wifiDhcpEnable = result.data.get("dhcp_en").getAsInt() == 1;
            wifiIp = result.data.get("ip").getAsString();
            wifiMask = result.data.get("netmask").getAsString();
            wifiGateway = result.data.get("gw").getAsString();
            wifiDns = result.data.get("dns").getAsString();
            setDhcpEnable(wifiDhcpEnable);
            setIpInfo();
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_WIFI_SETTINGS) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (result.result_code != 0) return;
            setNetworkSettings();
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_NETWORK_SETTINGS) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.result_code == 0) {
                ToastUtils.showToast(this, "Set up succeed");
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
        }
    }

    private void setDhcpEnable(boolean enable) {
        mBind.imgDhcp.setImageResource(enable ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        mBind.clIp.setVisibility(enable ? View.GONE : View.VISIBLE);
    }

    private void setIpInfo() {
        mBind.etIp.setText(wifiIp);
        mBind.etMask.setText(wifiMask);
        mBind.etGateway.setText(wifiGateway);
        mBind.etDns.setText(wifiDns);
    }


    private void onSelectCountry() {
        if (isWindowLocked()) return;
        Bottom20DDialog dialog = new Bottom20DDialog();
        dialog.setDatas(new ArrayList<>(Arrays.asList(countryBrand)), countrySelected);
        dialog.setListener(value -> {
            countrySelected = value;
            mBind.tvCountryBrand.setText(countryBrand[value]);
        });
        dialog.show(getSupportFragmentManager());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        super.offline(event, mMokoDevice.mac);
    }

    public void onBack(View view) {
        finish();
    }

    private void setWifiSettings() {
        String ssid = mBind.etSsid.getText().toString();
        String password = mBind.etPassword.getText().toString();
        int msgId = MQTTConstants.CONFIG_MSG_ID_WIFI_SETTINGS;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("ssid", ssid);
        jsonObject.addProperty("passwd", password);
        jsonObject.addProperty("wifi_channel", countrySelected);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setNetworkSettings() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_NETWORK_SETTINGS;
        JsonObject jsonObject = new JsonObject();
        int enable = wifiDhcpEnable ? 1 : 0;
        jsonObject.addProperty("dhcp_en", enable);
        jsonObject.addProperty("ip", mBind.etIp.getText().toString());
        jsonObject.addProperty("netmask", mBind.etMask.getText().toString());
        jsonObject.addProperty("gw", mBind.etGateway.getText().toString());
        jsonObject.addProperty("dns", mBind.etDns.getText().toString());
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getWifiSettings() {
        int msgId = MQTTConstants.READ_MSG_ID_WIFI_SETTINGS;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getNetworkSettings() {
        int msgId = MQTTConstants.READ_MSG_ID_NETWORK_SETTINGS;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (!isParaError()) {
            saveParams();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isParaError() {
        String ssid = mBind.etSsid.getText().toString();
        if (TextUtils.isEmpty(ssid)) return true;
        if (!wifiDhcpEnable) {
            //检查ip地址是否合法
            String ip = mBind.etIp.getText().toString();
            String mask = mBind.etMask.getText().toString();
            String gateway = mBind.etGateway.getText().toString();
            String dns = mBind.etDns.getText().toString();
            Matcher matcherIp = pattern.matcher(ip);
            Matcher matcherMask = pattern.matcher(mask);
            Matcher matcherGateway = pattern.matcher(gateway);
            Matcher matcherDns = pattern.matcher(dns);
            if (!matcherIp.matches()
                    || !matcherMask.matches()
                    || !matcherGateway.matches()
                    || !matcherDns.matches())
                return true;
            wifiIp = ip;
            wifiMask = mask;
            wifiGateway = gateway;
            wifiDns = dns;
        }
        return false;
    }

    private void saveParams() {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        XLog.i("查询设备当前状态");
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 50 * 1000);
        showLoadingProgressDialog();
        setWifiSettings();
    }

}
