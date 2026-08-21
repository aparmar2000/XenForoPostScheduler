package aparmar2000.xenforoposter.extension.toolbar;

import org.jetbrains.annotations.NotNull;

public interface EditorContext {
    @NotNull String getText();
    @NotNull String getSelectedText();
    void replaceSelection(@NotNull String newText);
    void insertAtCaret(@NotNull String text);
    int getCaretPosition();
    void setCaretPosition(int position);
    void wrapSelection(@NotNull String prefix, @NotNull String suffix);
}
