package aparmar2000.xenforoposter.syntax;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAstParser;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodePreviewRenderer;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinitionRegistry;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer;
import aparmar2000.xenforoposter.syntax.html.HtmlAst;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;

@DisplayName("DefaultTagDefinitions & BbCodePreviewRenderer AST Pipeline Tests")
class DefaultTagDefinitionsTest {

	private HtmlTagDefinitionRegistry htmlRegistry;
	private BbCodeTagDefinitionRegistry bbCodeRegistry;
	private BbCodeTokenizer tokenizer;
	private BbCodeAstParser parser;
	private BbCodePreviewRenderer renderer;

	@BeforeEach
	void setUp() {
		htmlRegistry = new HtmlTagDefinitionRegistry();
		bbCodeRegistry = new BbCodeTagDefinitionRegistry();
		DefaultTagDefinitions.registerBaseTags(bbCodeRegistry, htmlRegistry);

		tokenizer = new BbCodeTokenizer(bbCodeRegistry);
		parser = new BbCodeAstParser(bbCodeRegistry, tokenizer);
		renderer = new BbCodePreviewRenderer(parser);
	}

	@Nested
	@DisplayName("Registry Initialization Tests")
	class RegistryInitializationTests {

		@Test
		@DisplayName("Base HTML tags are registered")
		void testBaseHtmlTagsRegistered() {
			assertNotNull(htmlRegistry.getByTagString("b"));
			assertNotNull(htmlRegistry.getByTagString("i"));
			assertNotNull(htmlRegistry.getByTagString("u"));
			assertNotNull(htmlRegistry.getByTagString("s"));
			assertNotNull(htmlRegistry.getByTagString("h1"));
			assertNotNull(htmlRegistry.getByTagString("h2"));
			assertNotNull(htmlRegistry.getByTagString("h3"));
			assertNotNull(htmlRegistry.getByTagString("h4"));
			assertNotNull(htmlRegistry.getByTagString("h5"));
			assertNotNull(htmlRegistry.getByTagString("h6"));
			assertNotNull(htmlRegistry.getByTagString("table"));
			assertNotNull(htmlRegistry.getByTagString("ul"));
			assertNotNull(htmlRegistry.getByTagString("ol"));
			assertNotNull(htmlRegistry.getByTagString("li"));
		}

		@Test
		@DisplayName("Base BBCode tags are registered")
		void testBaseBbCodeTagsRegistered() {
			assertFalse(bbCodeRegistry.getRegisteredTagDefinitions().isEmpty());
			assertTrue(bbCodeRegistry.getRegisteredTagDefinitions().stream().anyMatch(t -> t.getTag().equals("B")));
			assertTrue(bbCodeRegistry.getRegisteredTagDefinitions().stream().anyMatch(t -> t.getTag().equals("I")));
			assertTrue(bbCodeRegistry.getRegisteredTagDefinitions().stream().anyMatch(t -> t.getTag().equals("HEADING")));
			assertTrue(bbCodeRegistry.getRegisteredTagDefinitions().stream().anyMatch(t -> t.getTag().equals("COLOR")));
			assertTrue(bbCodeRegistry.getRegisteredTagDefinitions().stream().anyMatch(t -> t.getTag().equals("SIZE")));
			assertTrue(bbCodeRegistry.getRegisteredTagDefinitions().stream().anyMatch(t -> t.getTag().equals("URL")));
			assertTrue(bbCodeRegistry.getRegisteredTagDefinitions().stream().anyMatch(t -> t.getTag().equals("QUOTE")));
			assertTrue(bbCodeRegistry.getRegisteredTagDefinitions().stream().anyMatch(t -> t.getTag().equals("CODE")));
			assertTrue(bbCodeRegistry.getRegisteredTagDefinitions().stream().anyMatch(t -> t.getTag().equals("LIST")));
		}

	}

	@Nested
	@DisplayName("Formatting and Text Styling Tests")
	class FormattingAndStyleTests {

