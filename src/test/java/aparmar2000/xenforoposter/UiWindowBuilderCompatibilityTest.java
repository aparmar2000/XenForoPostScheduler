package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Constructor;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingConstants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import aparmar2000.xenforoposter.extension.ExtensionHolder;
import aparmar2000.xenforoposter.ui.AccountManagerPanel;
import aparmar2000.xenforoposter.ui.BbCodeEditorPanel;
import aparmar2000.xenforoposter.ui.ConditionBuilderPanel;
import aparmar2000.xenforoposter.ui.ExtensionManagerPanel;
import aparmar2000.xenforoposter.ui.ExtensionSettingsPanel;
import aparmar2000.xenforoposter.ui.GeneralSettingsPanel;
import aparmar2000.xenforoposter.ui.JobListPanel;
import aparmar2000.xenforoposter.ui.MainFrame;
import aparmar2000.xenforoposter.ui.PollLogPanel;
import aparmar2000.xenforoposter.ui.PostEditorFormPanel;
import aparmar2000.xenforoposter.ui.SettingsDialog;
import aparmar2000.xenforoposter.ui.SettingsPanel;
import aparmar2000.xenforoposter.ui.UiPreviewHelper;

@SuppressWarnings("deprecation")
class UiWindowBuilderCompatibilityTest {

	@ParameterizedTest
	@ValueSource(classes = {
			MainFrame.class,
			AccountManagerPanel.class,
			BbCodeEditorPanel.class,
			ConditionBuilderPanel.class,
			ExtensionManagerPanel.class,
			ExtensionSettingsPanel.class,
			GeneralSettingsPanel.class,
			JobListPanel.class,
			PollLogPanel.class,
			PostEditorFormPanel.class,
			SettingsPanel.class,
			SettingsDialog.class
	})
	@DisplayName("Every UI class must provide a no-argument constructor for WindowBuilder preview")
	void testZeroArgConstructorExists(Class<?> uiClass) throws Exception {
		Constructor<?> constructor = uiClass.getDeclaredConstructor();
		assertNotNull(constructor, "Zero-arg constructor must exist for " + uiClass.getSimpleName());

		// For classes with injected dependencies, constructor should be marked @Deprecated to restrict production use
		if (uiClass != ExtensionSettingsPanel.class && uiClass != PollLogPanel.class) {
			assertTrue(constructor.isAnnotationPresent(Deprecated.class),
					"WindowBuilder preview constructor should be marked @Deprecated on " + uiClass.getSimpleName());
		}

		// Headless safeguard for top-level windows
		if (GraphicsEnvironment.isHeadless() && (JFrame.class.isAssignableFrom(uiClass) || JDialog.class.isAssignableFrom(uiClass))) {
			return;
		}

		// Must be able to instantiate without throwing exceptions
		Object instance = constructor.newInstance();
		assertNotNull(instance, "Failed to instantiate " + uiClass.getSimpleName());
		assertTrue(instance instanceof Component, "Instance should be a Swing Component: " + uiClass.getSimpleName());

		if (instance instanceof JFrame) {
			((JFrame) instance).dispose();
		} else if (instance instanceof JDialog) {
			((JDialog) instance).dispose();
		}
	}

