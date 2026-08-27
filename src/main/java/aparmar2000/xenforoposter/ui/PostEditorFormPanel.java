package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.JobPriority;
import aparmar2000.xenforoposter.model.ScheduledJob;
import aparmar2000.xenforoposter.scheduler.SchedulerEngine;
import lombok.AccessLevel;
import lombok.Getter;

public class PostEditorFormPanel extends JPanel {
	private static final long serialVersionUID = -4342831032879819425L;

	private final SchedulerEngine schedulerEngine;

	@Getter(AccessLevel.PACKAGE) private final JTextField jobNameField;
	@Getter(AccessLevel.PACKAGE) private final JComboBox<ProfileItem> profileComboBox;
	@Getter(AccessLevel.PACKAGE) private final JTextField threadUrlField;
	@Getter(AccessLevel.PACKAGE) private final JComboBox<JobPriority> priorityComboBox;
	@Getter(AccessLevel.PACKAGE) private final JSpinner pollIntervalSpinner;

	private final JTabbedPane tabbedPane;
	private final ConditionBuilderPanel conditionBuilder;
	private final BbCodeEditorPanel bbCodeEditor;

	@Getter(AccessLevel.PACKAGE) private final JButton cancelBtn;
	@Getter(AccessLevel.PACKAGE) private final JButton saveDraftBtn;
	@Getter(AccessLevel.PACKAGE) private final JButton scheduleBtn;

	@Getter(AccessLevel.PACKAGE) private String editingJobId = null;
	private PostEditorListener editorListener;

	public interface PostEditorListener {
		void onPostScheduled(@NotNull ScheduledJob job);
		default void onPostDraftSaved(@NotNull ScheduledJob job) {}
		default void onEditorCancelled() {}
	}

	/**
	 * Design-time only constructor for Eclipse WindowBuilder preview.
	 * @wbp.parser.constructor
	 * @deprecated For WindowBuilder GUI designer and preview use only.
	 */
	@Deprecated
	@ApiStatus.Internal
	public PostEditorFormPanel() {
		this(UiPreviewHelper.createPreviewSchedulerEngine(), UiPreviewHelper.createPreviewExtensionManager(), UiPreviewHelper.createPreviewBbCodePreviewRenderer());
	}

	public PostEditorFormPanel(@NotNull SchedulerEngine schedulerEngine, @NotNull ExtensionManager extensionManager) {
		this(schedulerEngine, extensionManager, UiPreviewHelper.createPreviewBbCodePreviewRenderer());
	}

	public PostEditorFormPanel(@NotNull SchedulerEngine schedulerEngine, @NotNull ExtensionManager extensionManager, @NotNull aparmar2000.xenforoposter.syntax.bbcode.BbCodePreviewRenderer previewRenderer) {
		this.schedulerEngine = schedulerEngine;
		setLayout(new BorderLayout(8, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		// Top Metadata Config Form
		JPanel configPanel = new JPanel(new GridBagLayout());
		configPanel.setBorder(BorderFactory.createTitledBorder("Target & Schedule Settings"));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 6, 4, 6);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Row 0: Job Name & Forum Profile
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
		configPanel.add(new JLabel("Job Name:"), gbc);

		gbc.gridx = 1; gbc.weightx = 0.4;
		jobNameField = new JTextField("Scheduled Post #" + (schedulerEngine.getJobs().size() + 1));
		configPanel.add(jobNameField, gbc);

		gbc.gridx = 2; gbc.weightx = 0.1;
		configPanel.add(new JLabel("Forum Account:"), gbc);

		gbc.gridx = 3; gbc.weightx = 0.4;
		profileComboBox = new JComboBox<>();
		refreshProfileDropdown();
		configPanel.add(profileComboBox, gbc);

		// Row 1: Thread URL & Priority
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1;
		configPanel.add(new JLabel("Thread URL:"), gbc);

		gbc.gridx = 1; gbc.weightx = 0.4;
		threadUrlField = new JTextField("https://forum.example.com/threads/my-thread.12345/");
		configPanel.add(threadUrlField, gbc);

		gbc.gridx = 2; gbc.weightx = 0.1;
		configPanel.add(new JLabel("Priority & Poll Interval:"), gbc);

		gbc.gridx = 3; gbc.weightx = 0.4;
		JPanel prioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		priorityComboBox = new JComboBox<>(JobPriority.values());
		priorityComboBox.setSelectedItem(JobPriority.NORMAL);
		prioPanel.add(priorityComboBox);

		prioPanel.add(new JLabel("Min Poll (s):"));
		pollIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 30, 3600, 5));
		prioPanel.add(pollIntervalSpinner);

