package aparmar2000.xenforoposter.settings.defs;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BooleanSettingDefinition extends SettingDefinition<Boolean> {
    private final JCheckBox checkBox;

    public BooleanSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, boolean defaultValue) {
        this(key, label, description, defaultValue, null);
    }

    public BooleanSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, boolean defaultValue, @Nullable String group) {
        super(key, label, description, defaultValue, group);
        this.checkBox = new JCheckBox("Enabled", defaultValue);
        this.checkBox.addActionListener(e -> notifyChangeListeners());
    }

    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public boolean validate(Boolean value) {
        return value != null;
    }

    @Override
    public String serialize(Boolean value) {
        return String.valueOf(value);
    }

    @Override
    public Boolean deserialize(String rawValue) {
        return Boolean.parseBoolean(rawValue);
    }

    @Override
    public @NotNull JComponent getUiComponent() {
        return checkBox;
    }

    @Override
    public @NotNull Boolean getUiValue() {
        return checkBox.isSelected();
    }

    @Override
    public void setUiValue(@NotNull Boolean value) {
        checkBox.setSelected(Boolean.TRUE.equals(value));
    }
}

