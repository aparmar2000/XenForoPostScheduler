package aparmar2000.xenforoposter.extension;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import aparmar2000.xenforoposter.extension.condition.ConditionProvider;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import aparmar2000.xenforoposter.settings.SettingsHolder;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import lombok.Getter;

public class InternalExtensionContext implements ExtensionContext {
    private final Path dataDirectory;
    @Getter
    private final SettingsHolder settingsHolder;
    private final List<BbCodeToolbarItem> toolbarItems = new ArrayList<>();
    private final List<ConditionProvider> conditionProviders = new ArrayList<>();

    public interface Factory {
        InternalExtensionContext create(@NotNull Path dataDirectory);
    }

    @Inject
    public InternalExtensionContext(@Assisted @NotNull Path dataDirectory, @NotNull SettingsHolder.Factory settingsHolderFactory) {
        this(dataDirectory, Objects.requireNonNull(settingsHolderFactory, "settingsHolderFactory cannot be null")
                .create(dataDirectory.resolve("settings.json")));
    }

    private InternalExtensionContext(@NotNull Path dataDirectory, @NotNull SettingsHolder settingsHolder) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory cannot be null");
        this.settingsHolder = Objects.requireNonNull(settingsHolder, "settingsHolder cannot be null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerSetting(@NotNull SettingDefinition<?> setting) {
        Objects.requireNonNull(setting, "setting cannot be null");
        settingsHolder.register((SettingDefinition<Object>) setting);
    }

    @Override
    public <T> @Nullable T getSettingValue(@NotNull String key, @NotNull Class<T> type) {
        return settingsHolder.getSettingValue(key, type);
    }

    @Override
    public <T> void setSettingValue(@NotNull String key, @NotNull T value) {
        settingsHolder.setSettingValue(key, value);
    }

    @Override
    public @NotNull ImmutableList<SettingDefinition<?>> getRegisteredSettings() {
        return settingsHolder.getRegisteredSettings();
    }

    @Override
    public void saveSettings() {
        settingsHolder.save();
    }

    @Override
    public void loadSettings() {
        settingsHolder.load();
    }

    @Override
    public void registerToolbarItem(@NotNull BbCodeToolbarItem item) {
        toolbarItems.add(Objects.requireNonNull(item, "item cannot be null"));
    }

    @Override
    public @NotNull ImmutableList<BbCodeToolbarItem> getRegisteredToolbarItems() {
        return ImmutableList.copyOf(toolbarItems);
    }

    @Override
    public void registerCondition(@NotNull ConditionProvider provider) {
        conditionProviders.add(Objects.requireNonNull(provider, "provider cannot be null"));
    }

    @Override
    public @NotNull ImmutableList<ConditionProvider> getRegisteredConditions() {
        return ImmutableList.copyOf(conditionProviders);
    }

    @Override
    public @NotNull Path getDataDirectory() {
        return dataDirectory;
    }
}