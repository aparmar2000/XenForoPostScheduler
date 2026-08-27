package aparmar2000.xenforoposter.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.Gson;

import aparmar2000.xenforoposter.extension.Extension;
import aparmar2000.xenforoposter.extension.ExtensionContext;
import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.InternalExtensionContext;
import aparmar2000.xenforoposter.extension.hook.Hook;
import aparmar2000.xenforoposter.extension.hook.HookPhase;
import aparmar2000.xenforoposter.extension.hook.PreTokenizeStringEvent;
import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.ScheduledJob;
import aparmar2000.xenforoposter.security.SafetyRateLimiter;
import aparmar2000.xenforoposter.settings.SettingsHolder;
import aparmar2000.xenforoposter.syntax.DefaultTagDefinitions;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAstParser;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeProcessor;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinitionRegistry;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;
import aparmar2000.xenforoposter.web.XenForoWebClient;

@ExtendWith(MockitoExtension.class)
class SchedulerEngineHooksTest {

	@Mock
	private SafetyRateLimiter rateLimiter;
	@Mock
	private JobStorageService storageService;
	@Mock
	private XenForoWebClient webClient;

	private ExtensionManager extensionManager;
	private BbCodeProcessor processor;
	private SchedulerEngine engine;
	private ForumProfile profile;

	@BeforeEach
	void setUp() throws IOException {
		when(storageService.loadProfiles()).thenReturn(new ArrayList<>());
		when(storageService.loadJobs()).thenReturn(new ArrayList<>());

		Path tempBaseDir = Files.createTempDirectory("xf_engine_hook_test");
		SettingsHolder.Factory settingsHolderFactory = mock(SettingsHolder.Factory.class);
		when(settingsHolderFactory.create(any()))
				.thenReturn(mock(SettingsHolder.class));
		BbCodeTagDefinitionRegistry bbRegistry = new BbCodeTagDefinitionRegistry();
		HtmlTagDefinitionRegistry htmlRegistry = new HtmlTagDefinitionRegistry();
		DefaultTagDefinitions.registerBaseTags(bbRegistry, htmlRegistry);

		InternalExtensionContext.Factory contextFactory = (dataDir, extId) ->
				new InternalExtensionContext(dataDir, extId, settingsHolderFactory, bbRegistry, htmlRegistry);

		extensionManager = new ExtensionManager(tempBaseDir, new Gson(), contextFactory);
		BbCodeTokenizer tokenizer = new BbCodeTokenizer(bbRegistry);
		BbCodeAstParser parser = new BbCodeAstParser(bbRegistry, tokenizer);
		processor = new BbCodeProcessor(extensionManager, tokenizer, parser);

		engine = new SchedulerEngine(rateLimiter, storageService, webClient, processor);

		profile = ForumProfile.builder()
				.id("prof-1")
				.name("Test Forum")
				.baseUrl("https://forum.example.com")
				.username("User")
				.build();
		engine.addOrUpdateProfile(profile);
	}

	static class TextModifierExtension implements Extension {
		@Override public @NotNull String getId() { return "ext.modifier"; }
		@Override public @NotNull String getName() { return "Modifier"; }
		@Override public @NotNull String getVersion() { return "1.0"; }
		@Override public @NotNull String getAuthor() { return "test"; }
		@Override public @NotNull String getDescription() { return "test"; }
		@Override public void initialize(@NotNull ExtensionContext context) {}

		@Hook(phases = {HookPhase.POST})
		public void onPost(PreTokenizeStringEvent event) {
			event.setText("[B]" + event.getText() + "[/B]");
		}
	}

	@Test
	@DisplayName("SchedulerEngine should process BBCode through post hooks before submitting to web client")
	void testPostSubmissionWithHooks() {
		extensionManager.registerInternalExtension(new TextModifierExtension());
		extensionManager.loadAllExtensions();

		String threadUrl = "https://forum.example.com/threads/99";
		ScheduledJob job = ScheduledJob.builder()
				.id("job-hook-test")
				.name("Hook Job")
				.forumProfileId(profile.getId())
				.threadUrl(threadUrl)
				.bbCodeContent("Hello raw text")
				.conditions(Collections.emptyList())
				.build();

		engine.addOrUpdateJob(job);

		when(rateLimiter.checkCanPost(threadUrl)).thenReturn(SafetyRateLimiter.RateLimitCheckResult.allowed());
		when(webClient.submitReply(eq(profile), eq(threadUrl), eq("[B]Hello raw text[/B]")))
				.thenReturn(XenForoWebClient.PostSubmissionResult.success("Post #123 created"));

		engine.evaluateSingleJob(job);

		verify(webClient).submitReply(eq(profile), eq(threadUrl), eq("[B]Hello raw text[/B]"));
	}

	static class FaultyPostExtension implements Extension {
		@Override public @NotNull String getId() { return "ext.faulty"; }
		@Override public @NotNull String getName() { return "Faulty"; }
		@Override public @NotNull String getVersion() { return "1.0"; }
		@Override public @NotNull String getAuthor() { return "test"; }
		@Override public @NotNull String getDescription() { return "test"; }
		@Override public void initialize(@NotNull ExtensionContext context) {}

		@Hook(phases = {HookPhase.POST})
		public void onPost(PreTokenizeStringEvent event) throws Exception {
			throw new RuntimeException("Simulated post hook crash");
		}
	}

	@Test
	@DisplayName("SchedulerEngine should abort and fail job without posting when a post hook throws an exception")
	void testPostBailoutOnHookException() {
		extensionManager.registerInternalExtension(new FaultyPostExtension());
		extensionManager.loadAllExtensions();

		String threadUrl = "https://forum.example.com/threads/100";
		ScheduledJob job = ScheduledJob.builder()
				.id("job-faulty-test")
				.name("Faulty Hook Job")
				.forumProfileId(profile.getId())
				.threadUrl(threadUrl)
				.bbCodeContent("Dangerous raw text")
				.conditions(Collections.emptyList())
				.build();

		engine.addOrUpdateJob(job);

		when(rateLimiter.checkCanPost(threadUrl)).thenReturn(SafetyRateLimiter.RateLimitCheckResult.allowed());

		engine.evaluateSingleJob(job);

		ScheduledJob failedJob = engine.getJobs().stream()
				.filter(j -> j.getId().equals("job-faulty-test"))
				.findFirst()
				.orElseThrow();

		assertEquals(ScheduledJob.JobStatus.FAILED, failedJob.getStatus());
		assertTrue(failedJob.getCurrentStatusReason().contains("Extension hook error during post processing"));
		verify(webClient, never()).submitReply(any(), any(), any());
	}
}
