package aparmar2000.xenforoposter.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.settings.GeneralSettings;
import lombok.Getter;

public class SettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    @Getter
    private final JTabbedPane tabbedPane;
    @Getter
    private final GeneralSettingsPanel generalSettingsPanel;
    @Getter
    private final ExtensionManagerPanel extensionManagerPanel;

    /**
     * Design-time only constructor for Eclipse WindowBuilder preview.
     * @wbp.parser.constructor
     * @deprecated For WindowBuilder GUI designer and preview use only.
     */
    @Deprecated
    @ApiStatus.Internal
    public SettingsPanel() {
        this(UiPreviewHelper.createPreviewExtensionManager(), UiPreviewHelper.createPreviewGeneralSettings());
    }

    public SettingsPanel(@NotNull ExtensionManager extensionManager, @NotNull GeneralSettings generalSettings) {
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane(SwingConstants.LEFT);
        generalSettingsPanel = new GeneralSettingsPanel(generalSettings);
        extensionManagerPanel = new ExtensionManagerPanel(extensionManager);

        tabbedPane.addTab("General Settings", generalSettingsPanel);
        tabbedPane.addTab("Extension Settings", extensionManagerPanel);

        add(tabbedPane, BorderLayout.CENTER);
        setPreferredSize(new Dimension(880, 580));
    }

    /**
     * Standalone launcher for WindowBuilder GUI design preview.
     */
    public static void main(String[] args) {
        UiPreviewHelper.showPreviewFrame(new SettingsPanel(), "Settings", 900, 600);
    }
}
