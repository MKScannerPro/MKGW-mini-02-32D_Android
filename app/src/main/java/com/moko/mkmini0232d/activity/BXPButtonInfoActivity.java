package com.moko.mkmini0232d.activity;

import android.annotation.SuppressLint;
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
import com.moko.mkmini0232d.databinding.ActivityBxpButtonInfoMini0232dBinding;
import com.moko.mkmini0232d.db.DBTools;
import com.moko.mkmini0232d.dialog.AlertMessageDialog;
import com.moko.mkmini0232d.dialog.LedBuzzerControlDialog;
import com.moko.mkmini0232d.entity.MQTTConfig;
import com.moko.mkmini0232d.entity.MokoDevice;
import com.moko.mkmini0232d.utils.SPUtiles;
import com.moko.mkmini0232d.utils.ToastUtils;
import com.moko.support.mini0232d.MQTTConstants;
import com.moko.support.mini0232d.MQTTSupport;
import com.moko.support.mini0232d.entity.BXPButtonInfo;
import com.moko.support.mini0232d.entity.MsgNotify;
import com.moko.support.mini0232d.event.DeviceModifyNameEvent;
import com.moko.support.mini0232d.event.DeviceOnlineEvent;
import com.moko.support.mini0232d.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class BXPButtonInfoActivity extends BaseActivity<ActivityBxpButtonInfoMini0232dBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;
    private BXPButtonInfo mBXPButtonInfo;
    private Handler mHandler;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate() {
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDevice.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());

        mBXPButtonInfo = (BXPButtonInfo) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_BXP_BUTTON_INFO);
        mBind.tvDeviceName.setText(mMokoDevice.name);
        mBind.tvProductModel.setText(mBXPButtonInfo.product_model);
        mBind.tvManufacturer.setText(mBXPButtonInfo.company_name);
        mBind.tvDeviceFirmwareVersion.setText(mBXPButtonInfo.firmware_version);
        mBind.tvDeviceHardwareVersion.setText(mBXPButtonInfo.hardware_version);
        mBind.tvDeviceSoftwareVersion.setText(mBXPButtonInfo.software_version);
        mBind.tvDeviceMac.setText(mBXPButtonInfo.mac);
        mBind.tvBatteryVoltage.setText(String.format("%dmV", mBXPButtonInfo.battery_v));
        mBind.tvSinglePressCount.setText(String.valueOf(mBXPButtonInfo.single_alarm_num));
        mBind.tvDoublePressCount.setText(String.valueOf(mBXPButtonInfo.double_alarm_num));
        mBind.tvLongPressCount.setText(String.valueOf(mBXPButtonInfo.long_alarm_num));
        String alarmStatusStr;
        if (mBXPButtonInfo.alarm_status == 0) {
            alarmStatusStr = "Not triggered";
        } else {
            StringBuilder modeStr = new StringBuilder();
            if ((mBXPButtonInfo.alarm_status & 0x01) == 0x01)
                modeStr.append("1&");
            if ((mBXPButtonInfo.alarm_status & 0x02) == 0x02)
                modeStr.append("2&");
            if ((mBXPButtonInfo.alarm_status & 0x04) == 0x04)
                modeStr.append("3&");
            if ((mBXPButtonInfo.alarm_status & 0x08) == 0x08)
                modeStr.append("4&");
            String mode = modeStr.substring(0, modeStr.length() - 1);
            alarmStatusStr = String.format("Mode %s triggered", mode);
        }
        mBind.tvAlarmStatus.setText(alarmStatusStr);
        mBind.tvLedControl.setOnClickListener(v -> showControl(0));
        mBind.tvBuzzerControl.setOnClickListener(v -> showControl(1));
    }

    @Override
    protected ActivityBxpButtonInfoMini0232dBinding getViewBinding() {
        return ActivityBxpButtonInfoMini0232dBinding.inflate(getLayoutInflater());
    }

    private void showControl(int index) {
        LedBuzzerControlDialog dialog = new LedBuzzerControlDialog(index);
        dialog.setOnConfirmClickListener((duration, interval, from) -> {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            int msgId = from == 0 ? MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_LED : MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_BUZZER;
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("mac", mBXPButtonInfo.mac);
            jsonObject.addProperty(from == 0 ? "flash_time" : "ring_time", duration);
            jsonObject.addProperty(from == 0 ? "flash_interval" : "ring_interval", interval);
            String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
            try {
                MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
            } catch (MqttException e) {
                XLog.e(e);
            }
        });
        dialog.showNow(getSupportFragmentManager(), "control");
    }

    @SuppressLint("DefaultLocale")
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
            XLog.e(e);
            return;
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_STATUS) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BXPButtonInfo>>() {
            }.getType();
            MsgNotify<BXPButtonInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            BXPButtonInfo bxpButtonInfo = result.data;
            if (bxpButtonInfo.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            ToastUtils.showToast(this, "Setup succeed!");
            mBind.tvBatteryVoltage.setText(String.format("%dmV", bxpButtonInfo.battery_v));
            mBind.tvSinglePressCount.setText(String.valueOf(bxpButtonInfo.single_alarm_num));
            mBind.tvDoublePressCount.setText(String.valueOf(bxpButtonInfo.double_alarm_num));
            mBind.tvLongPressCount.setText(String.valueOf(bxpButtonInfo.long_alarm_num));
            String alarmStatusStr;
            if (bxpButtonInfo.alarm_status == 0) {
                alarmStatusStr = "Not triggered";
            } else {
                StringBuilder modeStr = new StringBuilder();
                if ((bxpButtonInfo.alarm_status & 0x01) == 0x01)
                    modeStr.append("1&");
                if ((bxpButtonInfo.alarm_status & 0x02) == 0x02)
                    modeStr.append("2&");
                if ((bxpButtonInfo.alarm_status & 0x04) == 0x04)
                    modeStr.append("3&");
                if ((bxpButtonInfo.alarm_status & 0x08) == 0x08)
                    modeStr.append("4&");
                String mode = modeStr.substring(0, modeStr.length() - 1);
                alarmStatusStr = String.format("Mode %s triggered", mode);
            }
            mBind.tvAlarmStatus.setText(alarmStatusStr);
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_DISMISS_ALARM) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BXPButtonInfo>>() {
            }.getType();
            MsgNotify<BXPButtonInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            BXPButtonInfo bxpButtonInfo = result.data;
            if (bxpButtonInfo.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            ToastUtils.showToast(this, "Setup succeed!");
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            getBXPButtonStatus();
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_DISCONNECTED
                || msg_id == MQTTConstants.CONFIG_MSG_ID_BLE_DISCONNECT) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            ToastUtils.showToast(this, "Bluetooth disconnect");
            finish();
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_LED || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_BUZZER) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BXPButtonInfo>>() {
            }.getType();
            MsgNotify<BXPButtonInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            ToastUtils.showToast(this, result.data.result_code == 0 ? "Setup succeed!" : "Setup failed");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        MokoDevice device = DBTools.getInstance(getApplicationContext()).selectDevice(mMokoDevice.mac);
        mMokoDevice.name = device.name;
        mBind.tvDeviceName.setText(mMokoDevice.name);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        String mac = event.getMac();
        if (!mMokoDevice.mac.equals(mac)) return;
        boolean online = event.isOnline();
        if (!online) {
            ToastUtils.showToast(this, "device is off-line");
            finish();
        }
    }

    public void onDFU(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent intent = new Intent(this, BeaconDFUActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, mBXPButtonInfo.mac);
        startBeaconDFU.launch(intent);
    }
    private final ActivityResultLauncher<Intent> startBeaconDFU = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && null != result.getData()) {
            int code = result.getData().getIntExtra("code", 0);
            if (code != 3) {
                ToastUtils.showToast(this, "Bluetooth disconnect");
                finish();
            }
        }
    });

    public void onReadBXPButtonStatus(View view) {
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getBXPButtonStatus();
    }

    public void onDismissAlarmStatus(View view) {
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        dismissAlarmStatus();
    }

    public void onDisconnect(View view) {
        if (isWindowLocked()) return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage("Please confirm again whether to disconnect the gateway from BLE devices?");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            disconnectDevice();
        });
        dialog.show(getSupportFragmentManager());
    }

    private void disconnectDevice() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_DISCONNECT;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mBXPButtonInfo.mac);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    private void getBXPButtonStatus() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_STATUS;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mBXPButtonInfo.mac);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    private void dismissAlarmStatus() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_DISMISS_ALARM;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mBXPButtonInfo.mac);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        backToDetail();
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        backToDetail();
    }

    private void backToDetail() {
        Intent intent = new Intent(this, DeviceDetailActivity.class);
        startActivity(intent);
    }
}
