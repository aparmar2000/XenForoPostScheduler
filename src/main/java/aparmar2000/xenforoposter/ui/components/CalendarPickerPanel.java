package aparmar2000.xenforoposter.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CalendarPickerPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

	private YearMonth currentYearMonth;
	private LocalDate selectedDate;
	private Consumer<LocalDate> onDateSelected;

	private final JLabel monthYearLabel;
	private final JPanel daysGridPanel;

	public CalendarPickerPanel() {
		this(LocalDate.now());
	}

	public CalendarPickerPanel(@Nullable LocalDate initialDate) {
		this.selectedDate = initialDate;
		this.currentYearMonth = initialDate != null ? YearMonth.from(initialDate) : YearMonth.now();

		setLayout(new BorderLayout(5, 5));
		setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		// Header: Navigation and Month/Year label
		JPanel headerPanel = new JPanel(new BorderLayout(4, 0));
		JButton prevBtn = new JButton("<");
		prevBtn.setFocusable(false);
		prevBtn.addActionListener(e -> {
			currentYearMonth = currentYearMonth.minusMonths(1);
			refreshCalendar();
		});

		JButton nextBtn = new JButton(">");
		nextBtn.setFocusable(false);
		nextBtn.addActionListener(e -> {
			currentYearMonth = currentYearMonth.plusMonths(1);
			refreshCalendar();
		});

		monthYearLabel = new JLabel("", SwingConstants.CENTER);
		monthYearLabel.setFont(monthYearLabel.getFont().deriveFont(Font.BOLD, 13f));

		headerPanel.add(prevBtn, BorderLayout.WEST);
		headerPanel.add(monthYearLabel, BorderLayout.CENTER);
		headerPanel.add(nextBtn, BorderLayout.EAST);

		// Days of week and calendar grid container
		JPanel centerPanel = new JPanel(new BorderLayout(2, 2));

		// Day of week header (Mon - Sun)
		JPanel dayNamesPanel = new JPanel(new GridLayout(1, 7, 2, 2));
		DayOfWeek[] daysOfWeek = {
				DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
				DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
		};
		for (DayOfWeek dow : daysOfWeek) {
			JLabel lbl = new JLabel(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
			lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
			if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
				lbl.setForeground(new Color(150, 150, 200));
			}
			dayNamesPanel.add(lbl);
		}

		daysGridPanel = new JPanel(new GridLayout(6, 7, 2, 2));
		daysGridPanel.setPreferredSize(new Dimension(280, 170));

		centerPanel.add(dayNamesPanel, BorderLayout.NORTH);
		centerPanel.add(daysGridPanel, BorderLayout.CENTER);

		// Bottom action bar (Today and Clear)
		JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
		JButton todayBtn = new JButton("Today");
		todayBtn.setFocusable(false);
		todayBtn.addActionListener(e -> {
			LocalDate today = LocalDate.now();
			setSelectedDate(today);
			currentYearMonth = YearMonth.from(today);
			refreshCalendar();
			if (onDateSelected != null) {
				onDateSelected.accept(today);
			}
		});

		JButton clearBtn = new JButton("Clear");
		clearBtn.setFocusable(false);
		clearBtn.addActionListener(e -> {
			setSelectedDate(null);
			refreshCalendar();
			if (onDateSelected != null) {
				onDateSelected.accept(null);
			}
		});

		footerPanel.add(todayBtn);
		footerPanel.add(clearBtn);

		add(headerPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(footerPanel, BorderLayout.SOUTH);

		refreshCalendar();
	}

	public void setOnDateSelected(@Nullable Consumer<LocalDate> listener) {
		this.onDateSelected = listener;
	}

	public @Nullable LocalDate getSelectedDate() {
		return selectedDate;
	}

	public void setSelectedDate(@Nullable LocalDate date) {
		this.selectedDate = date;
		if (date != null) {
			this.currentYearMonth = YearMonth.from(date);
		}
		refreshCalendar();
	}

	private void refreshCalendar() {
		monthYearLabel.setText(currentYearMonth.format(MONTH_YEAR_FORMATTER));
		daysGridPanel.removeAll();

		LocalDate firstOfMonth = currentYearMonth.atDay(1);
		int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue(); // 1 = Mon, 7 = Sun
		int leadEmptyDays = dayOfWeekValue - 1;
		int lengthOfMonth = currentYearMonth.lengthOfMonth();

		LocalDate today = LocalDate.now();

		// Previous month filler days
		YearMonth prevMonth = currentYearMonth.minusMonths(1);
		int prevMonthDays = prevMonth.lengthOfMonth();
		for (int i = 0; i < leadEmptyDays; i++) {
			int dayNum = prevMonthDays - leadEmptyDays + i + 1;
			LocalDate dayDate = prevMonth.atDay(dayNum);
			JButton btn = createDayButton(String.valueOf(dayNum), dayDate, false, false);
			daysGridPanel.add(btn);
		}

		// Current month days
		for (int day = 1; day <= lengthOfMonth; day++) {
			LocalDate dayDate = currentYearMonth.atDay(day);
			boolean isSelected = selectedDate != null && selectedDate.equals(dayDate);
			boolean isToday = today.equals(dayDate);
			JButton btn = createDayButton(String.valueOf(day), dayDate, true, isSelected);
			if (isToday && !isSelected) {
				btn.setFont(btn.getFont().deriveFont(Font.BOLD));
			}
			daysGridPanel.add(btn);
		}

		// Trailing empty days to complete 42 cells (6 rows * 7 columns)
		int totalCells = leadEmptyDays + lengthOfMonth;
		int trailEmptyDays = 42 - totalCells;
		YearMonth nextMonth = currentYearMonth.plusMonths(1);
		for (int day = 1; day <= trailEmptyDays; day++) {
			LocalDate dayDate = nextMonth.atDay(day);
			JButton btn = createDayButton(String.valueOf(day), dayDate, false, false);
			daysGridPanel.add(btn);
		}

		daysGridPanel.revalidate();
		daysGridPanel.repaint();
	}

	private JButton createDayButton(String text, LocalDate date, boolean isCurrentMonth, boolean isSelected) {
		JButton btn = new JButton(text);
		btn.setMargin(new java.awt.Insets(1, 1, 1, 1));
		btn.setFocusable(false);

		if (!isCurrentMonth) {
			btn.setEnabled(false);
		} else if (isSelected) {
			btn.setBackground(new Color(40, 110, 220));
			btn.setForeground(Color.WHITE);
			btn.setFont(btn.getFont().deriveFont(Font.BOLD));
		}

		btn.addActionListener(e -> {
			setSelectedDate(date);
			if (onDateSelected != null) {
				onDateSelected.accept(date);
			}
		});

		return btn;
	}

	/**
	 * Helper dialog to prompt user to pick a date.
	 */
	public static @Nullable LocalDate showDialog(@Nullable Component parent, @NotNull String title, @Nullable LocalDate initialDate) {
		final LocalDate[] result = new LocalDate[] { initialDate };
		final boolean[] confirmed = new boolean[] { false };

		CalendarPickerPanel panel = new CalendarPickerPanel(initialDate);
		panel.setOnDateSelected(date -> result[0] = date);

		JDialog dialog = new JDialog(
				parent != null ? SwingUtilities.getWindowAncestor(parent) : null,
						title,
						JDialog.ModalityType.APPLICATION_MODAL
				);
		dialog.setLayout(new BorderLayout());
		dialog.add(panel, BorderLayout.CENTER);

		JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(e -> {
			confirmed[0] = true;
			dialog.dispose();
		});
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(e -> {
			confirmed[0] = false;
			dialog.dispose();
		});

		buttonBar.add(okBtn);
		buttonBar.add(cancelBtn);
		dialog.add(buttonBar, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);

		return confirmed[0] ? result[0] : initialDate;
	}
}
