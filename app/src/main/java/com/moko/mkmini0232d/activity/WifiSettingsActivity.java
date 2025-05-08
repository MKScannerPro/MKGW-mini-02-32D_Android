package com.moko.mkmini0232d.activity;

import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkmini0232d.AppConstants;
import com.moko.mkmini0232d.R;
import com.moko.mkmini0232d.base.BaseActivity;
import com.moko.mkmini0232d.databinding.ActivityWifiSettingsMimi0232dBinding;
import com.moko.lib.scannerui.dialog.BottomDialog;
import com.moko.mkmini0232d.utils.FileUtils;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.support.mini0232d.MokoSupport;
import com.moko.support.mini0232d.OrderTaskAssembler;
import com.moko.support.mini0232d.entity.OrderCHAR;
import com.moko.support.mini0232d.entity.ParamsKeyEnum;
import com.moko.support.mini0232d.entity.ParamsLongKeyEnum;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiSettingsActivity extends BaseActivity<ActivityWifiSettingsMimi0232dBinding> {
    private final String FILTER_ASCII = "[ -~]*";
    private final String[] mSecurityValues = {"Personal", "Enterprise"};
    private int mSecuritySelected;
    private final String[] mEAPTypeValues = {"PEAP-MSCHAPV2", "TTLS-MSCHAPV2", "TLS"};
    private int mEAPTypeSelected;
    private boolean mSavedParamsError;
    private boolean mIsSaved;
    private String mCaPath;
    private String mCertPath;
    private String mKeyPath;
    private int requestCode;
    private boolean wifiDhcpEnable;
    private String wifiIp;
    private String wifiMask;
    private String wifiGateway;
    private String wifiDns;
    private Pattern pattern;

    @Override
    protected void onCreate() {
        mBind.cbVerifyServer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mSecuritySelected != 0 && mEAPTypeSelected != 2)
                mBind.llCa.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        String IP_REGEX = "((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))*";
        pattern = Pattern.compile(IP_REGEX);
        mBind.etUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        mBind.etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etEapPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etSsid.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        mBind.etDomainId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>(10);
        orderTasks.add(OrderTaskAssembler.getWifiSecurityType());
        orderTasks.add(OrderTaskAssembler.getWifiSSID());
        orderTasks.add(OrderTaskAssembler.getWifiPassword());
        orderTasks.add(OrderTaskAssembler.getWifiEapType());
        orderTasks.add(OrderTaskAssembler.getWifiEapUsername());
        orderTasks.add(OrderTaskAssembler.getWifiEapPassword());
        orderTasks.add(OrderTaskAssembler.getWifiEapDomainId());
        orderTasks.add(OrderTaskAssembler.getWifiEapVerifyServiceEnable());
        orderTasks.add(OrderTaskAssembler.getNetworkDHCP());
        orderTasks.add(OrderTaskAssembler.getNetworkIPInfo());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));

        mBind.layoutIp.imgDhcp.setOnClickListener(v -> {
            wifiDhcpEnable = !wifiDhcpEnable;
            setDhcpEnable(wifiDhcpEnable);
        });
    }

    @Override
    protected ActivityWifiSettingsMimi0232dBinding getViewBinding() {
        return ActivityWifiSettingsMimi0232dBinding.inflate(getLayoutInflater());
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
            if (orderCHAR == OrderCHAR.CHAR_PARAMS) {
                if (value.length >= 4) {
                    int header = value[0] & 0xFF;// 0xED
                    int flag = value[1] & 0xFF;// read or write
                    int cmd = value[2] & 0xFF;
                    if (header == 0xEE) {
                        ParamsLongKeyEnum configKeyEnum = ParamsLongKeyEnum.fromParamKey(cmd);
                        if (configKeyEnum == null) {
                            return;
                        }
                        if (flag == 0x01) {
                            // write
                            int result = value[4] & 0xFF;
                            switch (configKeyEnum) {
                                case KEY_WIFI_CLIENT_KEY:
                                case KEY_WIFI_CLIENT_CERT:
                                case KEY_WIFI_CA:
                                    if (result != 1) {
                                        mSavedParamsError = true;
                                    }
                                    break;
                            }
                        }
                    }
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
                                case KEY_WIFI_SECURITY_TYPE:
                                case KEY_WIFI_SSID:
                                case KEY_WIFI_EAP_USERNAME:
                                case KEY_WIFI_EAP_PASSWORD:
                                case KEY_WIFI_EAP_DOMAIN_ID:
                                case KEY_WIFI_EAP_VERIFY_SERVICE_ENABLE:
                                case KEY_WIFI_PASSWORD:
                                case KEY_NETWORK_IP_INFO:
                                case KEY_NETWORK_DHCP:
                                    if (result != 1) {
                                        mSavedParamsError = true;
                                    }
                                    break;
                                case KEY_WIFI_EAP_TYPE:
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
                                case KEY_WIFI_SECURITY_TYPE:
                                    mSecuritySelected = value[4];
                                    mBind.tvSecurity.setText(mSecurityValues[mSecuritySelected]);
                                    mBind.clEapType.setVisibility(mSecuritySelected != 0 ? View.VISIBLE : View.GONE);
                                    mBind.clPassword.setVisibility(mSecuritySelected != 0 ? View.GONE : View.VISIBLE);
                                    if (mSecuritySelected == 0) {
                                        mBind.llCa.setVisibility(View.GONE);
                                    } else {
                                        if (mEAPTypeSelected != 2) {
                                            mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                                        } else {
                                            mBind.llCa.setVisibility(View.VISIBLE);
                                        }
                                    }
                                    break;
                                case KEY_WIFI_SSID:
                                    mBind.etSsid.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                    break;
                                case KEY_WIFI_PASSWORD:
                                    mBind.etPassword.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                    break;
                                case KEY_WIFI_EAP_PASSWORD:
                                    mBind.etEapPassword.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                    break;
                                case KEY_WIFI_EAP_TYPE:
                                    mEAPTypeSelected = value[4];
                                    mBind.tvEapType.setText(mEAPTypeValues[mEAPTypeSelected]);
                                    if (mSecuritySelected == 0) {
                                        mBind.llCa.setVisibility(View.GONE);
                                        mBind.clUsername.setVisibility(View.GONE);
                                        mBind.clEapPassword.setVisibility(View.GONE);
                                        mBind.cbVerifyServer.setVisibility(View.GONE);
                                        mBind.clDomainId.setVisibility(View.GONE);
                                        mBind.llCert.setVisibility(View.GONE);
                                        mBind.llKey.setVisibility(View.GONE);
                                    } else {
                                        if (mEAPTypeSelected != 2)
                                            mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                                        else
                                            mBind.llCa.setVisibility(View.VISIBLE);
                                        mBind.clUsername.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                                        mBind.clEapPassword.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                                        mBind.cbVerifyServer.setVisibility(mEAPTypeSelected == 2 ? View.INVISIBLE : View.VISIBLE);
                                        mBind.clDomainId.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                                        mBind.llCert.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                                        mBind.llKey.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                                    }
                                    break;
                                case KEY_WIFI_EAP_USERNAME:
                                    mBind.etUsername.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                    break;
                                case KEY_WIFI_EAP_DOMAIN_ID:
                                    mBind.etDomainId.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                    break;
                                case KEY_WIFI_EAP_VERIFY_SERVICE_ENABLE:
                                    mBind.cbVerifyServer.setChecked(value[4] == 1);
                                    if (mSecuritySelected != 0 && mEAPTypeSelected != 2)
                                        mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                                    break;

                                case KEY_NETWORK_DHCP:
                                    wifiDhcpEnable = (value[4] & 0xff) == 1;
                                    setDhcpEnable(wifiDhcpEnable);
                                    break;

                                case KEY_NETWORK_IP_INFO:
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
                            }
                        }
                    }
                }
            }
        }
    }

    private void setDhcpEnable(boolean enable) {
        mBind.layoutIp.imgDhcp.setImageResource(enable ? R.drawable.ic_checkbox_open : R.drawable.ic_checkbox_close);
        mBind.layoutIp.clIp.setVisibility(enable ? View.GONE : View.VISIBLE);
    }

    private void setIpInfo() {
        mBind.layoutIp.etIp.setText(wifiIp);
        mBind.layoutIp.etMask.setText(wifiMask);
        mBind.layoutIp.etGateway.setText(wifiGateway);
        mBind.layoutIp.etDns.setText(wifiDns);
    }

    public void onSelectSecurity(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas((new ArrayList<>(Arrays.asList(mSecurityValues))), mSecuritySelected);
        dialog.setListener(value -> {
            mSecuritySelected = value;
            mBind.tvSecurity.setText(mSecurityValues[value]);
            mBind.clEapType.setVisibility(mSecuritySelected != 0 ? View.VISIBLE : View.GONE);
            mBind.clPassword.setVisibility(mSecuritySelected != 0 ? View.GONE : View.VISIBLE);
            if (mSecuritySelected == 0) {
                mBind.llCa.setVisibility(View.GONE);
                mBind.clUsername.setVisibility(View.GONE);
                mBind.clEapPassword.setVisibility(View.GONE);
                mBind.cbVerifyServer.setVisibility(View.GONE);
                mBind.clDomainId.setVisibility(View.GONE);
                mBind.llCert.setVisibility(View.GONE);
                mBind.llKey.setVisibility(View.GONE);
            } else {
                if (mEAPTypeSelected != 2)
                    mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                else
                    mBind.llCa.setVisibility(View.VISIBLE);
                mBind.clUsername.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                mBind.clEapPassword.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                mBind.cbVerifyServer.setVisibility(mEAPTypeSelected == 2 ? View.INVISIBLE : View.VISIBLE);
                mBind.clDomainId.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                mBind.llCert.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                mBind.llKey.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onSelectEAPType(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(new ArrayList<>(Arrays.asList(mEAPTypeValues)), mEAPTypeSelected);
        dialog.setListener(value -> {
            mEAPTypeSelected = value;
            mBind.tvEapType.setText(mEAPTypeValues[value]);
            mBind.clUsername.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
            mBind.clEapPassword.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
            mBind.cbVerifyServer.setVisibility(mEAPTypeSelected == 2 ? View.INVISIBLE : View.VISIBLE);
            mBind.clDomainId.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            if (mEAPTypeSelected != 2)
                mBind.llCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
            else
                mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llCert.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            mBind.llKey.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
        });
        dialog.show(getSupportFragmentManager());
    }

    private final ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
        if (null != result) {
            String filePath = FileUtils.getPath(this, result);
            if (TextUtils.isEmpty(filePath)) {
                ToastUtils.showToast(this, "file path error!");
                return;
            }
            final File file = new File(filePath);
            if (file.exists()) {
                if (requestCode == AppConstants.REQUEST_CODE_SELECT_CA) {
                    mCaPath = filePath;
                    mBind.tvCaFile.setText(filePath);
                }
                if (requestCode == AppConstants.REQUEST_CODE_SELECT_CLIENT_CERT) {
                    mCertPath = filePath;
                    mBind.tvCertFile.setText(filePath);
                }
                if (requestCode == AppConstants.REQUEST_CODE_SELECT_CLIENT_KEY) {
                    mKeyPath = filePath;
                    mBind.tvKeyFile.setText(filePath);
                }
            } else {
                ToastUtils.showToast(this, "file is not exists!");
            }
        }
    });

    public void selectCAFile(View view) {
        if (isWindowLocked()) return;
        requestCode = AppConstants.REQUEST_CODE_SELECT_CA;
        launcher.launch("*/*");
    }

    public void selectCertFile(View view) {
        if (isWindowLocked()) return;
        requestCode = AppConstants.REQUEST_CODE_SELECT_CLIENT_CERT;
        launcher.launch("*/*");
    }

    public void selectKeyFile(View view) {
        if (isWindowLocked()) return;
        requestCode = AppConstants.REQUEST_CODE_SELECT_CLIENT_KEY;
        launcher.launch("*/*");
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
        if (mSecuritySelected != 0) {
            if (mEAPTypeSelected != 2 && mBind.cbVerifyServer.isChecked()) {
                return TextUtils.isEmpty(mCaPath);
            } else if (mEAPTypeSelected == 2) {
                return TextUtils.isEmpty(mCaPath) || TextUtils.isEmpty(mCertPath) || TextUtils.isEmpty(mKeyPath);
            }
        }
        if (!wifiDhcpEnable) {
            //检查ip地址是否合法
            String ip = mBind.layoutIp.etIp.getText().toString();
            String mask = mBind.layoutIp.etMask.getText().toString();
            String gateway = mBind.layoutIp.etGateway.getText().toString();
            String dns = mBind.layoutIp.etDns.getText().toString();
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
            mSavedParamsError = false;
            String ssid = mBind.etSsid.getText().toString();
            String username = mBind.etUsername.getText().toString();
            String password = mBind.etPassword.getText().toString();
            String eapPassword = mBind.etEapPassword.getText().toString();
            String domainId = mBind.etDomainId.getText().toString();
            showLoadingProgressDialog();
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setWifiSecurityType(mSecuritySelected));
            if (mSecuritySelected == 0) {
                orderTasks.add(OrderTaskAssembler.setWifiSSID(ssid));
                orderTasks.add(OrderTaskAssembler.setWifiPassword(password));
            } else {
                if (mEAPTypeSelected != 2) {
                    orderTasks.add(OrderTaskAssembler.setWifiSSID(ssid));
                    orderTasks.add(OrderTaskAssembler.setWifiEapUsername(username));
                    orderTasks.add(OrderTaskAssembler.setWifiEapPassword(eapPassword));
                    orderTasks.add(OrderTaskAssembler.setWifiEapVerifyServiceEnable(mBind.cbVerifyServer.isChecked() ? 1 : 0));
                    if (mBind.cbVerifyServer.isChecked())
                        orderTasks.add(OrderTaskAssembler.setWifiCA(new File(mCaPath)));
                } else {
                    orderTasks.add(OrderTaskAssembler.setWifiSSID(ssid));
                    orderTasks.add(OrderTaskAssembler.setWifiEapDomainId(domainId));
                    orderTasks.add(OrderTaskAssembler.getWifiEapVerifyServiceEnable());
                    orderTasks.add(OrderTaskAssembler.setWifiCA(new File(mCaPath)));
                    orderTasks.add(OrderTaskAssembler.setWifiClientCert(new File(mCertPath)));
                    orderTasks.add(OrderTaskAssembler.setWifiClientKey(new File(mKeyPath)));
                }
            }
            if (!wifiDhcpEnable) {
                String[] ipInfo = getIpInfo();
                orderTasks.add(OrderTaskAssembler.setNetworkIPInfo(ipInfo[0], ipInfo[1], ipInfo[2], ipInfo[3]));
            }
            orderTasks.add(OrderTaskAssembler.setNetworkDHCP(wifiDhcpEnable ? 1 : 0));
            orderTasks.add(OrderTaskAssembler.setWifiEapType(mEAPTypeSelected));
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } catch (Exception e) {
            ToastUtils.showToast(this, "File is missing");
        }
    }

    private String[] getIpInfo() {
        String ip = mBind.layoutIp.etIp.getText().toString();
        String mask = mBind.layoutIp.etMask.getText().toString();
        String gateway = mBind.layoutIp.etGateway.getText().toString();
        String dns = mBind.layoutIp.etDns.getText().toString();
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
        back();
    }

    private void back() {
        if (mIsSaved) setResult(RESULT_OK);
        finish();
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }
}
