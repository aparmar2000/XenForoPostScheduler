package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.condition.ConditionProvider;
import aparmar2000.xenforoposter.model.conditions.AntiNecropostCondition;
import aparmar2000.xenforoposter.model.conditions.ConditionType;
import aparmar2000.xenforoposter.model.conditions.DateRangeCondition;
import aparmar2000.xenforoposter.model.conditions.DayOfWeekCondition;
import aparmar2000.xenforoposter.model.conditions.PostCondition;
import aparmar2000.xenforoposter.model.conditions.PostGapCondition;
import aparmar2000.xenforoposter.model.conditions.ThreadStatusCondition;
import aparmar2000.xenforoposter.model.conditions.TimeRangeCondition;
import aparmar2000.xenforoposter.ui.components.CalendarPickerPanel;

public class ConditionBuilderPanel extends JPanel {
	private static final long serialVersionUID = -7484395343109260674L;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final List<PostCondition> conditions = new ArrayList<>();
	private final JPanel conditionsListContainer;
	private final ExtensionManager extensionManager;

	/**
	 * Design-time only constructor for Eclipse WindowBuilder preview.
	 * @wbp.parser.constructor
	 * @deprecated For WindowBuilder GUI designer and preview use only.
	 */
	@Deprecated
	@ApiStatus.Internal
	public ConditionBuilderPanel() {
		this(UiPreviewHelper.createPreviewExtensionManager());
	}

	public ConditionBuilderPanel(@NotNull ExtensionManager extensionManager) {
		this.extensionManager = extensionManager;
		setLayout(new BorderLayout(5, 5));
		setBorder(BorderFactory.createTitledBorder("Post Execution Conditions"));

		// Container for condition cards
		conditionsListContainer = new JPanel();
		conditionsListContainer.setLayout(new BoxLayout(conditionsListContainer, BoxLayout.Y_AXIS));
		JScrollPane scrollPane = new JScrollPane(conditionsListContainer);
		scrollPane.setPreferredSize(new Dimension(480, 190));

		// Add condition buttons toolbar
		JPanel addBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

		JButton addDateRangeBtn = new JButton("+ Date Range");
		addDateRangeBtn.addActionListener(e -> promptAddDateRangeCondition());
		addBar.add(addDateRangeBtn);

		JButton addDayOfWeekBtn = new JButton("+ Day of Week");
		addDayOfWeekBtn.addActionListener(e -> promptAddDayOfWeekCondition());
		addBar.add(addDayOfWeekBtn);

		JButton addTimeRangeBtn = new JButton("+ Time Range");
		addTimeRangeBtn.addActionListener(e -> promptAddTimeRangeCondition());
		addBar.add(addTimeRangeBtn);

		JButton addGapBtn = new JButton("+ Post Gap Trigger");
		addGapBtn.addActionListener(e -> promptAddPostGapCondition());
		addBar.add(addGapBtn);

		JButton addNecroBtn = new JButton("+ Anti-Necropost");
		addNecroBtn.addActionListener(e -> promptAddAntiNecropostCondition());
		addBar.add(addNecroBtn);

		JButton addStatusBtn = new JButton("+ Thread Status");
		addStatusBtn.addActionListener(e -> {
			conditions.add(ThreadStatusCondition.builder().build());
			refreshConditionCards();
		});
		addBar.add(addStatusBtn);

		// Extension custom conditions menu
		JButton addExtCondBtn = new JButton("+ Custom Extension Condition");
		addExtCondBtn.addActionListener(e -> showExtensionConditionMenu(addExtCondBtn));
		addBar.add(addExtCondBtn);

		add(scrollPane, BorderLayout.CENTER);
		add(addBar, BorderLayout.SOUTH);

		// Default with 30-day anti-necropost
		resetToDefaults();
	}

	public void resetToDefaults() {
		conditions.clear();
		conditions.add(AntiNecropostCondition.builder().build());
		refreshConditionCards();
	}

	public void setConditions(@NotNull List<PostCondition> newConditions) {
		conditions.clear();
		conditions.addAll(newConditions);
		refreshConditionCards();
	}

	public List<PostCondition> getConditions() {
		return new ArrayList<>(conditions);
	}

