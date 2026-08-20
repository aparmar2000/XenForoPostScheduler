package aparmar2000.xenforoposter.extension.setting;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;

@Getter
public class IntegerSettingDefinition extends SettingDefinition<Integer> {
    private final int minValue;
    private final int maxValue;
    private final JSpinner spinner;

    public IntegerSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, int defaultValue, int minValue, int maxValue) {
        this(key, label, description, defaultValue, minValue, maxValue, null);
    }

    public IntegerSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, int defaultValue, int minValue, int maxValue, @Nullable String group) {
        super(key, label, description, defaultValue, group);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.spinner = new JSpinner(new SpinnerNumberModel(defaultValue, minValue, maxValue, 1));
        this.spinner.addChangeListener(e -> notifyChangeListeners());
    }

    @Override
    public Class<Integer> getValueType() {
        return Integer.class;
    }

    @Override
    public boolean validate(Integer value) {
        return value != null && value >= minValue && value <= maxValue;
    }

    @Override
    public String serialize(Integer value) {
        return String.valueOf(value);
    }

    @Override
    public Integer deserialize(String rawValue) {
        try {
            int parsed = Integer.parseInt(rawValue);
            return Math.min(maxValue, Math.max(minValue, parsed));
        } catch (Exception e) {
            return getDefaultValue();
        }
    }

    @Override
    public @NotNull JComponent getUiComponent() {
        return spinner;
    }

    @Override
    public @NotNull Integer getUiValue() {
        return (Integer) spinner.getValue();
    }

    @Override
    public void setUiValue(@NotNull Integer value) {
        int clamped = Math.min(maxValue, Math.max(minValue, value != null ? value : getDefaultValue()));
        spinner.setValue(clamped);
    }
}

