package aparmar2000.xenforoposter.security;

import java.io.Serializable;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

/**
 * A wrapper for sensitive string values (such as passwords and session tokens)
 */
public final class SecureString implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String clearText;

	private SecureString(@Nullable String clearText) {
		this.clearText = clearText;
	}

	public static @Nullable SecureString of(@Nullable String value) {
		if (value == null) {
			return null;
		}
		return new SecureString(value);
	}
	public static @Nullable SecureString of(char @Nullable [] chars) {
		if (chars == null) {
			return null;
		}
		return new SecureString(new String(chars));
	}

	public @Nullable String getClearText() {
		return clearText;
	}

	public boolean isEmpty() {
		return clearText == null || clearText.isEmpty();
	}
	public boolean isBlank() {
		return clearText == null || clearText.isBlank();
	}
	public int length() {
		return clearText != null ? clearText.length() : 0;
	}

	@Override
	public String toString() {
		return getClass().getName() + '@' + Integer.toHexString(hashCode());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SecureString)) {
			return false;
		}
		SecureString that = (SecureString) o;
		return Objects.equals(clearText, that.clearText);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(clearText);
	}
}
