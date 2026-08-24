package aparmar2000.xenforoposter.extension;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.extension.condition.ConditionProvider;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition;

public interface ExtensionContext {
	void registerSetting(@NotNull SettingDefinition<?> setting);
	<T> @Nullable T getSettingValue(@NotNull String key, @NotNull Class<T> type);
	<T> void setSettingValue(@NotNull String key, @NotNull T value);
	@NotNull List<SettingDefinition<?>> getRegisteredSettings();

	void registerToolbarItem(@NotNull BbCodeToolbarItem item);
	@NotNull List<BbCodeToolbarItem> getRegisteredToolbarItems();

	void registerCondition(@NotNull ConditionProvider provider);
	@NotNull List<ConditionProvider> getRegisteredConditions();

	BbCodeTagDefinition registerBbCodeTag(@NotNull BbCodeTagDefinition tagDefinition);
	BbCodeTagDefinition registerBbCodeTagIfAbsent(@NotNull String tag, @NotNull Supplier<BbCodeTagDefinition> supplier);
	boolean unregisterBbCodeTag(@NotNull BbCodeTagDefinition tagDefinition);
	boolean unregisterBbCodeTag(@NotNull String tag);

	HtmlTagDefinition registerHtmlTag(@NotNull HtmlTagDefinition tagDefinition);
	HtmlTagDefinition registerHtmlTagIfAbsent(@NotNull String tag, @NotNull Supplier<HtmlTagDefinition> supplier);
	boolean unregisterHtmlTag(@NotNull HtmlTagDefinition tagDefinition);
	boolean unregisterHtmlTag(@NotNull String tag);

	@NotNull Path getDataDirectory();

	void saveSettings();
	void loadSettings();
	void resetSettingsToDefaults();
}
