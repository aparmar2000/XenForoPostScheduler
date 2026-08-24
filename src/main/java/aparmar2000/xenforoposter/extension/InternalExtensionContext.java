package aparmar2000.xenforoposter.extension;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import aparmar2000.xenforoposter.extension.condition.ConditionProvider;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import aparmar2000.xenforoposter.settings.SettingsHolder;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import aparmar2000.xenforoposter.syntax.TagSource;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinitionRegistry;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;
import lombok.Getter;
import lombok.NonNull;

public class InternalExtensionContext implements ExtensionContext {
	@Getter
	private final Path dataDirectory;
	@Getter
	private final SettingsHolder settingsHolder;
	private final BbCodeTagDefinitionRegistry bbCodeTagDefinitionRegistry;
	private final HtmlTagDefinitionRegistry htmlTagDefinitionRegistry;
	@Getter
	private final TagSource tagSource;
	private final List<BbCodeToolbarItem> toolbarItems = new ArrayList<>();
	private final List<ConditionProvider> conditionProviders = new ArrayList<>();

	public interface Factory {
		InternalExtensionContext create(@NonNull Path dataDirectory, @NonNull String extensionId);
	}

	@Inject
	public InternalExtensionContext(@Assisted @NonNull Path dataDirectory,
			@Assisted @NonNull String extensionId,
			@NonNull SettingsHolder.Factory settingsHolderFactory,
			@NonNull BbCodeTagDefinitionRegistry bbCodeTagDefinitionRegistry,
			@NonNull HtmlTagDefinitionRegistry htmlTagDefinitionRegistry) {
		this.dataDirectory = dataDirectory;
		this.settingsHolder = settingsHolderFactory.create(dataDirectory.resolve("settings.json"));
		this.bbCodeTagDefinitionRegistry = bbCodeTagDefinitionRegistry;
		this.htmlTagDefinitionRegistry = htmlTagDefinitionRegistry;
		this.tagSource = TagSource.of(dataDirectory.getFileName() != null
				? dataDirectory.getFileName().toString()
				: "unknown");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void registerSetting(@NonNull SettingDefinition<?> setting) {
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
	public void resetSettingsToDefaults() {
		settingsHolder.resetAllToDefaults();
	}

	@Override
	public void registerToolbarItem(@NonNull BbCodeToolbarItem item) {
		toolbarItems.add(item);
	}

	@Override
	public @NotNull ImmutableList<BbCodeToolbarItem> getRegisteredToolbarItems() {
		return ImmutableList.copyOf(toolbarItems);
	}

	@Override
	public void registerCondition(@NonNull ConditionProvider provider) {
		conditionProviders.add(provider);
	}

	@Override
	public @NotNull ImmutableList<ConditionProvider> getRegisteredConditions() {
		return ImmutableList.copyOf(conditionProviders);
	}

	@Override
	public BbCodeTagDefinition registerBbCodeTag(@NonNull BbCodeTagDefinition tagDefinition) {
		return bbCodeTagDefinitionRegistry.register(tagSource, tagDefinition);
	}

	@Override
	public BbCodeTagDefinition registerBbCodeTagIfAbsent(@NonNull String tag,
			@NonNull Supplier<BbCodeTagDefinition> supplier) {
		return bbCodeTagDefinitionRegistry.registerIfAbsent(tagSource, tag, supplier);
	}

	@Override
	public boolean unregisterBbCodeTag(@NonNull BbCodeTagDefinition tagDefinition) {
		return bbCodeTagDefinitionRegistry.unregister(tagSource, tagDefinition);
	}

	@Override
	public boolean unregisterBbCodeTag(@NonNull String tag) {
		return bbCodeTagDefinitionRegistry.unregister(tagSource, tag);
	}

	@Override
	public HtmlTagDefinition registerHtmlTag(@NonNull HtmlTagDefinition tagDefinition) {
		return htmlTagDefinitionRegistry.register(tagSource, tagDefinition);
	}

	@Override
	public HtmlTagDefinition registerHtmlTagIfAbsent(@NonNull String tag,
			@NonNull Supplier<HtmlTagDefinition> supplier) {
		return htmlTagDefinitionRegistry.registerIfAbsent(tagSource, tag, supplier);
	}

	@Override
	public boolean unregisterHtmlTag(@NonNull HtmlTagDefinition tagDefinition) {
		return htmlTagDefinitionRegistry.unregister(tagSource, tagDefinition);
	}

	@Override
	public boolean unregisterHtmlTag(@NonNull String tag) {
		return htmlTagDefinitionRegistry.unregister(tagSource, tag);
	}
}