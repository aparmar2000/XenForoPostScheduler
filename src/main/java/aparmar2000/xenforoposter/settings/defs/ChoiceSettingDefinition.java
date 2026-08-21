package aparmar2000.xenforoposter.settings.defs;

import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;

@Getter
public class ChoiceSettingDefinition extends SettingDefinition<String> {
    private final List<String> options;
    private final JComboBox<String> comboBox;

    public ChoiceSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, @NotNull String defaultValue, @NotNull List<String> options) {
        this(key, label, description, defaultValue, options, null);
    }

    public ChoiceSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, @NotNull String defaultValue, @NotNull List<String> options, @Nullable String group) {
        super(key, label, description, defaultValue, group);
        this.options = Collections.unmodifiableList(options);
        this.comboBox = new JComboBox<>(options.toArray(new String[0]));
        this.comboBox.setSelectedItem(defaultValue);
        this.comboBox.addActionListener(e -> notifyChangeListeners());
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }

    @Override
    public boolean validate(String value) {
        return value != null && options.contains(value);
    }

    @Override
    public String serialize(String value) {
        return value != null ? value : getDefaultValue();
    }

    @Override
    public String deserialize(String rawValue) {
        if (rawValue != null && options.contains(rawValue)) {
            return rawValue;
        }
        return getDefaultValue();
    }

    @Override
    public @NotNull JComponent getUiComponent() {
        return comboBox;
    }

    @Override
    public @NotNull String getUiValue() {
        Object item = comboBox.getSelectedItem();
        return item != null ? item.toString() : getDefaultValue();
    }

    @Override
    public void setUiValue(@NotNull String value) {
        if (options.contains(value)) {
            comboBox.setSelectedItem(value);
        }
    }
}