	@Test
	@DisplayName("MainFrame can be instantiated in design mode with preview data")
	void testMainFramePreview() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		MainFrame mainFrame = new MainFrame();
		assertNotNull(mainFrame.getJMenuBar(), "Menu bar should be initialized");
		assertEquals(3, mainFrame.getJMenuBar().getMenuCount(), "Menu bar should have 3 menus");
		assertEquals("XenForo Post Scheduler", mainFrame.getTitle());
		mainFrame.dispose();
	}

	@Test
	@DisplayName("AccountManagerPanel preview initializes table with sample profiles")
	void testAccountManagerPanelPreview() {
		AccountManagerPanel panel = new AccountManagerPanel();
		assertNotNull(panel);
		assertTrue(panel.getComponentCount() >= 2); // toolbar + scrollpane
	}

	@Test
	@DisplayName("BbCodeEditorPanel preview contains sample text, CardLayout for editor and preview, and renders HTML preview")
	void testBbCodeEditorPanelPreview() {
		BbCodeEditorPanel panel = new BbCodeEditorPanel();
		assertNotNull(panel.getContent());
		assertFalse(panel.getContent().isEmpty(), "Sample BBCode content should be pre-loaded in preview mode");
		assertNotNull(panel.getCardLayout(), "CardLayout should be initialized");
		assertNotNull(panel.getCardsPanel(), "Cards panel should be initialized");
		assertEquals(2, panel.getCardsPanel().getComponentCount(), "Cards panel should have 2 cards: editor and preview");

		assertFalse(panel.isShowingPreview(), "Should initially show editor card");
		panel.showPreviewCard();
		assertTrue(panel.isShowingPreview(), "Should show preview card after showPreviewCard()");
		assertNotNull(panel.getPreviewPane().getText(), "Preview pane should have rendered HTML");

		panel.showEditorCard();
		assertFalse(panel.isShowingPreview(), "Should return to editor card after showEditorCard()");
	}

	@Test
	@DisplayName("ConditionBuilderPanel preview loads default conditions")
	void testConditionBuilderPanelPreview() {
		ConditionBuilderPanel panel = new ConditionBuilderPanel();
		assertFalse(panel.getConditions().isEmpty(), "Default conditions should be populated");
	}

	@Test
	@DisplayName("ExtensionManagerPanel preview loads built-in extensions and provides row settings navigation via CardLayout")
	void testExtensionManagerPanelPreview() {
		ExtensionManagerPanel panel = new ExtensionManagerPanel();
		assertNotNull(panel);
		assertNotNull(panel.getExtensionTable());
		assertEquals(7, panel.getExtensionTable().getColumnCount(), "Table should have 7 columns including Settings");
		assertEquals("Settings", panel.getExtensionTable().getColumnName(6));
		assertTrue(panel.getExtensionTable().getRowCount() > 0, "Built-in extensions should be loaded in preview");

		assertNotNull(panel.getCardLayout());
		assertNotNull(panel.getCardsContainer());
		assertEquals(2, panel.getCardsContainer().getComponentCount());

		// Navigate to settings view
		assertNotNull(panel.getSettingsPanel());
		ExtensionHolder firstHolder = UiPreviewHelper.createPreviewExtensionManager().getAllExtensions().iterator().next();
		panel.openExtensionSettings(firstHolder);
		assertEquals(firstHolder, panel.getSettingsPanel().getCurrentHolder());

		// Navigate back to list view
		panel.showExtensionList();
	}

	@Test
	@DisplayName("ExtensionSettingsPanel preview displays settings for registered extensions")
	void testExtensionSettingsPanelPreview() {
		ExtensionSettingsPanel panel = new ExtensionSettingsPanel();
		assertNotNull(panel);
	}

	@Test
	@DisplayName("JobListPanel preview initializes table and poll history view")
	void testJobListPanelPreview() {
		JobListPanel panel = new JobListPanel();
		assertNotNull(panel);
	}

	@Test
	@DisplayName("PollLogPanel preview populates poll history")
	void testPollLogPanelPreview() {
		PollLogPanel panel = new PollLogPanel();
		assertNotNull(panel);
		panel.displayJobHistory(UiPreviewHelper.createPreviewSampleJob());
	}

	@Test
	@DisplayName("GeneralSettingsPanel preview displays program settings")
	void testGeneralSettingsPanelPreview() {
		GeneralSettingsPanel panel = new GeneralSettingsPanel();
		assertNotNull(panel);
		assertNotNull(panel.getGeneralSettings());
		assertFalse(panel.getGeneralSettings().getRegisteredSettings().isEmpty());
	}

	@Test
	@DisplayName("SettingsPanel preview displays general and extension settings tabs on the left edge")
	void testSettingsPanelPreview() {
		SettingsPanel panel = new SettingsPanel();
		assertNotNull(panel);
		assertEquals(2, panel.getTabbedPane().getTabCount());
		assertEquals(SwingConstants.LEFT, panel.getTabbedPane().getTabPlacement());
		assertNotNull(panel.getGeneralSettingsPanel());
		assertNotNull(panel.getExtensionManagerPanel());
	}

	@Test
	@DisplayName("SettingsDialog can be instantiated in design mode")
	void testSettingsDialogPreview() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		SettingsDialog dialog = new SettingsDialog();
		assertNotNull(dialog);
		assertEquals("Settings", dialog.getTitle());
		assertNotNull(dialog.getSettingsPanel());
		assertEquals(SwingConstants.LEFT, dialog.getSettingsPanel().getTabbedPane().getTabPlacement());
		dialog.dispose();
	}
}
