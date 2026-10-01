package org.lineageos.settings.device;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class StepperSeekBarPreference extends Preference {
    private int mMin = 0;
    private int mMax = 100;
    private int mStep = 1;
    private int mValue = 0;
    private String mSuffix = "";

    private SeekBar mSeekBar;
    private TextView mValueText;

    public StepperSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_stepper_seekbar);
    }

    public StepperSeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        if (defaultValue instanceof Integer) {
            setValue(getPersistedInt((Integer) defaultValue));
        } else {
            setValue(getPersistedInt(mMin));
        }
    }

    public void setMin(int min) {
        mMin = min;
        if (mValue < mMin) setValue(mMin);
    }

    public int getMin() {
        return mMin;
    }

    public void setMax(int max) {
        mMax = max;
        if (mValue > mMax) setValue(mMax);
    }

    public int getMax() {
        return mMax;
    }

    public void setStep(int step) {
        if (step > 0) mStep = step;
    }

    public void setSuffix(String suffix) {
        mSuffix = suffix != null ? suffix : "";
        updateValueDisplay();
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value) {
        int clamped = Math.max(mMin, Math.min(mMax, value));
        if (clamped != mValue || !shouldPersist()) {
            mValue = clamped;
            persistInt(mValue);
            notifyChanged();
            callChangeListener(mValue);
            updateValueDisplay();
            if (mSeekBar != null) {
                mSeekBar.setProgress(mValue - mMin);
            }
        }
    }

    private void updateValueDisplay() {
        if (mValueText != null) {
            mValueText.setText(mValue + mSuffix);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mValueText = (TextView) holder.findViewById(R.id.stepper_value);
        mSeekBar = (SeekBar) holder.findViewById(R.id.stepper_seekbar);
        View btnMinus = holder.findViewById(R.id.btn_minus);
        View btnPlus = holder.findViewById(R.id.btn_plus);

        if (mSeekBar != null) {
            mSeekBar.setMax(mMax - mMin);
            mSeekBar.setProgress(mValue - mMin);
            mSeekBar.setEnabled(isEnabled());
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        int newValue = mMin + progress;
                        mValue = newValue;
                        updateValueDisplay();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int finalValue = mMin + seekBar.getProgress();
                    setValue(finalValue);
                }
            });
        }

        if (btnMinus != null) {
            btnMinus.setEnabled(isEnabled());
            btnMinus.setOnClickListener(v -> setValue(mValue - mStep));
        }

        if (btnPlus != null) {
            btnPlus.setEnabled(isEnabled());
            btnPlus.setOnClickListener(v -> setValue(mValue + mStep));
        }

        updateValueDisplay();
    }
}
