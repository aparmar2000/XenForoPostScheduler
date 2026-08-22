package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import lombok.Getter;

public abstract class AbstractSettingsPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	@Getter
	protected final JPanel formContainer;
	@Getter
	protected final JLabel headerLabel;
	@Getter
	protected final JScrollPane scrollPane;
	@Getter
	protected final JPanel actionBar;
	@Getter
	protected JButton reloadBtn;
	@Getter
	protected JButton resetBtn;
	@Getter
	protected JButton saveBtn;

	public AbstractSettingsPanel() {
		this("Settings", "Configure settings below");
	}

	public AbstractSettingsPanel(@NotNull String panelTitle, @NotNull String defaultHeaderText) {
		setLayout(new BorderLayout(5, 5));
		setBorder(BorderFactory.createTitledBorder(panelTitle));

		headerLabel = new JLabel(defaultHeaderText);
		headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		headerLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		formContainer = new JPanel(new GridBagLayout());
		formContainer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		scrollPane = new JScrollPane(formContainer);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		actionBar = createActionBar();

		add(headerLabel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		add(actionBar, BorderLayout.SOUTH);
	}

	protected JPanel createActionBar() {
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

		reloadBtn = new JButton("Revert");
		reloadBtn.setToolTipText("Revert to saved settings");
		reloadBtn.addActionListener(e -> reloadSettings());
		bar.add(reloadBtn);

		resetBtn = new JButton("Reset to Defaults");
		resetBtn.setToolTipText("Reset all settings to their default values");
		resetBtn.addActionListener(e -> resetDefaults());
		bar.add(resetBtn);

		saveBtn = new JButton("Save Settings");
		saveBtn.setToolTipText("Save current settings to disk");
		saveBtn.addActionListener(e -> saveSettings());
		bar.add(saveBtn);

		return bar;
	}

	@NotNull
	public static JPanel createActionBar(@Nullable Runnable onRevert,
			@Nullable Runnable onResetDefaults,
			@Nullable Runnable onSave) {
		JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

		JButton revertButton = new JButton("Revert");
		revertButton.setToolTipText("Revert to saved settings");
		if (onRevert != null) {
			revertButton.addActionListener(e -> onRevert.run());
		} else {
			revertButton.setEnabled(false);
		}
		bar.add(revertButton);

		JButton resetButton = new JButton("Reset to Defaults");
		resetButton.setToolTipText("Reset all settings to their default values");
		if (onResetDefaults != null) {
			resetButton.addActionListener(e -> onResetDefaults.run());
		} else {
			resetButton.setEnabled(false);
		}
		bar.add(resetButton);

		JButton saveButton = new JButton("Save Settings");
		saveButton.setToolTipText("Save current settings to disk");
		if (onSave != null) {
			saveButton.addActionListener(e -> onSave.run());
		} else {
			saveButton.setEnabled(false);
		}
		bar.add(saveButton);

		return bar;
	}

	public JButton getRevertBtn() {
		return reloadBtn;
	}

	public void setActionButtonsEnabled(boolean enabled) {
		if (reloadBtn != null) {
			reloadBtn.setEnabled(enabled);
		}
		if (resetBtn != null) {
			resetBtn.setEnabled(enabled);
		}
		if (saveBtn != null) {
			saveBtn.setEnabled(enabled);
		}
	}

	public abstract void saveSettings();

	public abstract void reloadSettings();

	public void revertSettings() {
		reloadSettings();
	}

	public abstract void resetDefaults();

	@NotNull
	public JFrame createStandaloneFrame(@NotNull String title, int width, int height) {
		return wrapInFrame(this, title, width, height);
	}

	@NotNull
	public static JFrame wrapInFrame(@NotNull AbstractSettingsPanel panel, @NotNull String title, int width, int height) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setSize(width, height);
		frame.setLocationRelativeTo(null);
		frame.getContentPane().add(panel);
		return frame;
	}

	public void setHeaderText(@NotNull String text) {
		headerLabel.setText(text);
	}

	public void clearSettings() {
		formContainer.removeAll();
		formContainer.revalidate();
		formContainer.repaint();
	}

	public void populateSettings(@Nullable List<SettingDefinition<?>> settings) {
		populateSettings(settings, "<html><i>No configurable settings available.</i></html>");
	}

	public void populateSettings(@Nullable List<SettingDefinition<?>> settings, @NotNull String emptyMessage) {
		formContainer.removeAll();

		if (settings == null || settings.isEmpty()) {
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.insets = new Insets(8, 8, 8, 8);
			formContainer.add(new JLabel(emptyMessage), gbc);

			gbc.gridy = 1;
			gbc.weighty = 1.0;
			gbc.fill = GridBagConstraints.BOTH;
			formContainer.add(Box.createVerticalGlue(), gbc);
		} else {
			// Group settings
			Map<String, List<SettingDefinition<?>>> groupedSettings = new LinkedHashMap<>();
			for (SettingDefinition<?> setting : settings) {
				String groupKey = setting.getGroup() != null ? setting.getGroup() : "";
				groupedSettings.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(setting);
			}

			int row = 0;
			boolean isFirstGroup = true;

			for (Map.Entry<String, List<SettingDefinition<?>>> entry : groupedSettings.entrySet()) {
				String groupName = entry.getKey();
				List<SettingDefinition<?>> groupList = entry.getValue();

				// Group header row
				if (!groupName.isEmpty()) {
					addGroupHeader(row, isFirstGroup, groupName);
					row++;
				}

				isFirstGroup = false;

				// Setting rows
				for (SettingDefinition<?> setting : groupList) {
					addSettingRow(row, setting);
					row++;
				}
			}

			// Bottom vertical filler
			GridBagConstraints gbcGlue = new GridBagConstraints();
			gbcGlue.gridx = 0;
			gbcGlue.gridy = row;
			gbcGlue.gridwidth = GridBagConstraints.REMAINDER;
			gbcGlue.weighty = 1.0;
			gbcGlue.fill = GridBagConstraints.BOTH;
			formContainer.add(Box.createVerticalGlue(), gbcGlue);
		}

		formContainer.revalidate();
		formContainer.repaint();
	}

	protected void addGroupHeader(int row, boolean isFirstGroup, @NotNull String groupName) {
		GridBagConstraints gbcHeader = new GridBagConstraints();
		gbcHeader.gridx = 0;
		gbcHeader.gridy = row;
		gbcHeader.gridwidth = 1;
		gbcHeader.weightx = 0.0;
		gbcHeader.anchor = GridBagConstraints.WEST;
		gbcHeader.fill = GridBagConstraints.NONE;
		gbcHeader.insets = new Insets(isFirstGroup ? 4 : 18, 6, 6, 8);

		JLabel groupLabel = new JLabel(groupName);
		groupLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
		if (UIManager.getColor("Component.accentColor") != null) {
			groupLabel.setForeground(UIManager.getColor("Component.accentColor"));
		}
		formContainer.add(groupLabel, gbcHeader);

		GridBagConstraints gbcSep = new GridBagConstraints();
		gbcSep.gridx = 1;
		gbcSep.gridy = row;
		gbcSep.gridwidth = GridBagConstraints.REMAINDER;
		gbcSep.weightx = 1.0;
		gbcSep.anchor = GridBagConstraints.CENTER;
		gbcSep.fill = GridBagConstraints.HORIZONTAL;
		gbcSep.insets = new Insets(isFirstGroup ? 4 : 18, 0, 6, 6);

		formContainer.add(new JSeparator(SwingConstants.HORIZONTAL), gbcSep);
	}

	protected void addSettingRow(int row, @NotNull SettingDefinition<?> setting) {
		// Label & description
		GridBagConstraints gbcLabel = new GridBagConstraints();
		gbcLabel.gridx = 0;
		gbcLabel.gridy = row;
		gbcLabel.gridwidth = 1;
		gbcLabel.weightx = 0.0;
		gbcLabel.weighty = 0.0;
		gbcLabel.anchor = setting.expandsVertically() ? GridBagConstraints.NORTHWEST : GridBagConstraints.WEST;
		gbcLabel.fill = GridBagConstraints.NONE;
		gbcLabel.insets = new Insets(6, 12, 6, 12);

		JLabel nameLabel = new JLabel("<html><b>" + setting.getLabel() + "</b>"
				+ (!setting.getDescription().isEmpty()
						? "<br><font color='gray' size='-2'>" + setting.getDescription() + "</font>"
								: "") + "</html>");
		formContainer.add(nameLabel, gbcLabel);

		// Bind setting listeners & initial value
		bindSetting(setting);

		// Field UI component
		GridBagConstraints gbcField = new GridBagConstraints();
		gbcField.gridx = 1;
		gbcField.gridy = row;
		gbcField.gridwidth = GridBagConstraints.REMAINDER;
		gbcField.weightx = setting.expandsHorizontally() ? 1.0 : 0.0;
		gbcField.weighty = setting.expandsVertically() ? 1.0 : 0.0;
		gbcField.anchor = setting.expandsVertically() ? GridBagConstraints.NORTHWEST : GridBagConstraints.WEST;
		gbcField.insets = new Insets(6, 6, 6, 10);

		if (setting.expandsHorizontally() && setting.expandsVertically()) {
			gbcField.fill = GridBagConstraints.BOTH;
		} else if (setting.expandsHorizontally()) {
			gbcField.fill = GridBagConstraints.HORIZONTAL;
		} else if (setting.expandsVertically()) {
			gbcField.fill = GridBagConstraints.VERTICAL;
		} else {
			gbcField.fill = GridBagConstraints.NONE;
		}

		formContainer.add(setting.getUiComponent(), gbcField);
	}

	/**
	 * Subclasses must implement this method to bind the setting's UI to the underlying model or context.
	 *
	 * @param setting the setting definition to bind
	 */
	protected abstract void bindSetting(@NotNull SettingDefinition<?> setting);
}
