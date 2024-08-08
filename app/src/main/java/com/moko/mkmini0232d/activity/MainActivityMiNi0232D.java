package com.moko.mkmini0232d.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkmini0232d.AppConstants;
import com.moko.mkmini0232d.BuildConfig;
import com.moko.mkmini0232d.R;
import com.moko.mkmini0232d.activity.set.DeviceSettingActivity;
import com.moko.mkmini0232d.activity.set.ModifySettingsActivity;
import com.moko.mkmini0232d.adapter.DeviceAdapter;
import com.moko.mkmini0232d.base.BaseActivity;
import com.moko.mkmini0232d.databinding.ActivityMainMini0232dBinding;
import com.moko.mkmini0232d.databinding.ActivityMainRemote20dBinding;
import com.moko.mkmini0232d.db.DBTools;
import com.moko.mkmini0232d.dialog.AlertMessageDialog;
import com.moko.mkmini0232d.entity.MQTTConfig;
import com.moko.mkmini0232d.entity.MokoDevice;
import com.moko.mkmini0232d.utils.SPUtiles;
import com.moko.mkmini0232d.utils.ToastUtils;
import com.moko.mkmini0232d.utils.Utils;
import com.moko.support.mini0232d.MQTTConstants;
import com.moko.support.mini0232d.MQTTSupport;
import com.moko.support.mini0232d.MokoSupport;
import com.moko.support.mini0232d.entity.MsgNotify;
import com.moko.support.mini0232d.event.DeviceDeletedEvent;
import com.moko.support.mini0232d.event.DeviceModifyNameEvent;
import com.moko.support.mini0232d.event.DeviceOnlineEvent;
import com.moko.support.mini0232d.event.MQTTConnectionCompleteEvent;
import com.moko.support.mini0232d.event.MQTTConnectionFailureEvent;
import com.moko.support.mini0232d.event.MQTTConnectionLostEvent;
import com.moko.support.mini0232d.event.MQTTMessageArrivedEvent;
import com.moko.support.mini0232d.event.MQTTUnSubscribeFailureEvent;
import com.moko.support.mini0232d.event.MQTTUnSubscribeSuccessEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;

public class MainActivityMiNi0232D extends BaseActivity<ActivityMainMini0232dBinding> implements BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemLongClickListener {
    private ArrayList<MokoDevice> devices;
    private DeviceAdapter adapter;
    public Handler mHandler;
    public String mAppMqttConfigStr;
    private MQTTConfig mAppMqttConfig;
    public static String PATH_LOGCAT;

