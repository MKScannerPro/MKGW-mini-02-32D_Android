package com.moko.mkmini0232d.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.moko.mkmini0232d.R;
import com.moko.mkmini0232d.databinding.FragmentSslDeviceMini0232dBinding;
import com.moko.lib.scannerui.dialog.BottomDialog;
import com.moko.mkmini0232d.utils.FileUtils;
import com.moko.lib.scannerui.utils.ToastUtils;

import java.io.File;
import java.util.ArrayList;

public class SSLDeviceFragment extends Fragment {
    public static final int REQUEST_CODE_SELECT_CA = 0x10;
    public static final int REQUEST_CODE_SELECT_CLIENT_KEY = 0x11;
    public static final int REQUEST_CODE_SELECT_CLIENT_CERT = 0x12;
    private static final String TAG = SSLDeviceFragment.class.getSimpleName();
    private FragmentSslDeviceMini0232dBinding mBind;
    private int mConnectMode;
    private String caPath;
    private String clientKeyPath;
    private String clientCertPath;
    private ArrayList<String> values;
    private int selected;
    private int requestCode;

    public SSLDeviceFragment() {
    }

    public static SSLDeviceFragment newInstance() {
        return new SSLDeviceFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentSslDeviceMini0232dBinding.inflate(inflater, container, false);
        mBind.clCertificate.setVisibility(mConnectMode > 0 ? View.VISIBLE : View.GONE);
        mBind.cbSsl.setChecked(mConnectMode > 0);
        mBind.cbSsl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                mConnectMode = 0;
            } else {
                mConnectMode = selected + 1;
            }
            mBind.clCertificate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        values = new ArrayList<>();
        values.add("CA signed server certificate");
        values.add("CA certificate file");
        values.add("Self signed certificates");
        if (mConnectMode > 0) {
            selected = mConnectMode - 1;
            mBind.tvCaFile.setText(caPath);
            mBind.tvClientKeyFile.setText(clientKeyPath);
            mBind.tvClientCertFile.setText(clientCertPath);
            mBind.tvCertification.setText(values.get(selected));
        }
        if (selected == 0) {
            mBind.llCa.setVisibility(View.GONE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (selected == 1) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (selected == 2) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.VISIBLE);
            mBind.llClientCert.setVisibility(View.VISIBLE);
        }
        return mBind.getRoot();
    }

    public void setConnectMode(int connectMode) {
        this.mConnectMode = connectMode;
        if (mBind == null) return;
        mBind.clCertificate.setVisibility(mConnectMode > 0 ? View.VISIBLE : View.GONE);
        if (mConnectMode > 0) {
            selected = mConnectMode - 1;
            mBind.tvCaFile.setText(caPath);
            mBind.tvClientKeyFile.setText(clientKeyPath);
            mBind.tvClientCertFile.setText(clientCertPath);
            mBind.tvCertification.setText(values.get(selected));
        }
        mBind.cbSsl.setChecked(mConnectMode > 0);
        if (selected == 0) {
            mBind.llCa.setVisibility(View.GONE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (selected == 1) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (selected == 2) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.VISIBLE);
            mBind.llClientCert.setVisibility(View.VISIBLE);
        }
    }

    public void setCAPath(String caPath) {
        this.caPath = caPath;
        if (mBind == null) return;
        mBind.tvCaFile.setText(caPath);
    }

    public void setClientKeyPath(String clientKeyPath) {
        this.clientKeyPath = clientKeyPath;
        if (mBind == null) return;
        mBind.tvClientKeyFile.setText(clientKeyPath);
    }

    public void setClientCertPath(String clientCertPath) {
        this.clientCertPath = clientCertPath;
        if (mBind == null) return;
        mBind.tvClientCertFile.setText(clientCertPath);
    }

    public void selectCertificate() {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(values, selected);
        dialog.setListener(value -> {
            selected = value;
            mBind.tvCertification.setText(values.get(selected));
            if (selected == 0) {
                mConnectMode = 1;
                mBind.llCa.setVisibility(View.GONE);
                mBind.llClientKey.setVisibility(View.GONE);
                mBind.llClientCert.setVisibility(View.GONE);
            } else if (selected == 1) {
                mConnectMode = 2;
                mBind.llCa.setVisibility(View.VISIBLE);
                mBind.llClientKey.setVisibility(View.GONE);
                mBind.llClientCert.setVisibility(View.GONE);
            } else if (selected == 2) {
                mConnectMode = 3;
                mBind.llCa.setVisibility(View.VISIBLE);
                mBind.llClientKey.setVisibility(View.VISIBLE);
                mBind.llClientCert.setVisibility(View.VISIBLE);
            }
        });
        dialog.show(getChildFragmentManager());
    }

    private final ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
        if (null == result) return;
        String filePath = FileUtils.getPath(requireContext(), result);
        if (TextUtils.isEmpty(filePath)) {
            ToastUtils.showToast(requireContext(), "file path error!");
            return;
        }
        final File file = new File(filePath);
        if (file.exists()) {
            if (requestCode == REQUEST_CODE_SELECT_CA) {
                caPath = filePath;
                mBind.tvCaFile.setText(filePath);
            }
            if (requestCode == REQUEST_CODE_SELECT_CLIENT_KEY) {
                clientKeyPath = filePath;
                mBind.tvClientKeyFile.setText(filePath);
            }
            if (requestCode == REQUEST_CODE_SELECT_CLIENT_CERT) {
                clientCertPath = filePath;
                mBind.tvClientCertFile.setText(filePath);
            }
        } else {
            ToastUtils.showToast(requireContext(), "file is not exists!");
        }
    });

    public void selectCAFile() {
        requestCode = REQUEST_CODE_SELECT_CA;
        launcher.launch("*/*");
    }

    public void selectKeyFile() {
        requestCode = REQUEST_CODE_SELECT_CLIENT_KEY;
        launcher.launch("*/*");
    }

    public void selectCertFile() {
        requestCode = REQUEST_CODE_SELECT_CLIENT_CERT;
        launcher.launch("*/*");
    }

    public boolean isValid() {
        final String caFile = mBind.tvCaFile.getText().toString();
        final String clientKeyFile = mBind.tvClientKeyFile.getText().toString();
        final String clientCertFile = mBind.tvClientCertFile.getText().toString();
        if (mConnectMode == 2) {
            if (TextUtils.isEmpty(caFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_ca));
                return false;
            }
        } else if (mConnectMode == 3) {
            if (TextUtils.isEmpty(caFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_ca));
                return false;
            }
            if (TextUtils.isEmpty(clientKeyFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_client_key));
                return false;
            }
            if (TextUtils.isEmpty(clientCertFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_client_cert));
                return false;
            }
        }
        return true;
    }

    public int getConnectMode() {
        return mConnectMode;
    }

    public String getCaPath() {
        return caPath;
    }

    public String getClientKeyPath() {
        return clientKeyPath;
    }

    public String getClientCertPath() {
        return clientCertPath;
    }
}
