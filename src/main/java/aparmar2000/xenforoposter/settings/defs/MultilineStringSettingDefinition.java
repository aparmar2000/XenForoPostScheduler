package aparmar2000.xenforoposter.settings.defs;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MultilineStringSettingDefinition extends SettingDefinition<String> {
    private final JTextArea textArea;
    private final JScrollPane scrollPane;

    public MultilineStringSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, @NotNull String defaultValue) {
        this(key, label, description, defaultValue, 3, 20, null);
    }

    public MultilineStringSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, @NotNull String defaultValue, @Nullable String group) {
        this(key, label, description, defaultValue, 3, 20, group);
    }

    public MultilineStringSettingDefinition(@NotNull String key, @NotNull String label, @Nullable String description, @NotNull String defaultValue, int rows, int columns, @Nullable String group) {
        super(key, label, description, defaultValue, group);

        this.textArea = new JTextArea(defaultValue, rows, columns);
        this.textArea.setLineWrap(true);
        this.textArea.setWrapStyleWord(true);
        this.scrollPane = new JScrollPane(this.textArea);

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

        this.textArea.getDocument().addDocumentListener(docListener);
        this.textArea.addFocusListener(focusAdapter);
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
        return scrollPane;
    }

    @Override
    public boolean expandsHorizontally() {
        return true;
    }

    @Override
    public boolean expandsVertically() {
        return true;
    }

    @Override
    public @NotNull String getUiValue() {
        return textArea.getText();
    }

    @Override
    public void setUiValue(@NotNull String value) {
        textArea.setText(value != null ? value : "");
    }
}
