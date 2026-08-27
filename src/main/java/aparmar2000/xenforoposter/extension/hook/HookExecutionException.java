package aparmar2000.xenforoposter.extension.hook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;

@Getter
public class HookExecutionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final String extensionId;
	private final String methodName;

	public HookExecutionException(@NotNull String extensionId,
			@NotNull String methodName,
			@NotNull String message,
			@Nullable Throwable cause) {
		super(message, cause);
		this.extensionId = extensionId;
		this.methodName = methodName;
	}
}
