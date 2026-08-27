package aparmar2000.xenforoposter.syntax.bbcode;

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

import aparmar2000.xenforoposter.extension.Extension;
import aparmar2000.xenforoposter.extension.ExtensionContext;
import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.InternalExtensionContext;
import aparmar2000.xenforoposter.extension.hook.BbCodeAstEvent;
import aparmar2000.xenforoposter.extension.hook.Hook;
import aparmar2000.xenforoposter.extension.hook.HookPriority;
import aparmar2000.xenforoposter.extension.hook.HtmlAstEvent;
import aparmar2000.xenforoposter.extension.hook.PreTokenizeStringEvent;
import aparmar2000.xenforoposter.extension.hook.TokenizedBbCodeEvent;
import aparmar2000.xenforoposter.settings.SettingsHolder;
import aparmar2000.xenforoposter.syntax.DefaultTagDefinitions;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;

class BbCodeProcessorPipelineTest {

	private ExtensionManager extensionManager;
	private BbCodeTokenizer tokenizer;
	private BbCodeAstParser parser;
	private BbCodeProcessor processor;

	@BeforeEach
	void setUp() throws IOException {
		Path tempBaseDir = Files.createTempDirectory("xf_pipeline_test");
		SettingsHolder.Factory settingsHolderFactory = mock(SettingsHolder.Factory.class);
		when(settingsHolderFactory.create(any()))
				.thenReturn(mock(SettingsHolder.class));
		BbCodeTagDefinitionRegistry bbRegistry = new BbCodeTagDefinitionRegistry();
		HtmlTagDefinitionRegistry htmlRegistry = new HtmlTagDefinitionRegistry();
		DefaultTagDefinitions.registerBaseTags(bbRegistry, htmlRegistry);

		InternalExtensionContext.Factory contextFactory = (dataDir, extId) ->
				new InternalExtensionContext(dataDir, extId, settingsHolderFactory, bbRegistry, htmlRegistry);

		extensionManager = new ExtensionManager(tempBaseDir, new Gson(), contextFactory);
		tokenizer = new BbCodeTokenizer(bbRegistry);
		parser = new BbCodeAstParser(bbRegistry, tokenizer);
		processor = new BbCodeProcessor(extensionManager, tokenizer, parser);
	}

	static class PipelineMonitoringExtension implements Extension {
		static final List<String> eventTrace = new ArrayList<>();

		@Override public @NotNull String getId() { return "ext.pipeline_monitor"; }
		@Override public @NotNull String getName() { return "Pipeline Monitor"; }
		@Override public @NotNull String getVersion() { return "1.0"; }
		@Override public @NotNull String getAuthor() { return "test"; }
		@Override public @NotNull String getDescription() { return "test"; }
		@Override public void initialize(@NotNull ExtensionContext context) {}

		@Hook(priority = HookPriority.HIGH)
		public void onPreTokenize(PreTokenizeStringEvent event) {
			eventTrace.add("PreTokenizeStringEvent:" + event.getPhase());
			event.setText(event.getText().replace("{{MACRO}}", "[B]Expanded[/B]"));
		}

		@Hook(priority = HookPriority.NORMAL)
		public void onTokens(TokenizedBbCodeEvent event) {
			eventTrace.add("TokenizedBbCodeEvent:" + event.getPhase());
			event.setTokens(BbCodeTokenUtils.replaceText(event.getTokens(), "foo", "bar"));
		}

		@Hook(priority = HookPriority.NORMAL)
		public void onBbCodeAst(BbCodeAstEvent event) {
			eventTrace.add("BbCodeAstEvent:" + event.getPhase());
			event.getBbCodeAst().replaceText("bar", "baz");
		}

		@Hook(priority = HookPriority.NORMAL)
		public void onHtmlAst(HtmlAstEvent event) {
			eventTrace.add("HtmlAstEvent:" + event.getPhase());
			event.getHtmlAst().replaceText("baz", "qux");
		}
	}

	@Test
	@DisplayName("Post-time pipeline should execute string, token, and BBCode AST hooks and return string")
	void testPostTimePipeline() {
		PipelineMonitoringExtension.eventTrace.clear();

		extensionManager.registerInternalExtension(new PipelineMonitoringExtension());
		extensionManager.loadAllExtensions();

		String result = processor.processForPost("Hello {{MACRO}} and foo!");

		assertEquals(List.of(
				"PreTokenizeStringEvent:POST",
				"TokenizedBbCodeEvent:POST",
				"BbCodeAstEvent:POST"
		), PipelineMonitoringExtension.eventTrace);

		assertEquals("Hello [B]Expanded[/B] and baz!", result);
	}

	@Test
	@DisplayName("Preview-time pipeline should execute all 4 hooks and render to HTML")
	void testPreviewTimePipeline() {
		PipelineMonitoringExtension.eventTrace.clear();

		extensionManager.registerInternalExtension(new PipelineMonitoringExtension());
		extensionManager.loadAllExtensions();

		String html = processor.processToHtmlStringForPreview("Hello {{MACRO}} and foo!");

		assertEquals(List.of(
				"PreTokenizeStringEvent:PREVIEW",
				"TokenizedBbCodeEvent:PREVIEW",
				"BbCodeAstEvent:PREVIEW",
				"HtmlAstEvent:PREVIEW"
		), PipelineMonitoringExtension.eventTrace);

		assertTrue(html.contains("<b>Expanded</b>"));
		assertTrue(html.contains("qux"));
	}
}
