package aparmar2000.xenforoposter.scheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.inject.Inject;

import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.PollRecord;
import aparmar2000.xenforoposter.model.ScheduledJob;
import aparmar2000.xenforoposter.model.ScrapedThreadData;
import aparmar2000.xenforoposter.model.ThreadPost;
import aparmar2000.xenforoposter.model.conditions.ConditionResult;
import aparmar2000.xenforoposter.model.conditions.ConditionType;
import aparmar2000.xenforoposter.model.conditions.EvaluationContext;
import aparmar2000.xenforoposter.model.conditions.PostCondition;
import aparmar2000.xenforoposter.security.SafetyRateLimiter;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeProcessor;
import aparmar2000.xenforoposter.web.XenForoWebClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchedulerEngine {
	@Getter
	private final SafetyRateLimiter rateLimiter;
	@Getter
	private final JobStorageService storageService;
	private final XenForoWebClient webClient;
	private final BbCodeProcessor bbCodeProcessor;

	private final List<ScheduledJob> jobs = new CopyOnWriteArrayList<>();
	private final Map<String, ForumProfile> profiles = new ConcurrentHashMap<>();
	private final List<JobUpdateListener> listeners = new CopyOnWriteArrayList<>();

	private final Map<String, ScrapedThreadData> threadDataCache = new ConcurrentHashMap<>();
	private final Map<String, Integer> pendingPageRequests = new ConcurrentHashMap<>();

	private ScheduledExecutorService executor;
	private boolean running = false;

	public interface JobUpdateListener {
		void onJobsChanged();
		void onJobUpdated(@NotNull ScheduledJob job);
	}

	public SchedulerEngine(@NotNull SafetyRateLimiter rateLimiter,
			@NotNull JobStorageService storageService,
			@NotNull XenForoWebClient webClient) {
		this(rateLimiter, storageService, webClient, null);
	}

	@Inject
	public SchedulerEngine(@NotNull SafetyRateLimiter rateLimiter,
			@NotNull JobStorageService storageService,
			@NotNull XenForoWebClient webClient,
			@Nullable BbCodeProcessor bbCodeProcessor) {
		this.rateLimiter = rateLimiter;
		this.storageService = storageService;
		this.webClient = webClient;
		this.bbCodeProcessor = bbCodeProcessor;

		// Load initial state
		loadState();
	}

	public synchronized void start() {
		if (running) {
			return;
		}
		running = true;
		executor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "SchedulerEngine-Worker");
			t.setDaemon(true);
			return t;
		});

		// Run evaluation loop every 3 seconds
		executor.scheduleWithFixedDelay(this::evaluateAllJobs, 1, 3, TimeUnit.SECONDS);
		log.info("SchedulerEngine started");
	}

	public synchronized void stop() {
		if (!running) {
			return;
		}
		running = false;
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		log.info("SchedulerEngine stopped");
	}

	public boolean isRunning() {
		return running;
	}

	public void addListener(@NotNull JobUpdateListener listener) {
		listeners.add(listener);
	}

	public void removeListener(@NotNull JobUpdateListener listener) {
		listeners.remove(listener);
	}

	public List<ScheduledJob> getJobs() {
		return Collections.unmodifiableList(jobs);
	}

	public Collection<ForumProfile> getProfiles() {
		return Collections.unmodifiableCollection(profiles.values());
	}

	@Nullable
	public ForumProfile getProfile(@NotNull String profileId) {
		return profiles.get(profileId);
	}

	public void addOrUpdateJob(@NotNull ScheduledJob job) {
		int index = findJobIndex(job.getId());
		if (index >= 0) {
			jobs.set(index, job);
		} else {
			jobs.add(job);
		}
		persistJobs();
		notifyJobUpdated(job);
		notifyJobsChanged();
	}

	public void deleteJob(@NotNull String jobId) {
		jobs.removeIf(j -> j.getId().equals(jobId));
		persistJobs();
		notifyJobsChanged();
	}

	public void addOrUpdateProfile(@NotNull ForumProfile profile) {
		profiles.put(profile.getId(), profile);
		persistProfiles();
		notifyJobsChanged();
	}

	public void deleteProfile(@NotNull String profileId) {
		profiles.remove(profileId);
		persistProfiles();
		notifyJobsChanged();
	}

	private void loadState() {
		List<ForumProfile> loadedProfiles = storageService.loadProfiles();
		for (ForumProfile p : loadedProfiles) {
			profiles.put(p.getId(), p);
		}

		List<ScheduledJob> loadedJobs = storageService.loadJobs();
		jobs.addAll(loadedJobs);
	}

	public void persistJobs() {
		storageService.saveJobs(new ArrayList<>(jobs));
	}

	public void persistProfiles() {
		storageService.saveProfiles(new ArrayList<>(profiles.values()));
	}

	private int findJobIndex(String jobId) {
		for (int i = 0; i < jobs.size(); i++) {
			if (jobs.get(i).getId().equals(jobId)) {
				return i;
			}
		}
		return -1;
	}

	public void evaluateAllJobs() {
		if (!running) {
			return;
		}

		List<ScheduledJob> activeJobs = new ArrayList<>();
		for (ScheduledJob job : jobs) {
			if (job.getStatus().isActive()) {
				activeJobs.add(job);
			}
		}

		if (activeJobs.isEmpty()) {
			return;
		}

		// Sort active jobs by priority (descending weight) then by lastPollTime ascending
		activeJobs.sort((a, b) -> {
			int weightCmp = Integer.compare(b.getPriority().getWeight(), a.getPriority().getWeight());
			if (weightCmp != 0) {
				return weightCmp;
			}
			Instant aLast = a.getLastPollTime() != null ? a.getLastPollTime() : Instant.EPOCH;
			Instant bLast = b.getLastPollTime() != null ? b.getLastPollTime() : Instant.EPOCH;
			return aLast.compareTo(bLast);
		});

		for (ScheduledJob job : activeJobs) {
			try {
				evaluateSingleJob(job);
			} catch (Exception e) {
				log.error("Error evaluating job {}", job.getName(), e);
			}
		}
	}

	public void evaluateSingleJob(@NotNull ScheduledJob job) {
		ForumProfile profile = profiles.get(job.getForumProfileId());
		if (profile == null) {
			updateJobStatus(job, ScheduledJob.JobStatus.FAILED, "Forum profile not found: " + job.getForumProfileId(), null);
			return;
		}

		EvaluationContext localContext = EvaluationContext.builder()
				.evaluationTime(Instant.now())
				.forumProfile(profile)
				.threadMetadata(null)
				.build();

		// --- Evaluate LOCAL conditions
		List<PostCondition> localConditions = job.getConditions().stream()
				.filter(c -> c.getType() == ConditionType.LOCAL)
				.collect(java.util.stream.Collectors.toList());

		for (PostCondition cond : localConditions) {
			ConditionResult result = cond.evaluate(localContext);
			if (!result.isSatisfied()) {
				updateJobStatus(job, ScheduledJob.JobStatus.WAITING_LOCAL_CONDITIONS, result.getMessage(), null);
				return;
			}
		}

		// --- Check if there are thread-dependent conditions
		List<PostCondition> threadConditions = job.getConditions().stream()
				.filter(c -> c.getType() == ConditionType.THREAD_DEPENDENT)
				.collect(Collectors.toList());

		if (threadConditions.isEmpty()) {
			attemptPost(job, profile, null);
			return;
		}

		// --- Obtain / Refresh Scraped Thread Data
		ScrapedThreadData threadData = threadDataCache.get(job.getThreadUrl());
		Integer requestedPage = pendingPageRequests.get(job.getId());

		boolean needsFetch = (threadData == null) || (requestedPage != null && !threadData.hasPage(requestedPage));

		if (needsFetch) {
			String domain = profile.getDomain();
			if (!rateLimiter.canPollWebsite(domain)) {
				long wait = rateLimiter.getSecondsUntilWebsitePollReady(domain);
				updateJobStatus(job, ScheduledJob.JobStatus.WAITING_THREAD_POLL,
						String.format("Waiting for domain scrape cooldown (%s): %ds remaining", domain, wait), null);
				return;
			}

			if (!rateLimiter.canPollThread(job.getThreadUrl(), job.getCustomPollIntervalSeconds())) {
				long wait = rateLimiter.getSecondsUntilThreadPollReady(job.getThreadUrl(), job.getCustomPollIntervalSeconds());
				updateJobStatus(job, ScheduledJob.JobStatus.WAITING_THREAD_POLL,
						String.format("Waiting for thread poll interval: %ds remaining", wait), null);
				return;
			}

			try {
				if (threadData == null) {
					threadData = webClient.fetchThreadData(job.getThreadUrl());
					threadDataCache.put(job.getThreadUrl(), threadData);
				} else if (requestedPage != null && !threadData.hasPage(requestedPage)) {
					List<ThreadPost> posts = webClient.fetchThreadPage(job.getThreadUrl(), requestedPage);
					threadData = threadData.withPage(requestedPage, posts);
					threadDataCache.put(job.getThreadUrl(), threadData);
					pendingPageRequests.remove(job.getId());
				}
				rateLimiter.recordWebsitePoll(domain);
				rateLimiter.recordThreadPoll(job.getThreadUrl());
			} catch (Exception e) {
				String errorMsg = "Failed to poll thread page: " + e.getMessage();
				PollRecord record = PollRecord.builder()
						.timestamp(Instant.now())
						.pollType(PollRecord.PollType.THREAD_FETCH)
						.success(false)
						.summary(errorMsg)
						.details(e.toString())
						.build();
				updateJobStatus(job, ScheduledJob.JobStatus.WAITING_THREAD_POLL, errorMsg, record);
				return;
			}
		}

		// Check if there is still an unfulfilled page request
		Integer currentReqPage = pendingPageRequests.get(job.getId());
		if (currentReqPage != null && !threadData.hasPage(currentReqPage)) {
			updateJobStatus(job, ScheduledJob.JobStatus.WAITING_THREAD_POLL,
					String.format("Awaiting scrape of requested page %d before condition re-evaluation", currentReqPage), null);
			return;
		}

		// --- Evaluate THREAD-DEPENDENT conditions
		EvaluationContext threadContext = EvaluationContext.builder()
				.evaluationTime(Instant.now())
				.forumProfile(profile)
				.threadMetadata(threadData.getMetadata())
				.threadData(threadData)
				.build();

		for (PostCondition cond : threadConditions) {
			ConditionResult result = cond.evaluate(threadContext);
			if (!result.isSatisfied()) {
				if (result.hasPageRequest()) {
					int reqPage = result.getRequestedPage();
					pendingPageRequests.put(job.getId(), reqPage);
					PollRecord record = PollRecord.builder()
							.timestamp(Instant.now())
							.pollType(PollRecord.PollType.THREAD_FETCH)
							.success(false)
							.summary(String.format("Condition requested page %d: %s", reqPage, cond.getDisplayName()))
							.details(result.getMessage())
							.build();

					updateJobStatus(job, ScheduledJob.JobStatus.WAITING_THREAD_POLL,
							String.format("[%s] requested page %d: %s", cond.getDisplayName(), reqPage, result.getMessage()), record);
					return;
				}

				PollRecord record = PollRecord.builder()
						.timestamp(Instant.now())
						.pollType(PollRecord.PollType.THREAD_FETCH)
						.success(false)
						.summary("Condition failed: " + cond.getDisplayName())
						.details(result.getMessage())
						.build();

				updateJobStatus(job, ScheduledJob.JobStatus.WAITING_THREAD_CONDITIONS,
						result.getMessage(), record);
				return;
			}
		}

		pendingPageRequests.remove(job.getId());

		PollRecord successPoll = PollRecord.builder()
				.timestamp(Instant.now())
				.pollType(PollRecord.PollType.THREAD_FETCH)
				.success(true)
				.summary("All conditions satisfied on thread poll")
				.details(String.format("Pages: %d, Replies: %d", threadData.getTotalPages(), threadData.getMetadata().getReplyCount()))
				.build();

		attemptPost(job, profile, successPoll);
	}

	private void attemptPost(@NotNull ScheduledJob job, @NotNull ForumProfile profile, @Nullable PollRecord priorPollRecord) {
		SafetyRateLimiter.RateLimitCheckResult postCheck = rateLimiter.checkCanPost(job.getThreadUrl());
		if (!postCheck.isAllowed()) {
			updateJobStatus(job, ScheduledJob.JobStatus.READY_TO_POST,
					"Conditions met, waiting on post rate limit: " + postCheck.getReason(), priorPollRecord);
			return;
		}

		// Dispatch post
		updateJobStatus(job, ScheduledJob.JobStatus.POSTING, "Submitting reply to forum...", priorPollRecord);

		String rawBbCode = job.getBbCodeContent();
		String bbCode;
		try {
			bbCode = (bbCodeProcessor != null) ? bbCodeProcessor.processForPost(rawBbCode) : rawBbCode;
		} catch (Exception e) {
			log.error("Failed to process BBCode at post-time for job {}", job.getName(), e);
			PollRecord hookFailRecord = PollRecord.builder()
					.timestamp(Instant.now())
					.pollType(PollRecord.PollType.POST_SUBMISSION)
					.success(false)
					.summary("Post cancelled: Extension hook failed during post processing")
					.details(e.getMessage())
					.build();
			updateJobStatus(job, ScheduledJob.JobStatus.FAILED,
					"Extension hook error during post processing: " + e.getMessage(), hookFailRecord);
			return;
		}

		XenForoWebClient.PostSubmissionResult result = webClient.submitReply(profile, job.getThreadUrl(), bbCode);
		if (result.isSuccessful()) {
			rateLimiter.recordPostDispatched(job.getThreadUrl());

			PollRecord postRecord = PollRecord.builder()
					.timestamp(Instant.now())
					.pollType(PollRecord.PollType.POST_SUBMISSION)
					.success(true)
					.summary("Post published successfully")
					.details(result.getMessage())
					.build();

			ScheduledJob completedJob = job.toBuilder()
					.status(ScheduledJob.JobStatus.COMPLETED)
					.currentStatusReason("Post published successfully: " + result.getMessage())
					.completedAt(Instant.now())
					.pollRecord(postRecord)
					.build();

			int idx = findJobIndex(job.getId());
			if (idx >= 0) {
				jobs.set(idx, completedJob);
			}
			persistJobs();
			notifyJobUpdated(completedJob);
			notifyJobsChanged();
		} else {
			PollRecord postFail = PollRecord.builder()
					.timestamp(Instant.now())
					.pollType(PollRecord.PollType.POST_SUBMISSION)
					.success(false)
					.summary("Post submission rejected by forum")
					.details(result.getMessage())
					.build();

			updateJobStatus(job, ScheduledJob.JobStatus.FAILED,
					"Submission failed: " + result.getMessage(), postFail);
		}
	}

	private void updateJobStatus(@NotNull ScheduledJob job,
			@NotNull ScheduledJob.JobStatus newStatus,
			@NotNull String reason,
			@Nullable PollRecord pollRecord) {
		ScheduledJob.ScheduledJobBuilder builder = job.toBuilder()
				.status(newStatus)
				.currentStatusReason(reason)
				.lastPollTime(Instant.now())
				.lastPollStatus(newStatus.getDisplayName());

		if (pollRecord != null) {
			builder.pollRecord(pollRecord);
		}

		ScheduledJob updated = builder.build();
		int idx = findJobIndex(job.getId());
		if (idx >= 0) {
			jobs.set(idx, updated);
		}
		persistJobs();
		notifyJobUpdated(updated);
		notifyJobsChanged();
	}

	private void notifyJobsChanged() {
		for (JobUpdateListener l : listeners) {
			try {
				l.onJobsChanged();
			} catch (Exception e) {
				log.error("Error in onJobsChanged listener", e);
			}
		}
	}

	private void notifyJobUpdated(ScheduledJob job) {
		for (JobUpdateListener l : listeners) {
			try {
				l.onJobUpdated(job);
			} catch (Exception e) {
				log.error("Error in onJobUpdated listener", e);
			}
		}
	}

	@Nullable
	public ScrapedThreadData getCachedThreadData(@NotNull String threadUrl) {
		return threadDataCache.get(threadUrl);
	}

	public void setCachedThreadData(@NotNull String threadUrl, @NotNull ScrapedThreadData data) {
		threadDataCache.put(threadUrl, data);
	}

	@Nullable
	public Integer getPendingPageRequest(@NotNull String jobId) {
		return pendingPageRequests.get(jobId);
	}

	public void clearCache() {
		threadDataCache.clear();
		pendingPageRequests.clear();
	}
}