    @Override
    protected void onCreate() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 优先保存到SD卡中
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PATH_LOGCAT = getExternalFilesDir(null).getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKScannerPro" : "MKRemoteGW20D");
            } else {
                PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKScannerPro" : "MKRemoteGW20D");
            }
        } else {
            // 如果SD卡不存在，就保存到本应用的目录下
            PATH_LOGCAT = getFilesDir().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKScannerPro" : "MKRemoteGW20D");
        }
        if (!BuildConfig.IS_LIBRARY) {
            // 记录机型
            String buffer = "机型：" +
                    Build.MODEL +
                    "=====" +
                    // 记录版本号
                    "手机系统版本：" +
                    Build.VERSION.RELEASE +
                    "=====" +
                    // 记录APP版本
                    "APP版本：" +
                    Utils.getVersionInfo(this);
            XLog.d(buffer);
        }
        MokoSupport.getInstance().init(getApplicationContext());
        MQTTSupport.getInstance().init(getApplicationContext());
        devices = DBTools.getInstance(getApplicationContext()).selectAllDevice();
        adapter = new DeviceAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(devices);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        mBind.rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvDeviceList.setAdapter(adapter);
        if (devices.isEmpty()) {
            mBind.rlEmpty.setVisibility(View.VISIBLE);
            mBind.rvDeviceList.setVisibility(View.GONE);
        } else {
            mBind.rvDeviceList.setVisibility(View.VISIBLE);
            mBind.rlEmpty.setVisibility(View.GONE);
        }
        mHandler = new Handler(Looper.getMainLooper());
        mAppMqttConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (!TextUtils.isEmpty(mAppMqttConfigStr)) {
            mAppMqttConfig = new Gson().fromJson(mAppMqttConfigStr, MQTTConfig.class);
            mBind.tvTitle.setText(getString(R.string.mqtt_connecting));
        }
        try {
            MQTTSupport.getInstance().connectMqtt(mAppMqttConfigStr);
        } catch (FileNotFoundException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ToastUtils.showToast(this, "Please select your SSL certificates again, otherwise the APP can't use normally.");
                launcher.launch(new Intent(this, SetAppMQTTActivity.class));
            }
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            XLog.e(result.toString());
        }
        if (!BuildConfig.IS_LIBRARY) {
            mBind.tvTitle.setOnClickListener(v -> {
                if (isWindowLocked()) return;
                // 关于
                startActivity(new Intent(this, AboutActivity.class));
            });
        }
    }

    @Override
    protected ActivityMainMini0232dBinding getViewBinding() {
        return ActivityMainMini0232dBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
        mBind.tvTitle.setText(getString(R.string.app_name));
        // 订阅所有设备的Topic
        subscribeAllDevices();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionLostEvent(MQTTConnectionLostEvent event) {
        mBind.tvTitle.setText(getString(R.string.mqtt_connecting));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionFailureEvent(MQTTConnectionFailureEvent event) {
        mBind.tvTitle.setText(getString(R.string.mqtt_connect_failed));
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        runOnUiThread(() -> {
            // 更新所有设备的网络状态
            updateDeviceNetworkStatus(event);
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTUnSubscribeSuccessEvent(MQTTUnSubscribeSuccessEvent event) {
        dismissLoadingProgressDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTUnSubscribeFailureEvent(MQTTUnSubscribeFailureEvent event) {
        dismissLoadingProgressDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        if (!devices.isEmpty()) {
            for (MokoDevice device : devices) {
                if (device.mac.equals(event.getMac())) {
                    device.name = DBTools.getInstance(getApplicationContext()).selectDevice(device.mac).name;
                    break;
                }
            }
        }
        adapter.replaceData(devices);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDeletedEvent(DeviceDeletedEvent event) {
        // 删除了设备
        int id = event.getId();
        if (id > 0 && mHandler.hasMessages(id)) {
            mHandler.removeMessages(id);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        XLog.i("onNewIntent...");
        setIntent(intent);
        if (getIntent().getExtras() != null) {
            String from = getIntent().getStringExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY);
            String mac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);
            if (ModifyNameActivity.TAG.equals(from)
                    || DeviceSettingActivity.TAG.equals(from)) {
                devices.clear();
                devices.addAll(DBTools.getInstance(getApplicationContext()).selectAllDevice());
                if (!TextUtils.isEmpty(mac)) {
                    for (final MokoDevice device : devices) {
                        if (mac.equals(device.mac)) {
                            device.isOnline = true;
                            if (mHandler.hasMessages(device.id)) {
                                mHandler.removeMessages(device.id);
                            }
                            Message message = Message.obtain(mHandler, () -> {
                                device.isOnline = false;
                                XLog.i(device.mac + "离线");
                                adapter.replaceData(devices);
                            });
                            message.what = device.id;
                            mHandler.sendMessageDelayed(message, 60 * 1000);
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
                if (!devices.isEmpty()) {
                    mBind.rvDeviceList.setVisibility(View.VISIBLE);
                    mBind.rlEmpty.setVisibility(View.GONE);
                } else {
                    mBind.rvDeviceList.setVisibility(View.GONE);
                    mBind.rlEmpty.setVisibility(View.VISIBLE);
                }
            }
            if (ModifySettingsActivity.TAG.equals(from)) {
                if (!TextUtils.isEmpty(mac)) {
                    MokoDevice mokoDevice = DBTools.getInstance(getApplicationContext()).selectDevice(mac);
                    for (final MokoDevice device : devices) {
                        if (mac.equals(device.mac)) {
                            if (TextUtils.isEmpty(mAppMqttConfig.topicSubscribe)) {
                                try {
                                    if (!device.topicPublish.equals(mokoDevice.topicPublish)) {
                                        // 取消订阅旧主题
                                        MQTTSupport.getInstance().unSubscribe(device.topicPublish);
                                        // 订阅新主题
                                        MQTTSupport.getInstance().subscribe(mokoDevice.topicPublish, mAppMqttConfig.qos);
                                    }
                                    if (device.lwtEnable == 1
                                            && !TextUtils.isEmpty(device.lwtTopic)
                                            && !device.lwtTopic.equals(mokoDevice.topicPublish)) {
                                        // 取消订阅旧遗愿主题
                                        MQTTSupport.getInstance().unSubscribe(device.lwtTopic);
                                        // 订阅新遗愿主题
                                        MQTTSupport.getInstance().subscribe(mokoDevice.lwtTopic, mAppMqttConfig.qos);

                                    }
                                } catch (MqttException e) {
                                    XLog.e(e);
                                }
                            }
                            device.mqttInfo = mokoDevice.mqttInfo;
                            device.topicPublish = mokoDevice.topicPublish;
                            device.topicSubscribe = mokoDevice.topicSubscribe;
                            device.lwtEnable = mokoDevice.lwtEnable;
                            device.lwtTopic = mokoDevice.lwtTopic;
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
            }
        }
    }

    public void setAppMQTTConfig(View view) {
        if (isWindowLocked()) return;
        launcher.launch(new Intent(this, SetAppMQTTActivity.class));
    }

    public void mainAddDevices(View view) {
        if (isWindowLocked()) return;
        if (TextUtils.isEmpty(mAppMqttConfigStr)) {
            launcher.launch(new Intent(this, SetAppMQTTActivity.class));
            return;
        }
        if (Utils.isNetworkAvailable(this)) {
            MQTTConfig MQTTAppConfig = new Gson().fromJson(mAppMqttConfigStr, MQTTConfig.class);
            if (TextUtils.isEmpty(MQTTAppConfig.host)) {
                launcher.launch(new Intent(this, SetAppMQTTActivity.class));
                return;
            }
            startActivity(new Intent(this, DeviceScannerActivity.class));
        } else {
            String ssid = Utils.getWifiSSID(this);
            ToastUtils.showToast(this, String.format("SSID:%s, the network cannot available,please check", ssid));
            XLog.i(String.format("SSID:%s, the network cannot available,please check", ssid));
        }
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice mokoDevice = (MokoDevice) adapter.getItem(position);
        if (mokoDevice == null) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mokoDevice.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(this, DeviceDetailActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDevice);
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDevice mokoDevice = (MokoDevice) adapter.getItem(position);
        if (mokoDevice == null) return true;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Remove Device");
        dialog.setMessage("Please confirm again whether to \n remove the device");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(MainActivityMiNi0232D.this, R.string.network_error);
                return;
            }
            showLoadingProgressDialog();
            // 取消订阅
            if (TextUtils.isEmpty(mAppMqttConfig.topicSubscribe)) {
                try {
                    MQTTSupport.getInstance().unSubscribe(mokoDevice.topicPublish);
                    if (mokoDevice.lwtEnable == 1
                            && !TextUtils.isEmpty(mokoDevice.lwtTopic)
                            && !mokoDevice.lwtTopic.equals(mokoDevice.topicPublish))
                        MQTTSupport.getInstance().unSubscribe(mokoDevice.lwtTopic);
                } catch (MqttException e) {
                    XLog.e(e);
                }
            }
            XLog.i(String.format("删除设备:%s", mokoDevice.name));
            DBTools.getInstance(getApplicationContext()).deleteDevice(mokoDevice);
            EventBus.getDefault().post(new DeviceDeletedEvent(mokoDevice.id));
            devices.remove(mokoDevice);
            this.adapter.replaceData(devices);
            if (devices.isEmpty()) {
                mBind.rlEmpty.setVisibility(View.VISIBLE);
                mBind.rvDeviceList.setVisibility(View.GONE);
            }
        });
        dialog.show(getSupportFragmentManager());
        return true;
    }

    private void subscribeAllDevices() {
        if (!TextUtils.isEmpty(mAppMqttConfig.topicSubscribe)) {
            try {
                MQTTSupport.getInstance().subscribe(mAppMqttConfig.topicSubscribe, mAppMqttConfig.qos);
            } catch (MqttException e) {
                XLog.e(e);
            }
        } else {
            if (devices.isEmpty()) return;
            for (MokoDevice device : devices) {
                try {
                    // 订阅设备发布主题
                    if (TextUtils.isEmpty(mAppMqttConfig.topicSubscribe))
                        MQTTSupport.getInstance().subscribe(device.topicPublish, mAppMqttConfig.qos);
                    // 订阅遗愿主题
                    if (device.lwtEnable == 1
                            && !TextUtils.isEmpty(device.lwtTopic)
                            && !device.lwtTopic.equals(device.topicPublish))
                        MQTTSupport.getInstance().subscribe(device.lwtTopic, mAppMqttConfig.qos);
                } catch (MqttException e) {
                    XLog.e(e);
                }
            }
        }
    }

    private void updateDeviceNetworkStatus(MQTTMessageArrivedEvent event) {
        if (devices.isEmpty()) return;
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
        // 收到任何信息都认为在线，除了遗愿信息
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_SCAN_RESULT && isDurationVoid()) return;
        Type type = new TypeToken<MsgNotify<Object>>() {
        }.getType();
        MsgNotify<Object> msgNotify = new Gson().fromJson(message, type);
        final String mac = msgNotify.device_info.mac;
        for (final MokoDevice device : devices) {
            if (device.mac.equals(mac)) {
                if ((msg_id == MQTTConstants.NOTIFY_MSG_ID_OFFLINE
                        || msg_id == MQTTConstants.NOTIFY_MSG_ID_BUTTON_RESET) && device.isOnline) {
                    // 收到遗愿信息或者按键重置，设备离线
                    device.isOnline = false;
                    if (mHandler.hasMessages(device.id)) {
                        mHandler.removeMessages(device.id);
                    }
                    XLog.i(device.mac + "离线");
                    adapter.replaceData(devices);
                    EventBus.getDefault().post(new DeviceOnlineEvent(mac, false));
                    break;
                }
                if (msg_id == MQTTConstants.NOTIFY_MSG_ID_NETWORKING_STATUS) {
                    Type netType = new TypeToken<MsgNotify<JsonObject>>() {
                    }.getType();
                    MsgNotify<JsonObject> netMsgNotify = new Gson().fromJson(message, netType);
                    device.wifiRssi = netMsgNotify.data.get("wifi_rssi").getAsInt();
                }
                device.isOnline = true;
                if (mHandler.hasMessages(device.id)) {
                    mHandler.removeMessages(device.id);
                }
                Message offline = Message.obtain(mHandler, () -> {
                    device.isOnline = false;
                    XLog.i(device.mac + "离线");
                    adapter.replaceData(devices);
                    EventBus.getDefault().post(new DeviceOnlineEvent(mac, false));
                });
                offline.what = device.id;
                mHandler.sendMessageDelayed(offline, 62 * 1000);
                adapter.replaceData(devices);
                break;
            }
        }
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (null == result || result.getResultCode() != RESULT_OK || null == result.getData()) return;
        mAppMqttConfigStr = result.getData().getStringExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_APP);
        mAppMqttConfig = new Gson().fromJson(mAppMqttConfigStr, MQTTConfig.class);
        mBind.tvTitle.setText(getString(R.string.app_name));
        // 订阅所有设备的Topic
        subscribeAllDevices();
    });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MQTTSupport.getInstance().disconnectMqtt();
        if (devices != null && !devices.isEmpty()) {
            for (final MokoDevice device : devices) {
                if (mHandler.hasMessages(device.id)) {
                    mHandler.removeMessages(device.id);
                }
            }
        }
    }

    // 记录上次收到信息的时间,屏蔽无效事件
    protected long mLastMessageTime = 0;

    public boolean isDurationVoid() {
        long current = SystemClock.elapsedRealtime();
        if (current - mLastMessageTime > 500) {
            mLastMessageTime = current;
            return false;
        } else {
            return true;
        }
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        back();
    }

    private void back() {
        if (BuildConfig.IS_LIBRARY) {
            finish();
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setMessage(R.string.main_exit_tips);
            dialog.setOnAlertConfirmListener(this::finish);
            dialog.show(getSupportFragmentManager());
        }
    }
}
