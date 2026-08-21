package aparmar2000.xenforoposter.extension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.swing.JCheckBox;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;

import aparmar2000.xenforoposter.di.AppModule;
import aparmar2000.xenforoposter.extension.condition.ConditionProvider;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import aparmar2000.xenforoposter.extension.toolbar.EditorContext;
import aparmar2000.xenforoposter.settings.SettingsHolder;
import aparmar2000.xenforoposter.settings.defs.BooleanSettingDefinition;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import aparmar2000.xenforoposter.settings.defs.StringSettingDefinition;
import aparmar2000.xenforoposter.ui.ExtensionManagerPanel;
import aparmar2000.xenforoposter.ui.ExtensionSettingsPanel;

class ExtensionSystemTest {
	@TempDir
	Path tempDir;

	private InternalExtensionContext.Factory contextFactory;
	private ExtensionManager extensionManager;

	@BeforeEach
	void setUp() {
		contextFactory = mock(InternalExtensionContext.Factory.class);
		when(contextFactory.create(any())).thenAnswer(inv -> {
			Path path = inv.getArgument(0);
			SettingsHolder mockHolder = mock(SettingsHolder.class);
			Map<String, Object> values = new ConcurrentHashMap<>();
			List<SettingDefinition<?>> defs = new ArrayList<>();
			when(mockHolder.register(any())).thenAnswer(regInv -> {
				SettingDefinition<?> def = regInv.getArgument(0);
				defs.add(def);
				values.put(def.getKey(), def.getDefaultValue());
				Supplier<Object> sup = () -> values.get(def.getKey());
				return sup;
			});
			when(mockHolder.getRegisteredSettings()).thenAnswer(gInv -> ImmutableList.copyOf(defs));
			when(mockHolder.getSettingValue(anyString(), any())).thenAnswer(sInv -> {
				String k = sInv.getArgument(0);
				Class<?> t = sInv.getArgument(1);
				Object v = values.get(k);
				return t.isInstance(v) ? t.cast(v) : null;
			});
			doAnswer(sInv -> {
				String k = sInv.getArgument(0);
				Object v = sInv.getArgument(1);
				values.put(k, v);
				return true;
			}).when(mockHolder).setSettingValue(anyString(), any());
			SettingsHolder.Factory holderFactory = mock(SettingsHolder.Factory.class);
			when(holderFactory.create(any())).thenReturn(mockHolder);
			return new InternalExtensionContext(path, holderFactory);
		});
		extensionManager = new ExtensionManager(tempDir, new Gson(), contextFactory);
	}

	@Test
	@DisplayName("Should register internal extension and execute toolbar item against mocked EditorContext")
	void testExtensionRegistration() {
		TestPlugin plugin = new TestPlugin();
		extensionManager.registerInternalExtension(plugin);
		extensionManager.loadAllExtensions();

		assertEquals(1, extensionManager.getAllExtensions().size());
		List<BbCodeToolbarItem> toolbarItems = extensionManager.getActiveToolbarItems();
		assertEquals(1, toolbarItems.size());
		assertEquals("Insert Test", toolbarItems.get(0).getLabel());

		// Test EditorContext interaction with Mockito mock
		EditorContext mockEditor = mock(EditorContext.class);
		toolbarItems.get(0).execute(mockEditor);
		verify(mockEditor).wrapSelection("[B]", "[/B]");
	}

