package aparmar2000.xenforoposter.ui;
import java.beans.Beans;
import java.util.List;

import javax.swing.JOptionPane;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import aparmar2000.xenforoposter.extension.ExtensionContext;
import aparmar2000.xenforoposter.extension.ExtensionHolder;
import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import lombok.Getter;

public class ExtensionSettingsPanel extends AbstractSettingsPanel {
	private static final long serialVersionUID = 3310528990886890843L;

	@Getter
	private ExtensionHolder currentHolder;

	/**
	 * Design-time constructor for ExtensionSettingsPanel.
	 * @wbp.parser.constructor
	 */
	public ExtensionSettingsPanel() {
		super("Extension Settings", "Select an extension to configure settings");
		setActionButtonsEnabled(false);

		if (Beans.isDesignTime()) {
			ExtensionManager previewManager = UiPreviewHelper.createPreviewExtensionManager();
			List<ExtensionHolder> holders = ImmutableList.copyOf(previewManager.getAllExtensions());
			if (!holders.isEmpty()) {
				displayExtensionSettings(holders.get(0));
			}
		}
	}

	public void displayExtensionSettings(@Nullable ExtensionHolder holder) {
		this.currentHolder = holder;

		if (holder == null) {
			setHeaderText("No extension selected");
			clearSettings();
			setActionButtonsEnabled(false);
			return;
		}

		setHeaderText(String.format( "%s (v%s) — [%s]", holder.getExtension().getName(), holder.getExtension().getVersion(), holder.getMetadata().getSource() ));
		ExtensionContext context = holder.getContext();
		List<SettingDefinition<?>> settings = context.getRegisteredSettings();

		populateSettings(settings, "<html><i>This extension has no configurable settings.</i></html>");
		setActionButtonsEnabled(!settings.isEmpty());
	}

	@Override
	protected void bindSetting(@NotNull SettingDefinition<?> setting) {
		if (currentHolder != null) {
			bindSettingToContext(setting, currentHolder.getContext());
		}
	}

	private <T> void bindSettingToContext(SettingDefinition<T> setting, ExtensionContext context) {
		setting.clearChangeListeners();
		T currentVal = context.getSettingValue(setting.getKey(), setting.getValueType());
		if (currentVal != null) {
			setting.setUiValue(currentVal);
		} else {
			setting.resetToDefault();
		}

		setting.addChangeListener(() -> {
			if (setting.isUiValueValid()) {
				context.setSettingValue(setting.getKey(), setting.getUiValue());
			}
		});
	}

	@Override
	public void saveSettings() {
		if (currentHolder == null) {
			return;
		}
		currentHolder.getContext().saveSettings();
		JOptionPane.showMessageDialog(this,
				String.format("Settings for '%s' saved successfully.", currentHolder.getExtension().getName()),
				"Settings Saved",
				JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void reloadSettings() {
		if (currentHolder == null) {
			return;
		}
		currentHolder.getContext().loadSettings();
		displayExtensionSettings(currentHolder);
	}

	@Override
	public void resetDefaults() {
		if (currentHolder == null) {
			return;
		}
		int choice = JOptionPane.showConfirmDialog(this,
				String.format("Are you sure you want to reset all settings for '%s' to default values?",
						currentHolder.getExtension().getName()),
				"Reset Settings",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (choice == JOptionPane.YES_OPTION) {
			currentHolder.getContext().resetSettingsToDefaults();
			displayExtensionSettings(currentHolder);
		}
	}

	/**
	 * Standalone launcher for WindowBuilder GUI design preview.
	 */
	public static void main(String[] args) {
		ExtensionSettingsPanel panel = new ExtensionSettingsPanel();
		ExtensionManager previewManager = UiPreviewHelper.createPreviewExtensionManager();
		List<ExtensionHolder> holders = ImmutableList.copyOf(previewManager.getAllExtensions());
		if (!holders.isEmpty()) {
			panel.displayExtensionSettings(holders.get(0));
		}
		UiPreviewHelper.showPreviewFrame(panel, "Extension Settings", 550, 480);
	}
}