		configPanel.add(prioPanel, gbc);

		// First Tab: Scheduling & Metadata (combines target config and condition builder)
		conditionBuilder = new ConditionBuilderPanel(extensionManager);
		JPanel metadataTabPanel = new JPanel(new BorderLayout(8, 8));
		metadataTabPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		metadataTabPanel.add(configPanel, BorderLayout.NORTH);
		metadataTabPanel.add(conditionBuilder, BorderLayout.CENTER);

		// Second Tab: Post Editor & Preview (CardLayout based)
		bbCodeEditor = new BbCodeEditorPanel(extensionManager, previewRenderer);

		// Bottom Tabbed Pane
		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		tabbedPane.addTab("Scheduling & Metadata", metadataTabPanel);
		tabbedPane.addTab("Post Editor", bbCodeEditor);

		// Bottom Action Bar
		JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(e -> {
			resetForm();
			if (editorListener != null) {
				editorListener.onEditorCancelled();
			}
		});
		actionBar.add(cancelBtn);

		saveDraftBtn = new JButton("Save as Draft");
		saveDraftBtn.addActionListener(e -> saveJob(ScheduledJob.JobStatus.DRAFT));
		actionBar.add(saveDraftBtn);

		scheduleBtn = new JButton("Schedule Post");
		scheduleBtn.setFont(scheduleBtn.getFont().deriveFont(Font.BOLD));
		scheduleBtn.addActionListener(e -> saveJob(ScheduledJob.JobStatus.SCHEDULED));
		actionBar.add(scheduleBtn);

		add(tabbedPane, BorderLayout.CENTER);
		add(actionBar, BorderLayout.SOUTH);