	@Test
	@DisplayName("Toggling extension off should trigger lifecycle methods on mocked Extension and remove active toolbar items")
	void testExtensionToggle() {
		Extension mockPlugin = mock(Extension.class);
		when(mockPlugin.getId()).thenReturn("mock.plugin");
		when(mockPlugin.getName()).thenReturn("Mock Plugin");

		ExtensionManager.ExtensionChangeListener mockListener = mock(ExtensionManager.ExtensionChangeListener.class);
		extensionManager.addChangeListener(mockListener);

		extensionManager.registerInternalExtension(mockPlugin);
		extensionManager.loadAllExtensions();

		verify(mockPlugin).initialize(any());
		verify(mockPlugin).onEnable();
		verify(mockPlugin, never()).onDisable();
		verify(mockListener, atLeastOnce()).onExtensionsUpdated();

		extensionManager.setExtensionEnabled("mock.plugin", false);
		verify(mockPlugin).onDisable();
		assertEquals(0, extensionManager.getActiveToolbarItems().size());

		extensionManager.setExtensionEnabled("mock.plugin", true);
		verify(mockPlugin, times(2)).onEnable();
	}

	@Test
	@DisplayName("ExtensionHolder initialize invokes extension initialize, context loadSettings, and onEnable on mocks")
	void testExtensionHolderInitializeWithMocks() {
		Extension mockExt = mock(Extension.class);
		InternalExtensionContext mockContext = mock(InternalExtensionContext.class);
		ExtensionMetadata mockMeta = mock(ExtensionMetadata.class);
		Path extDir = tempDir.resolve("mock_holder_ext");

		ExtensionHolder holder = new ExtensionHolder(mockExt, extDir, mockMeta, mockContext);
		assertFalse(holder.isInitialized());
		
		holder.initialize();
		holder.setEnabled(true);

		assertTrue(holder.isInitialized());
		verify(mockExt).initialize(mockContext);
		verify(mockContext).loadSettings();
		verify(mockExt).onEnable();
	}

	@Test
	@DisplayName("ExtensionHolder setEnabled toggles onEnable and onDisable on mocked Extension")
	void testExtensionHolderSetEnabledWithMocks() {
		Extension mockExt = mock(Extension.class);
		InternalExtensionContext mockContext = mock(InternalExtensionContext.class);
		ExtensionMetadata mockMeta = mock(ExtensionMetadata.class);
		Path extDir = tempDir.resolve("mock_holder_ext");

		ExtensionHolder holder = new ExtensionHolder(mockExt, extDir, mockMeta, mockContext);
		assertFalse(holder.isEnabled());
		
		// Before initialization
		holder.setEnabled(true);
		assertTrue(holder.isEnabled());
		verify(mockExt, never()).onEnable();

		holder.setEnabled(false);
		assertFalse(holder.isEnabled());
		verify(mockExt, never()).onDisable();
		
		holder.initialize();
		assertFalse(holder.isEnabled());
		verify(mockExt, never()).onEnable();
		verify(mockExt, never()).onDisable();

		// After initialization
		holder.setEnabled(true);
		assertTrue(holder.isEnabled());
		verify(mockExt).onEnable();

		holder.setEnabled(false);
		assertFalse(holder.isEnabled());
		verify(mockExt).onDisable();
	}

	private static class TestPlugin implements Extension {
		@Override public @NotNull String getId() { return "test.plugin"; }
		@Override public @NotNull String getName() { return "Test Plugin"; }
		@Override public @NotNull String getVersion() { return "1.0.0"; }
		@Override public @NotNull String getAuthor() { return "Tester"; }
		@Override public @NotNull String getDescription() { return "Unit test plugin"; }

		@Override
		public void initialize(@NotNull ExtensionContext context) {
			context.registerSetting(new BooleanSettingDefinition("enabled_feature", "Feature", "Toggle feature", true, "General Options"));
			context.registerSetting(new StringSettingDefinition("tag_name", "Tag Name", "Tag", "default_tag", "General Options"));

			context.registerToolbarItem(BbCodeToolbarItem.builder()
					.id("test_btn")
					.label("Insert Test")
					.action(ctx -> ctx.wrapSelection("[B]", "[/B]"))
					.build());
		}
	}


