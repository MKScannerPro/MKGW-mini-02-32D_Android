package com.moko.mkmini0232d.activity;

import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkmini0232d.R;
import com.moko.mkmini0232d.base.BaseActivity;
import com.moko.mkmini0232d.databinding.ActivityWifiSettings20dBinding;
import com.moko.mkmini0232d.dialog.Bottom20DDialog;
import com.moko.mkmini0232d.utils.ToastUtils;
import com.moko.support.remotegw20d.MokoSupport;
import com.moko.support.remotegw20d.OrderTaskAssembler;
import com.moko.support.remotegw20d.entity.OrderCHAR;
import com.moko.support.remotegw20d.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiSettings20DActivity extends BaseActivity<ActivityWifiSettings20dBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    private boolean mSavedParamsError;
    private boolean mIsSaved;
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

        mBind.tvCountryBrand.setOnClickListener(v -> onSelectCountry());
        mBind.imgDhcp.setOnClickListener(v -> {
            wifiDhcpEnable = !wifiDhcpEnable;
            setDhcpEnable(wifiDhcpEnable);
        });
        showLoadingProgressDialog();
        mBind.tvTitle.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getWifiSSID());
            orderTasks.add(OrderTaskAssembler.getWifiPassword());
            orderTasks.add(OrderTaskAssembler.getWifiDHCP());
            orderTasks.add(OrderTaskAssembler.getWifiIPInfo());
            orderTasks.add(OrderTaskAssembler.getCountry());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);
    }

    @Override
    protected ActivityWifiSettings20dBinding getViewBinding() {
        return ActivityWifiSettings20dBinding.inflate(getLayoutInflater());
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
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            if (orderCHAR == OrderCHAR.CHAR_PARAMS) {
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
                        if (flag == 0x01) {
                            // write
                            int result = value[4] & 0xFF;
                            switch (configKeyEnum) {
                                case KEY_WIFI_SSID:
                                case KEY_WIFI_PASSWORD:
                                case KEY_WIFI_DHCP:
                                case KEY_WIFI_IP_INFO:
                                    if (result != 1) {
                                        mSavedParamsError = true;
                                    }
                                    break;
                                case KEY_COUNTRY_BRAND:
                                    if (result != 1) {
                                        mSavedParamsError = true;
                                    }
                                    if (mSavedParamsError) {
                                        ToastUtils.showToast(this, "Setup failed！");
                                    } else {
                                        mIsSaved = true;
                                        ToastUtils.showToast(this, "Setup succeed！");
                                    }
                                    break;
                            }
                        }
                        if (flag == 0x00) {
                            if (length == 0) return;
                            // read
                            switch (configKeyEnum) {
                                case KEY_WIFI_SSID:
                                    mBind.etSsid.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                    break;
                                case KEY_WIFI_PASSWORD:
                                    mBind.etPassword.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                    break;
                                case KEY_WIFI_DHCP:
                                    wifiDhcpEnable = (value[4] & 0xff) == 1;
                                    setDhcpEnable(wifiDhcpEnable);
                                    break;
                                case KEY_WIFI_IP_INFO:
                                    if (length == 16) {
                                        wifiIp = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                                                value[4] & 0xFF, value[5] & 0xFF, value[6] & 0xFF, value[7] & 0xFF);
                                        wifiMask = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                                                value[8] & 0xFF, value[9] & 0xFF, value[10] & 0xFF, value[11] & 0xFF);
                                        wifiGateway = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                                                value[12] & 0xFF, value[13] & 0xFF, value[14] & 0xFF, value[15] & 0xFF);
                                        wifiDns = String.format(Locale.getDefault(), "%d.%d.%d.%d",
                                                value[16] & 0xFF, value[17] & 0xFF, value[18] & 0xFF, value[19] & 0xFF);
                                    }
                                    setIpInfo();
                                    break;
                                case KEY_COUNTRY_BRAND:
                                    if (length == 1) {
                                        countrySelected = value[4] & 0xff;
                                        mBind.tvCountryBrand.setText(countryBrand[countrySelected]);
                                    }
                                    break;
                            }
                        }
                    }
                }
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
        try {
            String ssid = mBind.etSsid.getText().toString();
            String password = mBind.etPassword.getText().toString();
            showLoadingProgressDialog();
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setWifiSSID(ssid));
            orderTasks.add(OrderTaskAssembler.setWifiPassword(password));
            if (!wifiDhcpEnable) {
                String[] ipInfo = getIpInfo();
                orderTasks.add(OrderTaskAssembler.setWifiIPInfo(ipInfo[0], ipInfo[1], ipInfo[2], ipInfo[3]));
            }
            orderTasks.add(OrderTaskAssembler.setWifiDHCP(wifiDhcpEnable ? 1 : 0));
            orderTasks.add(OrderTaskAssembler.setCountryBrand(countrySelected));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } catch (Exception e) {
            ToastUtils.showToast(this, "File is missing");
        }
    }

    private String[] getIpInfo() {
        String ip = mBind.etIp.getText().toString();
        String mask = mBind.etMask.getText().toString();
        String gateway = mBind.etGateway.getText().toString();
        String dns = mBind.etDns.getText().toString();
        String[] ipArray = ip.split("\\.");
        String ipHex = String.format("%s%s%s%s",
                MokoUtils.int2HexString(Integer.parseInt(ipArray[0])),
                MokoUtils.int2HexString(Integer.parseInt(ipArray[1])),
                MokoUtils.int2HexString(Integer.parseInt(ipArray[2])),
                MokoUtils.int2HexString(Integer.parseInt(ipArray[3])));
        String[] maskArray = mask.split("\\.");
        String maskHex = String.format("%s%s%s%s",
                MokoUtils.int2HexString(Integer.parseInt(maskArray[0])),
                MokoUtils.int2HexString(Integer.parseInt(maskArray[1])),
                MokoUtils.int2HexString(Integer.parseInt(maskArray[2])),
                MokoUtils.int2HexString(Integer.parseInt(maskArray[3])));
        String[] gatewayArray = gateway.split("\\.");
        String gatewayHex = String.format("%s%s%s%s",
                MokoUtils.int2HexString(Integer.parseInt(gatewayArray[0])),
                MokoUtils.int2HexString(Integer.parseInt(gatewayArray[1])),
                MokoUtils.int2HexString(Integer.parseInt(gatewayArray[2])),
                MokoUtils.int2HexString(Integer.parseInt(gatewayArray[3])));
        String[] dnsArray = dns.split("\\.");
        String dnsHex = String.format("%s%s%s%s",
                MokoUtils.int2HexString(Integer.parseInt(dnsArray[0])),
                MokoUtils.int2HexString(Integer.parseInt(dnsArray[1])),
                MokoUtils.int2HexString(Integer.parseInt(dnsArray[2])),
                MokoUtils.int2HexString(Integer.parseInt(dnsArray[3])));
        return new String[]{ipHex, maskHex, gatewayHex, dnsHex};
    }
    
    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        back();
    }

    private void back() {
        if (mIsSaved)
            setResult(RESULT_OK);
        finish();
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }
}
