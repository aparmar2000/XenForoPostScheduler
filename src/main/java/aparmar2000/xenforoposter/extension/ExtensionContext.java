package aparmar2000.xenforoposter.extension;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.extension.condition.ConditionProvider;
import aparmar2000.xenforoposter.extension.hook.AbstractHookEvent;
import aparmar2000.xenforoposter.extension.hook.HookHandler;
import aparmar2000.xenforoposter.extension.hook.HookPhase;
import aparmar2000.xenforoposter.extension.hook.HookPriority;
import aparmar2000.xenforoposter.extension.hook.RegisteredHook;
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
	@NotNull String getExtensionId();

	<H extends AbstractHookEvent<S>, S> void registerHook(@NotNull RegisteredHook<H, S> hook);
	<H extends AbstractHookEvent<S>, S> void registerHook(
			@NotNull Class<H> eventClass,
			@NotNull HookHandler<H, S> handler,
			@NotNull HookPriority priority,
			@Nullable String hookName,
			@Nullable HookPhase... phases);
	<H extends AbstractHookEvent<S>, S> void registerHook(
			@NotNull Class<H> eventClass,
			@NotNull HookHandler<H, S> handler);
	void registerAnnotatedHooks(@NotNull Object extensionInstance);
	@NotNull List<RegisteredHook<?,?>> getRegisteredHooks();

	void saveSettings();
	void loadSettings();
	void resetSettingsToDefaults();
}