	@Test
	@DisplayName("ExtensionSettingsPanel displays settings and updates ExtensionContext on value changes")
	void testExtensionSettingsPanelIntegration() {
		TestPlugin plugin = new TestPlugin();
		extensionManager.registerInternalExtension(plugin);
		extensionManager.loadAllExtensions();

		ExtensionSettingsPanel settingsPanel = new ExtensionSettingsPanel();
		ExtensionHolder holder = extensionManager.getExtensionHolder("test.plugin");
		assertNotNull(holder);

		settingsPanel.displayExtensionSettings(holder);
		assertEquals(holder, settingsPanel.getCurrentHolder());

		// Update UI setting and verify context receives value
		List<SettingDefinition<?>> settings = holder.getContext().getRegisteredSettings();
		assertEquals(2, settings.size());

		BooleanSettingDefinition boolDef = (BooleanSettingDefinition) settings.get(0);
		boolDef.setUiValue(false);
		// Trigger simulated action
		((JCheckBox) boolDef.getUiComponent()).doClick();

		Boolean updatedVal = holder.getContext().getSettingValue("enabled_feature", Boolean.class);
		assertNotNull(updatedVal);
		assertTrue(updatedVal);
		assertTrue(settingsPanel.getHeaderLabel().getText().contains("Built-in"));
	}

	@Test
	@DisplayName("ExtensionMetadata correctly records origin for built-in and external JAR extensions")
	void testExtensionMetadataTracking() {
		// Built-in metadata verification
		TestPlugin plugin = new TestPlugin();
		extensionManager.registerInternalExtension(plugin);
		extensionManager.loadAllExtensions();

		ExtensionHolder builtInHolder = extensionManager.getExtensionHolder("test.plugin");
		assertNotNull(builtInHolder);
		assertNotNull(builtInHolder.getMetadata());
		assertTrue(builtInHolder.getMetadata().isBuiltIn());
		assertEquals("Built-in", builtInHolder.getMetadata().getSource());

		// External JAR metadata verification
		Extension mockExt = mock(Extension.class);
		when(mockExt.getId()).thenReturn("external.sample");
		when(mockExt.getName()).thenReturn("Sample External Plugin");
		when(mockExt.getVersion()).thenReturn("2.1.0");
		when(mockExt.getAuthor()).thenReturn("External Dev");

		File jarFile = new File("sample-extension-1.0.0.jar");
		ExtensionMetadata jarMeta = ExtensionMetadata.fromJar(jarFile);
		assertFalse(jarMeta.isBuiltIn());
		assertEquals("sample-extension-1.0.0.jar", jarMeta.getSource());
		assertEquals("sample-extension-1.0.0.jar", jarMeta.getJarFileName());

		InternalExtensionContext mockContext = mock(InternalExtensionContext.class);
		ExtensionHolder externalHolder = new ExtensionHolder(mockExt, tempDir.resolve("external.sample"), jarMeta, mockContext);
		assertNotNull(externalHolder.getMetadata());
		assertFalse(externalHolder.getMetadata().isBuiltIn());
		assertEquals("sample-extension-1.0.0.jar", externalHolder.getMetadata().getSource());
	}

	@Test
	@DisplayName("ExtensionManagerPanel displays origin metadata in the Source table column")
	void testExtensionManagerPanelDisplaysOrigin() {
		TestPlugin plugin = new TestPlugin();
		extensionManager.registerInternalExtension(plugin);
		extensionManager.loadAllExtensions();

		ExtensionManagerPanel panel = new ExtensionManagerPanel(extensionManager);
		javax.swing.JTable table = panel.getExtensionTable();

		assertEquals("Source", table.getColumnName(4));
		assertEquals(1, table.getRowCount());
		assertEquals("Built-in", table.getValueAt(0, 4));
	}

