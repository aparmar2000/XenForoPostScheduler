package aparmar2000.xenforoposter.extension.toolbar;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BbCodeToolbarItem {
    @NotNull String id;
    @NotNull String label;
    @Nullable String tooltip;
    @Nullable String iconName;
    @NotNull Consumer<EditorContext> action;

    public void execute(@NotNull EditorContext context) {
        action.accept(context);
    }
}