		@Test
		@DisplayName("Should render all heading levels 1 through 6")
		void testHeadings() {
			for (int h = 1; h <= 6; h++) {
				String bb = "[HEADING=" + h + "]Heading Level " + h + "[/HEADING]";
				String html = renderer.convertBbCodeToHtml(bb);
				assertEquals("<h" + h + " class=\"bbcode-heading\">Heading Level " + h + "</h" + h + ">", html);
			}
		}

		@Test
		@DisplayName("Should render fonts, colors, and sizes with cleaned quotes")
		void testFontColorSizeWithQuotes() {
			String bb = "[FONT='Trebuchet MS'][COLOR=\"#ff0000\"][SIZE='5']Styled Text[/SIZE][/COLOR][/FONT]";
			String html = renderer.convertBbCodeToHtml(bb);
			assertEquals("<span style=\"font-family: Trebuchet MS;\"><span style=\"color: #ff0000;\"><span style=\"font-size: 18px;\">Styled Text</span></span></span>", html);
		}

		@Test
		@DisplayName("Should render alignments and indents")
		void testAlignmentsAndIndents() {
			assertEquals("<div style=\"text-align: center;\">Center</div>", renderer.convertBbCodeToHtml("[CENTER]Center[/CENTER]"));
			assertEquals("<div style=\"text-align: left;\">Left</div>", renderer.convertBbCodeToHtml("[LEFT]Left[/LEFT]"));
			assertEquals("<div style=\"text-align: right;\">Right</div>", renderer.convertBbCodeToHtml("[RIGHT]Right[/RIGHT]"));
			assertEquals("<div style=\"text-align: justify;\">Justify</div>", renderer.convertBbCodeToHtml("[ALIGN=justify]Justify[/ALIGN]"));

			assertEquals("<div class=\"bbcode-indent\" style=\"margin-left: 20px;\">Indented</div>", renderer.convertBbCodeToHtml("[INDENT]Indented[/INDENT]"));
			assertEquals("<div class=\"bbcode-indent\" style=\"margin-left: calc(3 * 20px);\">Indented x3</div>", renderer.convertBbCodeToHtml("[INDENT=3]Indented x3[/INDENT]"));
		}
	}

	@Nested
	@DisplayName("Lists, Tables, and Complex Structures Tests")
	class ListsAndTablesTests {

		@Test
		@DisplayName("Should render unordered and ordered lists with multiple items and formatting")
		void testListWithFormatting() {
			String bb = "[LIST]\n[*]First [B]bold[/B] item\n[*]Second [I]italic[/I] item\n[/LIST]";
			String html = renderer.convertBbCodeToHtml(bb);
			assertEquals("<ul class=\"bbcode-list\"><li>First <b>bold</b> item</li><li>Second <i>italic</i> item</li></ul>", html);

			String olBb = "[LIST=1]\n[*]Num 1\n[*]Num 2\n[/LIST]";
			String olHtml = renderer.convertBbCodeToHtml(olBb);
			assertEquals("<ol class=\"bbcode-list\"><li>Num 1</li><li>Num 2</li></ol>", olHtml);
		}

		@Test
		@DisplayName("Should render tables with rows, headers, and cells")
		void testTableStructure() {
			String bb = "[TABLE][TR][TH]Col 1[/TH][TH]Col 2[/TH][/TR][TR][TD]Val 1[/TD][TD]Val 2[/TD][/TR][/TABLE]";
			String html = renderer.convertBbCodeToHtml(bb);
			assertEquals("<table class=\"bbcode-table\"><tr><th>Col 1</th><th>Col 2</th></tr><tr><td>Val 1</td><td>Val 2</td></tr></table>", html);
		}