	@Test
	@DisplayName("InternalExtensionContext delegates settings storage, retrieval, and registration to SettingsHolder mock")
	void testExtensionContextSettingsHolderDelegation() {
		SettingsHolder mockSettingsHolder = mock(SettingsHolder.class);
		SettingsHolder.Factory mockHolderFactory = mock(SettingsHolder.Factory.class);
		when(mockHolderFactory.create(any())).thenReturn(mockSettingsHolder);

		Path extDir = tempDir.resolve("test_delegation");
		InternalExtensionContext context = new InternalExtensionContext(extDir, mockHolderFactory);

		StringSettingDefinition strDef = new StringSettingDefinition("custom.key", "Custom Key", "Desc", "default_val", "Group");
		context.registerSetting(strDef);
		verify(mockSettingsHolder).register(strDef);

		when(mockSettingsHolder.getSettingValue("custom.key", String.class)).thenReturn("mock_val");
		assertEquals("mock_val", context.getSettingValue("custom.key", String.class));
		verify(mockSettingsHolder).getSettingValue("custom.key", String.class);

		context.setSettingValue("custom.key", "new_val");
		verify(mockSettingsHolder).setSettingValue("custom.key", "new_val");

		context.saveSettings();
		verify(mockSettingsHolder).save();

		context.loadSettings();
		verify(mockSettingsHolder).load();

		when(mockSettingsHolder.getRegisteredSettings()).thenReturn(ImmutableList.of(strDef));
		assertEquals(1, context.getRegisteredSettings().size());
		verify(mockSettingsHolder).getRegisteredSettings();
		assertSame(mockSettingsHolder, context.getSettingsHolder());
	}

	@Test
	@DisplayName("InternalExtensionContext manages toolbars, conditions, and directory paths")
	void testExtensionContextToolbarsAndConditions() {
		SettingsHolder mockSettingsHolder = mock(SettingsHolder.class);
		SettingsHolder.Factory mockHolderFactory = mock(SettingsHolder.Factory.class);
		when(mockHolderFactory.create(any())).thenReturn(mockSettingsHolder);

		Path extDir = tempDir.resolve("test_misc");
		InternalExtensionContext context = new InternalExtensionContext(extDir, mockHolderFactory);

		assertEquals(extDir, context.getDataDirectory());

		BbCodeToolbarItem item = BbCodeToolbarItem.builder().id("item1").label("Item 1").action(ctx -> {}).build();
		context.registerToolbarItem(item);
		assertEquals(1, context.getRegisteredToolbarItems().size());
		assertEquals("item1", context.getRegisteredToolbarItems().get(0).getId());

		ConditionProvider provider = mock(ConditionProvider.class);
		context.registerCondition(provider);
		assertEquals(1, context.getRegisteredConditions().size());
		assertSame(provider, context.getRegisteredConditions().get(0));
	}

	@Test
	@DisplayName("Guice properly injects InternalExtensionContext.Factory and wires ExtensionManager")
	void testGuiceDependencyInjection() {
		@SuppressWarnings("deprecation")
		Injector injector = Guice.createInjector(new AppModule(tempDir));
		InternalExtensionContext.Factory injectedContextFactory = injector.getInstance(InternalExtensionContext.Factory.class);
		assertNotNull(injectedContextFactory);

		Path extDir = tempDir.resolve("injected_ext");
		InternalExtensionContext context = injectedContextFactory.create(extDir);
		assertNotNull(context);
		assertNotNull(context.getSettingsHolder());
		assertEquals(extDir.resolve("settings.json"), context.getSettingsHolder().getSettingsFile());

		ExtensionManager injectedManager = injector.getInstance(ExtensionManager.class);
		assertNotNull(injectedManager);

		TestPlugin plugin = new TestPlugin();
		injectedManager.registerInternalExtension(plugin);
		injectedManager.loadAllExtensions();

		ExtensionHolder holder = injectedManager.getExtensionHolder("test.plugin");
		assertNotNull(holder);
		assertNotNull(holder.getContext());
		assertEquals(Boolean.TRUE, holder.getContext().getSettingValue("enabled_feature", Boolean.class));
	}
}

