package com.moko.mkmini0232d.dialog;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.moko.lib.scannerui.dialog.MokoBaseDialog;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.mkmini0232d.R;
import com.moko.mkmini0232d.databinding.DialogFilterTestBinding;

public class FilterTestDialog extends MokoBaseDialog<DialogFilterTestBinding> {
    public static final String TAG = FilterTestDialog.class.getSimpleName();

    @Override
    protected DialogFilterTestBinding getViewBind(LayoutInflater inflater, ViewGroup container) {
        return DialogFilterTestBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onCreateView() {
        mBind.tvCancel.setOnClickListener(v -> {
            dismiss();
        });
        mBind.tvEnsure.setOnClickListener(v -> {
            String durationStr = mBind.etDuration.getText().toString();
            if (TextUtils.isEmpty(durationStr)) {
                ToastUtils.showToast(getContext(), "Para Error");
                return;
            }
            int duration = Integer.parseInt(durationStr);
            if (duration < 10 || duration > 600) {
                ToastUtils.showToast(getContext(), "Para Error");
                return;
            }
            dismiss();
            if (filterTestClickListener != null)
                filterTestClickListener.onEnsureClicked(duration);
        });
    }

    @Override
    public int getDialogStyle() {
        return R.style.CenterDialog;
    }

    @Override
    public int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @Override
    public boolean getCancelOutside() {
        return false;
    }

    @Override
    public boolean getCancellable() {
        return true;
    }

    private FilterTestClickListener filterTestClickListener;

    public void setOnFilterTestClicked(FilterTestClickListener filterTestClickListener) {
        this.filterTestClickListener = filterTestClickListener;
    }

    public interface FilterTestClickListener {

        void onEnsureClicked(int duration);
    }
}