		@Test
		@DisplayName("Should render tables with newlines between tags cleanly without stray <br/> tags")
		void testTableWithNewlinesBetweenTags() {
			String bb = "\n[TABLE]\n[TR]\n[TH]Col 1[/TH]\n[TH]Col 2[/TH]\n[/TR]\n[TR]\n[TD]Val 1[/TD]\n[TD]Val 2[/TD]\n[/TR]\n[/TABLE]\n";
			String html = renderer.convertBbCodeToHtml(bb);
			assertTrue(html.contains("<table class=\"bbcode-table\"><tr><th>Col 1</th><th>Col 2</th></tr><tr><td>Val 1</td><td>Val 2</td></tr></table>"));
			assertFalse(html.contains("<table class=\"bbcode-table\"><br/>"));
			assertFalse(html.contains("<tr><br/>"));
		}

		@Test
		@DisplayName("Should render lists with multiline items and newline line breaks")
		void testListWithMultilineItems() {
			String bb = "[LIST]\n[*]Item 1 Line 1\nItem 1 Line 2\n[*]Item 2\n[/LIST]";
			String html = renderer.convertBbCodeToHtml(bb);
			assertEquals("<ul class=\"bbcode-list\"><li>Item 1 Line 1<br/>Item 1 Line 2</li><li>Item 2</li></ul>", html);
		}

		@Test
		@DisplayName("Should render quotes with and without author attribution")
		void testQuotes() {
			assertEquals("<blockquote class=\"bbcode-quote\">Simple quote</blockquote>", renderer.convertBbCodeToHtml("[QUOTE]Simple quote[/QUOTE]"));
			assertEquals("<blockquote class=\"bbcode-quote\"><div class=\"quote-author\">Admin said:</div>Attributed quote</blockquote>",
					renderer.convertBbCodeToHtml("[QUOTE='Admin']Attributed quote[/QUOTE]"));
		}

		@Test
		@DisplayName("Should render spoilers with and without title")
		void testSpoilers() {
			assertEquals("<div class=\"bbcode-spoiler\"><div class=\"spoiler-title\">Spoiler</div><div class=\"spoiler-body\">Secret</div></div>",
					renderer.convertBbCodeToHtml("[SPOILER]Secret[/SPOILER]"));
			assertEquals("<div class=\"bbcode-spoiler\"><div class=\"spoiler-title\">Spoiler: Plot</div><div class=\"spoiler-body\">Secret</div></div>",
					renderer.convertBbCodeToHtml("[SPOILER='Plot']Secret[/SPOILER]"));
			assertEquals("<span class=\"bbcode-ispoiler\" style=\"background-color: #555; color: #555; border-radius: 2px; padding: 0 4px;\" title=\"Spoiler\">Inline secret</span>",
					renderer.convertBbCodeToHtml("[ISPOILER]Inline secret[/ISPOILER]"));
		}
	}

	@Nested
	@DisplayName("Extension Dynamic Tag Addition Tests")
	class ExtensionTagAdditionTests {

		@Test
		@DisplayName("Dynamically registered custom tags are parsed and rendered seamlessly")
		void testDynamicExtensionTags() {
			// Register custom HTML tag
			HtmlTagDefinition badgeHtml = new HtmlTagDefinition("badge", true, false,
					(tagDef, params, innerText) -> "<span class=\"badge badge-" + params.getOrDefault("type", "primary") + "\">" + innerText + "</span>");
			htmlRegistry.register(TagSource.of("ext-badge"), badgeHtml);

			// Register custom BBCode tag [BADGE=warning]Alert[/BADGE]
			bbCodeRegistry.register(TagSource.of("ext-badge"), new BbCodeTagDefinition("BADGE", true,
					(tagDef, params, childNodes) -> {
						String type = params.get(BbCodeAst.BbCodeAstNodeTag.ROOT_PARAMETER_NAME);
						ImmutableMap<String, String> htmlParams = type != null
								? ImmutableMap.of("type", type)
								: ImmutableMap.of();
						return AbstractAst.wrap(new HtmlAst.HtmlAstNodeTag(badgeHtml, htmlParams), childNodes);
					}));

			String bb = "System alert: [BADGE=danger]Critical Error[/BADGE]!";
			String html = renderer.convertBbCodeToHtml(bb);
			assertEquals("System alert: <span class=\"badge badge-danger\">Critical Error</span>!", html);
		}
	}
}
