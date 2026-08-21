package aparmar2000.xenforoposter.settings.defs;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;

@Getter
public abstract class SettingDefinition<T> {
    private final String key;
    private final String label;
    private final String description;
    private final T defaultValue;
    private final String group;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    protected SettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, @NotNull T defaultValue) {
        this(key, label, description, defaultValue, null);
    }

    protected SettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, @NotNull T defaultValue, @Nullable String group) {
        this.key = key;
        this.label = label;
        this.description = description != null ? description : "";
        this.defaultValue = defaultValue;
        this.group = group != null && !group.trim().isEmpty() ? group.trim() : null;
    }

    public abstract Class<T> getValueType();
    public abstract boolean validate(T value);
    public abstract String serialize(T value);
    public abstract T deserialize(String rawValue);

    public abstract @NotNull JComponent getUiComponent();

    public boolean expandsHorizontally() {
        return false;
    }

    public boolean expandsVertically() {
        return false;
    }

    public abstract @NotNull T getUiValue();
    public abstract void setUiValue(@NotNull T value);
    public void resetToDefault() {
        setUiValue(defaultValue);
    }

    public boolean isUiValueValid() {
        return validate(getUiValue());
    }

    public void addChangeListener(@NotNull Runnable listener) {
        changeListeners.add(listener);
    }
    public void removeChangeListener(@NotNull Runnable listener) {
        changeListeners.remove(listener);
    }
    public void clearChangeListeners() {
        changeListeners.clear();
    }

    protected void notifyChangeListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }
}