	private void refreshConditionCards() {
		conditionsListContainer.removeAll();

		if (conditions.isEmpty()) {
			JPanel emptyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			emptyPanel.add(new JLabel("<html><i>No conditions configured. Post will execute immediately once rate limits allow.</i></html>"));
			conditionsListContainer.add(emptyPanel);
		} else {
			for (int i = 0; i < conditions.size(); i++) {
				PostCondition condition = conditions.get(i);
				final int idx = i;

				JPanel card = new JPanel(new BorderLayout(8, 4));
				card.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createEmptyBorder(2, 4, 2, 4),
						BorderFactory.createEtchedBorder()
						));

				String typeBadge = condition.getType() == ConditionType.LOCAL ? "[LOCAL]" : "[THREAD-DEPENDENT]";
				JLabel titleLabel = new JLabel("<html><b>" + condition.getDisplayName() + "</b> <font color='gray'>" + typeBadge + "</font></html>");
				JLabel descLabel = new JLabel("<html>" + condition.getDescription() + "</html>");

				JPanel infoPanel = new JPanel(new GridLayout(2, 1, 2, 2));
				infoPanel.add(titleLabel);
				infoPanel.add(descLabel);

				JButton removeBtn = new JButton("Remove");
				removeBtn.addActionListener(e -> {
					conditions.remove(idx);
					refreshConditionCards();
				});

				card.add(infoPanel, BorderLayout.CENTER);
				card.add(removeBtn, BorderLayout.EAST);

				conditionsListContainer.add(card);
			}
		}

		conditionsListContainer.revalidate();
		conditionsListContainer.repaint();
	}

	private void promptAddDateRangeCondition() {
		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		final LocalDate[] startHolder = new LocalDate[] { LocalDate.now() };
		final LocalDate[] endHolder = new LocalDate[] { null };

		JTextField startField = new JTextField(startHolder[0] != null ? startHolder[0].format(DATE_FORMATTER) : "", 10);
		startField.setEditable(false);
		JButton pickStartBtn = new JButton("Pick Calendar...");
		pickStartBtn.addActionListener(e -> {
			LocalDate picked = CalendarPickerPanel.showDialog(this, "Select Start Date", startHolder[0]);
			startHolder[0] = picked;
			startField.setText(picked != null ? picked.format(DATE_FORMATTER) : "");
		});
		JButton clearStartBtn = new JButton("Clear");
		clearStartBtn.addActionListener(e -> {
			startHolder[0] = null;
			startField.setText("");
		});

		JTextField endField = new JTextField(endHolder[0] != null ? endHolder[0].format(DATE_FORMATTER) : "", 10);
		endField.setEditable(false);
		JButton pickEndBtn = new JButton("Pick Calendar...");
		pickEndBtn.addActionListener(e -> {
			LocalDate picked = CalendarPickerPanel.showDialog(this, "Select End Date", endHolder[0]);
			endHolder[0] = picked;
			endField.setText(picked != null ? picked.format(DATE_FORMATTER) : "");
		});
		JButton clearEndBtn = new JButton("Clear");
		clearEndBtn.addActionListener(e -> {
			endHolder[0] = null;
			endField.setText("");
		});

		// Row 0: Start Date
		gbc.gridx = 0; gbc.gridy = 0;
		form.add(new JLabel("Start Date (optional):"), gbc);
		gbc.gridx = 1;
		form.add(startField, gbc);
		gbc.gridx = 2;
		form.add(pickStartBtn, gbc);
		gbc.gridx = 3;
		form.add(clearStartBtn, gbc);

		// Row 1: End Date
		gbc.gridx = 0; gbc.gridy = 1;
		form.add(new JLabel("End Date (optional):"), gbc);
		gbc.gridx = 1;
		form.add(endField, gbc);
		gbc.gridx = 2;
		form.add(pickEndBtn, gbc);
		gbc.gridx = 3;
		form.add(clearEndBtn, gbc);

		// Row 2: Presets
		JPanel presetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JButton todayOnlyBtn = new JButton("Today Only");
		todayOnlyBtn.addActionListener(e -> {
			LocalDate today = LocalDate.now();
			startHolder[0] = today;
			endHolder[0] = today;
			startField.setText(today.format(DATE_FORMATTER));
			endField.setText(today.format(DATE_FORMATTER));
		});
		JButton next7DaysBtn = new JButton("Next 7 Days");
		next7DaysBtn.addActionListener(e -> {
			LocalDate today = LocalDate.now();
			startHolder[0] = today;
			endHolder[0] = today.plusDays(7);
			startField.setText(today.format(DATE_FORMATTER));
			endField.setText(endHolder[0].format(DATE_FORMATTER));
		});
		JButton next30DaysBtn = new JButton("Next 30 Days");
		next30DaysBtn.addActionListener(e -> {
			LocalDate today = LocalDate.now();
			startHolder[0] = today;
			endHolder[0] = today.plusDays(30);
			startField.setText(today.format(DATE_FORMATTER));
			endField.setText(endHolder[0].format(DATE_FORMATTER));
		});

		presetsPanel.add(todayOnlyBtn);
		presetsPanel.add(next7DaysBtn);
		presetsPanel.add(next30DaysBtn);

		gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4;
		form.add(presetsPanel, gbc);

		int res = JOptionPane.showConfirmDialog(this, form, "Configure Date Range Condition", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (res == JOptionPane.OK_OPTION) {
			LocalDate start = startHolder[0];
			LocalDate end = endHolder[0];

			if (start == null && end == null) {
				JOptionPane.showMessageDialog(this, "Please select at least a start date or an end date.", "Invalid Date Range", JOptionPane.WARNING_MESSAGE);
				return;
			}

			if (start != null && end != null && start.isAfter(end)) {
				JOptionPane.showMessageDialog(this, "Start date cannot be after end date.", "Invalid Date Range", JOptionPane.ERROR_MESSAGE);
				return;
			}

			conditions.add(DateRangeCondition.builder()
					.startDate(start)
					.endDate(end)
					.build());
			refreshConditionCards();
		}
	}

	private void promptAddDayOfWeekCondition() {
		JPanel form = new JPanel(new BorderLayout(6, 6));

		JPanel checkboxesPanel = new JPanel(new GridLayout(2, 4, 4, 4));
		checkboxesPanel.setBorder(BorderFactory.createTitledBorder("Allowed Days of the Week"));

		Map<DayOfWeek, JCheckBox> dayBoxes = new LinkedHashMap<>();
		DayOfWeek[] days = DayOfWeek.values();
		for (DayOfWeek d : days) {
			JCheckBox box = new JCheckBox(d.getDisplayName(TextStyle.FULL, Locale.getDefault()), true);
			dayBoxes.put(d, box);
			checkboxesPanel.add(box);
		}

		JPanel presetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JButton allDaysBtn = new JButton("All Days");
		allDaysBtn.addActionListener(e -> dayBoxes.values().forEach(b -> b.setSelected(true)));

		JButton weekdaysBtn = new JButton("Weekdays (Mon-Fri)");
		weekdaysBtn.addActionListener(e -> dayBoxes.forEach((d, b) -> {
			b.setSelected(d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY);
		}));

		JButton weekendsBtn = new JButton("Weekends (Sat-Sun)");
		weekendsBtn.addActionListener(e -> dayBoxes.forEach((d, b) -> {
			b.setSelected(d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY);
		}));

		presetsPanel.add(allDaysBtn);
		presetsPanel.add(weekdaysBtn);
		presetsPanel.add(weekendsBtn);

		form.add(checkboxesPanel, BorderLayout.CENTER);
		form.add(presetsPanel, BorderLayout.SOUTH);

		int res = JOptionPane.showConfirmDialog(this, form, "Configure Day of Week Condition", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (res == JOptionPane.OK_OPTION) {
			Set<DayOfWeek> selected = EnumSet.noneOf(DayOfWeek.class);
			dayBoxes.forEach((d, b) -> {
				if (b.isSelected()) {
					selected.add(d);
				}
			});

			if (selected.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Please select at least one day of the week.", "No Days Selected", JOptionPane.WARNING_MESSAGE);
				return;
			}

			conditions.add(DayOfWeekCondition.builder()
					.allowedDays(selected)
					.build());
			refreshConditionCards();
		}
	}

	private void promptAddTimeRangeCondition() {
		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 9);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		Date startDefault = cal.getTime();

		cal.set(Calendar.HOUR_OF_DAY, 17);
		cal.set(Calendar.MINUTE, 0);
		Date endDefault = cal.getTime();

		JSpinner startSpinner = new JSpinner(new SpinnerDateModel(startDefault, null, null, Calendar.MINUTE));
		JSpinner.DateEditor startEditor = new JSpinner.DateEditor(startSpinner, "HH:mm");
		startSpinner.setEditor(startEditor);

		JSpinner endSpinner = new JSpinner(new SpinnerDateModel(endDefault, null, null, Calendar.MINUTE));
		JSpinner.DateEditor endEditor = new JSpinner.DateEditor(endSpinner, "HH:mm");
		endSpinner.setEditor(endEditor);

		gbc.gridx = 0; gbc.gridy = 0;
		form.add(new JLabel("Start Time (HH:mm):"), gbc);
		gbc.gridx = 1;
		form.add(startSpinner, gbc);

		gbc.gridx = 0; gbc.gridy = 1;
		form.add(new JLabel("End Time (HH:mm):"), gbc);
		gbc.gridx = 1;
		form.add(endSpinner, gbc);

		// Presets
		JPanel presetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JButton businessHoursBtn = new JButton("Business Hours (09:00 - 17:00)");
		businessHoursBtn.addActionListener(e -> {
			Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, 9); c.set(Calendar.MINUTE, 0);
			startSpinner.setValue(c.getTime());
			c.set(Calendar.HOUR_OF_DAY, 17); c.set(Calendar.MINUTE, 0);
			endSpinner.setValue(c.getTime());
		});

		JButton eveningBtn = new JButton("Evening (18:00 - 23:00)");
		eveningBtn.addActionListener(e -> {
			Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, 18); c.set(Calendar.MINUTE, 0);
			startSpinner.setValue(c.getTime());
			c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 0);
			endSpinner.setValue(c.getTime());
		});

		JButton overnightBtn = new JButton("Overnight (22:00 - 06:00)");
		overnightBtn.addActionListener(e -> {
			Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, 22); c.set(Calendar.MINUTE, 0);
			startSpinner.setValue(c.getTime());
			c.set(Calendar.HOUR_OF_DAY, 6); c.set(Calendar.MINUTE, 0);
			endSpinner.setValue(c.getTime());
		});

		presetsPanel.add(businessHoursBtn);
		presetsPanel.add(eveningBtn);
		presetsPanel.add(overnightBtn);

		gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
		form.add(presetsPanel, gbc);

		int res = JOptionPane.showConfirmDialog(this, form, "Configure Time Range Condition", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (res == JOptionPane.OK_OPTION) {
			Date startDate = (Date) startSpinner.getValue();
			Date endDate = (Date) endSpinner.getValue();

			Calendar sCal = Calendar.getInstance();
			sCal.setTime(startDate);
			LocalTime startLocal = LocalTime.of(sCal.get(Calendar.HOUR_OF_DAY), sCal.get(Calendar.MINUTE));

			Calendar eCal = Calendar.getInstance();
			eCal.setTime(endDate);
			LocalTime endLocal = LocalTime.of(eCal.get(Calendar.HOUR_OF_DAY), eCal.get(Calendar.MINUTE));

			conditions.add(TimeRangeCondition.builder()
					.startTime(startLocal)
					.endTime(endLocal)
					.build());
			refreshConditionCards();
		}
	}

	private void promptAddPostGapCondition() {
		JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
		JSpinner minPostsSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
		JCheckBox useBaselineCheck = new JCheckBox("Use Baseline Total Replies Count");
		JSpinner baselineCountSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
		baselineCountSpinner.setEnabled(false);

		useBaselineCheck.addActionListener(e -> baselineCountSpinner.setEnabled(useBaselineCheck.isSelected()));

		form.add(new JLabel("Min Posts / Replies:"));
		form.add(minPostsSpinner);
		form.add(useBaselineCheck);
		form.add(new JLabel(""));
		form.add(new JLabel("Baseline Reply Count:"));
		form.add(baselineCountSpinner);

		int res = JOptionPane.showConfirmDialog(this, form, "Configure Post Gap Trigger", JOptionPane.OK_CANCEL_OPTION);
		if (res == JOptionPane.OK_OPTION) {
			int min = (Integer) minPostsSpinner.getValue();
			boolean useBaseline = useBaselineCheck.isSelected();
			int baseline = (Integer) baselineCountSpinner.getValue();

			conditions.add(PostGapCondition.builder()
					.minPostsSinceUser(min)
					.useBaselineCount(useBaseline)
					.baselineReplyCount(baseline)
					.build());
			refreshConditionCards();
		}
	}

	private void promptAddAntiNecropostCondition() {
		JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
		JSpinner daysSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 365, 1));
		JCheckBox authorExemptionCheck = new JCheckBox("Exempt if user is thread author", true);

		form.add(new JLabel("Max Inactive Days:"));
		form.add(daysSpinner);
		form.add(new JLabel(""));
		form.add(authorExemptionCheck);

		int res = JOptionPane.showConfirmDialog(this, form, "Configure Anti-Necropost Rule", JOptionPane.OK_CANCEL_OPTION);
		if (res == JOptionPane.OK_OPTION) {
			conditions.add(AntiNecropostCondition.builder()
					.maxInactiveDays((Integer) daysSpinner.getValue())
					.allowAuthorExemption(authorExemptionCheck.isSelected())
					.build());
			refreshConditionCards();
		}
	}

	private void showExtensionConditionMenu(Component invoker) {
		List<ConditionProvider> providers = extensionManager.getActiveConditionProviders();
		if (providers.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No custom conditions provided by active extensions.", "Custom Conditions", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		JPopupMenu menu = new JPopupMenu();
		for (ConditionProvider provider : providers) {
			JMenuItem item = new JMenuItem(provider.getDisplayName());
			item.setToolTipText(provider.getDescription());
			item.addActionListener(e -> {
				PostCondition cond = provider.createDefaultInstance();
				conditions.add(cond);
				refreshConditionCards();
			});
			menu.add(item);
		}
		menu.show(invoker, 0, invoker.getHeight());
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		UiPreviewHelper.showPreviewFrame(new ConditionBuilderPanel(), "Condition Builder", 650, 400);
	}
}
