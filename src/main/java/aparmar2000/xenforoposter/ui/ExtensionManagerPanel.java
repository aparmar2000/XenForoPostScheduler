package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.extension.ExtensionHolder;
import aparmar2000.xenforoposter.extension.ExtensionManager;
import lombok.Getter;

@SuppressWarnings("unused")
public class ExtensionManagerPanel extends JPanel {
	private static final long serialVersionUID = 4021584852732669605L;

	public static final String CARD_LIST = "LIST";
	public static final String CARD_SETTINGS = "SETTINGS";

	private static final int COL_ENABLED = 0;
	private static final int COL_NAME = 1;
	private static final int COL_VERSION = 2;
	private static final int COL_AUTHOR = 3;
	private static final int COL_SOURCE = 4;
	private static final int COL_ID = 5;
	private static final int COL_SETTINGS = 6;

	private final ExtensionManager extensionManager;
	@Getter
	private final JTable extensionTable;
	private final DefaultTableModel tableModel;
	@Getter
	private final ExtensionSettingsPanel settingsPanel;
	@Getter
	private final CardLayout cardLayout;
	@Getter
	private final JPanel cardsContainer;
	private final List<ExtensionHolder> currentList = new ArrayList<>();

	/**
	 * Design-time only constructor for Eclipse WindowBuilder preview.
	 * @wbp.parser.constructor
	 * @deprecated For WindowBuilder GUI designer and preview use only.
	 */
	@Deprecated
	@ApiStatus.Internal
	public ExtensionManagerPanel() {
		this(UiPreviewHelper.createPreviewExtensionManager());
	}

