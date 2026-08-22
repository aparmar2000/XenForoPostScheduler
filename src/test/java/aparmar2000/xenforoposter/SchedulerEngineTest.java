package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.ScheduledJob;
import aparmar2000.xenforoposter.model.ScrapedThreadData;
import aparmar2000.xenforoposter.model.ThreadMetadata;
import aparmar2000.xenforoposter.model.ThreadPost;
import aparmar2000.xenforoposter.model.conditions.AntiNecropostCondition;
import aparmar2000.xenforoposter.model.conditions.DateRangeCondition;
import aparmar2000.xenforoposter.model.conditions.PostGapCondition;
import aparmar2000.xenforoposter.scheduler.JobStorageService;
import aparmar2000.xenforoposter.scheduler.SchedulerEngine;
import aparmar2000.xenforoposter.security.SafetyRateLimiter;
import aparmar2000.xenforoposter.web.XenForoWebClient;

@ExtendWith(MockitoExtension.class)
class SchedulerEngineTest {

	@Mock
	private SafetyRateLimiter rateLimiter;

	@Mock
	private JobStorageService storageService;

	@Mock
	private XenForoWebClient webClient;

	@Mock
	private SchedulerEngine.JobUpdateListener jobListener;

	private SchedulerEngine engine;
	private ForumProfile profile;

	@BeforeEach
	void setUp() {
		when(storageService.loadProfiles()).thenReturn(new ArrayList<>());
		when(storageService.loadJobs()).thenReturn(new ArrayList<>());

		engine = new SchedulerEngine(rateLimiter, storageService, webClient);
		engine.addListener(jobListener);

		profile = ForumProfile.builder()
				.id("profile-1")
				.name("Test Forum")
				.baseUrl("https://forum.example.com")
				.username("TestUser")
				.build();
		engine.addOrUpdateProfile(profile);
	}

	@Test
	@DisplayName("Unmet local condition should set status to WAITING_LOCAL_CONDITIONS without network scraping")
	void testLocalConditionShortCircuit() {
		LocalDate futureDate = LocalDate.now().plusDays(2);

		ScheduledJob job = ScheduledJob.builder()
				.id("job-1")
				.name("Future Job")
				.forumProfileId(profile.getId())
				.threadUrl("https://forum.example.com/threads/123")
				.bbCodeContent("[B]Hello future[/B]")
				.conditions(List.of(
						DateRangeCondition.builder().startDate(futureDate).build(),
						PostGapCondition.builder().minPostsSinceUser(5).build()
						))
				.build();

		engine.addOrUpdateJob(job);
		engine.evaluateSingleJob(job);

		ScheduledJob updated = engine.getJobs().get(0);
		assertEquals(ScheduledJob.JobStatus.WAITING_LOCAL_CONDITIONS, updated.getStatus());
		assertTrue(updated.getCurrentStatusReason().contains("Waiting for start date"));

		// No thread poll should have occurred on web client
		verifyNoInteractions(webClient);
		assertEquals(0, updated.getPollHistory().size());
		verify(jobListener, atLeastOnce()).onJobUpdated(any());
	}

	@Test
	@DisplayName("Condition requesting page should record pending request and avoid re-evaluating until page is polled")
	void testConditionPageRequestAndDeferredEvaluation() {
		String threadUrl = "https://forum.example.com/threads/456";

		ThreadMetadata meta = ThreadMetadata.builder()
				.threadUrl(threadUrl)
				.title("Multi-page Thread")
				.canReply(true)
				.totalPages(3)
				.build();

		ScrapedThreadData initialData = ScrapedThreadData.builder()
				.metadata(meta)
				.page(1, List.of(
						ThreadPost.builder()
						.postId("1")
						.author("OtherUser")
						.timestamp(Instant.now().minus(Duration.ofDays(10)))
						.pageNumber(1)
						.build()
						))
				.build();

		engine.setCachedThreadData(threadUrl, initialData);

		ScheduledJob job = ScheduledJob.builder()
				.id("job-2")
				.name("Necropost Monitored Job")
				.forumProfileId(profile.getId())
				.threadUrl(threadUrl)
				.bbCodeContent("Hello from page 3")
				.conditions(List.of(
						AntiNecropostCondition.builder()
						.maxInactiveDays(30)
						.allowAuthorExemption(false)
						.build()
						))
				.build();

		engine.addOrUpdateJob(job);

		// Condition fails requesting page 3
		engine.evaluateSingleJob(job);

		assertEquals(Integer.valueOf(3), engine.getPendingPageRequest(job.getId()));
		ScheduledJob afterFirstEval = engine.getJobs().get(0);
		assertEquals(ScheduledJob.JobStatus.WAITING_THREAD_POLL, afterFirstEval.getStatus());
		assertTrue(afterFirstEval.getCurrentStatusReason().contains("requested page 3"));

		// Mock page 3 being supplied/scraped
		ScrapedThreadData withPage3 = initialData.withPage(3, List.of(
				ThreadPost.builder()
				.postId("30")
				.author("RecentUser")
				.timestamp(Instant.now().minus(Duration.ofHours(2)))
				.pageNumber(3)
				.build()
				));
		engine.setCachedThreadData(threadUrl, withPage3);

		when(rateLimiter.checkCanPost(threadUrl)).thenReturn(SafetyRateLimiter.RateLimitCheckResult.allowed());
		when(webClient.submitReply(profile, threadUrl, "Hello from page 3"))
		.thenReturn(XenForoWebClient.PostSubmissionResult.success("Published OK"));

		// Now that page 3 is present, condition passes and post is dispatched
		engine.evaluateSingleJob(afterFirstEval);

		assertNull(engine.getPendingPageRequest(job.getId()));
		ScheduledJob afterSecondEval = engine.getJobs().get(0);
		assertEquals(ScheduledJob.JobStatus.COMPLETED, afterSecondEval.getStatus());
		verify(webClient).submitReply(profile, threadUrl, "Hello from page 3");
		verify(rateLimiter).recordPostDispatched(threadUrl);
	}

