package aparmar2000.xenforoposter.extension;

import org.jetbrains.annotations.NotNull;

public interface Extension {
	@NotNull String getId();
	@NotNull String getName();
	@NotNull String getVersion();
	@NotNull String getAuthor();
	@NotNull String getDescription();

	void initialize(@NotNull ExtensionContext context);
	default void onEnable() {}
	default void onDisable() {}
}
