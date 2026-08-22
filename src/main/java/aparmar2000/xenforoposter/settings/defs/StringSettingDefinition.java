package aparmar2000.xenforoposter.settings.defs;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringSettingDefinition extends SettingDefinition<String> {
	private final JTextField textField;

	public StringSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, @NotNull String defaultValue) {
		this(key, label, description, defaultValue, null);
	}

	public StringSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, @NotNull String defaultValue, @Nullable String group) {
		super(key, label, description, defaultValue, group);

		this.textField = new JTextField(defaultValue);

		DocumentListener docListener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				notifyChangeListeners();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				notifyChangeListeners();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				notifyChangeListeners();
			}
		};

		FocusAdapter focusAdapter = new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				notifyChangeListeners();
			}
		};

		this.textField.getDocument().addDocumentListener(docListener);
		this.textField.addFocusListener(focusAdapter);
	}

	@Override
	public Class<String> getValueType() {
		return String.class;
	}

	@Override
	public boolean validate(String value) {
		return value != null;
	}

	@Override
	public String serialize(String value) {
		return value != null ? value : "";
	}

	@Override
	public String deserialize(String rawValue) {
		return rawValue != null ? rawValue : getDefaultValue();
	}

	@Override
	public @NotNull JComponent getUiComponent() {
		return textField;
	}

	@Override
	public boolean expandsHorizontally() {
		return true;
	}

	@Override
	public @NotNull String getUiValue() {
		return textField.getText();
	}

	@Override
	public void setUiValue(@NotNull String value) {
		textField.setText(value != null ? value : "");
	}
}

