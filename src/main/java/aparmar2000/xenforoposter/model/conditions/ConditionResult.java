package aparmar2000.xenforoposter.model.conditions;

import java.time.Duration;
import java.time.Instant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ConditionResult {
	boolean satisfied;
	@NotNull String message;
	@NotNull Instant evaluatedAt;
	@Nullable Instant expiry;
	@Nullable Integer requestedPage;

	public static ConditionResult pass(@NotNull String message) {
		return new ConditionResult(true, message, Instant.now(), null, null);
	}

	public static ConditionResult pass(@NotNull String message, @Nullable Instant expiry) {
		return new ConditionResult(true, message, Instant.now(), expiry, null);
	}

	public static ConditionResult pass(@NotNull String message, @NotNull Duration ttl) {
		Instant now = Instant.now();
		return new ConditionResult(true, message, now, now.plus(ttl), null);
	}

	public static ConditionResult fail(@NotNull String reason) {
		return new ConditionResult(false, reason, Instant.now(), null, null);
	}

	public static ConditionResult fail(@NotNull String reason, @Nullable Instant expiry) {
		return new ConditionResult(false, reason, Instant.now(), expiry, null);
	}

	public static ConditionResult fail(@NotNull String reason, @NotNull Duration ttl) {
		Instant now = Instant.now();
		return new ConditionResult(false, reason, now, now.plus(ttl), null);
	}

	public static ConditionResult failWithPageRequest(@NotNull String reason, int requestedPage) {
		return new ConditionResult(false, reason, Instant.now(), null, requestedPage);
	}

	public static ConditionResult failWithPageRequest(@NotNull String reason, int requestedPage, @Nullable Instant expiry) {
		return new ConditionResult(false, reason, Instant.now(), expiry, requestedPage);
	}

	public static ConditionResult failWithPageRequest(@NotNull String reason, int requestedPage, @NotNull Duration ttl) {
		Instant now = Instant.now();
		return new ConditionResult(false, reason, now, now.plus(ttl), requestedPage);
	}

	public boolean hasPageRequest() {
		return requestedPage != null;
	}

	public boolean isExpired() {
		return isExpired(Instant.now());
	}
	public boolean isExpired(@NotNull Instant now) {
		return hasExpiry() && !now.isBefore(expiry);
	}

	public boolean isEligibleForRetest() {
		return !isExpired();
	}
	public boolean isEligibleForRetest(@NotNull Instant now) {
		return !isExpired(now);
	}

	public boolean hasExpiry() {
		return expiry != null;
	}
}

