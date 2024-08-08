package com.moko.mkmini0232d.activity.filter;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkmini0232d.AppConstants;
import com.moko.mkmini0232d.base.BaseActivity;
import com.moko.mkmini0232d.databinding.ActivityDuplicateDataFilterMini0232dBinding;
import com.moko.mkmini0232d.dialog.BottomDialog;
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
import java.util.ArrayList;
import java.util.Arrays;

public class DuplicateDataFilterActivity extends BaseActivity<ActivityDuplicateDataFilterMini0232dBinding> {
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;
    public Handler mHandler;
    private final String[] mValues = {"None", "MAC", "MAC+Data type", "MAC+Raw data"};
    private int mSelected;
    private final String[] strategyValues = {"Strategy 1", "Strategy 2"};
    private int strategySelected;

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
        getDuplicateDataFilter();
        mBind.tvFilterStrategy.setOnClickListener(v -> {
            BottomDialog dialog = new BottomDialog();
            dialog.setDatas((ArrayList<String>) Arrays.asList(strategyValues), strategySelected);
            dialog.setListener(value -> {
                strategySelected = value;
                mBind.tvFilterStrategy.setText(strategyValues[value]);
            });
            dialog.show(getSupportFragmentManager());
        });
    }

    @Override
    protected ActivityDuplicateDataFilterMini0232dBinding getViewBinding() {
        return ActivityDuplicateDataFilterMini0232dBinding.inflate(getLayoutInflater());
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
            return;
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_DUPLICATE_DATA_FILTER) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mSelected = result.data.get("rule").getAsInt();
            mBind.tvFilerBy.setText(mValues[mSelected]);
            mBind.rlFilteringPeriod.setVisibility(mSelected > 0 ? View.VISIBLE : View.GONE);
            mBind.etFilteringPeriod.setText(String.valueOf(result.data.get("timeout").getAsInt()));
            strategySelected = result.data.get("mode").getAsInt();
            mBind.tvFilterStrategy.setText(strategyValues[strategySelected]);
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_DUPLICATE_DATA_FILTER) {
            Type type = new TypeToken<MsgConfigResult<?>>() {
            }.getType();
            MsgConfigResult<?> result = new Gson().fromJson(message, type);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        super.offline(event, mMokoDevice.mac);
    }

    public void back(View view) {
        finish();
    }

    private void setFilterPeriod(int filterPeriod) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_DUPLICATE_DATA_FILTER;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("rule", mSelected);
        jsonObject.addProperty("timeout", filterPeriod);
        jsonObject.addProperty("mode", strategySelected);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    private void getDuplicateDataFilter() {
        int msgId = MQTTConstants.READ_MSG_ID_DUPLICATE_DATA_FILTER;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    public void onFilterStrategy(View view) {

    }

    public void onFilterBy(View view) {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas((ArrayList<String>) Arrays.asList(mValues), mSelected);
        dialog.setListener(value -> {
            mSelected = value;
            mBind.tvFilerBy.setText(mValues[value]);
            mBind.rlFilteringPeriod.setVisibility(mSelected > 0 ? View.VISIBLE : View.GONE);
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        String filterPeriod = mBind.etFilteringPeriod.getText().toString();
        if (TextUtils.isEmpty(filterPeriod)) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        int period = Integer.parseInt(filterPeriod);
        if (period < 1 || period > 86400) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setFilterPeriod(period);
    }
}
