package aparmar2000.xenforoposter.extension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import aparmar2000.xenforoposter.extension.hook.Hook;
import aparmar2000.xenforoposter.extension.hook.HookExecutionException;
import aparmar2000.xenforoposter.extension.hook.HookPhase;
import aparmar2000.xenforoposter.extension.hook.HookPriority;
import aparmar2000.xenforoposter.extension.hook.PreTokenizeStringEvent;
import aparmar2000.xenforoposter.settings.SettingsHolder;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinitionRegistry;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;

class ExtensionHookSystemTest {

	private ExtensionManager extensionManager;
	private Path tempBaseDir;

	@BeforeEach
	void setUp() throws IOException {
		tempBaseDir = Files.createTempDirectory("xf_hook_test");
		SettingsHolder.Factory settingsHolderFactory = mock(SettingsHolder.Factory.class);
		when(settingsHolderFactory.create(any()))
				.thenReturn(mock(SettingsHolder.class));
		BbCodeTagDefinitionRegistry bbRegistry = new BbCodeTagDefinitionRegistry();
		HtmlTagDefinitionRegistry htmlRegistry = new HtmlTagDefinitionRegistry();

		InternalExtensionContext.Factory contextFactory = (dataDir, extId) ->
				new InternalExtensionContext(dataDir, extId, settingsHolderFactory, bbRegistry, htmlRegistry);

		extensionManager = new ExtensionManager(tempBaseDir, new Gson(), contextFactory);
	}

	static class PriorityAndOrderingExtension implements Extension {
		static final List<String> executionOrder = new ArrayList<>();

		@Override public @NotNull String getId() { return "ext.b_second"; }
		@Override public @NotNull String getName() { return "Ext B"; }
		@Override public @NotNull String getVersion() { return "1.0"; }
		@Override public @NotNull String getAuthor() { return "test"; }
		@Override public @NotNull String getDescription() { return "test"; }
		@Override public void initialize(@NotNull ExtensionContext context) {}

		@Hook(priority = HookPriority.NORMAL)
		public void normalMethod2(PreTokenizeStringEvent event) {
			executionOrder.add("ext.b_second:normalMethod2");
		}

		@Hook(priority = HookPriority.NORMAL)
		public void normalMethod1(PreTokenizeStringEvent event) {
			executionOrder.add("ext.b_second:normalMethod1");
		}

		@Hook(priority = HookPriority.HIGHEST)
		public void highestMethod(PreTokenizeStringEvent event) {
			executionOrder.add("ext.b_second:highestMethod");
		}
	}

	static class FirstExtension implements Extension {
		@Override public @NotNull String getId() { return "ext.a_first"; }
		@Override public @NotNull String getName() { return "Ext A"; }
		@Override public @NotNull String getVersion() { return "1.0"; }
		@Override public @NotNull String getAuthor() { return "test"; }
		@Override public @NotNull String getDescription() { return "test"; }
		@Override public void initialize(@NotNull ExtensionContext context) {}

		@Hook(priority = HookPriority.NORMAL)
		public void normalMethodA(PreTokenizeStringEvent event) {
			PriorityAndOrderingExtension.executionOrder.add("ext.a_first:normalMethodA");
		}

		@Hook(priority = HookPriority.LOWEST)
		public void lowestMethodA(PreTokenizeStringEvent event) {
			PriorityAndOrderingExtension.executionOrder.add("ext.a_first:lowestMethodA");
		}
	}

	@Test
	@DisplayName("Hooks should execute in strict order: priority -> source extension id -> hook method name")
	void testHookOrdering() {
		PriorityAndOrderingExtension.executionOrder.clear();

		extensionManager.registerInternalExtension(new PriorityAndOrderingExtension());
		extensionManager.registerInternalExtension(new FirstExtension());
		extensionManager.loadAllExtensions();

		PreTokenizeStringEvent event = new PreTokenizeStringEvent(HookPhase.PREVIEW, "test string");
		extensionManager.fireHookEvent(event);

		List<String> expected = List.of(
				"ext.b_second:highestMethod",   // HIGHEST
				"ext.a_first:normalMethodA",     // NORMAL, ext.a_first
				"ext.b_second:normalMethod1",    // NORMAL, ext.b_second, method1
				"ext.b_second:normalMethod2",    // NORMAL, ext.b_second, method2
				"ext.a_first:lowestMethodA"      // LOWEST
		);

		assertEquals(expected, PriorityAndOrderingExtension.executionOrder);
	}

	static class PhaseFilteringExtension implements Extension {
		static final List<String> triggeredPhases = new ArrayList<>();

		@Override public @NotNull String getId() { return "ext.phases"; }
		@Override public @NotNull String getName() { return "Phase Test"; }
		@Override public @NotNull String getVersion() { return "1.0"; }
		@Override public @NotNull String getAuthor() { return "test"; }
		@Override public @NotNull String getDescription() { return "test"; }
		@Override public void initialize(@NotNull ExtensionContext context) {}

