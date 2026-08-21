package aparmar2000.xenforoposter.extension;

import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.extension.condition.ConditionProvider;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;

public interface ExtensionContext {
    void registerSetting(@NotNull SettingDefinition<?> setting);
    <T> @Nullable T getSettingValue(@NotNull String key, @NotNull Class<T> type);
    <T> void setSettingValue(@NotNull String key, @NotNull T value);
    @NotNull List<SettingDefinition<?>> getRegisteredSettings();

    void registerToolbarItem(@NotNull BbCodeToolbarItem item);
    @NotNull List<BbCodeToolbarItem> getRegisteredToolbarItems();

    void registerCondition(@NotNull ConditionProvider provider);
    @NotNull List<ConditionProvider> getRegisteredConditions();

    @NotNull Path getDataDirectory();

    void saveSettings();
    void loadSettings();
}
