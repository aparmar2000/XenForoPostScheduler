package aparmar2000.xenforoposter.syntax.bbcode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.inject.Inject;

import aparmar2000.xenforoposter.utils.InternalResourceLoader;
import lombok.NonNull;

public class BbCodePreviewRenderer {

	private static final String PREVIEW_TEMPLATE_PATH = "bbcode/preview_template.html";
	private static final String PREVIEW_DARK_CSS_PATH = "bbcode/preview_dark.css";
	private static final String PREVIEW_LIGHT_CSS_PATH = "bbcode/preview_light.css";

	private final BbCodeAstParser parser;
	private final String templateHtml;
	private final String darkCss;
	private final String lightCss;

	@Inject
	public BbCodePreviewRenderer(@NonNull BbCodeAstParser parser) {
		this.parser = parser;
		this.templateHtml = InternalResourceLoader.tryGetInternalResourceAsStringSilent(PREVIEW_TEMPLATE_PATH)
				.orElse("<!DOCTYPE html>\n<html>\n<head>\n<style>\n{{CSS_STYLES}}\n</style>\n</head>\n<body>\n{{CONTENT}}\n</body>\n</html>");
		this.darkCss = InternalResourceLoader.tryGetInternalResourceAsStringSilent(PREVIEW_DARK_CSS_PATH)
				.orElse("");
		this.lightCss = InternalResourceLoader.tryGetInternalResourceAsStringSilent(PREVIEW_LIGHT_CSS_PATH)
				.orElse("");
	}

	@NotNull
	public String renderToHtml(@Nullable String bbCode, boolean darkTheme) {
		if (bbCode == null || bbCode.trim().isEmpty()) {
			return wrapInXenForoStyle("<p><i>(Empty preview)</i></p>", darkTheme);
		}

		String html = convertBbCodeToHtml(bbCode);
		return wrapInXenForoStyle(html, darkTheme);
	}

	@NotNull
	public String convertBbCodeToHtml(@NotNull String bbCode) {
		String text = bbCode.replace("\r\n", "\n").replace("\r", "\n");
		BbCodeAst ast = parser.parseString(text);
		return ast.mapToHtmlString();
	}

	private String wrapInXenForoStyle(String contentHtml, boolean darkTheme) {
		String css = darkTheme ? darkCss : lightCss;
		return templateHtml
				.replace("{{CSS_STYLES}}", css)
				.replace("{{CONTENT}}", contentHtml != null ? contentHtml : "");
	}
}
