package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.bbcode.BbCodePreviewRenderer;

class BbCodePreviewRendererTest {
	private BbCodePreviewRenderer renderer;

	@BeforeEach
	void setUp() {
		renderer = new BbCodePreviewRenderer();
	}

	@Test
	@DisplayName("Should render headings with proper h1/h2/h3 tags")
	void testHeadings() {
		String bb = "[HEADING=1]Main Title[/HEADING=1]\n[HEADING=2]Subtitle[/HEADING=2]\n[HEADING=3]Section[/HEADING=3]";
		String html = renderer.convertBbCodeToHtml(bb);

		assertTrue(html.contains("<h1 class=\"bbcode-heading\">Main Title</h1>"));
		assertTrue(html.contains("<h2 class=\"bbcode-heading\">Subtitle</h2>"));
		assertTrue(html.contains("<h3 class=\"bbcode-heading\">Section</h3>"));
	}

	@Test
	@DisplayName("Should render bold, italic, underline, and strikethrough styling")
	void testInlineStyles() {
		String bb = "This is [B]bold[/B], [I]italic[/I], [U]underlined[/U], and [S]strikethrough[/S] text.";
		String html = renderer.convertBbCodeToHtml(bb);

		assertTrue(html.contains("<b>bold</b>"));
		assertTrue(html.contains("<i>italic</i>"));
		assertTrue(html.contains("<u>underlined</u>"));
		assertTrue(html.contains("<s>strikethrough</s>"));
	}

	@Test
	@DisplayName("Should render inline code and fenced code blocks while escaping HTML inside")
	void testCodeBlocks() {
		String bb = "Use [ICODE]<var> x = 10;[/ICODE] in code.\n\n[CODE=java]\npublic static void main(String[] args) {\n    System.out.println(\"<hello>\");\n}\n[/CODE]";
		String html = renderer.convertBbCodeToHtml(bb);

		assertTrue(html.contains("<code class=\"bbcode-icode\">&lt;var&gt; x = 10;</code>"));
		assertTrue(html.contains("<pre class=\"bbcode-code\">"));
		assertTrue(html.contains("<div class=\"code-header\">Code (java):</div>"));
		assertTrue(html.contains("&lt;hello&gt;"));
	}

	@Test
	@DisplayName("Should render quotes and author attribution")
	void testBlockquotes() {
		String bb = "[QUOTE='Admin']This is an important quote.[/QUOTE]\n[QUOTE]Anonymous quote[/QUOTE]";
		String html = renderer.convertBbCodeToHtml(bb);

		assertTrue(html.contains("<blockquote class=\"bbcode-quote\">"));
		assertTrue(html.contains("<div class=\"quote-author\">Admin said:</div>"));
		assertTrue(html.contains("This is an important quote."));
		assertTrue(html.contains("Anonymous quote"));
	}

	@Test
	@DisplayName("Should render links, images, and user tags")
	void testLinksImagesUsers() {
		String bb = "[URL='https://example.com']Click Here[/URL] and [IMG]https://example.com/pic.png[/IMG] and [USER=42]TestMember[/USER]";
		String html = renderer.convertBbCodeToHtml(bb);

		assertTrue(html.contains("<a href=\"https://example.com\" target=\"_blank\">Click Here</a>"));
		assertTrue(html.contains("<img src=\"https://example.com/pic.png\""));
		assertTrue(html.contains("<span class=\"bbcode-user\">@TestMember</span>"));
	}

	@Test
	@DisplayName("Should render unordered and ordered lists")
	void testLists() {
		String bb = "[LIST]\n[*]First item\n[*]Second item\n[/LIST]\n\n[LIST=1]\n[*]Numbered one\n[*]Numbered two\n[/LIST]";
		String html = renderer.convertBbCodeToHtml(bb);

		assertTrue(html.contains("<ul class=\"bbcode-list\">"));
		assertTrue(html.contains("<li>First item</li>"));
		assertTrue(html.contains("<li>Second item</li>"));
		assertTrue(html.contains("<ol class=\"bbcode-list\">"));
		assertTrue(html.contains("<li>Numbered one</li>"));
		assertTrue(html.contains("<li>Numbered two</li>"));
	}

	@Test
	@DisplayName("Should render tables and spoilers")
	void testTablesAndSpoilers() {
		String bb = "[TABLE][TR][TH]Header[/TH][/TR][TR][TD]Cell[/TD][/TR][/TABLE]\n[SPOILER='Secret']Hidden details[/SPOILER]";
		String html = renderer.convertBbCodeToHtml(bb);

		assertTrue(html.contains("<table class=\"bbcode-table\">"));
		assertTrue(html.contains("<tr><th>Header</th></tr>"));
		assertTrue(html.contains("<tr><td>Cell</td></tr>"));
		assertTrue(html.contains("<div class=\"bbcode-spoiler\">"));
		assertTrue(html.contains("<div class=\"spoiler-title\">Spoiler: Secret</div>"));
		assertTrue(html.contains("Hidden details"));
	}

	@Test
	@DisplayName("Should handle empty and blank strings gracefully")
	void testEmpty() {
		String html = renderer.renderToHtml("", false);
		assertTrue(html.contains("Empty preview"));

		String nullHtml = renderer.renderToHtml(null, true);
		assertTrue(nullHtml.contains("Empty preview"));
	}

	@Test
	@DisplayName("Should render articles, indentation, and inline spoilers")
	void testArticlesIndentsAndInlineSpoilers() {
		String bb = "[ARTICLE]Featured article text[/ARTICLE]\n[INDENT]Indented content[/INDENT]\nCheck this [ISPOILER]hidden spoiler[/ISPOILER] secret!";
		String html = renderer.convertBbCodeToHtml(bb);

		assertTrue(html.contains("<div class=\"bbcode-article\""));
		assertTrue(html.contains("Featured article text"));
		assertTrue(html.contains("<div class=\"bbcode-indent\""));
		assertTrue(html.contains("Indented content"));
		assertTrue(html.contains("<span class=\"bbcode-ispoiler\""));
		assertTrue(html.contains("hidden spoiler"));
	}

	@Test
	@DisplayName("Should render fixed numeric font sizes 1 to 7 with proper pixel styling")
	void testNumericFontSizes() {
		String bb = "[SIZE=1]Small[/SIZE] [SIZE=4]Medium[/SIZE] [SIZE=7]Large[/SIZE]";
		String html = renderer.convertBbCodeToHtml(bb);

		assertTrue(html.contains("<span style=\"font-size: 9px;\">Small</span>"));
		assertTrue(html.contains("<span style=\"font-size: 15px;\">Medium</span>"));
		assertTrue(html.contains("<span style=\"font-size: 26px;\">Large</span>"));
	}

	@Test
	@DisplayName("Should render full HTML with internal stylesheet for both light and dark themes")
	void testFullHtmlRenderingWithThemeStyles() {
		String lightHtml = renderer.renderToHtml("[B]Hello[/B]", false);
		assertTrue(lightHtml.contains("<!DOCTYPE html>"));
		assertTrue(lightHtml.contains("<style>"));
		assertTrue(lightHtml.contains("<b>Hello</b>"));
		assertTrue(lightHtml.contains("#ffffff")); // light mode background

		String darkHtml = renderer.renderToHtml("[B]Hello[/B]", true);
		assertTrue(darkHtml.contains("<!DOCTYPE html>"));
		assertTrue(darkHtml.contains("<style>"));
		assertTrue(darkHtml.contains("<b>Hello</b>"));
		assertTrue(darkHtml.contains("#1e2227")); // dark mode background
	}
}
