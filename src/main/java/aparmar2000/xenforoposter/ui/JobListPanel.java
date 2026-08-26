package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.model.JobPriority;
import aparmar2000.xenforoposter.model.ScheduledJob;
import aparmar2000.xenforoposter.model.conditions.PostCondition;
import aparmar2000.xenforoposter.scheduler.SchedulerEngine;
import lombok.AccessLevel;
import lombok.Getter;

public class JobListPanel extends JPanel {
	private static final long serialVersionUID = -7726194559699546924L;

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	private final SchedulerEngine schedulerEngine;
	@Getter(AccessLevel.PACKAGE) private final JTable jobTable;
	private final DefaultTableModel tableModel;
	private final JTextArea conditionSummaryArea;
	private final PollLogPanel pollLogPanel;

	private final List<ScheduledJob> currentJobs = new ArrayList<>();
	private final JobEditCallback editCallback;

	@Getter(AccessLevel.PACKAGE) private final JButton newPostBtn;
	@Getter(AccessLevel.PACKAGE) private final JButton copyPostBtn;
	@Getter(AccessLevel.PACKAGE) private final JButton editBtn;
	@Getter(AccessLevel.PACKAGE) private final JButton pauseBtn;
	@Getter(AccessLevel.PACKAGE) private final JButton triggerNowBtn;
	@Getter(AccessLevel.PACKAGE) private final JButton deleteBtn;

	public interface JobEditCallback {
		void onEditJob(@NotNull ScheduledJob job);
	}

	/**
	 * Design-time only constructor for Eclipse WindowBuilder preview.
	 * @wbp.parser.constructor
	 * @deprecated For WindowBuilder GUI designer and preview use only.
	 */
	@Deprecated
	@ApiStatus.Internal
	public JobListPanel() {
		this(UiPreviewHelper.createPreviewSchedulerEngine(), job -> {});
	}