		schedulerEngine.addListener(new SchedulerEngine.JobUpdateListener() {
			@Override
			public void onJobsChanged() {
				SwingUtilities.invokeLater(PostEditorFormPanel.this::refreshProfileDropdown);
			}

			@Override
			public void onJobUpdated(@NotNull ScheduledJob job) {}
		});
	}

	public void setEditorListener(PostEditorListener editorListener) {
		this.editorListener = editorListener;
	}

	public void setListener(PostEditorListener editorListener) {
		this.editorListener = editorListener;
	}

	public void refreshProfileDropdown() {
		ProfileItem selected = (ProfileItem) profileComboBox.getSelectedItem();
		String selectedId = selected != null ? selected.profile.getId() : null;

		profileComboBox.removeAllItems();
		List<ForumProfile> profiles = new ArrayList<>(schedulerEngine.getProfiles());

		for (ForumProfile p : profiles) {
			ProfileItem item = new ProfileItem(p);
			profileComboBox.addItem(item);
			if (p.getId().equals(selectedId)) {
				profileComboBox.setSelectedItem(item);
			}
		}
	}

	public void loadJobForEditing(@NotNull ScheduledJob job) {
		if (job.getStatus() == ScheduledJob.JobStatus.COMPLETED) {
			JOptionPane.showMessageDialog(this, "Completed posts cannot be edited.", "Cannot Edit Post", JOptionPane.WARNING_MESSAGE);
			return;
		}

		this.editingJobId = job.getId();
		jobNameField.setText(job.getName());
		threadUrlField.setText(job.getThreadUrl());
		priorityComboBox.setSelectedItem(job.getPriority());
		pollIntervalSpinner.setValue(Math.max(30, job.getCustomPollIntervalSeconds()));

		for (int i = 0; i < profileComboBox.getItemCount(); i++) {
			ProfileItem item = profileComboBox.getItemAt(i);
			if (item.profile.getId().equals(job.getForumProfileId())) {
				profileComboBox.setSelectedIndex(i);
				break;
			}
		}

		conditionBuilder.setConditions(job.getConditions());
		bbCodeEditor.setContent(job.getBbCodeContent());
	}

	public void resetForm() {
		this.editingJobId = null;
		jobNameField.setText("Scheduled Post #" + (schedulerEngine.getJobs().size() + 1));
		threadUrlField.setText("");
		priorityComboBox.setSelectedItem(JobPriority.NORMAL);
		pollIntervalSpinner.setValue(30);
		conditionBuilder.resetToDefaults();
		bbCodeEditor.setContent("");
	}

	private void saveJob(ScheduledJob.JobStatus targetStatus) {
		String name = jobNameField.getText().trim();
		String threadUrl = threadUrlField.getText().trim();
		ProfileItem profileItem = (ProfileItem) profileComboBox.getSelectedItem();
		String bbCode = bbCodeEditor.getContent().trim();

		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter a Job Name.", "Validation Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (profileItem == null) {
			JOptionPane.showMessageDialog(this, "Please select or add a Forum Account first.", "Validation Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (targetStatus == ScheduledJob.JobStatus.SCHEDULED) {
			if (threadUrl.isEmpty() || !threadUrl.toLowerCase().startsWith("http")) {
				JOptionPane.showMessageDialog(this, "Please enter a valid Thread URL (e.g. https://forum.example.com/threads/12345/).", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if (bbCode.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Post BBCode content cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		JobPriority priority = (JobPriority) priorityComboBox.getSelectedItem();
		int interval = (Integer) pollIntervalSpinner.getValue();

		ScheduledJob.ScheduledJobBuilder builder = ScheduledJob.builder();
		if (editingJobId != null) {
			ScheduledJob existing = schedulerEngine.getJobs().stream()
					.filter(j -> j.getId().equals(editingJobId))
					.findFirst()
					.orElse(null);
			if (existing != null) {
				builder = existing.toBuilder();
			}
		}

		ScheduledJob job = builder
				.name(name)
				.forumProfileId(profileItem.profile.getId())
				.threadUrl(threadUrl)
				.bbCodeContent(bbCode)
				.priority(priority != null ? priority : JobPriority.NORMAL)
				.customPollIntervalSeconds(interval)
				.conditions(conditionBuilder.getConditions())
				.status(targetStatus)
				.currentStatusReason(targetStatus == ScheduledJob.JobStatus.DRAFT ? "Draft saved" : "Scheduled, awaiting evaluation")
				.createdAt(Instant.now())
				.build();

		schedulerEngine.addOrUpdateJob(job);

		if (targetStatus == ScheduledJob.JobStatus.SCHEDULED) {
			JOptionPane.showMessageDialog(this, "Job successfully Scheduled!", "Success", JOptionPane.INFORMATION_MESSAGE);
			resetForm();
			if (editorListener != null) {
				editorListener.onPostScheduled(job);
			}
		} else {
			this.editingJobId = job.getId();
			JOptionPane.showMessageDialog(this, "Job successfully saved as Draft!", "Success", JOptionPane.INFORMATION_MESSAGE);
			if (editorListener != null) {
				editorListener.onPostDraftSaved(job);
			}
		}
	}

	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}

	public ConditionBuilderPanel getConditionBuilder() {
		return conditionBuilder;
	}

	public BbCodeEditorPanel getBbCodeEditor() {
		return bbCodeEditor;
	}

	public void switchToMetadataTab() {
		tabbedPane.setSelectedIndex(0);
	}

	public void switchToEditorTab() {
		tabbedPane.setSelectedIndex(1);
	}

	private static class ProfileItem {
		final ForumProfile profile;

		ProfileItem(ForumProfile profile) {
			this.profile = profile;
		}

		@Override
		public String toString() {
			return profile.getName() + " (" + profile.getBaseUrl() + ")";
		}
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		UiPreviewHelper.showPreviewFrame(new PostEditorFormPanel(), "Post Composer & Scheduler", 1000, 700);
	}
}
