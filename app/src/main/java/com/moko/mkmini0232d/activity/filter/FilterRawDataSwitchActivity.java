package com.moko.mkmini0232d.activity.filter;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkmini0232d.AppConstants;
import com.moko.mkmini0232d.R;
import com.moko.mkmini0232d.base.BaseActivity;
import com.moko.mkmini0232d.databinding.ActivityFilterRawDataMini2032dBinding;
import com.moko.mkmini0232d.entity.MQTTConfig;
import com.moko.mkmini0232d.entity.MokoDevice;
import com.moko.mkmini0232d.utils.SPUtiles;
import com.moko.mkmini0232d.utils.ToastUtils;
import com.moko.support.mini0232d.MQTTConstants;
import com.moko.support.mini0232d.MQTTSupport;
import com.moko.support.mini0232d.entity.MsgConfigResult;
import com.moko.support.mini0232d.entity.MsgReadResult;
import com.moko.support.mini0232d.event.DeviceOnlineEvent;
import com.moko.support.mini0232d.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class FilterRawDataSwitchActivity extends BaseActivity<ActivityFilterRawDataMini2032dBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;
    public Handler mHandler;
    private boolean isBXPDeviceInfoOpen;
    private boolean isBXPAccOpen;
    private boolean isBXPTHOpen;

    @Override
    protected void onCreate() {
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
        getFilterRawDataSwitch();
    }

    @Override
    protected ActivityFilterRawDataMini2032dBinding getViewBinding() {
        return ActivityFilterRawDataMini2032dBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message)) return;
        int msg_id;
        try {
            JsonObject object = new Gson().fromJson(message, JsonObject.class);
            JsonElement element = object.get("msg_id");
            msg_id = element.getAsInt();
        } catch (Exception e) {
            XLog.e(e);
            return;
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_RAW_DATA_SWITCH) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mBind.tvFilterByIbeacon.setText(result.data.get("ibeacon").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByUid.setText(result.data.get("eddystone_uid").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByUrl.setText(result.data.get("eddystone_url").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByTlm.setText(result.data.get("eddystone_tlm").getAsInt() == 1 ? "ON" : "OFF");
            isBXPDeviceInfoOpen = result.data.get("bxp_devinfo").getAsInt() == 1;
            isBXPAccOpen = result.data.get("bxp_acc").getAsInt() == 1;
            isBXPTHOpen = result.data.get("bxp_th").getAsInt() == 1;
            mBind.ivFilterByBxpInfo.setImageResource(isBXPDeviceInfoOpen ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            mBind.ivFilterByBxpAcc.setImageResource(isBXPAccOpen ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            mBind.ivFilterByBxpTh.setImageResource(isBXPTHOpen ? R.drawable.ic_cb_open : R.drawable.ic_cb_close);
            mBind.tvFilterByBxpButton.setText(result.data.get("bxp_button").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByBxpTag.setText(result.data.get("bxp_tag").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByPir.setText(result.data.get("pir").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByTof.setText(result.data.get("mk_tof").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByOther.setText(result.data.get("other").getAsInt() == 1 ? "ON" : "OFF");
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_DEVICE_INFO
                || msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_ACC
                || msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_TH) {
            Type type = new TypeToken<MsgConfigResult<?>>() {
            }.getType();
            MsgConfigResult<?> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (result.result_code == 0) {
                getFilterRawDataSwitch();
                ToastUtils.showToast(this, "Set up succeed");
            } else {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                ToastUtils.showToast(this, "Set up failed");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        super.offline(event, mMokoDevice.mac);
    }

    private void getFilterRawDataSwitch() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_RAW_DATA_SWITCH;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void onFilterByBXPDeviceInfo(View view) {
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setBXPDevice();
    }

    public void onFilterByBXPAcc(View view) {
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setBXPAcc();
    }

    public void onFilterByBXPTH(View view) {
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setBXPTH();
    }

    private void setBXPDevice() {
        isBXPDeviceInfoOpen = !isBXPDeviceInfoOpen;
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_DEVICE_INFO;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", isBXPDeviceInfoOpen ? 1 : 0);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    private void setBXPAcc() {
        isBXPAccOpen = !isBXPAccOpen;
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_ACC;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", isBXPAccOpen ? 1 : 0);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    private void setBXPTH() {
        isBXPTHOpen = !isBXPTHOpen;
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_TH;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", isBXPTHOpen ? 1 : 0);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    public void onFilterByIBeacon(View view) {
        start(FilterIBeaconActivity.class);
    }

    public void onFilterByUid(View view) {
        start(FilterUIDActivity.class);
    }

    public void onFilterByUrl(View view) {
        start(FilterUrlActivity.class);
    }

    public void onFilterByTlm(View view) {
        start(FilterTLMActivity.class);
    }

    public void onFilterByBXPButton(View view) {
        start(FilterBXPButtonActivity.class);
    }

    public void onFilterByBXPTag(View view) {
        start(FilterBXPTagActivity.class);
    }

    public void onFilterByPIRPresence(View view) {
        start(FilterPIRActivity.class);
    }

    public void onFilterByMKTOF(View view) {
        start(FilterMKTOFActivity.class);
    }


    public void onFilterByOther(View view) {
        start(FilterOtherActivity.class);
    }

    private void start(Class<?> clazz) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, clazz);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startFilterDetail.launch(i);
    }

    private final ActivityResultLauncher<Intent> startFilterDetail = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getFilterRawDataSwitch();
    });
}