	@Test
	@DisplayName("Should successfully post when all conditions pass and rate limit allows")
	void testSuccessfulPostSubmission() {
		String threadUrl = "https://forum.example.com/threads/789";

		ScheduledJob job = ScheduledJob.builder()
				.id("job-post-ok")
				.name("Instant Post Job")
				.forumProfileId(profile.getId())
				.threadUrl(threadUrl)
				.bbCodeContent("Direct post content")
				.conditions(Collections.emptyList())
				.build();

		engine.addOrUpdateJob(job);

		when(rateLimiter.checkCanPost(threadUrl)).thenReturn(SafetyRateLimiter.RateLimitCheckResult.allowed());
		when(webClient.submitReply(profile, threadUrl, "Direct post content"))
		.thenReturn(XenForoWebClient.PostSubmissionResult.success("Created post #99"));

		engine.evaluateSingleJob(job);

		ScheduledJob completed = engine.getJobs().get(0);
		assertEquals(ScheduledJob.JobStatus.COMPLETED, completed.getStatus());
		assertTrue(completed.getCurrentStatusReason().contains("Created post #99"));
		assertNotNull(completed.getCompletedAt());

		verify(webClient).submitReply(profile, threadUrl, "Direct post content");
		verify(rateLimiter).recordPostDispatched(threadUrl);
		verify(storageService, atLeastOnce()).saveJobs(any());
		verify(jobListener, atLeastOnce()).onJobUpdated(any());
	}

	@Test
	@DisplayName("Should wait when post rate limiter denies dispatch")
	void testPostRateLimitBlocking() {
		String threadUrl = "https://forum.example.com/threads/rate-limited";

		ScheduledJob job = ScheduledJob.builder()
				.id("job-rate-limit")
				.name("Rate Limited Job")
				.forumProfileId(profile.getId())
				.threadUrl(threadUrl)
				.bbCodeContent("Content waiting for cooldown")
				.conditions(Collections.emptyList())
				.build();

		engine.addOrUpdateJob(job);

		when(rateLimiter.checkCanPost(threadUrl))
		.thenReturn(SafetyRateLimiter.RateLimitCheckResult.rejected("Global cooldown active (45s)"));

		engine.evaluateSingleJob(job);

		ScheduledJob waiting = engine.getJobs().get(0);
		assertEquals(ScheduledJob.JobStatus.READY_TO_POST, waiting.getStatus());
		assertTrue(waiting.getCurrentStatusReason().contains("Global cooldown active"));

		verify(webClient, never()).submitReply(any(), any(), any());
		verify(rateLimiter, never()).recordPostDispatched(any());
	}

	@Test
	@DisplayName("Should mark job as FAILED when forum rejects post submission")
	void testPostSubmissionRejection() {
		String threadUrl = "https://forum.example.com/threads/locked-thread";

		ScheduledJob job = ScheduledJob.builder()
				.id("job-rejected")
				.name("Rejected Job")
				.forumProfileId(profile.getId())
				.threadUrl(threadUrl)
				.bbCodeContent("Content for locked thread")
				.conditions(Collections.emptyList())
				.build();

		engine.addOrUpdateJob(job);

		when(rateLimiter.checkCanPost(threadUrl)).thenReturn(SafetyRateLimiter.RateLimitCheckResult.allowed());
		when(webClient.submitReply(profile, threadUrl, "Content for locked thread"))
		.thenReturn(XenForoWebClient.PostSubmissionResult.failure("Thread is locked"));

		engine.evaluateSingleJob(job);

		ScheduledJob failed = engine.getJobs().get(0);
		assertEquals(ScheduledJob.JobStatus.FAILED, failed.getStatus());
		assertTrue(failed.getCurrentStatusReason().contains("Thread is locked"));

		verify(rateLimiter, never()).recordPostDispatched(any());
	}

	@Test
	@DisplayName("Evaluating job with non-existent forum profile should fail gracefully")
	void testMissingForumProfileFailsGracefully() {
		ScheduledJob job = ScheduledJob.builder()
				.id("job-no-profile")
				.name("Missing Profile Job")
				.forumProfileId("non-existent-profile-id")
				.threadUrl("https://forum.example.com/threads/1")
				.bbCodeContent("Hello")
				.conditions(Collections.emptyList())
				.build();

		engine.addOrUpdateJob(job);
		engine.evaluateSingleJob(job);

		ScheduledJob failed = engine.getJobs().get(0);
		assertEquals(ScheduledJob.JobStatus.FAILED, failed.getStatus());
		assertTrue(failed.getCurrentStatusReason().contains("Forum profile not found"));
		verifyNoInteractions(webClient);
	}
}
