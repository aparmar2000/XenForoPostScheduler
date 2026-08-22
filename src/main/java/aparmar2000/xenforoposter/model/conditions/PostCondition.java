package aparmar2000.xenforoposter.model.conditions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.model.ScrapedThreadData;
import aparmar2000.xenforoposter.model.ThreadMetadata;
import aparmar2000.xenforoposter.model.ThreadPost;

public abstract class PostCondition {
	public abstract @NotNull String getId();
	public abstract @NotNull String getDisplayName();
	public abstract @NotNull String getDescription();
	public abstract @NotNull ConditionType getType();

	/**
	 * Evaluates this condition against the provided evaluation context.
	 * Delegates to {@link #innerEvaluate(EvaluationContext)} and unwraps any thrown {@link ConditionEvaluationException}.
	 *
	 * @param context the evaluation context
	 * @return the resulting {@link ConditionResult}
	 */
	public @NotNull ConditionResult evaluate(@NotNull EvaluationContext context) {
		try {
			return innerEvaluate(context);
		} catch (ConditionEvaluationException e) {
			return e.getResult();
		}
	}

	/**
	 * Subclasses implement evaluation logic here.
	 * Throw {@link ConditionEvaluationException} or call {@link #fail(String)} / {@link #failWithPageRequest(String, int)}
	 * to immediately short-circuit evaluation.
	 *
	 * @param context the evaluation context
	 * @return the passing {@link ConditionResult}
	 * @throws ConditionEvaluationException on condition failure or unmet prerequisite
	 */
	protected abstract @NotNull ConditionResult innerEvaluate(@NotNull EvaluationContext context) throws ConditionEvaluationException;

	// Fail Helpers
	protected void fail(@NotNull String reason) {
		throw new ConditionEvaluationException(ConditionResult.fail(reason));
	}

	protected void fail(@NotNull String reason, @Nullable Instant expiry) {
		throw new ConditionEvaluationException(ConditionResult.fail(reason, expiry));
	}

	protected void fail(@NotNull String reason, @NotNull Duration ttl) {
		throw new ConditionEvaluationException(ConditionResult.fail(reason, ttl));
	}

	protected void failWithPageRequest(@NotNull String reason, int requestedPage) {
		throw new ConditionEvaluationException(ConditionResult.failWithPageRequest(reason, requestedPage));
	}

	protected void failWithPageRequest(@NotNull String reason, int requestedPage, @Nullable Instant expiry) {
		throw new ConditionEvaluationException(ConditionResult.failWithPageRequest(reason, requestedPage, expiry));
	}

	protected void failWithPageRequest(@NotNull String reason, int requestedPage, @NotNull Duration ttl) {
		throw new ConditionEvaluationException(ConditionResult.failWithPageRequest(reason, requestedPage, ttl));
	}

	// Pass Helpers
	protected @NotNull ConditionResult pass(@NotNull String message) {
		return ConditionResult.pass(message);
	}

	protected @NotNull ConditionResult pass(@NotNull String message, @Nullable Instant expiry) {
		return ConditionResult.pass(message, expiry);
	}

	protected @NotNull ConditionResult pass(@NotNull String message, @NotNull Duration ttl) {
		return ConditionResult.pass(message, ttl);
	}

	// Extraction & Requirement helpers
	public @NotNull ThreadMetadata getThreadMetadataOrFail(@NotNull EvaluationContext context) {
		ThreadMetadata metadata = context.getThreadMetadata();
		if (metadata == null) {
			fail("Thread metadata is not available");
		}
		return metadata;
	}

	public @NotNull ScrapedThreadData getThreadDataOrFail(@NotNull EvaluationContext context) {
		ScrapedThreadData threadData = context.getThreadData();
		if (threadData == null) {
			fail("Thread data is not available");
		}
		return threadData;
	}

	public @NotNull String getUsernameOrFail(@NotNull EvaluationContext context) {
		String username = context.getForumProfile().getUsername();
		if (username == null || username.trim().isEmpty()) {
			fail( "Forum profile username is not configured");
		}
		return username.trim();
	}

	public void requirePageLoadedOrFail(@NotNull ScrapedThreadData threadData, int pageNumber) {
		if (!threadData.hasPage(pageNumber)) {
			failWithPageRequest("Requires page %d to be loaded", pageNumber);
		}
	}

	public @NotNull List<ThreadPost> getPagePostsOrFail(@NotNull ScrapedThreadData threadData, int pageNumber) {
		requirePageLoadedOrFail(threadData, pageNumber);
		return threadData.getPostsOnPage(pageNumber);
	}

	public void requireLatestPageLoadedOrFail(@NotNull ScrapedThreadData threadData) {
		int lastPage = threadData.getTotalPages();
		requirePageLoadedOrFail(threadData, lastPage);
	}

	public @NotNull List<ThreadPost> getLatestPagePostsOrFail(@NotNull ScrapedThreadData threadData) {
		int lastPage = threadData.getTotalPages();
		return getPagePostsOrFail(threadData, lastPage);
	}
}
