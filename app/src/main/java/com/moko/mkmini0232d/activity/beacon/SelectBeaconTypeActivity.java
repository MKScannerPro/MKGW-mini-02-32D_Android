package com.moko.mkmini0232d.activity.beacon;

import android.content.Intent;
import android.view.View;

import com.moko.lib.mqtt.event.MQTTConnectionCompleteEvent;
import com.moko.mkmini0232d.AppConstants;
import com.moko.mkmini0232d.activity.BleManagerActivity;
import com.moko.mkmini0232d.base.BaseActivity;
import com.moko.mkmini0232d.databinding.ActivitySelectBeaconTypeBinding;
import com.moko.mkmini0232d.entity.MokoDevice;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author: jun.liu
 * @date: 2023/8/30 15:28
 * @des:
 */
public class SelectBeaconTypeActivity extends BaseActivity<ActivitySelectBeaconTypeBinding> {
    private MokoDevice mMokoDevice;

    @Override
    protected ActivitySelectBeaconTypeBinding getViewBinding() {
        return ActivitySelectBeaconTypeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate() {
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mBind.tvBxpB.setOnClickListener(v -> onItemClick(0));
        mBind.tvBxpC.setOnClickListener(v -> onItemClick(1));
        mBind.tvOther.setOnClickListener(v -> onItemClick(2));
    }

    private void onItemClick(int from) {
        Intent intent = new Intent(this, BleManagerActivity.class);
        intent.putExtra("from", from);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(intent);
    }

    public void back(View view){
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
    }
}