	public ExtensionManagerPanel(@NotNull ExtensionManager extensionManager) {
		this.extensionManager = extensionManager;
		setLayout(new BorderLayout());

		cardLayout = new CardLayout();
		cardsContainer = new JPanel(cardLayout);

		// === Extensions List
		JPanel listView = new JPanel(new BorderLayout(5, 5));

		// Top Toolbar
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
		JButton loadJarBtn = new JButton("Load Extension JAR...");
		loadJarBtn.addActionListener(e -> chooseAndLoadJar());
		toolbar.add(loadJarBtn);

		JButton toggleBtn = new JButton("Toggle Enable / Disable");
		toggleBtn.addActionListener(e -> toggleSelectedExtension());
		toolbar.add(toggleBtn);

		// Extensions Table
		String[] cols = {"Enabled", "Name", "Version", "Author", "Source", "ID", "Settings"};
		tableModel = new DefaultTableModel(cols, 0) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if (columnIndex == COL_ENABLED) {
					return Boolean.class;
				}
				return String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return column == COL_ENABLED || column == COL_SETTINGS;
			}
		};

		extensionTable = new JTable(tableModel);
		extensionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		extensionTable.setRowHeight(28);

		// Column widths
		extensionTable.getColumnModel().getColumn(COL_ENABLED).setPreferredWidth(60);
		extensionTable.getColumnModel().getColumn(COL_ENABLED).setMaxWidth(70);
		extensionTable.getColumnModel().getColumn(COL_VERSION).setPreferredWidth(65);
		extensionTable.getColumnModel().getColumn(COL_VERSION).setMaxWidth(80);
		extensionTable.getColumnModel().getColumn(COL_SETTINGS).setPreferredWidth(95);
		extensionTable.getColumnModel().getColumn(COL_SETTINGS).setMaxWidth(110);

		// Settings Button Column Renderer and Editor
		TableButtonRenderer buttonRenderer = new TableButtonRenderer("Settings");
		TableButtonEditor buttonEditor = new TableButtonEditor(row -> {
			if (row >= 0 && row < currentList.size()) {
				openExtensionSettings(currentList.get(row));
			}
		});

		extensionTable.getColumnModel().getColumn(COL_SETTINGS).setCellRenderer(buttonRenderer);
		extensionTable.getColumnModel().getColumn(COL_SETTINGS).setCellEditor(buttonEditor);

		tableModel.addTableModelListener(e -> {
			if (e.getColumn() == COL_ENABLED && e.getFirstRow() >= 0 && e.getFirstRow() < currentList.size()) {
				boolean enabled = (Boolean) tableModel.getValueAt(e.getFirstRow(), COL_ENABLED);
				ExtensionHolder holder = currentList.get(e.getFirstRow());
				extensionManager.setExtensionEnabled(holder.getExtension().getId(), enabled);
			}
		});

		// Double-click row to open settings
		extensionTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int row = extensionTable.getSelectedRow();
					if (row >= 0 && row < currentList.size()) {
						openExtensionSettings(currentList.get(row));
					}
				}
			}
		});

		listView.add(toolbar, BorderLayout.NORTH);
		listView.add(new JScrollPane(extensionTable), BorderLayout.CENTER);

		// === Extension Settings Pane
		JPanel settingsView = new JPanel(new BorderLayout(5, 5));

		JPanel backBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
		JButton backBtn = new JButton("← Back to Extensions");
		backBtn.setMnemonic('B');
		backBtn.addActionListener(e -> showExtensionList());
		backBar.add(backBtn);

		settingsPanel = new ExtensionSettingsPanel();

		settingsView.add(backBar, BorderLayout.NORTH);
		settingsView.add(settingsPanel, BorderLayout.CENTER);

		// Add views to card layout container
		cardsContainer.add(listView, CARD_LIST);
		cardsContainer.add(settingsView, CARD_SETTINGS);

		add(cardsContainer, BorderLayout.CENTER);

		extensionManager.addChangeListener(this::refreshTable);
		refreshTable();
	}

	public void openExtensionSettings(@NotNull ExtensionHolder holder) {
		settingsPanel.displayExtensionSettings(holder);
		cardLayout.show(cardsContainer, CARD_SETTINGS);
	}

	public void showExtensionList() {
		cardLayout.show(cardsContainer, CARD_LIST);
	}

	public void refreshTable() {
		currentList.clear();
		currentList.addAll(extensionManager.getAllExtensions());
		tableModel.setRowCount(0);

		for (ExtensionHolder holder : currentList) {
			tableModel.addRow(new Object[]{
					holder.isEnabled(),
					holder.getExtension().getName(),
					holder.getExtension().getVersion(),
					holder.getExtension().getAuthor(),
					holder.getMetadata().getSource(),
					holder.getExtension().getId(),
					"Settings"
			});
		}

		if (!currentList.isEmpty() && extensionTable.getSelectedRow() < 0) {
			extensionTable.setRowSelectionInterval(0, 0);
		}
	}

	private void chooseAndLoadJar() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select Extension JAR File");
		chooser.setFileFilter(new FileNameExtensionFilter("Java Archive (*.jar)", "jar"));

		int res = chooser.showOpenDialog(this);
		if (res == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			boolean success = extensionManager.loadExtensionJar(file);
			if (success) {
				JOptionPane.showMessageDialog(this, "Extension loaded successfully from: " + file.getName(),
						"Extension Loaded", JOptionPane.INFORMATION_MESSAGE);
				refreshTable();
			} else {
				JOptionPane.showMessageDialog(this, "Failed to load extension from jar. Please check logs.",
						"Load Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void toggleSelectedExtension() {
		int row = extensionTable.getSelectedRow();
		if (row >= 0 && row < currentList.size()) {
			ExtensionHolder holder = currentList.get(row);
			boolean newState = !holder.isEnabled();
			extensionManager.setExtensionEnabled(holder.getExtension().getId(), newState);
			tableModel.setValueAt(newState, row, COL_ENABLED);
		}
	}

	/**
	 * Table cell renderer for row action buttons.
	 */
	private static class TableButtonRenderer extends JButton implements TableCellRenderer {
		private static final long serialVersionUID = 1L;

		public TableButtonRenderer(String defaultText) {
			super(defaultText);
			setOpaque(true);
			setMargin(new Insets(2, 6, 2, 6));
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus,
				int row, int column) {
			setText(value != null ? value.toString() : "Settings");
			return this;
		}
	}

	/**
	 * Table cell editor for row action buttons.
	 */
	private static class TableButtonEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;

		private final JButton button;
		private int currentRow = -1;

		public TableButtonEditor(Consumer<Integer> clickAction) {
			button = new JButton("Settings");
			button.setMargin(new Insets(2, 6, 2, 6));
			button.addActionListener(e -> {
				int row = currentRow;
				fireEditingStopped();
				if (row >= 0) {
					clickAction.accept(row);
				}
			});
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column) {
			this.currentRow = row;
			button.setText(value != null ? value.toString() : "Settings");
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			return button.getText();
		}
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		UiPreviewHelper.showPreviewFrame(new ExtensionManagerPanel(), "Extension Manager", 850, 500);
	}
}
