package com.xda.sa2ration;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Html;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.xda.sa2ration.databinding.ActivityMainBinding;

import java.util.Locale;
import java.util.Optional;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public enum keys {
        SATURATION, CM, CONTRAST,
        RED_SATURATION, GREEN_SATURATION, BLUE_SATURATION,
        RED_CONTRAST, GREEN_CONTRAST, BLUE_CONTRAST
    }

    public static final int DEFAULT_PROGRESS = 100;
    public static final int MAX_PROGRESS = 1000;
    public static final String PERSISTENT_COLOR_SATURATION = "persist.sys.sf.color_saturation";
    public static final String PERSISTENT_NATIVE_MODE = "persist.sys.sf.native_mode";

    private ActivityMainBinding binding;
    private float saturation = ColorMatrixController.DEFAULT_MULTIPLIER;
    private float contrast = ColorMatrixController.DEFAULT_MULTIPLIER;
    private float redSaturation = ColorMatrixController.DEFAULT_MULTIPLIER;
    private float greenSaturation = ColorMatrixController.DEFAULT_MULTIPLIER;
    private float blueSaturation = ColorMatrixController.DEFAULT_MULTIPLIER;
    private float redContrast = ColorMatrixController.DEFAULT_MULTIPLIER;
    private float greenContrast = ColorMatrixController.DEFAULT_MULTIPLIER;
    private float blueContrast = ColorMatrixController.DEFAULT_MULTIPLIER;
    private String cm = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CommandController.testSudo()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.warning_no_root)
                    .setCancelable(false)
                    .setPositiveButton(R.string.accept, (v, a) -> finish())
                    .show();

        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        restoreSettings();
        initColorControls();
        initImageView();
        initCm();
        initButtons();
    }


    // Values are persisted on pause, in order to restore them when system is rebooted
    // anyways, you can force persist them with the save button
    @Override
    protected void onPause() {
        super.onPause();
        persistSettings();
    }

    /**
     * Initialized bottom buttons, for force save and default values.
     */
    private void initButtons() {
        binding.content.reset.setOnClickListener(v -> reset());
        binding.content.apply.setOnClickListener(v -> {
            super.onPause();
            applyColorSettings();
            persistSettings();
            Toast.makeText(this, R.string.values_are_saved, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Initializes color management control.
     */
    private void initCm() {
        SwitchMaterial dci = binding.content.dci;
        boolean enabled;
        Optional<String> nativeMode = PersistenceController.getInstance(this)
                .restoreFromProperties(keys.CM.name());
        if (nativeMode.isPresent()) {
            cm = nativeMode.get();
            enabled = cm.equals("0");
            binding.content.dci.setChecked(enabled);
        }
        dci.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cm = isChecked ? "0" : "1";
            CommandController.execCommand("service call SurfaceFlinger 1023 i32 " + cm);
            CommandController.setProp(PERSISTENT_NATIVE_MODE, cm);
        });
    }

    private void initColorControls() {
        bindControl(binding.content.saturationSeekBar, binding.content.saturationInput,
                saturation, value -> saturation = value);
        bindControl(binding.content.contrastSeekBar, binding.content.contrastInput,
                contrast, value -> contrast = value);
        bindControl(binding.content.redSaturationSeekBar, binding.content.redSaturationInput,
                redSaturation, value -> redSaturation = value);
        bindControl(binding.content.greenSaturationSeekBar, binding.content.greenSaturationInput,
                greenSaturation, value -> greenSaturation = value);
        bindControl(binding.content.blueSaturationSeekBar, binding.content.blueSaturationInput,
                blueSaturation, value -> blueSaturation = value);
        bindControl(binding.content.redContrastSeekBar, binding.content.redContrastInput,
                redContrast, value -> redContrast = value);
        bindControl(binding.content.greenContrastSeekBar, binding.content.greenContrastInput,
                greenContrast, value -> greenContrast = value);
        bindControl(binding.content.blueContrastSeekBar, binding.content.blueContrastInput,
                blueContrast, value -> blueContrast = value);
    }

    /**
     * Initialized ImageView with its onClick Listener.
     */
    private void initImageView() {
        ImageView preview = findViewById(R.id.imageView);
        preview.setOnClickListener(v -> {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.photo_by)
                    .setMessage(Html.fromHtml(getResources().getString(R.string.photo_by_desc), 0))
                    .show();

            TextView link = alertDialog.findViewById(android.R.id.message);
            link.setLinksClickable(true);
            link.setMovementMethod(LinkMovementMethod.getInstance());
        });
    }

    /**
     * Reset values to default ones.
     */
    private void reset() {
        binding.content.saturationSeekBar.setProgress(DEFAULT_PROGRESS);
        binding.content.contrastSeekBar.setProgress(DEFAULT_PROGRESS);
        binding.content.redSaturationSeekBar.setProgress(DEFAULT_PROGRESS);
        binding.content.greenSaturationSeekBar.setProgress(DEFAULT_PROGRESS);
        binding.content.blueSaturationSeekBar.setProgress(DEFAULT_PROGRESS);
        binding.content.redContrastSeekBar.setProgress(DEFAULT_PROGRESS);
        binding.content.greenContrastSeekBar.setProgress(DEFAULT_PROGRESS);
        binding.content.blueContrastSeekBar.setProgress(DEFAULT_PROGRESS);
        saturation = contrast = ColorMatrixController.DEFAULT_MULTIPLIER;
        redSaturation = greenSaturation = blueSaturation = ColorMatrixController.DEFAULT_MULTIPLIER;
        redContrast = greenContrast = blueContrast = ColorMatrixController.DEFAULT_MULTIPLIER;
        updateAllInputs();
        binding.content.dci.setChecked(false);
        applyColorSettings();
        persistSettings();
    }

    /**
     * Formats current progress for its representation in TextView.
     * @param progress current progress.
     * @return formatted string that represents progress.
     */
    private String format(float progress) {
        return String.format(Locale.US, "%.2f", progress);
    }

    private void bindControl(SeekBar seekBar, EditText input, float initialValue,
                             ValueSetter valueSetter) {
        seekBar.setMax(MAX_PROGRESS);
        seekBar.setProgress(Math.round(initialValue * 100f));
        input.setText(format(initialValue));
        final boolean[] synchronizing = {false};

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                float value = progress / 100f;
                valueSetter.set(value);
                if (fromUser) {
                    synchronizing[0] = true;
                    input.setText(format(value));
                    input.setSelection(input.length());
                    synchronizing[0] = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                applyColorSettings();
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (synchronizing[0]) {
                    return;
                }
                Float parsed = parseMultiplier(editable.toString());
                if (parsed == null) {
                    return;
                }
                float value = ColorMatrixController.clamp(parsed);
                valueSetter.set(value);
                synchronizing[0] = true;
                seekBar.setProgress(Math.round(value * 100f));
                if (parsed != value) {
                    input.setText(format(value));
                    input.setSelection(input.length());
                }
                synchronizing[0] = false;
            }
        });

        input.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                normalizeInput(input, seekBar);
            }
        });
        input.setOnEditorActionListener((view, actionId, event) -> {
            boolean done = actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            if (done) {
                normalizeInput(input, seekBar);
            }
            return false;
        });
    }

    private void normalizeInput(EditText input, SeekBar seekBar) {
        Float parsed = parseMultiplier(input.getText().toString());
        float value = parsed == null
                ? seekBar.getProgress() / 100f
                : ColorMatrixController.clamp(parsed);
        input.setText(format(value));
        input.setSelection(input.length());
        applyColorSettings();
    }

    private Float parseMultiplier(String value) {
        try {
            return Float.parseFloat(value.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void restoreSettings() {
        saturation = restoreMultiplier(keys.SATURATION);
        contrast = restoreMultiplier(keys.CONTRAST);
        redSaturation = restoreMultiplier(keys.RED_SATURATION);
        greenSaturation = restoreMultiplier(keys.GREEN_SATURATION);
        blueSaturation = restoreMultiplier(keys.BLUE_SATURATION);
        redContrast = restoreMultiplier(keys.RED_CONTRAST);
        greenContrast = restoreMultiplier(keys.GREEN_CONTRAST);
        blueContrast = restoreMultiplier(keys.BLUE_CONTRAST);
        Log.d(getClass().getName(), "Restored color settings");
    }

    private float restoreMultiplier(keys key) {
        Optional<String> saved = PersistenceController.getInstance(this)
                .restoreFromProperties(key.name());
        if (!saved.isPresent()) {
            return ColorMatrixController.DEFAULT_MULTIPLIER;
        }
        Float parsed = parseMultiplier(saved.get());
        return parsed == null
                ? ColorMatrixController.DEFAULT_MULTIPLIER
                : ColorMatrixController.clamp(parsed);
    }

    private void applyColorSettings() {
        float[] matrix = ColorMatrixController.createMatrix(
                contrast, redContrast, greenContrast, blueContrast,
                redSaturation, greenSaturation, blueSaturation);
        CommandController.execCommand(
                "setprop " + PERSISTENT_COLOR_SATURATION + " " + format(saturation),
                "service call SurfaceFlinger 1022 f " + format(saturation),
                ColorMatrixController.createSurfaceFlingerCommand(matrix));
    }

    private void persistSettings() {
        PersistenceController persistence = PersistenceController.getInstance(this);
        persistence.storeToProperties(keys.SATURATION.name(), format(saturation));
        persistence.storeToProperties(keys.CONTRAST.name(), format(contrast));
        persistence.storeToProperties(keys.RED_SATURATION.name(), format(redSaturation));
        persistence.storeToProperties(keys.GREEN_SATURATION.name(), format(greenSaturation));
        persistence.storeToProperties(keys.BLUE_SATURATION.name(), format(blueSaturation));
        persistence.storeToProperties(keys.RED_CONTRAST.name(), format(redContrast));
        persistence.storeToProperties(keys.GREEN_CONTRAST.name(), format(greenContrast));
        persistence.storeToProperties(keys.BLUE_CONTRAST.name(), format(blueContrast));
        persistence.storeToProperties(keys.CM.name(), cm);
        persistence.persist();
    }

    private void updateAllInputs() {
        binding.content.saturationInput.setText(format(saturation));
        binding.content.contrastInput.setText(format(contrast));
        binding.content.redSaturationInput.setText(format(redSaturation));
        binding.content.greenSaturationInput.setText(format(greenSaturation));
        binding.content.blueSaturationInput.setText(format(blueSaturation));
        binding.content.redContrastInput.setText(format(redContrast));
        binding.content.greenContrastInput.setText(format(greenContrast));
        binding.content.blueContrastInput.setText(format(blueContrast));
    }

    private interface ValueSetter {
        void set(float value);
    }


}
