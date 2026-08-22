package aparmar2000.xenforoposter.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SafetyRateLimiter {
	// Non-bypassable hardcoded safety limits to prevent trivial abuse/spam
	// Probably bypassable with sufficient effort, but so is making a new spam program of one's own
	// Therefore, this only needs to prevent accidental spamming and minimal-effort non-technical bad actors
	public static final int MIN_GLOBAL_POST_INTERVAL_SECONDS = 60;
	public static final int MIN_THREAD_POST_INTERVAL_SECONDS = 60 * 5;
	public static final int MAX_POSTS_PER_HOUR = 12;
	public static final int MAX_POSTS_PER_DAY = 60;

	public static final int MIN_WEBSITE_POLL_INTERVAL_SECONDS = 30;
	public static final int MIN_THREAD_POLL_INTERVAL_SECONDS = 90;

	private final List<Instant> globalPostTimestamps = new ArrayList<>();
	private final Map<String, Instant> lastThreadPostTimes = new ConcurrentHashMap<>();
	private final Map<String, Instant> lastWebsitePollTimes = new ConcurrentHashMap<>();
	private final Map<String, Instant> lastThreadPollTimes = new ConcurrentHashMap<>();

	private final Object lock = new Object();

	public boolean canPollWebsite(@NotNull String domain) {
		Instant last = lastWebsitePollTimes.get(domain.toLowerCase());
		if (last == null) {
			return true;
		}
		return Duration.between(last, Instant.now()).getSeconds() >= MIN_WEBSITE_POLL_INTERVAL_SECONDS;
	}

	public long getSecondsUntilWebsitePollReady(@NotNull String domain) {
		Instant last = lastWebsitePollTimes.get(domain.toLowerCase());
		if (last == null) {
			return 0;
		}
		long elapsed = Duration.between(last, Instant.now()).getSeconds();
		return Math.max(0, MIN_WEBSITE_POLL_INTERVAL_SECONDS - elapsed);
	}

	public boolean canPollThread(@NotNull String threadUrl, int customMinIntervalSeconds) {
		int requiredInterval = Math.max(MIN_THREAD_POLL_INTERVAL_SECONDS, customMinIntervalSeconds);
		Instant last = lastThreadPollTimes.get(normalizeThreadUrl(threadUrl));
		if (last == null) {
			return true;
		}
		return Duration.between(last, Instant.now()).getSeconds() >= requiredInterval;
	}

	public long getSecondsUntilThreadPollReady(@NotNull String threadUrl, int customMinIntervalSeconds) {
		int requiredInterval = Math.max(MIN_THREAD_POLL_INTERVAL_SECONDS, customMinIntervalSeconds);
		Instant last = lastThreadPollTimes.get(normalizeThreadUrl(threadUrl));
		if (last == null) {
			return 0;
		}
		long elapsed = Duration.between(last, Instant.now()).getSeconds();
		return Math.max(0, requiredInterval - elapsed);
	}

	public void recordWebsitePoll(@NotNull String domain) {
		lastWebsitePollTimes.put(domain.toLowerCase(), Instant.now());
	}

	public void recordThreadPoll(@NotNull String threadUrl) {
		Instant now = Instant.now();
		lastThreadPollTimes.put(normalizeThreadUrl(threadUrl), now);
	}

	public RateLimitCheckResult checkCanPost(@NotNull String threadUrl) {
		synchronized (lock) {
			Instant now = Instant.now();

			// Clean up old timestamps (> 24 hours)
			globalPostTimestamps.removeIf(t -> Duration.between(t, now).toHours() >= 24);

			// Check global post interval
			if (!globalPostTimestamps.isEmpty()) {
				Instant lastPost = globalPostTimestamps.get(globalPostTimestamps.size() - 1);
				long elapsedSeconds = Duration.between(lastPost, now).getSeconds();
				if (elapsedSeconds < MIN_GLOBAL_POST_INTERVAL_SECONDS) {
					long waitSeconds = MIN_GLOBAL_POST_INTERVAL_SECONDS - elapsedSeconds;
					return RateLimitCheckResult.rejected(String.format(
							"Global posting cooldown active: wait %d more seconds (min interval %ds)",
							waitSeconds, MIN_GLOBAL_POST_INTERVAL_SECONDS));
				}
			}

			// Check per-thread post interval
			String normalizedThread = normalizeThreadUrl(threadUrl);
			Instant lastThreadPost = lastThreadPostTimes.get(normalizedThread);
			if (lastThreadPost != null) {
				long elapsedSeconds = Duration.between(lastThreadPost, now).getSeconds();
				if (elapsedSeconds < MIN_THREAD_POST_INTERVAL_SECONDS) {
					long waitSeconds = MIN_THREAD_POST_INTERVAL_SECONDS - elapsedSeconds;
					return RateLimitCheckResult.rejected(String.format(
							"Thread posting cooldown active: wait %d more seconds (min interval %ds)",
							waitSeconds, MIN_THREAD_POST_INTERVAL_SECONDS));
				}
			}

			// Check hourly post limit
			long postsLastHour = globalPostTimestamps.stream()
					.filter(t -> Duration.between(t, now).toMinutes() < 60)
					.count();
			if (postsLastHour >= MAX_POSTS_PER_HOUR) {
				return RateLimitCheckResult.rejected(String.format(
						"Hourly limit reached (%d/%d posts in last 60 minutes)",
						postsLastHour, MAX_POSTS_PER_HOUR));
			}

			// Check daily post limit
			long postsLastDay = globalPostTimestamps.size();
			if (postsLastDay >= MAX_POSTS_PER_DAY) {
				return RateLimitCheckResult.rejected(String.format(
						"Daily limit reached (%d/%d posts in last 24 hours)",
						postsLastDay, MAX_POSTS_PER_DAY));
			}

			return RateLimitCheckResult.allowed();
		}
	}

	public void recordPostDispatched(@NotNull String threadUrl) {
		synchronized (lock) {
			Instant now = Instant.now();
			globalPostTimestamps.add(now);
			lastThreadPostTimes.put(normalizeThreadUrl(threadUrl), now);
			log.info("Recorded post dispatch. Global 24h count: {}, 1h count: {}",
					globalPostTimestamps.size(),
					globalPostTimestamps.stream().filter(t -> Duration.between(t, now).toMinutes() < 60).count());
		}
	}

	private String normalizeThreadUrl(String url) {
		return url != null ? url.trim().toLowerCase().replaceAll("/+$", "") : "";
	}

	@lombok.Value
	public static class RateLimitCheckResult {
		boolean allowed;
		String reason;

		public static RateLimitCheckResult allowed() {
			return new RateLimitCheckResult(true, "Rate limits satisfied");
		}

		public static RateLimitCheckResult rejected(String reason) {
			return new RateLimitCheckResult(false, reason);
		}
	}
}
