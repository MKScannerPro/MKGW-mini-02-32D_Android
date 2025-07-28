package com.moko.mkmini0232d.activity.add;

import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkmini0232d.AppConstants;
import com.moko.mkmini0232d.base.BaseActivity;
import com.moko.mkmini0232d.databinding.ActivityDeviceInfoMini0232dBinding;
import com.moko.support.mini0232d.MokoSupport;
import com.moko.support.mini0232d.OrderTaskAssembler;
import com.moko.support.mini0232d.entity.OrderCHAR;
import com.moko.support.mini0232d.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceInformationActivity extends BaseActivity<ActivityDeviceInfoMini0232dBinding> {

    @Override
    protected void onCreate() {
        int selectedDeviceType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_SELECTED_DEVICE_TYPE, -1);
        mBind.layoutBTFirmware.setVisibility(selectedDeviceType == 0x71 ? View.VISIBLE : View.GONE);
        mBind.lineBTFirmware.setVisibility(selectedDeviceType == 0x71 ? View.VISIBLE : View.GONE);
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>(10);
        orderTasks.add(OrderTaskAssembler.getDeviceName());
        orderTasks.add(OrderTaskAssembler.getDeviceModel());
        orderTasks.add(OrderTaskAssembler.getManufacturer());
        orderTasks.add(OrderTaskAssembler.getHardwareVersion());
        orderTasks.add(OrderTaskAssembler.getWifiSoftwareVersion());
        orderTasks.add(OrderTaskAssembler.getWifiFirmwareVersion());
        if (selectedDeviceType != 0) {
            orderTasks.add(OrderTaskAssembler.getBleFirmwareVersion());
        }
        orderTasks.add(OrderTaskAssembler.getWifiMac());
        orderTasks.add(OrderTaskAssembler.getBleMac());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Override
    protected ActivityDeviceInfoMini0232dBinding getViewBinding() {
        return ActivityDeviceInfoMini0232dBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                finish();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_MODEL_NUMBER:
                    mBind.tvProductModel.setText(new String(value));
                    break;
                case CHAR_MANUFACTURER_NAME:
                    mBind.tvManufacturer.setText(new String(value));
                    break;
                case CHAR_HARDWARE_REVISION:
                    mBind.tvDeviceHardwareVersion.setText(new String(value));
                    break;
                case CHAR_PARAMS:
                    if (value.length >= 4) {
                        int header = value[0] & 0xFF;// 0xED
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header == 0xED) {
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x00) {
                                if (length == 0)
                                    return;
                                // read
                                switch (configKeyEnum) {
                                    case KEY_DEVICE_NAME:
                                        mBind.tvDeviceName.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                        break;
                                    case KEY_WIFI_SOFTWARE_VERSION:
                                        mBind.tvDeviceSoftwareVersion.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                        break;
                                    case KEY_WIFI_FIRMWARE_VERSION:
                                        mBind.tvWifiFirmwareVersion.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                        break;
                                    case KEY_WIFI_MAC:
                                        byte[] wifiMacBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                        mBind.tvWifiMac.setText(MokoUtils.bytesToHexString(wifiMacBytes).toUpperCase());
                                        break;
                                    case KEY_BLE_MAC:
                                        byte[] bleMacBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                        mBind.tvBtMac.setText(MokoUtils.bytesToHexString(bleMacBytes).toUpperCase());
                                        break;
                                    case KEY_BLE_FIRMWARE_VERSION:
                                        byte[] bleFirmwareVersionBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                        mBind.tvBtFirmwareVersion.setText(MokoUtils.bytesToHexString(bleFirmwareVersionBytes));
                                        break;

                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    public void onBack(View view) {
        finish();
    }
}
