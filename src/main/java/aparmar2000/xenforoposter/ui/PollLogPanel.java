package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.beans.Beans;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import aparmar2000.xenforoposter.model.PollRecord;
import aparmar2000.xenforoposter.model.ScheduledJob;

public class PollLogPanel extends JPanel {
	private static final long serialVersionUID = 6955461402546029424L;

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	private final JTable logTable;
	private final DefaultTableModel tableModel;
	private final JTextArea detailsArea;
	private ScheduledJob currentJob;

	/**
	 * Constructor for PollLogPanel.
	 * @wbp.parser.constructor
	 */
	public PollLogPanel() {
		setLayout(new BorderLayout(5, 5));
		setBorder(BorderFactory.createTitledBorder("Poll & Evaluation History"));

		String[] columns = {"Timestamp", "Type", "Status", "Summary"};
		tableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		logTable = new JTable(tableModel);
		logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		logTable.setRowHeight(22);

		// Render status with colors
		logTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				String val = String.valueOf(value);
				if ("PASSED".equals(val) || "SUCCESS".equals(val)) {
					label.setForeground(new Color(40, 167, 69));
				} else {
					label.setForeground(new Color(220, 53, 69));
				}
				return label;
			}
		});

		detailsArea = new JTextArea(4, 30);
		detailsArea.setEditable(false);
		detailsArea.setLineWrap(true);
		detailsArea.setWrapStyleWord(true);
		detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		logTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				int row = logTable.getSelectedRow();
				if (row >= 0 && currentJob != null && row < currentJob.getPollHistory().size()) {
					PollRecord record = currentJob.getPollHistory().get(currentJob.getPollHistory().size() - 1 - row);
					String details = record.getDetails();
					detailsArea.setText(details != null && !details.isEmpty() ? details : record.getSummary());
				} else {
					detailsArea.setText("");
				}
			}
		});

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				new JScrollPane(logTable),
				new JScrollPane(detailsArea));
		splitPane.setResizeWeight(0.7);

		add(splitPane, BorderLayout.CENTER);

		if (Beans.isDesignTime()) {
			displayJobHistory(UiPreviewHelper.createPreviewSampleJob());
		}
	}

	public void displayJobHistory(ScheduledJob job) {
		this.currentJob = job;
		tableModel.setRowCount(0);
		detailsArea.setText("");

		if (job == null || job.getPollHistory().isEmpty()) {
			return;
		}

		List<PollRecord> history = job.getPollHistory();
		// Display newest first
		for (int i = history.size() - 1; i >= 0; i--) {
			PollRecord record = history.get(i);
			tableModel.addRow(new Object[]{
					FORMATTER.format(record.getTimestamp()),
					record.getPollType().name(),
					record.isSuccess() ? "PASSED" : "FAILED",
							record.getSummary()
			});
		}

		if (logTable.getRowCount() > 0) {
			logTable.setRowSelectionInterval(0, 0);
		}
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		PollLogPanel panel = new PollLogPanel();
		panel.displayJobHistory(UiPreviewHelper.createPreviewSampleJob());
		UiPreviewHelper.showPreviewFrame(panel, "Poll Log Panel", 600, 350);
	}
}
