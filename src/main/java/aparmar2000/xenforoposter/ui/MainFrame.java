package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.beans.Beans;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.inject.Inject;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.scheduler.SchedulerEngine;
import aparmar2000.xenforoposter.settings.GeneralSettings;
import aparmar2000.xenforoposter.web.XenForoWebClient;

public class MainFrame extends JFrame {
	private static final long serialVersionUID = 3227622158757362809L;

	private final SchedulerEngine schedulerEngine;
	private final ExtensionManager extensionManager;
	private final GeneralSettings generalSettings;

	private final JTabbedPane tabbedPane;
	private final JobListPanel jobListPanel;
	private final PostEditorFormPanel postEditorPanel;
	private final AccountManagerPanel accountPanel;

	private final JLabel engineStatusLabel;
	private final JButton engineToggleBtn;

	/**
	 * Design-time only constructor for Eclipse WindowBuilder preview.
	 * @wbp.parser.constructor
	 * @deprecated For WindowBuilder GUI designer and preview use only.
	 */
	@Deprecated
	@ApiStatus.Internal
	public MainFrame() {
		this(UiPreviewHelper.createPreviewSchedulerEngine(),
				UiPreviewHelper.createPreviewExtensionManager(),
				UiPreviewHelper.createPreviewWebClient(),
				UiPreviewHelper.createPreviewGeneralSettings());
		if (schedulerEngine.isRunning()) {
			schedulerEngine.stop();
		}
	}

	@Inject
	public MainFrame(@NotNull SchedulerEngine schedulerEngine,
			@NotNull ExtensionManager extensionManager,
			@NotNull XenForoWebClient webClient,
			@NotNull GeneralSettings generalSettings) {
		super("XenForo Post Scheduler");
		this.schedulerEngine = schedulerEngine;
		this.extensionManager = extensionManager;
		this.generalSettings = generalSettings;

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setSize(1100, 750);
		setMinimumSize(new Dimension(850, 600));
		setLocationRelativeTo(null);

		// Build Menu Bar
		setJMenuBar(createMenuBar());

		// Tabbed Panels
		tabbedPane = new JTabbedPane();

		postEditorPanel = new PostEditorFormPanel(schedulerEngine, extensionManager);
		jobListPanel = new JobListPanel(schedulerEngine, job -> {
			postEditorPanel.loadJobForEditing(job);
			tabbedPane.setSelectedComponent(postEditorPanel);
		});
		accountPanel = new AccountManagerPanel(schedulerEngine, webClient);

		tabbedPane.addTab("Jobs & Diagnostics", jobListPanel);
		tabbedPane.addTab("Post Composer & Scheduler", postEditorPanel);
		tabbedPane.addTab("Forum Accounts", accountPanel);

		// Status Bar at Bottom
		JPanel statusBar = new JPanel(new BorderLayout(8, 2));
		statusBar.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
				BorderFactory.createEmptyBorder(4, 10, 4, 10)
				));

		engineStatusLabel = new JLabel("Engine: Running (Polling active)");
		engineStatusLabel.setIcon(UIManager.getIcon("Tree.leafIcon"));

		engineToggleBtn = new JButton("Pause Scheduler Engine");
		engineToggleBtn.addActionListener(e -> toggleEngine());

		statusBar.add(engineStatusLabel, BorderLayout.WEST);
		statusBar.add(engineToggleBtn, BorderLayout.EAST);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		getContentPane().add(statusBar, BorderLayout.SOUTH);

		// Start scheduler (only when not in design mode)
		if (!Beans.isDesignTime()) {
			schedulerEngine.start();
		}
		updateEngineStatusUI();
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);

		JMenuItem settingsItem = new JMenuItem("Settings...");
		settingsItem.setMnemonic(KeyEvent.VK_S);
		settingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
				java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
		settingsItem.addActionListener(e -> openSettingsDialog());
		fileMenu.add(settingsItem);

		fileMenu.addSeparator();

		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.setMnemonic(KeyEvent.VK_X);
		exitItem.addActionListener(e -> System.exit(0));
		fileMenu.add(exitItem);

		JMenu viewMenu = new JMenu("View");
		JMenuItem darkThemeItem = new JMenuItem("Dark Theme");
		darkThemeItem.addActionListener(e -> {
			FlatDarkLaf.setup();
			FlatLaf.updateUI();
			editorRefresh();
		});
		JMenuItem lightThemeItem = new JMenuItem("Light Theme");
		lightThemeItem.addActionListener(e -> {
			FlatLightLaf.setup();
			FlatLaf.updateUI();
			editorRefresh();
		});
		viewMenu.add(darkThemeItem);
		viewMenu.add(lightThemeItem);

		JMenu helpMenu = new JMenu("Help");
		JMenuItem aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
				"XenForo Post Scheduler\nVersion 1.0.0\n\nCondition-based automated poster with XenForo BBCode live preview,\nanti-abuse rate limiting, and extension support.",
				"About XenForo Post Scheduler", JOptionPane.INFORMATION_MESSAGE));
		helpMenu.add(aboutItem);

		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		menuBar.add(helpMenu);

		return menuBar;
	}

	private void editorRefresh() {
		SwingUtilities.updateComponentTreeUI(this);
	}

	public void openSettingsDialog() {
		SettingsDialog dialog = new SettingsDialog(this, extensionManager, generalSettings);
		dialog.setVisible(true);
	}

	private void toggleEngine() {
		if (schedulerEngine.isRunning()) {
			schedulerEngine.stop();
		} else {
			schedulerEngine.start();
		}
		updateEngineStatusUI();
	}

	private void updateEngineStatusUI() {
		if (schedulerEngine.isRunning()) {
			engineStatusLabel.setText("Engine: RUNNING (Automated polling & condition evaluations active)");
			engineStatusLabel.setForeground(new Color(40, 167, 69));
			engineToggleBtn.setText("Pause Scheduler Engine");
		} else {
			engineStatusLabel.setText("Engine: PAUSED (No background polling)");
			engineStatusLabel.setForeground(new Color(220, 53, 69));
			engineToggleBtn.setText("Resume Scheduler Engine");
		}
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		FlatDarkLaf.setup();
		SwingUtilities.invokeLater(() -> {
			MainFrame frame = new MainFrame();
			frame.setVisible(true);
		});
	}
}
