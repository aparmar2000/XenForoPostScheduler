package aparmar2000.xenforoposter.syntax.bbcode;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.inject.Inject;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.hook.BbCodeAstEvent;
import aparmar2000.xenforoposter.extension.hook.HookPhase;
import aparmar2000.xenforoposter.extension.hook.HtmlAstEvent;
import aparmar2000.xenforoposter.extension.hook.PreTokenizeStringEvent;
import aparmar2000.xenforoposter.extension.hook.TokenizedBbCodeEvent;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.ParsedToken;
import aparmar2000.xenforoposter.syntax.html.HtmlAst;
import lombok.Getter;
import lombok.NonNull;

public class BbCodeProcessor {
	@Getter
	private final ExtensionManager extensionManager;
	@Getter
	private final BbCodeTokenizer tokenizer;
	@Getter
	private final BbCodeAstParser parser;

	@Inject
	public BbCodeProcessor(@NonNull ExtensionManager extensionManager,
			@NonNull BbCodeTokenizer tokenizer,
			@NonNull BbCodeAstParser parser) {
		this.extensionManager = extensionManager;
		this.tokenizer = tokenizer;
		this.parser = parser;
	}

	@NotNull
	public String processForPost(@Nullable String rawBbCode) {
		if (rawBbCode == null) {
			return "";
		}

		// PreTokenizeStringEvent (POST)
		PreTokenizeStringEvent preTokenizeEvent = new PreTokenizeStringEvent(HookPhase.POST, rawBbCode);
		extensionManager.fireHookEvent(preTokenizeEvent);
		String text = preTokenizeEvent.getText();

		// Tokenize & TokenizedBbCodeEvent (POST)
		List<ParsedToken> tokens = tokenizer.tokenizeString(text);
		TokenizedBbCodeEvent tokenEvent = new TokenizedBbCodeEvent(HookPhase.POST, tokens);
		extensionManager.fireHookEvent(tokenEvent);
		tokens = tokenEvent.getTokens();

		// Parse AST & BbCodeAstEvent (POST)
		BbCodeAst ast = parser.parseTokens(tokens);
		BbCodeAstEvent astEvent = new BbCodeAstEvent(HookPhase.POST, ast);
		extensionManager.fireHookEvent(astEvent);
		ast = astEvent.getBbCodeAst();

		// Convert BbCodeAst back to String
		return ast.toBbCodeString();
	}

	@NotNull
	public HtmlAst processToHtmlAstForPreview(@Nullable String rawBbCode) {
		if (rawBbCode == null) {
			rawBbCode = "";
		}
		String normalized = rawBbCode.replace("\r\n", "\n").replace("\r", "\n");

		// PreTokenizeStringEvent (PREVIEW)
		PreTokenizeStringEvent preTokenizeEvent = new PreTokenizeStringEvent(HookPhase.PREVIEW, normalized);
		extensionManager.fireHookEvent(preTokenizeEvent);
		String text = preTokenizeEvent.getText();

		// Tokenize & TokenizedBbCodeEvent (PREVIEW)
		List<ParsedToken> tokens = tokenizer.tokenizeString(text);
		TokenizedBbCodeEvent tokenEvent = new TokenizedBbCodeEvent(HookPhase.PREVIEW, tokens);
		extensionManager.fireHookEvent(tokenEvent);
		tokens = tokenEvent.getTokens();

		// Parse AST & BbCodeAstEvent (PREVIEW)
		BbCodeAst ast = parser.parseTokens(tokens);
		BbCodeAstEvent astEvent = new BbCodeAstEvent(HookPhase.PREVIEW, ast);
		extensionManager.fireHookEvent(astEvent);
		ast = astEvent.getBbCodeAst();

		// Map to HtmlAst & HtmlAstEvent (PREVIEW)
		HtmlAst htmlAst = ast.mapToHtmlAst();
		HtmlAstEvent htmlEvent = new HtmlAstEvent(HookPhase.PREVIEW, htmlAst);
		extensionManager.fireHookEvent(htmlEvent);
		return htmlEvent.getHtmlAst();
	}

	@NotNull
	public String processToHtmlStringForPreview(@Nullable String rawBbCode) {
		return processToHtmlAstForPreview(rawBbCode).mapToHtmlString();
	}
}
