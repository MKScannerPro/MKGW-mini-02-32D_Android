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
import com.moko.mkmini0232d.databinding.FragmentSslAppMini0232dBinding;
import com.moko.mkmini0232d.dialog.BottomDialog;
import com.moko.mkmini0232d.utils.FileUtils;
import com.moko.mkmini0232d.utils.ToastUtils;

import java.io.File;
import java.util.ArrayList;

public class SSLFragment extends Fragment {
    public static final int REQUEST_CODE_SELECT_CA = 0x10;
    public static final int REQUEST_CODE_SELECT_CLIENT_KEY = 0x11;
    public static final int REQUEST_CODE_SELECT_CLIENT_CERT = 0x12;
    private static final String TAG = SSLFragment.class.getSimpleName();
    private FragmentSslAppMini0232dBinding mBind;
    private int connectMode;
    private String caPath;
    private String clientKeyPath;
    private String clientCertPath;
    private ArrayList<String> values;
    private int selected;

    public SSLFragment() {
    }

    public static SSLFragment newInstance() {
        return new SSLFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentSslAppMini0232dBinding.inflate(inflater, container, false);
        mBind.clCertificate.setVisibility(connectMode > 0 ? View.VISIBLE : View.GONE);
        mBind.cbSsl.setChecked(connectMode > 0);
        mBind.cbSsl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                connectMode = 0;
            } else {
                connectMode = selected + 1;
            }
            mBind.clCertificate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        values = new ArrayList<>();
        values.add("CA signed server certificate");
        values.add("CA certificate");
        values.add("Self signed certificates");
        if (connectMode > 0) {
            selected = connectMode - 1;
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

    @Override
    public void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }

    public void setConnectMode(int connectMode) {
        this.connectMode = connectMode;
        if (mBind == null)
            return;
        mBind.clCertificate.setVisibility(connectMode > 0 ? View.VISIBLE : View.GONE);
        if (connectMode > 0) {
            selected = connectMode - 1;
            mBind.tvCertification.setText(values.get(selected));
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
        mBind.cbSsl.setChecked(connectMode > 0);
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
                connectMode = 1;
                mBind.llCa.setVisibility(View.GONE);
                mBind.llClientKey.setVisibility(View.GONE);
                mBind.llClientCert.setVisibility(View.GONE);
            } else if (selected == 1) {
                connectMode = 2;
                mBind.llCa.setVisibility(View.VISIBLE);
                mBind.llClientKey.setVisibility(View.GONE);
                mBind.llClientCert.setVisibility(View.GONE);
            } else if (selected == 2) {
                connectMode = 3;
                mBind.llCa.setVisibility(View.VISIBLE);
                mBind.llClientKey.setVisibility(View.VISIBLE);
                mBind.llClientCert.setVisibility(View.VISIBLE);
            }
        });
        dialog.show(getChildFragmentManager());
    }

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

    private int requestCode;
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

    public boolean isValid() {
        final String caFile = mBind.tvCaFile.getText().toString();
        final String clientKeyFile = mBind.tvClientKeyFile.getText().toString();
        final String clientCertFile = mBind.tvClientCertFile.getText().toString();
        if (connectMode == 2) {
            if (TextUtils.isEmpty(caFile)) {
                ToastUtils.showToast(requireContext(), getString(R.string.mqtt_verify_ca));
                return false;
            }
        } else if (connectMode == 3) {
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
        return connectMode;
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
