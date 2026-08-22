package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.formdev.flatlaf.FlatDarkLaf;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.settings.GeneralSettings;
import lombok.Getter;

public class SettingsDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	@Getter
	private final SettingsPanel settingsPanel;

	/**
	 * Design-time only constructor for Eclipse WindowBuilder preview.
	 * @wbp.parser.constructor
	 * @deprecated For WindowBuilder GUI designer and preview use only.
	 */
	@Deprecated
	@ApiStatus.Internal
	public SettingsDialog() {
		this((Frame) null, UiPreviewHelper.createPreviewExtensionManager(), UiPreviewHelper.createPreviewGeneralSettings());
	}

	public SettingsDialog(@Nullable Frame owner,
			@NotNull ExtensionManager extensionManager,
			@NotNull GeneralSettings generalSettings) {
		this((Window) owner, extensionManager, generalSettings);
	}

	public SettingsDialog(@Nullable Window owner,
			@NotNull ExtensionManager extensionManager,
			@NotNull GeneralSettings generalSettings) {
		super(owner, "Settings", ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setSize(900, 600);
		setMinimumSize(new Dimension(750, 480));
		setLocationRelativeTo(owner);

		settingsPanel = new SettingsPanel(extensionManager, generalSettings);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(settingsPanel, BorderLayout.CENTER);
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		FlatDarkLaf.setup();
		SwingUtilities.invokeLater(() -> {
			SettingsDialog dialog = new SettingsDialog();
			dialog.setVisible(true);
		});
	}
}
