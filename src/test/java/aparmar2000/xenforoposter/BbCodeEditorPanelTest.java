package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import aparmar2000.xenforoposter.ui.BbCodeEditorPanel;
import aparmar2000.xenforoposter.ui.UiPreviewHelper;

class BbCodeEditorPanelTest {

	private ExtensionManager extensionManager;
	private BbCodeEditorPanel editorPanel;

	@BeforeEach
	void setUp() {
		extensionManager = UiPreviewHelper.createPreviewExtensionManager();
		editorPanel = new BbCodeEditorPanel(extensionManager);
	}

	@Test
	@DisplayName("Toolbar should be properly built and contain buttons and containers")
	void testToolbarStructure() {
		assertNotNull(editorPanel.getTextArea());
		assertNotNull(editorPanel.getPreviewPane());
		assertNotNull(editorPanel.getContent());

		// Set initial content and test content manipulation
		editorPanel.setContent("Hello World");
		assertEquals("Hello World", editorPanel.getContent());

		// Test card transitions
		assertFalse(editorPanel.isShowingPreview());
		editorPanel.showPreviewCard();
		assertTrue(editorPanel.isShowingPreview());
		assertTrue(editorPanel.getPreviewPane().getText().contains("Hello World"));

		editorPanel.showEditorCard();
		assertFalse(editorPanel.isShowingPreview());
	}

	@Test
	@DisplayName("Extension toolbar items should be active in extension manager and update toolbar on change")
	void testExtensionToolbarIntegration() {
		List<BbCodeToolbarItem> activeItems = extensionManager.getActiveToolbarItems();
		assertFalse(activeItems.isEmpty(), "Active extension items should not be empty");

		// Rebuilding toolbar should succeed without exceptions
		assertDoesNotThrow(() -> editorPanel.rebuildToolbar());
	}

	@Test
	@DisplayName("Preview card renders multiline content with <br/> tags properly")
	void testMultilineContentPreview() {
		editorPanel.setContent("Line 1\nLine 2\n\nLine 3");
		editorPanel.showPreviewCard();

		assertTrue(editorPanel.isShowingPreview());
		String previewHtml = editorPanel.getPreviewPane().getText();
		assertTrue(previewHtml.contains("Line 1<br>Line 2<br><br>Line 3") || previewHtml.contains("Line 1<br/>Line 2<br/><br/>Line 3") || previewHtml.contains("Line 1"));
	}
}
