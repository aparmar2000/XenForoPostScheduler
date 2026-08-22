package aparmar2000.xenforoposter.ui;

import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.settings.GeneralSettings;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import lombok.Getter;

public class GeneralSettingsPanel extends AbstractSettingsPanel {
    private static final long serialVersionUID = 1L;

    @Getter
    private final GeneralSettings generalSettings;

    /**
     * Design-time only constructor for Eclipse WindowBuilder preview.
     * @wbp.parser.constructor
     * @deprecated For WindowBuilder GUI designer and preview use only.
     */
    @Deprecated
    @ApiStatus.Internal
    public GeneralSettingsPanel() {
        this(UiPreviewHelper.createPreviewGeneralSettings());
    }

    public GeneralSettingsPanel(@NotNull GeneralSettings generalSettings) {
        super("General Settings", "Application-wide configuration and preferences");
        this.generalSettings = generalSettings;

        populateSettings(generalSettings.getRegisteredSettings());
    }

    @Override
    protected void bindSetting(@NotNull SettingDefinition<?> setting) {
        bindSettingToGeneralSettings(setting);
    }

    private <T> void bindSettingToGeneralSettings(SettingDefinition<T> setting) {
        setting.clearChangeListeners();
        T currentVal = generalSettings.getSettingValue(setting.getKey(), setting.getValueType());
        if (currentVal != null) {
            setting.setUiValue(currentVal);
        } else {
            setting.resetToDefault();
        }

        setting.addChangeListener(() -> {
            if (setting.isUiValueValid()) {
                generalSettings.setSettingValue(setting.getKey(), setting.getUiValue());
            }
        });
    }

    @Override
    public void saveSettings() {
        generalSettings.save();
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this,
                    "General settings saved successfully.",
                    "Settings Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void reloadSettings() {
        generalSettings.load();
        populateSettings(generalSettings.getRegisteredSettings());
    }

    @Override
    public void resetDefaults() {
        int choice = JOptionPane.YES_OPTION;
        if (!GraphicsEnvironment.isHeadless()) {
            choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to reset all settings to default values?",
                    "Reset Settings",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
        }
        if (choice == JOptionPane.YES_OPTION) {
            generalSettings.resetAllToDefaults();
            populateSettings(generalSettings.getRegisteredSettings());
        }
    }

    /**
     * Standalone launcher for WindowBuilder GUI design preview.
     */
    public static void main(String[] args) {
        GeneralSettings generalSettings = UiPreviewHelper.createPreviewGeneralSettings();
        GeneralSettingsPanel panel = new GeneralSettingsPanel(generalSettings);
        UiPreviewHelper.showPreviewFrame(panel, "General Settings", 600, 520);
    }
}