		@Hook(phases = {HookPhase.PREVIEW})
		public void onPreviewOnly(PreTokenizeStringEvent event) {
			triggeredPhases.add("PREVIEW_ONLY");
		}

		@Hook(phases = {HookPhase.POST})
		public void onPostOnly(PreTokenizeStringEvent event) {
			triggeredPhases.add("POST_ONLY");
		}

		@Hook(phases = {HookPhase.PREVIEW, HookPhase.POST})
		public void onBoth(PreTokenizeStringEvent event) {
			triggeredPhases.add("BOTH:" + event.getPhase());
		}
	}

	@Test
	@DisplayName("Phase filtering should restrict hook execution to specified phases")
	void testPhaseFiltering() {
		PhaseFilteringExtension.triggeredPhases.clear();

		extensionManager.registerInternalExtension(new PhaseFilteringExtension());
		extensionManager.loadAllExtensions();

		extensionManager.fireHookEvent(new PreTokenizeStringEvent(HookPhase.PREVIEW, "sample"));
		assertEquals(List.of("BOTH:PREVIEW", "PREVIEW_ONLY"), PhaseFilteringExtension.triggeredPhases);

		PhaseFilteringExtension.triggeredPhases.clear();
		extensionManager.fireHookEvent(new PreTokenizeStringEvent(HookPhase.POST, "sample"));
		assertEquals(List.of("BOTH:POST", "POST_ONLY"), PhaseFilteringExtension.triggeredPhases);
	}

	static class ExceptionThrowingExtension implements Extension {
		static boolean subsequentExecuted = false;

		@Override public @NotNull String getId() { return "ext.exceptions"; }
		@Override public @NotNull String getName() { return "Exception Test"; }
		@Override public @NotNull String getVersion() { return "1.0"; }
		@Override public @NotNull String getAuthor() { return "test"; }
		@Override public @NotNull String getDescription() { return "test"; }
		@Override public void initialize(@NotNull ExtensionContext context) {}

		@Hook(priority = HookPriority.HIGH)
		public void willThrow(PreTokenizeStringEvent event) throws Exception {
			event.setText(event.getText() + " [CORRUPTED]");
			throw new RuntimeException("Intentional hook error");
		}

		@Hook(priority = HookPriority.LOW)
		public void afterThrow(PreTokenizeStringEvent event) {
			subsequentExecuted = true;
			event.setText(event.getText() + " [modified]");
		}
	}

	@Test
	@DisplayName("In PREVIEW, exceptions thrown by hooks should revert event to clean snapshot and continue")
	void testPreviewPhaseExceptionRevert() {
		ExceptionThrowingExtension.subsequentExecuted = false;

		extensionManager.registerInternalExtension(new ExceptionThrowingExtension());
		extensionManager.loadAllExtensions();

		PreTokenizeStringEvent event = new PreTokenizeStringEvent(HookPhase.PREVIEW, "Hello");
		PreTokenizeStringEvent result = extensionManager.fireHookEvent(event);

		assertTrue(ExceptionThrowingExtension.subsequentExecuted);
		// Notice [CORRUPTED] was reverted back to "Hello" before afterThrow executed
		assertEquals("Hello [modified]", result.getText());
	}

	@Test
	@DisplayName("In POST, any exception thrown by a hook should bail out by throwing HookExecutionException")
	void testPostPhaseExceptionBailout() {
		ExceptionThrowingExtension.subsequentExecuted = false;

		extensionManager.registerInternalExtension(new ExceptionThrowingExtension());
		extensionManager.loadAllExtensions();

		PreTokenizeStringEvent event = new PreTokenizeStringEvent(HookPhase.POST, "Hello");
		assertThrows(HookExecutionException.class, () -> {
			extensionManager.fireHookEvent(event);
		});

		assertFalse(ExceptionThrowingExtension.subsequentExecuted);
	}

	@Test
	@DisplayName("Programmatic hook registration via ExtensionContext should work and execute")
	void testProgrammaticHookRegistration() {
		Extension ext = new Extension() {
			@Override public @NotNull String getId() { return "ext.prog"; }
			@Override public @NotNull String getName() { return "Prog"; }
			@Override public @NotNull String getVersion() { return "1.0"; }
			@Override public @NotNull String getAuthor() { return "test"; }
			@Override public @NotNull String getDescription() { return "test"; }
			@Override
			public void initialize(@NotNull ExtensionContext context) {
				context.registerHook(PreTokenizeStringEvent.class, event -> {
					event.setText("Programmatic: " + event.getText());
				}, HookPriority.HIGH, null);
			}
		};

		extensionManager.registerInternalExtension(ext);
		extensionManager.loadAllExtensions();

		PreTokenizeStringEvent event = extensionManager.fireHookEvent(new PreTokenizeStringEvent(HookPhase.PREVIEW, "Initial"));
		assertEquals("Programmatic: Initial", event.getText());
	}
}
