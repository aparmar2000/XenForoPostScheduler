package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;

import aparmar2000.xenforoposter.di.AppModule;
import aparmar2000.xenforoposter.extension.Extension;
import aparmar2000.xenforoposter.extension.ExtensionContext;
import aparmar2000.xenforoposter.extension.ExtensionHolder;
import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.ExtensionMetadata;
import aparmar2000.xenforoposter.extension.InternalExtensionContext;
import aparmar2000.xenforoposter.settings.defs.BooleanSettingDefinition;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import aparmar2000.xenforoposter.settings.defs.StringSettingDefinition;
import aparmar2000.xenforoposter.ui.ExtensionSettingsPanel;

class ExtensionSettingsTest {

	@TempDir
	Path tempDir;

	private ExtensionManager extensionManager;
	private MockedStatic<JOptionPane> mockedJOptionPane;

	@BeforeEach
	void setUp() {
		mockedJOptionPane = mockStatic(JOptionPane.class);
		mockedJOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt()))
				.thenReturn(JOptionPane.YES_OPTION);
		mockedJOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(JOptionPane.YES_OPTION);

		@SuppressWarnings("deprecation")
		Injector injector = Guice.createInjector(new AppModule(tempDir));
		extensionManager = injector.getInstance(ExtensionManager.class);
	}

	@AfterEach
	void tearDown() {
		if (mockedJOptionPane != null) {
			mockedJOptionPane.close();
		}
	}

	private static class SampleSettingsExtension implements Extension {
		@Override public String getId() { return "sample.settings.ext"; }
		@Override public String getName() { return "Sample Settings Extension"; }
		@Override public String getVersion() { return "1.0.0"; }
		@Override public String getAuthor() { return "Author"; }
		@Override public String getDescription() { return "Description"; }

		@Override
		public void initialize(ExtensionContext context) {
			context.registerSetting(new BooleanSettingDefinition("feature_enabled", "Enable Feature", "Toggle feature on or off", true, "Options"));
			context.registerSetting(new StringSettingDefinition("api_endpoint", "API Endpoint", "Server endpoint url", "https://api.example.com", "Network"));
		}
	}

	@Test
	@DisplayName("ExtensionSettingsPanel displays settings and updates ExtensionContext on UI change")
	void testExtensionSettingsPanelIntegration() {
		extensionManager.registerInternalExtension(new SampleSettingsExtension());
		extensionManager.loadAllExtensions();

		ExtensionSettingsPanel panel = new ExtensionSettingsPanel();
		ExtensionHolder holder = extensionManager.getExtensionHolder("sample.settings.ext");
		assertNotNull(holder);

		panel.displayExtensionSettings(holder);
		assertEquals(holder, panel.getCurrentHolder());
		assertTrue(panel.getHeaderLabel().getText().contains("Sample Settings Extension"));

		List<SettingDefinition<?>> settings = holder.getContext().getRegisteredSettings();
		assertEquals(2, settings.size());

		BooleanSettingDefinition boolDef = (BooleanSettingDefinition) settings.get(0);
		((JCheckBox) boolDef.getUiComponent()).doClick();

		Boolean updatedVal = holder.getContext().getSettingValue("feature_enabled", Boolean.class);
		assertNotNull(updatedVal);
		assertFalse(updatedVal);
	}

	@Test
	@DisplayName("ExtensionSettingsPanel action bar buttons and action methods operate on current holder")
	void testExtensionSettingsPanelActionButtons() {
		extensionManager.registerInternalExtension(new SampleSettingsExtension());
		extensionManager.loadAllExtensions();

		ExtensionSettingsPanel panel = new ExtensionSettingsPanel();

		// Initially no extension selected -> action buttons should be disabled
		assertNull(panel.getCurrentHolder());
		assertFalse(panel.getSaveBtn().isEnabled());
		assertFalse(panel.getReloadBtn().isEnabled());
		assertFalse(panel.getResetBtn().isEnabled());

		ExtensionHolder holder = extensionManager.getExtensionHolder("sample.settings.ext");
		assertNotNull(holder);

		panel.displayExtensionSettings(holder);
		assertTrue(panel.getSaveBtn().isEnabled());
		assertTrue(panel.getReloadBtn().isEnabled());
		assertTrue(panel.getResetBtn().isEnabled());

		// Modify setting value
		holder.getContext().setSettingValue("api_endpoint", "https://modified.example.com");
		assertEquals("https://modified.example.com", holder.getContext().getSettingValue("api_endpoint", String.class));

		// Test saving settings
		panel.saveSettings();

		// Test reverting settings
		panel.revertSettings();
		panel.reloadSettings();

		// Test resetting defaults: Cancel reset -> value preserved
		mockedJOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(JOptionPane.NO_OPTION);
		panel.resetDefaults();
		assertEquals("https://modified.example.com", holder.getContext().getSettingValue("api_endpoint", String.class));

		// Test resetting defaults: Confirm reset -> value restored to default
		mockedJOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(JOptionPane.YES_OPTION);
		panel.resetDefaults();
		assertEquals("https://api.example.com", holder.getContext().getSettingValue("api_endpoint", String.class));

		// Deselect extension -> action buttons disabled
		panel.displayExtensionSettings(null);
		assertNull(panel.getCurrentHolder());
		assertFalse(panel.getSaveBtn().isEnabled());
		assertFalse(panel.getReloadBtn().isEnabled());
		assertFalse(panel.getResetBtn().isEnabled());
	}

	@Test
	@DisplayName("ExtensionSettingsPanel action methods interact properly with mocked ExtensionHolder and ExtensionContext")
	void testExtensionSettingsPanelWithMocks() {
		Extension mockExt = mock(Extension.class);
		when(mockExt.getId()).thenReturn("mock.ext");
		when(mockExt.getName()).thenReturn("Mock Extension");
		when(mockExt.getVersion()).thenReturn("1.0.0");

		InternalExtensionContext mockContext = mock(InternalExtensionContext.class);
		when(mockContext.getRegisteredSettings()).thenReturn(ImmutableList.of());

		ExtensionMetadata metadata = ExtensionMetadata.builtIn();
		ExtensionHolder holder = new ExtensionHolder(mockExt, tempDir.resolve("mock_ext"), metadata, mockContext);

		ExtensionSettingsPanel panel = new ExtensionSettingsPanel();
		panel.displayExtensionSettings(holder);

		panel.saveSettings();
		verify(mockContext).saveSettings();

		panel.reloadSettings();
		verify(mockContext).loadSettings();

		// Test reset cancellation -> does not call resetSettingsToDefaults()
		mockedJOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(JOptionPane.NO_OPTION);
		panel.resetDefaults();
		verify(mockContext, never()).resetSettingsToDefaults();

		// Test reset confirmation -> calls resetSettingsToDefaults()
		mockedJOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(JOptionPane.YES_OPTION);
		panel.resetDefaults();
		verify(mockContext).resetSettingsToDefaults();
	}
}
