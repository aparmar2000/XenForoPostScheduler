package aparmar2000.xenforoposter.extension;

import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.extension.condition.ConditionProvider;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinitionRegistry;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;

public interface ExtensionContext {
	void registerSetting(@NotNull SettingDefinition<?> setting);
	<T> @Nullable T getSettingValue(@NotNull String key, @NotNull Class<T> type);
	<T> void setSettingValue(@NotNull String key, @NotNull T value);
	@NotNull List<SettingDefinition<?>> getRegisteredSettings();

	void registerToolbarItem(@NotNull BbCodeToolbarItem item);
	@NotNull List<BbCodeToolbarItem> getRegisteredToolbarItems();

	void registerCondition(@NotNull ConditionProvider provider);
	@NotNull List<ConditionProvider> getRegisteredConditions();

	void registerBbCodeTag(@NotNull BbCodeTagDefinition tagDefinition);
	@NotNull BbCodeTagDefinitionRegistry getBbCodeTagDefinitionRegistry();

	HtmlTagDefinition registerHtmlTag(@NotNull HtmlTagDefinition tagDefinition);
	@NotNull HtmlTagDefinitionRegistry getHtmlTagDefinitionRegistry();

	@NotNull Path getDataDirectory();

	void saveSettings();
	void loadSettings();
	void resetSettingsToDefaults();
}

