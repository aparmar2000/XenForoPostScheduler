package aparmar2000.xenforoposter.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.GraphicsEnvironment;
import java.time.Instant;
import java.util.UUID;

import javax.swing.JTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.model.ScheduledJob;
import aparmar2000.xenforoposter.scheduler.SchedulerEngine;
import aparmar2000.xenforoposter.settings.GeneralSettings;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodePreviewRenderer;
import aparmar2000.xenforoposter.web.XenForoWebClient;

class PostComposerFlowTest {

	private SchedulerEngine engine;
	private ExtensionManager extensionManager;
	private XenForoWebClient webClient;
	private GeneralSettings generalSettings;
	private BbCodePreviewRenderer previewRenderer;

	@BeforeEach
	void setUp() {
		engine = UiPreviewHelper.createPreviewSchedulerEngine();
		extensionManager = UiPreviewHelper.createPreviewExtensionManager();
		webClient = UiPreviewHelper.createPreviewWebClient();
		generalSettings = UiPreviewHelper.createPreviewGeneralSettings();
		previewRenderer = UiPreviewHelper.createPreviewBbCodePreviewRenderer();
	}

	@Test
	@DisplayName("Post composer tab starts disabled on MainFrame initialization")
	void testComposerTabStartsDisabled() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}

		MainFrame mainFrame = new MainFrame(engine, extensionManager, webClient, generalSettings, previewRenderer);
		try {
			assertFalse(mainFrame.getTabbedPane().isEnabledAt(1), "Post composer tab should start disabled");
			assertEquals(0, mainFrame.getTabbedPane().getSelectedIndex(), "Job list tab should be initially selected");
		} finally {
			mainFrame.dispose();
		}
	}

	@Test
	@DisplayName("Clicking New Post creates and persists draft, enables composer tab, and switches to it")
	void testNewPostCreatesDraftAndOpensComposer() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}

		MainFrame mainFrame = new MainFrame(engine, extensionManager, webClient, generalSettings, previewRenderer);
		try {
			JobListPanel jobListPanel = mainFrame.getJobListPanel();
			PostEditorFormPanel postEditorPanel = mainFrame.getPostEditorPanel();

			int initialJobCount = engine.getJobs().size();

			jobListPanel.getNewPostBtn().doClick();

			assertEquals(initialJobCount + 1, engine.getJobs().size(), "A new draft job should be created in the engine");
			assertTrue(mainFrame.getTabbedPane().isEnabledAt(1), "Composer tab should now be enabled");
			assertEquals(1, mainFrame.getTabbedPane().getSelectedIndex(), "Should have switched to Post Composer tab");
			assertNotNull(postEditorPanel.getEditingJobId(), "Post editor should have an active editing job ID");

			ScheduledJob createdDraft = engine.getJobs().stream()
					.filter(j -> j.getId().equals(postEditorPanel.getEditingJobId()))
					.findFirst()
					.orElse(null);
			assertNotNull(createdDraft);
			assertEquals(ScheduledJob.JobStatus.DRAFT, createdDraft.getStatus(), "New post should have DRAFT status");
		} finally {
			mainFrame.dispose();
		}
	}

	@Test
	@DisplayName("Clicking Copy Post creates draft copy, enables composer tab, and switches to it")
	void testCopyPostCreatesDraftAndOpensComposer() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}

		MainFrame mainFrame = new MainFrame(engine, extensionManager, webClient, generalSettings, previewRenderer);
		try {
			JobListPanel jobListPanel = mainFrame.getJobListPanel();
			PostEditorFormPanel postEditorPanel = mainFrame.getPostEditorPanel();

			JTable table = jobListPanel.getJobTable();
			table.setRowSelectionInterval(0, 0);

			ScheduledJob selectedOriginal = engine.getJobs().get(0);
			int initialJobCount = engine.getJobs().size();

			jobListPanel.getCopyPostBtn().doClick();

			assertEquals(initialJobCount + 1, engine.getJobs().size(), "A copied draft job should be created");
			assertTrue(mainFrame.getTabbedPane().isEnabledAt(1), "Composer tab should now be enabled");
			assertEquals(1, mainFrame.getTabbedPane().getSelectedIndex(), "Should switch to Post Composer tab");

			String newEditingId = postEditorPanel.getEditingJobId();
			assertNotNull(newEditingId);
			assertFalse(newEditingId.equals(selectedOriginal.getId()), "Copied job ID should be distinct from original");

			ScheduledJob copiedJob = engine.getJobs().stream()
					.filter(j -> j.getId().equals(newEditingId))
					.findFirst()
					.orElse(null);
			assertNotNull(copiedJob);
			assertEquals(ScheduledJob.JobStatus.DRAFT, copiedJob.getStatus(), "Copied job should have DRAFT status");
			assertTrue(copiedJob.getName().contains("(Copy)"), "Copied job name should indicate copy");
			assertEquals(selectedOriginal.getBbCodeContent(), copiedJob.getBbCodeContent(), "BBCode content should match original");
		} finally {
			mainFrame.dispose();
		}
	}

	@Test
	@DisplayName("Completed posts cannot be edited, and Edit button is disabled when selected")
	void testCompletedPostsNotEditable() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}

		ScheduledJob completedJob = ScheduledJob.builder()
				.id(UUID.randomUUID().toString())
				.name("Completed Post Execution")
				.forumProfileId("prof-1")
				.threadUrl("https://community.example.com/threads/completed.999/")
				.bbCodeContent("Completed post content")
				.status(ScheduledJob.JobStatus.COMPLETED)
				.completedAt(Instant.now())
				.build();

		engine.addOrUpdateJob(completedJob);

		MainFrame mainFrame = new MainFrame(engine, extensionManager, webClient, generalSettings, previewRenderer);
		try {
			JobListPanel jobListPanel = mainFrame.getJobListPanel();
			jobListPanel.refreshTable();

			JTable table = jobListPanel.getJobTable();
			int completedRow = -1;
			for (int i = 0; i < table.getRowCount(); i++) {
				if (table.getValueAt(i, 0).equals("Completed Post Execution")) {
					completedRow = i;
					break;
				}
			}

			assertTrue(completedRow >= 0, "Completed job must be found in table");
			table.setRowSelectionInterval(completedRow, completedRow);

			assertFalse(jobListPanel.getEditBtn().isEnabled(), "Edit button must be disabled for completed posts");
			assertFalse(jobListPanel.getPauseBtn().isEnabled(), "Pause button must be disabled for completed posts");
			assertTrue(jobListPanel.getCopyPostBtn().isEnabled(), "Copy Post button must be enabled for completed posts");

			// Copy Post on completed job creates editable draft
			jobListPanel.getCopyPostBtn().doClick();
			assertTrue(mainFrame.getTabbedPane().isEnabledAt(1), "Copying completed post enables composer");
			assertEquals(1, mainFrame.getTabbedPane().getSelectedIndex());
		} finally {
			mainFrame.dispose();
		}
	}

	@Test
	@DisplayName("Scheduling a post clears current editing job and switches back to job list tab with composer disabled")
	void testSchedulingClearsFormAndSelectsJobListTab() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}

		MainFrame mainFrame = new MainFrame(engine, extensionManager, webClient, generalSettings, previewRenderer);
		try {
			JobListPanel jobListPanel = mainFrame.getJobListPanel();
			PostEditorFormPanel postEditorPanel = mainFrame.getPostEditorPanel();

			jobListPanel.getNewPostBtn().doClick();
			assertTrue(mainFrame.getTabbedPane().isEnabledAt(1));
			assertEquals(1, mainFrame.getTabbedPane().getSelectedIndex());

			// Fill in required fields for scheduling
			postEditorPanel.getThreadUrlField().setText("https://community.example.com/threads/valid.12345/");
			postEditorPanel.getBbCodeEditor().setContent("[B]Hello world[/B]");

			postEditorPanel.getScheduleBtn().doClick();

			assertNull(postEditorPanel.getEditingJobId(), "Editing job ID should be cleared after scheduling");
			assertFalse(mainFrame.getTabbedPane().isEnabledAt(1), "Composer tab should be disabled after scheduling");
			assertEquals(0, mainFrame.getTabbedPane().getSelectedIndex(), "Job list tab should be selected after scheduling");
		} finally {
			mainFrame.dispose();
		}
	}

	@Test
	@DisplayName("Cancelling edit clears form, disables composer tab, and switches back to job list tab")
	void testCancelClearsFormAndSelectsJobListTab() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}

		MainFrame mainFrame = new MainFrame(engine, extensionManager, webClient, generalSettings, previewRenderer);
		try {
			JobListPanel jobListPanel = mainFrame.getJobListPanel();
			PostEditorFormPanel postEditorPanel = mainFrame.getPostEditorPanel();

			jobListPanel.getNewPostBtn().doClick();
			assertTrue(mainFrame.getTabbedPane().isEnabledAt(1));
			assertEquals(1, mainFrame.getTabbedPane().getSelectedIndex());

			postEditorPanel.getCancelBtn().doClick();

			assertNull(postEditorPanel.getEditingJobId(), "Editing job ID should be cleared after cancelling");
			assertFalse(mainFrame.getTabbedPane().isEnabledAt(1), "Composer tab should be disabled after cancelling");
			assertEquals(0, mainFrame.getTabbedPane().getSelectedIndex(), "Job list tab should be selected after cancelling");
		} finally {
			mainFrame.dispose();
		}
	}
}
