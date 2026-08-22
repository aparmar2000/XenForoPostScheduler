package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.collect.ImmutableList;

import aparmar2000.xenforoposter.settings.GeneralSettings;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import aparmar2000.xenforoposter.ui.AbstractSettingsPanel;
import aparmar2000.xenforoposter.ui.GeneralSettingsPanel;

class GeneralSettingsTest {

	@TempDir
	Path tempDir;

	@Test
	@DisplayName("GeneralSettings defaults should be properly initialized and registered")
	void testGeneralSettingsDefaults() {
		GeneralSettings settings = new GeneralSettings(null, null);
		List<SettingDefinition<?>> list = settings.getRegisteredSettings();

		assertNotNull(list);
		assertFalse(list.isEmpty());

		assertEquals(3, settings.getSettingValue("scheduler.poll_interval", Integer.class));
		assertEquals(true, settings.getSettingValue("scheduler.auto_start", Boolean.class));
		assertEquals(60, settings.getSettingValue("rate_limit.global_interval", Integer.class));
		assertEquals(300, settings.getSettingValue("rate_limit.thread_interval", Integer.class));
		assertEquals(12, settings.getSettingValue("rate_limit.max_per_hour", Integer.class));
		assertEquals(60, settings.getSettingValue("rate_limit.max_per_day", Integer.class));
		assertTrue(settings.getSettingValue("network.user_agent", String.class).contains("Mozilla"));
		assertEquals(15, settings.getSettingValue("network.timeout_seconds", Integer.class));
		assertEquals("Dark", settings.getSettingValue("ui.theme", String.class));

		// Test typed getters backed by memoized suppliers
		assertEquals(3, settings.getPollIntervalSeconds());
		assertTrue(settings.isAutoStartEngine());
		assertEquals(60, settings.getMinGlobalPostIntervalSeconds());
		assertEquals(300, settings.getMinThreadPostIntervalSeconds());
		assertEquals(12, settings.getMaxPostsPerHour());
		assertEquals(60, settings.getMaxPostsPerDay());
		assertTrue(settings.getDefaultUserAgent().contains("Mozilla"));
		assertEquals(15, settings.getRequestTimeoutSeconds());
		assertEquals("Dark", settings.getAppTheme());
	}

	@Test
	@DisplayName("GeneralSettings typed getters dynamically reflect updated values")
	void testTypedGettersReflectUpdates() {
		GeneralSettings settings = new GeneralSettings(null, null);

		settings.setSettingValue("scheduler.poll_interval", 15);
		assertEquals(15, settings.getPollIntervalSeconds());

		settings.setSettingValue("scheduler.auto_start", false);
		assertFalse(settings.isAutoStartEngine());

		settings.setSettingValue("ui.theme", "Light");
		assertEquals("Light", settings.getAppTheme());

		settings.resetAllToDefaults();
		assertEquals(3, settings.getPollIntervalSeconds());
		assertTrue(settings.isAutoStartEngine());
		assertEquals("Dark", settings.getAppTheme());
	}

	@Test
	@DisplayName("GeneralSettings should persist and restore values to/from disk")
	void testPersistence() {
		Path configFile = tempDir.resolve("general_settings.json");
		GeneralSettings settings = new GeneralSettings(configFile, null);

		// Update settings
		settings.setSettingValue("scheduler.poll_interval", 10);
		settings.setSettingValue("scheduler.auto_start", false);
		settings.setSettingValue("ui.theme", "Light");
		settings.save();

		// Reload in new instance
		GeneralSettings reloaded = new GeneralSettings(configFile, null);
		assertEquals(10, reloaded.getSettingValue("scheduler.poll_interval", Integer.class));
		assertEquals(10, reloaded.getPollIntervalSeconds());
		assertEquals(false, reloaded.getSettingValue("scheduler.auto_start", Boolean.class));
		assertFalse(reloaded.isAutoStartEngine());
		assertEquals("Light", reloaded.getSettingValue("ui.theme", String.class));
		assertEquals("Light", reloaded.getAppTheme());

		// Reset
		reloaded.resetAllToDefaults();
		assertEquals(3, reloaded.getSettingValue("scheduler.poll_interval", Integer.class));
		assertEquals(3, reloaded.getPollIntervalSeconds());
		assertEquals(true, reloaded.getSettingValue("scheduler.auto_start", Boolean.class));
		assertTrue(reloaded.isAutoStartEngine());
	}

	@Test
	@DisplayName("GeneralSettingsPanel should bind UI components to GeneralSettings model")
	void testGeneralSettingsPanel() {
		GeneralSettings settings = new GeneralSettings(null, null);
		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);

		assertNotNull(panel);
		assertNotNull(panel.getFormContainer());
		assertTrue(panel.getFormContainer().getComponentCount() > 0);
		assertNotNull(panel.getHeaderLabel());
		assertEquals("Application-wide configuration and preferences", panel.getHeaderLabel().getText());
	}

	@Test
	@DisplayName("GeneralSettingsPanel should interact with mocked GeneralSettings during reload")
	void testGeneralSettingsPanelWithMockedSettings() {
		GeneralSettings mockSettings = mock(GeneralSettings.class);
		when(mockSettings.getRegisteredSettings()).thenReturn(ImmutableList.of());

		GeneralSettingsPanel panel = new GeneralSettingsPanel(mockSettings);
		assertNotNull(panel);
		assertSame(mockSettings, panel.getGeneralSettings());

		panel.reloadSettings();
		verify(mockSettings).load();
		verify(mockSettings, atLeastOnce()).getRegisteredSettings();

		panel.revertSettings();
		verify(mockSettings, times(2)).load();

		panel.saveSettings();
		verify(mockSettings).save();
	}

	@Test
	@DisplayName("AbstractSettingsPanel action bar components and helper methods")
	void testAbstractSettingsPanelActionBarAndHelpers() {
		GeneralSettings settings = new GeneralSettings(null, null);
		GeneralSettingsPanel panel = new GeneralSettingsPanel(settings);

		assertNotNull(panel.getActionBar());
		assertNotNull(panel.getReloadBtn());
		assertSame(panel.getReloadBtn(), panel.getRevertBtn());
		assertNotNull(panel.getResetBtn());
		assertNotNull(panel.getSaveBtn());

		assertTrue(panel.getReloadBtn().isEnabled());
		panel.setActionButtonsEnabled(false);
		assertFalse(panel.getReloadBtn().isEnabled());
		assertFalse(panel.getResetBtn().isEnabled());
		assertFalse(panel.getSaveBtn().isEnabled());

		// Test static createActionBar helper
		boolean[] called = new boolean[3];
		JPanel customBar = AbstractSettingsPanel.createActionBar(
				() -> called[0] = true,
				() -> called[1] = true,
				() -> called[2] = true
				);
		assertNotNull(customBar);
		assertEquals(3, customBar.getComponentCount());

		((JButton) customBar.getComponent(0)).doClick();
		((JButton) customBar.getComponent(1)).doClick();
		((JButton) customBar.getComponent(2)).doClick();
		assertTrue(called[0]);
		assertTrue(called[1]);
		assertTrue(called[2]);

		// Test wrapInFrame / createStandaloneFrame
		if (!GraphicsEnvironment.isHeadless()) {
			JFrame frame = panel.createStandaloneFrame("Test Settings", 400, 300);
			assertNotNull(frame);
			assertEquals("Test Settings", frame.getTitle());
			frame.dispose();
		}
	}
}
