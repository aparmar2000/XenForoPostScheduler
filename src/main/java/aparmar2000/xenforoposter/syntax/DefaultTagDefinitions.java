package aparmar2000.xenforoposter.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeTag;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinitionRegistry;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNode;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeTag;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeText;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DefaultTagDefinitions {

	private static final Pattern SIZE_UNIT_PATTERN = Pattern.compile("^[0-9]+(?:px|pt)$", Pattern.CASE_INSENSITIVE);

	public static void registerBaseTags(@NotNull BbCodeTagDefinitionRegistry bbCodeRegistry,
			@NotNull HtmlTagDefinitionRegistry htmlRegistry) {
		registerBaseHtmlTags(htmlRegistry);
		registerBaseBbCodeTags(bbCodeRegistry, htmlRegistry);
	}

	public static void registerBaseHtmlTags(@NonNull HtmlTagDefinitionRegistry htmlRegistry) {
		// Formatting tags
		htmlRegistry.register(HtmlTagDefinition.simpleHtmlTagWrapper("b", false));
		htmlRegistry.register(HtmlTagDefinition.simpleHtmlTagWrapper("i", false));
		htmlRegistry.register(HtmlTagDefinition.simpleHtmlTagWrapper("u", false));
		htmlRegistry.register(HtmlTagDefinition.simpleHtmlTagWrapper("s", false));

		// Headings h1-h6
		for (int h = 1; h <= 6; h++) {
			final String headingTag = "h" + h;
			htmlRegistry.register(new HtmlTagDefinition(headingTag, true, false,
					(tagDef, params, innerText) -> "<" + tagDef.getTag() + " class=\"bbcode-heading\">" + innerText + "</" + tagDef.getTag() + ">"));
		}

		// Structure & Layout
		htmlRegistry.register(HtmlTagDefinition.simpleHtmlSingularTag("hr"));
		htmlRegistry.register(new HtmlTagDefinition("hr-styled", false, false,
				(tagDef, params, innerText) -> "<hr class=\"bbcode-hr\" />"));

		htmlRegistry.register(new HtmlTagDefinition("div-align", true, false,
				(tagDef, params, innerText) -> {
					String align = params.getOrDefault("align", "left");
					return "<div style=\"text-align: " + align + ";\">" + innerText + "</div>";
				}));

		htmlRegistry.register(new HtmlTagDefinition("div-indent", true, false,
				(tagDef, params, innerText) -> {
					String levelStr = params.get("level");
					int level = 1;
					if (levelStr != null) {
						try {
							level = Integer.parseInt(levelStr.trim());
						} catch (NumberFormatException ignored) {}
					}
					if (level > 1) {
						return "<div class=\"bbcode-indent\" style=\"margin-left: calc(" + level + " * 20px);\">" + innerText + "</div>";
					}
					return "<div class=\"bbcode-indent\" style=\"margin-left: 20px;\">" + innerText + "</div>";
				}));

		htmlRegistry.register(new HtmlTagDefinition("div-article", true, false,
				(tagDef, params, innerText) -> "<div class=\"bbcode-article\" style=\"border-left: 3px solid #007bff; padding: 6px 12px; margin: 8px 0;\">" + innerText + "</div>"));

		// Text Styles (color, font, size)
		htmlRegistry.register(new HtmlTagDefinition("span-color", true, false,
				(tagDef, params, innerText) -> {
					String color = params.getOrDefault("color", "inherit");
					return "<span style=\"color: " + color + ";\">" + innerText + "</span>";
				}));

		htmlRegistry.register(new HtmlTagDefinition("span-font", true, false,
				(tagDef, params, innerText) -> {
					String font = params.getOrDefault("font", "inherit");
					return "<span style=\"font-family: " + font + ";\">" + innerText + "</span>";
				}));

		htmlRegistry.register(new HtmlTagDefinition("span-size", true, false,
				(tagDef, params, innerText) -> {
					String size = params.getOrDefault("size", "inherit");
					return "<span style=\"font-size: " + size + ";\">" + innerText + "</span>";
				}));

		// Links and Media
		htmlRegistry.register(new HtmlTagDefinition("a", true, false,
				(tagDef, params, innerText) -> {
					String href = params.get("href");
					if (href == null || href.isEmpty()) {
						href = innerText;
					}
					return "<a href=\"" + href + "\" target=\"_blank\">" + innerText + "</a>";
				}));

		htmlRegistry.register(new HtmlTagDefinition("img", false, true,
				(tagDef, params, innerText) -> "<img src=\"" + innerText.trim() + "\" style=\"max-width: 100%; height: auto; border-radius: 4px;\" />"));

		htmlRegistry.register(new HtmlTagDefinition("span-user", true, false,
				(tagDef, params, innerText) -> "<span class=\"bbcode-user\">@" + innerText + "</span>"));

		// Spoilers and Quotes
		htmlRegistry.register(new HtmlTagDefinition("div-spoiler", true, false,
				(tagDef, params, innerText) -> {
					String title = params.get("title");
					if (title != null && !title.isEmpty()) {
						return "<div class=\"bbcode-spoiler\"><div class=\"spoiler-title\">Spoiler: " + title + "</div><div class=\"spoiler-body\">" + innerText + "</div></div>";
					}
					return "<div class=\"bbcode-spoiler\"><div class=\"spoiler-title\">Spoiler</div><div class=\"spoiler-body\">" + innerText + "</div></div>";
				}));

		htmlRegistry.register(new HtmlTagDefinition("span-ispoiler", true, false,
				(tagDef, params, innerText) -> "<span class=\"bbcode-ispoiler\" style=\"background-color: #555; color: #555; border-radius: 2px; padding: 0 4px;\" title=\"Spoiler\">" + innerText + "</span>"));

		htmlRegistry.register(new HtmlTagDefinition("blockquote-quote", true, false,
				(tagDef, params, innerText) -> {
					String author = params.get("author");
					if (author != null && !author.isEmpty()) {
						return "<blockquote class=\"bbcode-quote\"><div class=\"quote-author\">" + author + " said:</div>" + innerText + "</blockquote>";
					}
					return "<blockquote class=\"bbcode-quote\">" + innerText + "</blockquote>";
				}));

		// Code blocks and inline code
		htmlRegistry.register(new HtmlTagDefinition("pre-code", false, false,
				(tagDef, params, innerText) -> {
					String lang = params.get("lang");
					String header = (lang != null && !lang.isEmpty())
							? "<div class=\"code-header\">Code (" + lang + "):</div>"
							: "<div class=\"code-header\">Code:</div>";
					return "<pre class=\"bbcode-code\">" + header + "<code>" + innerText.trim() + "</code></pre>";
				}));

		htmlRegistry.register(new HtmlTagDefinition("code-icode", false, false,
				(tagDef, params, innerText) -> "<code class=\"bbcode-icode\">" + innerText + "</code>"));

		// Tables
		htmlRegistry.register(new HtmlTagDefinition("table", true, false,
				(tagDef, params, innerText) -> "<table class=\"bbcode-table\">" + innerText + "</table>"));
		htmlRegistry.register(HtmlTagDefinition.simpleHtmlTagWrapper("tr", false));
		htmlRegistry.register(HtmlTagDefinition.simpleHtmlTagWrapper("th", false));
		htmlRegistry.register(HtmlTagDefinition.simpleHtmlTagWrapper("td", false));

		// Lists
		htmlRegistry.register(new HtmlTagDefinition("ul", true, false,
				(tagDef, params, innerText) -> "<ul class=\"bbcode-list\">" + innerText + "</ul>"));
		htmlRegistry.register(new HtmlTagDefinition("ol", true, false,
				(tagDef, params, innerText) -> "<ol class=\"bbcode-list\">" + innerText + "</ol>"));
		htmlRegistry.register(HtmlTagDefinition.simpleHtmlTagWrapper("li", false));
	}

	public static void registerBaseBbCodeTags(@NonNull BbCodeTagDefinitionRegistry bbCodeRegistry,
			@NonNull HtmlTagDefinitionRegistry htmlRegistry) {
		// Basic formatting
		HtmlTagDefinition bHtml = htmlRegistry.getByTagString("b");
		if (bHtml != null) {
			bbCodeRegistry.register(BbCodeTagDefinition.simpleHtmlTagWrapper("B", bHtml));
		}
		HtmlTagDefinition iHtml = htmlRegistry.getByTagString("i");
		if (iHtml != null) {
			bbCodeRegistry.register(BbCodeTagDefinition.simpleHtmlTagWrapper("I", iHtml));
		}
		HtmlTagDefinition uHtml = htmlRegistry.getByTagString("u");
		if (uHtml != null) {
			bbCodeRegistry.register(BbCodeTagDefinition.simpleHtmlTagWrapper("U", uHtml));
		}
		HtmlTagDefinition sHtml = htmlRegistry.getByTagString("s");
		if (sHtml != null) {
			bbCodeRegistry.register(BbCodeTagDefinition.simpleHtmlTagWrapper("S", sHtml));
		}

		// Headings
		bbCodeRegistry.register(new BbCodeTagDefinition("HEADING", true,
				(tagDef, params, childNodes) -> {
					String levelStr = params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME);
					int level = 1;
					if (levelStr != null) {
						try {
							level = Integer.parseInt(levelStr.trim());
							if (level < 1 || level > 6) { level = 1; }
						} catch (NumberFormatException ignored) {}
					}
					HtmlTagDefinition hTag = htmlRegistry.getByTagString("h" + level);
					if (hTag == null) {
						hTag = HtmlTagDefinition.simpleHtmlTagWrapper("h" + level, false);
					}
					return AbstractAst.wrap(new HtmlAstNodeTag(hTag, params), childNodes);
				}));

		// Color
		HtmlTagDefinition colorHtml = htmlRegistry.getByTagString("span-color");
		bbCodeRegistry.register(new BbCodeTagDefinition("COLOR", true,
				(tagDef, params, childNodes) -> {
					String color = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					ImmutableMap<String, String> htmlParams = color != null
							? ImmutableMap.of("color", color)
							: ImmutableMap.of();
					return AbstractAst.wrap(new HtmlAstNodeTag(colorHtml, htmlParams), childNodes);
				}));

		// Font
		HtmlTagDefinition fontHtml = htmlRegistry.getByTagString("span-font");
		bbCodeRegistry.register(new BbCodeTagDefinition("FONT", true,
				(tagDef, params, childNodes) -> {
					String font = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					ImmutableMap<String, String> htmlParams = font != null
							? ImmutableMap.of("font", font)
							: ImmutableMap.of();
					return AbstractAst.wrap(new HtmlAstNodeTag(fontHtml, htmlParams), childNodes);
				}));

		// Size
		HtmlTagDefinition sizeHtml = htmlRegistry.getByTagString("span-size");
		bbCodeRegistry.register(new BbCodeTagDefinition("SIZE", true,
				(tagDef, params, childNodes) -> {
					String sizeVal = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					String mappedSize = mapSizeToCss(sizeVal);
					ImmutableMap<String, String> htmlParams = mappedSize != null
							? ImmutableMap.of("size", mappedSize)
							: ImmutableMap.of();
					return AbstractAst.wrap(new HtmlAstNodeTag(sizeHtml, htmlParams), childNodes);
				}));

		// Alignments
		HtmlTagDefinition alignHtml = htmlRegistry.getByTagString("div-align");
		bbCodeRegistry.register(new BbCodeTagDefinition("CENTER", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(alignHtml, ImmutableMap.of("align", "center")), childNodes)));
		bbCodeRegistry.register(new BbCodeTagDefinition("LEFT", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(alignHtml, ImmutableMap.of("align", "left")), childNodes)));
		bbCodeRegistry.register(new BbCodeTagDefinition("RIGHT", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(alignHtml, ImmutableMap.of("align", "right")), childNodes)));
		bbCodeRegistry.register(new BbCodeTagDefinition("ALIGN", true,
				(tagDef, params, childNodes) -> {
					String align = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					if (align == null || align.isEmpty()) { align = "left"; }
					return AbstractAst.wrap(new HtmlAstNodeTag(alignHtml, ImmutableMap.of("align", align)), childNodes);
				}));

		// Indentation
		HtmlTagDefinition indentHtml = htmlRegistry.getByTagString("div-indent");
		bbCodeRegistry.register(new BbCodeTagDefinition("INDENT", true,
				(tagDef, params, childNodes) -> {
					String level = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					ImmutableMap<String, String> htmlParams = level != null
							? ImmutableMap.of("level", level)
							: ImmutableMap.of();
					return AbstractAst.wrap(new HtmlAstNodeTag(indentHtml, htmlParams), childNodes);
				}));

		// URLs
		HtmlTagDefinition aHtml = htmlRegistry.getByTagString("a");
		bbCodeRegistry.register(new BbCodeTagDefinition("URL", true,
				(tagDef, params, childNodes) -> {
					String href = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					ImmutableMap<String, String> htmlParams = href != null
							? ImmutableMap.of("href", href)
							: ImmutableMap.of();
					return AbstractAst.wrap(new HtmlAstNodeTag(aHtml, htmlParams), childNodes);
				}));

		// Images
		HtmlTagDefinition imgHtml = htmlRegistry.getByTagString("img");
		bbCodeRegistry.register(new BbCodeTagDefinition("IMG", false,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(imgHtml, ImmutableMap.of()), childNodes)));

		// User mentions
		HtmlTagDefinition userHtml = htmlRegistry.getByTagString("span-user");
		bbCodeRegistry.register(new BbCodeTagDefinition("USER", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(userHtml, ImmutableMap.of()), childNodes)));

		// Articles
		HtmlTagDefinition articleHtml = htmlRegistry.getByTagString("div-article");
		bbCodeRegistry.register(new BbCodeTagDefinition("ARTICLE", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(articleHtml, ImmutableMap.of()), childNodes)));

		// Horizontal rule
		HtmlTagDefinition hrHtml = htmlRegistry.getByTagString("hr-styled");
		bbCodeRegistry.register(new BbCodeTagDefinition("HR", false,
				(tagDef, params, childNodes) -> new HtmlAstNodeTag(hrHtml, ImmutableMap.of())));

		// Spoilers
		HtmlTagDefinition spoilerHtml = htmlRegistry.getByTagString("div-spoiler");
		bbCodeRegistry.register(new BbCodeTagDefinition("SPOILER", true,
				(tagDef, params, childNodes) -> {
					String title = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					ImmutableMap<String, String> htmlParams = title != null
							? ImmutableMap.of("title", title)
							: ImmutableMap.of();
					return AbstractAst.wrap(new HtmlAstNodeTag(spoilerHtml, htmlParams), childNodes);
				}));

		HtmlTagDefinition ispoilerHtml = htmlRegistry.getByTagString("span-ispoiler");
		bbCodeRegistry.register(new BbCodeTagDefinition("ISPOILER", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(ispoilerHtml, ImmutableMap.of()), childNodes)));

		// Quotes
		HtmlTagDefinition quoteHtml = htmlRegistry.getByTagString("blockquote-quote");
		bbCodeRegistry.register(new BbCodeTagDefinition("QUOTE", true,
				(tagDef, params, childNodes) -> {
					String author = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					if (author == null) {
						author = cleanQuotes(params.get("author"));
					}
					ImmutableMap<String, String> htmlParams = author != null
							? ImmutableMap.of("author", author)
							: ImmutableMap.of();
					return AbstractAst.wrap(new HtmlAstNodeTag(quoteHtml, htmlParams), childNodes);
				}));

		// Code blocks
		HtmlTagDefinition codeHtml = htmlRegistry.getByTagString("pre-code");
		bbCodeRegistry.register(new BbCodeTagDefinition("CODE", false,
				(tagDef, params, childNodes) -> {
					String lang = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					ImmutableMap<String, String> htmlParams = lang != null
							? ImmutableMap.of("lang", lang)
							: ImmutableMap.of();
					return AbstractAst.wrap(new HtmlAstNodeTag(codeHtml, htmlParams), childNodes);
				}));

		HtmlTagDefinition icodeHtml = htmlRegistry.getByTagString("code-icode");
		bbCodeRegistry.register(new BbCodeTagDefinition("ICODE", false,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(icodeHtml, ImmutableMap.of()), childNodes)));

		// Tables
		HtmlTagDefinition tableHtml = htmlRegistry.getByTagString("table");
		bbCodeRegistry.register(new BbCodeTagDefinition("TABLE", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(tableHtml, ImmutableMap.of()), childNodes)));
		HtmlTagDefinition trHtml = htmlRegistry.getByTagString("tr");
		bbCodeRegistry.register(new BbCodeTagDefinition("TR", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(trHtml, ImmutableMap.of()), childNodes)));
		HtmlTagDefinition thHtml = htmlRegistry.getByTagString("th");
		bbCodeRegistry.register(new BbCodeTagDefinition("TH", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(thHtml, ImmutableMap.of()), childNodes)));
		HtmlTagDefinition tdHtml = htmlRegistry.getByTagString("td");
		bbCodeRegistry.register(new BbCodeTagDefinition("TD", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(tdHtml, ImmutableMap.of()), childNodes)));

		// Lists
		HtmlTagDefinition ulHtml = htmlRegistry.getByTagString("ul");
		HtmlTagDefinition olHtml = htmlRegistry.getByTagString("ol");
		HtmlTagDefinition liHtml = htmlRegistry.getByTagString("li");

		bbCodeRegistry.register(new BbCodeTagDefinition("LIST", true,
				(tagDef, params, childNodes) -> {
					String rootVal = cleanQuotes(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
					boolean ordered = "1".equals(rootVal);
					HtmlTagDefinition listTag = ordered ? olHtml : ulHtml;

					List<HtmlAstNode> listItems = new ArrayList<>();
					HtmlAstNodeTag currentLi = null;

					for (HtmlAstNode child : childNodes) {
						if (child instanceof HtmlAstNodeText) {
							String text = ((HtmlAstNodeText) child).getText();
							int starIdx;
							int start = 0;
							while ((starIdx = text.indexOf("[*]", start)) != -1) {
								String before = text.substring(start, starIdx);
								String strippedBefore = stripNewlines(before);
								if (strippedBefore != null && !strippedBefore.isEmpty() && currentLi != null) {
									currentLi.getChildren().add(new HtmlAstNodeText(strippedBefore));
								}
								currentLi = new HtmlAstNodeTag(liHtml, ImmutableMap.of());
								listItems.add(currentLi);
								start = starIdx + 3;
							}
							String remaining = text.substring(start);
							if (currentLi != null) {
								String strippedRemaining = stripNewlines(remaining);
								if (strippedRemaining != null && !strippedRemaining.isEmpty()) {
									currentLi.getChildren().add(new HtmlAstNodeText(strippedRemaining));
								}
							} else if (!remaining.trim().isEmpty()) {
								listItems.add(new HtmlAstNodeText(remaining));
							}
						} else {
							if (currentLi != null) {
								currentLi.getChildren().add(child);
							} else {
								listItems.add(child);
							}
						}
					}

					return AbstractAst.wrap(new HtmlAstNodeTag(listTag, ImmutableMap.of()), listItems);
				}));
	}

	private static String stripNewlines(String s) {
		if (s == null) { return null; }
		int start = 0;
		while (start < s.length() && (s.charAt(start) == '\n' || s.charAt(start) == '\r')) {
			start++;
		}
		int end = s.length();
		while (end > start && (s.charAt(end - 1) == '\n' || s.charAt(end - 1) == '\r')) {
			end--;
		}
		return s.substring(start, end);
	}


	private static String cleanQuotes(String val) {
		if (val == null) { return null; }
		String trimmed = val.trim();
		if ((trimmed.startsWith("'") && trimmed.endsWith("'")) || (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
			if (trimmed.length() >= 2) {
				return trimmed.substring(1, trimmed.length() - 1).trim();
			}
		}
		return trimmed;
	}

	private static String mapSizeToCss(String sizeVal) {
		if (sizeVal == null || sizeVal.isEmpty()) {
			return null;
		}
		switch (sizeVal) {
			case "1": return "9px";
			case "2": return "10px";
			case "3": return "12px";
			case "4": return "15px";
			case "5": return "18px";
			case "6": return "22px";
			case "7": return "26px";
			default:
				if (SIZE_UNIT_PATTERN.matcher(sizeVal).matches()) {
					return sizeVal;
				}
				try {
					Integer.parseInt(sizeVal);
					return sizeVal + "px";
				} catch (NumberFormatException e) {
					return sizeVal;
				}
		}
	}
}