	public JobListPanel(@NotNull SchedulerEngine schedulerEngine, @NotNull JobEditCallback editCallback) {
		this.schedulerEngine = schedulerEngine;
		this.editCallback = editCallback;
		setLayout(new BorderLayout(5, 5));

		// Main Table
		String[] cols = {"Name", "Priority", "Status", "Last Poll Time", "Waiting / Execution Reason", "Target Thread"};
		tableModel = new DefaultTableModel(cols, 0) {
			@Override
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};

		jobTable = new JTable(tableModel);
		jobTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jobTable.setRowHeight(24);

		// Top Action Bar
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));

		newPostBtn = new JButton("New Post");
		newPostBtn.addActionListener(e -> createNewPost());
		toolbar.add(newPostBtn);

		copyPostBtn = new JButton("Copy Post");
		copyPostBtn.addActionListener(e -> copySelectedPost());
		toolbar.add(copyPostBtn);

		editBtn = new JButton("Edit Post");
		editBtn.addActionListener(e -> {
			int row = jobTable.getSelectedRow();
			if (row >= 0 && row < currentJobs.size()) {
				ScheduledJob job = currentJobs.get(row);
				if (job.getStatus() != ScheduledJob.JobStatus.COMPLETED) {
					this.editCallback.onEditJob(job);
				}
			}
		});
		toolbar.add(editBtn);

		pauseBtn = new JButton("Pause / Resume");
		pauseBtn.addActionListener(e -> togglePauseSelectedJob());
		toolbar.add(pauseBtn);

		triggerNowBtn = new JButton("Evaluate Now");
		triggerNowBtn.addActionListener(e -> {
			int row = jobTable.getSelectedRow();
			if (row >= 0 && row < currentJobs.size()) {
				ScheduledJob job = currentJobs.get(row);
				new Thread(() -> schedulerEngine.evaluateSingleJob(job)).start();
			} else {
				new Thread(schedulerEngine::evaluateAllJobs).start();
			}
		});
		toolbar.add(triggerNowBtn);

		deleteBtn = new JButton("Delete");
		deleteBtn.addActionListener(e -> {
			int row = jobTable.getSelectedRow();
			if (row >= 0 && row < currentJobs.size()) {
				ScheduledJob job = currentJobs.get(row);
				int confirm = JOptionPane.showConfirmDialog(this,
						"Delete job '" + job.getName() + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION) {
					schedulerEngine.deleteJob(job.getId());
					refreshTable();
				}
			}
		});
		toolbar.add(deleteBtn);

		// Color status column
		jobTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				String val = String.valueOf(value);
				if (val.contains("Completed")) {
					label.setForeground(new Color(40, 167, 69));
				} else if (val.contains("Failed")) {
					label.setForeground(new Color(220, 53, 69));
				} else if (val.contains("Waiting") || val.contains("Queued")) {
					label.setForeground(new Color(255, 193, 7));
				} else {
					label.setForeground(UIManager.getColor("Label.foreground"));
				}
				return label;
			}
		});

		// Detail Bottom Panel: Split Condition summary & PollLogPanel
		conditionSummaryArea = new JTextArea(6, 25);
		conditionSummaryArea.setEditable(false);
		conditionSummaryArea.setLineWrap(true);
		conditionSummaryArea.setWrapStyleWord(true);
		JScrollPane conditionScroll = new JScrollPane(conditionSummaryArea);
		conditionScroll.setBorder(BorderFactory.createTitledBorder("Configured Conditions"));

		pollLogPanel = new PollLogPanel();

		JSplitPane bottomDetailSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, conditionScroll, pollLogPanel);
		bottomDetailSplit.setResizeWeight(0.35);

		JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(jobTable), bottomDetailSplit);
		mainSplit.setResizeWeight(0.6);

		add(toolbar, BorderLayout.NORTH);
		add(mainSplit, BorderLayout.CENTER);

		jobTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				updateSelectedJobDetails();
			}
		});

		schedulerEngine.addListener(new SchedulerEngine.JobUpdateListener() {
			@Override
			public void onJobsChanged() {
				SwingUtilities.invokeLater(JobListPanel.this::refreshTable);
			}

			@Override
			public void onJobUpdated(@NotNull ScheduledJob job) {
				SwingUtilities.invokeLater(JobListPanel.this::refreshTable);
			}
		});

		refreshTable();
	}

	public void refreshTable() {
		int selectedRow = jobTable.getSelectedRow();
		String selectedJobId = (selectedRow >= 0 && selectedRow < currentJobs.size()) ? currentJobs.get(selectedRow).getId() : null;

		currentJobs.clear();
		currentJobs.addAll(schedulerEngine.getJobs());
		tableModel.setRowCount(0);

		int newSelectIndex = -1;
		for (int i = 0; i < currentJobs.size(); i++) {
			ScheduledJob job = currentJobs.get(i);
			if (job.getId().equals(selectedJobId)) {
				newSelectIndex = i;
			}

			String pollTime = job.getLastPollTime() != null ? FORMATTER.format(job.getLastPollTime()) : "(Never)";
			tableModel.addRow(new Object[]{
					job.getName(),
					job.getPriority().getDisplayName(),
					job.getStatus().getDisplayName(),
					pollTime,
					job.getCurrentStatusReason(),
					job.getThreadUrl()
			});
		}

		if (newSelectIndex >= 0) {
			jobTable.setRowSelectionInterval(newSelectIndex, newSelectIndex);
		} else if (!currentJobs.isEmpty() && selectedRow < 0) {
			jobTable.setRowSelectionInterval(0, 0);
		} else {
			updateSelectedJobDetails();
		}
	}

	private void updateSelectedJobDetails() {
		int row = jobTable.getSelectedRow();
		if (row >= 0 && row < currentJobs.size()) {
			ScheduledJob job = currentJobs.get(row);
			copyPostBtn.setEnabled(true);
			deleteBtn.setEnabled(true);

			boolean isCompleted = (job.getStatus() == ScheduledJob.JobStatus.COMPLETED);
			editBtn.setEnabled(!isCompleted);
			pauseBtn.setEnabled(!isCompleted);

			pollLogPanel.displayJobHistory(job);

			StringBuilder sb = new StringBuilder();
			sb.append("Job: ").append(job.getName()).append("\n");
			sb.append("Priority: ").append(job.getPriority()).append("\n");
			sb.append("Custom Poll Interval: ").append(job.getCustomPollIntervalSeconds() > 0 ? job.getCustomPollIntervalSeconds() + "s" : "Default (30s)").append("\n\n");
			sb.append("Conditions:\n");

			if (job.getConditions().isEmpty()) {
				sb.append(" • (No conditions)\n");
			} else {
				for (PostCondition c : job.getConditions()) {
					sb.append(" • [").append(c.getType()).append("] ").append(c.getDisplayName()).append(":\n   ").append(c.getDescription()).append("\n");
				}
			}
			conditionSummaryArea.setText(sb.toString());
			conditionSummaryArea.setCaretPosition(0);
		} else {
			copyPostBtn.setEnabled(false);
			editBtn.setEnabled(false);
			pauseBtn.setEnabled(false);
			deleteBtn.setEnabled(false);
			pollLogPanel.displayJobHistory(null);
			conditionSummaryArea.setText("Select a scheduled job above to view condition details and poll logs.");
		}
	}

	private void createNewPost() {
		String defaultProfileId = schedulerEngine.getProfiles().isEmpty() ? "" : schedulerEngine.getProfiles().iterator().next().getId();
		ScheduledJob newDraft = ScheduledJob.builder()
				.id(UUID.randomUUID().toString())
				.name("Scheduled Post #" + (schedulerEngine.getJobs().size() + 1))
				.forumProfileId(defaultProfileId)
				.threadUrl("")
				.bbCodeContent("")
				.priority(JobPriority.NORMAL)
				.customPollIntervalSeconds(30)
				.conditions(ScheduledJob.defaultConditions())
				.status(ScheduledJob.JobStatus.DRAFT)
				.currentStatusReason("Draft created")
				.createdAt(Instant.now())
				.build();

		schedulerEngine.addOrUpdateJob(newDraft);
		refreshTable();
		this.editCallback.onEditJob(newDraft);
	}

	private void copySelectedPost() {
		int row = jobTable.getSelectedRow();
		if (row >= 0 && row < currentJobs.size()) {
			ScheduledJob original = currentJobs.get(row);
			ScheduledJob copyDraft = original.toBuilder()
					.id(UUID.randomUUID().toString())
					.name(original.getName() + " (Copy)")
					.status(ScheduledJob.JobStatus.DRAFT)
					.currentStatusReason("Draft created from " + original.getName())
					.createdAt(Instant.now())
					.completedAt(null)
					.lastPollTime(null)
					.lastPollStatus(null)
					.pollHistory(Collections.emptyList())
					.build();

			schedulerEngine.addOrUpdateJob(copyDraft);
			refreshTable();
			this.editCallback.onEditJob(copyDraft);
		}
	}

	private void togglePauseSelectedJob() {
		int row = jobTable.getSelectedRow();
		if (row >= 0 && row < currentJobs.size()) {
			ScheduledJob job = currentJobs.get(row);
			if (job.getStatus() == ScheduledJob.JobStatus.COMPLETED) {
				return;
			}
			ScheduledJob.JobStatus newStatus = (job.getStatus() == ScheduledJob.JobStatus.PAUSED)
					? ScheduledJob.JobStatus.SCHEDULED
							: ScheduledJob.JobStatus.PAUSED;

			ScheduledJob updated = job.toBuilder()
					.status(newStatus)
					.currentStatusReason(newStatus == ScheduledJob.JobStatus.PAUSED ? "Job paused by user" : "Job resumed")
					.build();

			schedulerEngine.addOrUpdateJob(updated);
			refreshTable();
		}
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		UiPreviewHelper.showPreviewFrame(new JobListPanel(), "Scheduled Jobs & Diagnostics", 950, 600);
	}
}
