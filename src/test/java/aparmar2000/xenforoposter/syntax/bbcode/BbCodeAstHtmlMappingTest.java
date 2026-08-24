package aparmar2000.xenforoposter.syntax.bbcode;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.AbstractAst;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeRoot;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeTag;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeText;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition.HtmlNodeMapper.HtmlMappingException;
import aparmar2000.xenforoposter.syntax.html.HtmlAst;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNode;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeRoot;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeTag;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeText;
import aparmar2000.xenforoposter.syntax.TagSource;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition;

@DisplayName("BbCodeAst HTML Mapping Tests (mapToHtmlString and mapToHtmlAst)")
class BbCodeAstHtmlMappingTest {

	private HtmlTagDefinition boldHtmlTag;
	private HtmlTagDefinition italicHtmlTag;
	private HtmlTagDefinition underlineHtmlTag;
	private HtmlTagDefinition colorHtmlTag;
	private HtmlTagDefinition urlHtmlTag;
	private HtmlTagDefinition quoteHtmlTag;
	private HtmlTagDefinition codeHtmlTag;
	private HtmlTagDefinition hrHtmlTag;

	private BbCodeTagDefinition boldTag;
	private BbCodeTagDefinition italicTag;
	private BbCodeTagDefinition underlineTag;
	private BbCodeTagDefinition colorTag;
	private BbCodeTagDefinition urlTag;
	private BbCodeTagDefinition quoteTag;
	private BbCodeTagDefinition codeTag;
	private BbCodeTagDefinition hrTag;
	private BbCodeTagDefinition failingTag;

	@BeforeEach
	void setUp() {
		boldHtmlTag = HtmlTagDefinition.simpleHtmlTagWrapper("b", false);
		boldTag = BbCodeTagDefinition.simpleHtmlTagWrapper("B", boldHtmlTag);

		italicHtmlTag = HtmlTagDefinition.simpleHtmlTagWrapper("i", false);
		italicTag = BbCodeTagDefinition.simpleHtmlTagWrapper("I", italicHtmlTag);

		underlineHtmlTag = HtmlTagDefinition.simpleHtmlTagWrapper("u", false);
		underlineTag = BbCodeTagDefinition.simpleHtmlTagWrapper("U", underlineHtmlTag);

		hrHtmlTag = HtmlTagDefinition.simpleHtmlSingularTag("hr");
		hrTag = BbCodeTagDefinition.simpleHtmlSingularTag("HR", hrHtmlTag);

		colorHtmlTag = new HtmlTagDefinition("span", true, false,
				(tagDef, params, innerText) -> {
					String color = params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME);
					return "<span style=\"color: " + (color != null ? color : "inherit") + ";\">" + innerText + "</span>";
				});
		colorTag = new BbCodeTagDefinition("COLOR", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(colorHtmlTag, params), childNodes));

		urlHtmlTag = new HtmlTagDefinition("a", true, false,
				(tagDef, params, innerText) -> {
					String href = params.getOrDefault(BbCodeAstNodeTag.ROOT_PARAMETER_NAME, innerText);
					String target = params.get("target");
					String targetAttr = target != null ? " target=\"" + target + "\"" : "";
					return "<a href=\"" + href + "\"" + targetAttr + ">" + innerText + "</a>";
				});
		urlTag = new BbCodeTagDefinition("URL", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(urlHtmlTag, params), childNodes));

		quoteHtmlTag = new HtmlTagDefinition("blockquote", true, false,
				(tagDef, params, innerText) -> {
					String author = params.get("author");
					String header = (author != null && !author.isBlank()) ? "<div class=\"quote-author\">" + author + " said:</div>" : "";
					return "<blockquote>" + header + innerText + "</blockquote>";
				});
		quoteTag = new BbCodeTagDefinition("QUOTE", true,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(quoteHtmlTag, params), childNodes));

		codeHtmlTag = new HtmlTagDefinition("pre", false, true,
				(tagDef, params, innerText) -> {
					String lang = params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME);
					String header = (lang != null && !lang.isBlank()) ? "<div class=\"code-header\">" + lang + "</div>" : "";
					return "<pre><code>" + header + innerText + "</code></pre>";
				});
		codeTag = new BbCodeTagDefinition("CODE", false,
				(tagDef, params, childNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(codeHtmlTag, params), childNodes));

		failingTag = new BbCodeTagDefinition("FAIL", true,
				(tagDef, params, childNodes) -> {
					throw new HtmlMappingException();
				});
	}

	@Nested
	@DisplayName("Empty and Text-Only AST Trees")
	class EmptyAndTextOnlyAstTests {

		@Test
		@DisplayName("Empty root node maps to an empty string")
		void testEmptyRootNode() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			BbCodeAst ast = new BbCodeAst(root);

			String html = ast.mapToHtmlString();
			assertEquals("", html);
		}

		@Test
		@DisplayName("Single text node under root maps directly to its text")
		void testSingleTextNode() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			root.getChildren().add(new BbCodeAstNodeText("Hello, World!"));
			BbCodeAst ast = new BbCodeAst(root);

			String html = ast.mapToHtmlString();
			assertEquals("Hello, World!", html);
		}

		@Test
		@DisplayName("Multiple consecutive text nodes map to concatenated text with <br/> for newlines")
		void testMultipleTextNodesConcatenation() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			root.getChildren().add(new BbCodeAstNodeText("First"));
			root.getChildren().add(new BbCodeAstNodeText(" "));
			root.getChildren().add(new BbCodeAstNodeText("Second"));
			root.getChildren().add(new BbCodeAstNodeText("\nThird"));
			BbCodeAst ast = new BbCodeAst(root);

			String html = ast.mapToHtmlString();
			assertEquals("First Second<br/>Third", html);
		}

		@Test
		@DisplayName("Text node with empty string maps to empty string")
		void testEmptyTextNode() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			root.getChildren().add(new BbCodeAstNodeText(""));
			BbCodeAst ast = new BbCodeAst(root);

			assertEquals("", ast.mapToHtmlString());
		}
	}

	@Nested
	@DisplayName("Single Tag AST Mapping")
	class SingleTagMappingTests {

		@Test
		@DisplayName("Single formatting tag wrapping text maps correctly to HTML")
		void testSingleFormattingTag() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			BbCodeAstNodeTag tagNode = new BbCodeAstNodeTag("[B]", boldTag, ImmutableMap.of());
			tagNode.getChildren().add(new BbCodeAstNodeText("Bold Text"));
			root.getChildren().add(tagNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<b>Bold Text</b>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Tag with no child nodes maps with empty inner text")
		void testTagWithNoChildren() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			BbCodeAstNodeTag hrNode = new BbCodeAstNodeTag("[HR]", hrTag, ImmutableMap.of());
			root.getChildren().add(hrNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<hr/>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Tag with multiple text children concatenates inner text before mapping")
		void testTagWithMultipleTextChildren() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			BbCodeAstNodeTag tagNode = new BbCodeAstNodeTag("[I]", italicTag, ImmutableMap.of());
			tagNode.getChildren().add(new BbCodeAstNodeText("Part 1"));
			tagNode.getChildren().add(new BbCodeAstNodeText(" - "));
			tagNode.getChildren().add(new BbCodeAstNodeText("Part 2"));
			root.getChildren().add(tagNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<i>Part 1 - Part 2</i>", ast.mapToHtmlString());
		}
	}

	@Nested
	@DisplayName("Tags with Parameters Mapping")
	class TagParametersMappingTests {

		@Test
		@DisplayName("Root parameter ($value) is passed and mapped correctly")
		void testRootParameterMapping() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			ImmutableMap<String, String> params = ImmutableMap.of(BbCodeAstNodeTag.ROOT_PARAMETER_NAME, "red");
			BbCodeAstNodeTag colorNode = new BbCodeAstNodeTag("[COLOR=red]", colorTag, params);
			colorNode.getChildren().add(new BbCodeAstNodeText("Red Text"));
			root.getChildren().add(colorNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<span style=\"color: red;\">Red Text</span>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Named parameters are passed and mapped correctly")
		void testNamedParametersMapping() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			ImmutableMap<String, String> params = ImmutableMap.of("author", "Alice");
			BbCodeAstNodeTag quoteNode = new BbCodeAstNodeTag("[QUOTE author=Alice]", quoteTag, params);
			quoteNode.getChildren().add(new BbCodeAstNodeText("This is an important message."));
			root.getChildren().add(quoteNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<blockquote><div class=\"quote-author\">Alice said:</div>This is an important message.</blockquote>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Both root parameter and named parameters are mapped correctly")
		void testRootAndNamedParametersMapping() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			ImmutableMap<String, String> params = ImmutableMap.of(
					BbCodeAstNodeTag.ROOT_PARAMETER_NAME, "https://example.com",
					"target", "_blank"
			);
			BbCodeAstNodeTag urlNode = new BbCodeAstNodeTag("[URL=https://example.com target=_blank]", urlTag, params);
			urlNode.getChildren().add(new BbCodeAstNodeText("Example Website"));
			root.getChildren().add(urlNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<a href=\"https://example.com\" target=\"_blank\">Example Website</a>", ast.mapToHtmlString());
		}
	}

	@Nested
	@DisplayName("Nested and Hierarchical Tag Mapping")
	class NestedTagsMappingTests {

		@Test
		@DisplayName("Nested tags (B -> I -> U) map hierarchically")
		void testDeeplyNestedFormattingTags() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();

			BbCodeAstNodeTag bNode = new BbCodeAstNodeTag("[B]", boldTag, ImmutableMap.of());
			BbCodeAstNodeTag iNode = new BbCodeAstNodeTag("[I]", italicTag, ImmutableMap.of());
			BbCodeAstNodeTag uNode = new BbCodeAstNodeTag("[U]", underlineTag, ImmutableMap.of());
			uNode.getChildren().add(new BbCodeAstNodeText("Nested Content"));

			iNode.getChildren().add(uNode);
			bNode.getChildren().add(iNode);
			root.getChildren().add(bNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<b><i><u>Nested Content</u></i></b>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Mixed text and tag siblings under root map sequentially")
		void testMixedSiblingsUnderRoot() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();

			root.getChildren().add(new BbCodeAstNodeText("Prefix "));

			BbCodeAstNodeTag bNode = new BbCodeAstNodeTag("[B]", boldTag, ImmutableMap.of());
			bNode.getChildren().add(new BbCodeAstNodeText("Bold"));
			root.getChildren().add(bNode);

			root.getChildren().add(new BbCodeAstNodeText(" Middle "));

			BbCodeAstNodeTag iNode = new BbCodeAstNodeTag("[I]", italicTag, ImmutableMap.of());
			iNode.getChildren().add(new BbCodeAstNodeText("Italic"));
			root.getChildren().add(iNode);

			root.getChildren().add(new BbCodeAstNodeText(" Suffix"));

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("Prefix <b>Bold</b> Middle <i>Italic</i> Suffix", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Tag containing text and nested tags maps properly")
		void testTagContainingTextAndNestedTag() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();

			BbCodeAstNodeTag bNode = new BbCodeAstNodeTag("[B]", boldTag, ImmutableMap.of());
			bNode.getChildren().add(new BbCodeAstNodeText("Hello "));

			BbCodeAstNodeTag iNode = new BbCodeAstNodeTag("[I]", italicTag, ImmutableMap.of());
			iNode.getChildren().add(new BbCodeAstNodeText("World"));
			bNode.getChildren().add(iNode);

			bNode.getChildren().add(new BbCodeAstNodeText("!"));
			root.getChildren().add(bNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<b>Hello <i>World</i>!</b>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Sibling nested tags inside parent tag map properly")
		void testSiblingNestedTagsInsideParent() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();

			BbCodeAstNodeTag quoteNode = new BbCodeAstNodeTag("[QUOTE]", quoteTag, ImmutableMap.of());

			BbCodeAstNodeTag bNode = new BbCodeAstNodeTag("[B]", boldTag, ImmutableMap.of());
			bNode.getChildren().add(new BbCodeAstNodeText("Bold Part"));
			quoteNode.getChildren().add(bNode);

			quoteNode.getChildren().add(new BbCodeAstNodeText(" and "));

			BbCodeAstNodeTag iNode = new BbCodeAstNodeTag("[I]", italicTag, ImmutableMap.of());
			iNode.getChildren().add(new BbCodeAstNodeText("Italic Part"));
			quoteNode.getChildren().add(iNode);

			root.getChildren().add(quoteNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<blockquote><b>Bold Part</b> and <i>Italic Part</i></blockquote>", ast.mapToHtmlString());
		}
	}

	@Nested
	@DisplayName("Non-Nesting Tag Mapping")
	class NonNestingTagMappingTests {

		@Test
		@DisplayName("Code block tag containing literal text and fake tags maps as raw text")
		void testCodeTagWithLiteralText() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			ImmutableMap<String, String> params = ImmutableMap.of(BbCodeAstNodeTag.ROOT_PARAMETER_NAME, "java");
			BbCodeAstNodeTag codeNode = new BbCodeAstNodeTag("[CODE=java]", codeTag, params);
			codeNode.getChildren().add(new BbCodeAstNodeText("System.out.println(\"[B]Not Bold[/B]\");"));
			root.getChildren().add(codeNode);

			BbCodeAst ast = new BbCodeAst(root);
			assertEquals("<pre><code><div class=\"code-header\">java</div>System.out.println(\"[B]Not Bold[/B]\");</code></pre>", ast.mapToHtmlString());
		}
	}

	@Nested
	@DisplayName("HTML AST Conversion (mapToHtmlAst)")
	class MapToHtmlAstTests {

		@Test
		@DisplayName("mapToHtmlAst converts BbCodeAst tree into HtmlCodeAst tree")
		void testMapToHtmlAstTreeStructure() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			BbCodeAstNodeTag bNode = new BbCodeAstNodeTag("[B]", boldTag, ImmutableMap.of());
			bNode.getChildren().add(new BbCodeAstNodeText("Bold Content"));
			root.getChildren().add(bNode);

			BbCodeAst ast = new BbCodeAst(root);
			HtmlAst htmlAst = ast.mapToHtmlAst();

			assertNotNull(htmlAst);
			HtmlAstNodeRoot htmlRoot = htmlAst.getRootNode();
			assertNotNull(htmlRoot);
			assertEquals(1, htmlRoot.getChildren().size());

			HtmlAstNode child = htmlRoot.getChildren().get(0);
			assertTrue(child instanceof HtmlAstNodeTag);
			HtmlAstNodeTag htmlTagNode = (HtmlAstNodeTag) child;
			assertEquals(boldHtmlTag, htmlTagNode.getTagDefinition());

			assertEquals(1, htmlTagNode.getChildren().size());
			assertTrue(htmlTagNode.getChildren().get(0) instanceof HtmlAstNodeText);
			assertEquals("Bold Content", ((HtmlAstNodeText) htmlTagNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("HtmlMappingException fallback wraps child nodes with raw opening and closing string text nodes")
		void testHtmlMappingExceptionFallback() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			String[] rawString = new String[] {"[FAIL]", "[/FAIL]"};
			BbCodeAstNodeTag failNode = new BbCodeAstNodeTag(rawString, failingTag, ImmutableMap.of());
			failNode.getChildren().add(new BbCodeAstNodeText("Inner Text"));
			root.getChildren().add(failNode);

			BbCodeAst ast = new BbCodeAst(root);
			HtmlAst htmlAst = ast.mapToHtmlAst();

			assertNotNull(htmlAst);
			HtmlAstNodeRoot htmlRoot = htmlAst.getRootNode();
			List<HtmlAstNode> children = htmlRoot.getChildren();
			assertEquals(3, children.size());

			assertTrue(children.get(0) instanceof HtmlAstNodeText);
			assertEquals("[FAIL]", ((HtmlAstNodeText) children.get(0)).getText());

			assertTrue(children.get(1) instanceof HtmlAstNodeText);
			assertEquals("Inner Text", ((HtmlAstNodeText) children.get(1)).getText());

			assertTrue(children.get(2) instanceof HtmlAstNodeText);
			assertEquals("[/FAIL]", ((HtmlAstNodeText) children.get(2)).getText());

			assertEquals("[FAIL]Inner Text[/FAIL]", ast.mapToHtmlString());
		}
	}

	@Nested
	@DisplayName("End-to-End Parsing and AST HTML Mapping Pipeline")
	class EndToEndParsingAndMappingTests {

		private BbCodeTagDefinitionRegistry registry;
		private BbCodeTokenizer tokenizer;
		private BbCodeAstParser parser;

		@BeforeEach
		void initPipeline() {
			registry = new BbCodeTagDefinitionRegistry();
			registry.register(TagSource.CORE, boldTag);
			registry.register(TagSource.CORE, italicTag);
			registry.register(TagSource.CORE, underlineTag);
			registry.register(TagSource.CORE, colorTag);
			registry.register(TagSource.CORE, urlTag);
			registry.register(TagSource.CORE, quoteTag);
			registry.register(TagSource.CORE, codeTag);
			registry.register(TagSource.CORE, hrTag);

			tokenizer = new BbCodeTokenizer(registry);
			parser = new BbCodeAstParser(registry, tokenizer);
		}

		@Test
		@DisplayName("Parsed plain text maps to identical HTML string")
		void testParsedPlainText() {
			BbCodeAst ast = parser.parseString("Simple plain text without tags.");
			assertEquals("Simple plain text without tags.", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Parsed basic inline tags map to HTML tags")
		void testParsedInlineTags() {
			BbCodeAst ast = parser.parseString("This is [B]bold[/B] and [I]italic[/I] text.");
			assertEquals("This is <b>bold</b> and <i>italic</i> text.", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Parsed nested tags map to nested HTML tags")
		void testParsedNestedTags() {
			BbCodeAst ast = parser.parseString("[B][I]Bold and Italic[/I][/B]");
			assertEquals("<b><i>Bold and Italic</i></b>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Parsed tags with parameters map to configured HTML attributes")
		void testParsedTagsWithParameters() {
			BbCodeAst ast = parser.parseString("[COLOR=blue]Blue text[/COLOR] and [URL=https://example.com target=_blank]Link[/URL]");
			assertEquals("<span style=\"color: blue;\">Blue text</span> and <a href=\"https://example.com\" target=\"_blank\">Link</a>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Parsed overlapping tags are split and correctly mapped to HTML")
		void testParsedOverlappingTags() {
			BbCodeAst ast = parser.parseString("[B][I]bi[/B]i[/I]");
			assertEquals("<b><i>bi</i></b><i>i</i>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Parsed non-nesting CODE tag ignores nested tags and preserves them in HTML output")
		void testParsedNonNestingTag() {
			BbCodeAst ast = parser.parseString("[CODE=java]int a = 1; [B]comment[/B][/CODE]");
			assertEquals("<pre><code><div class=\"code-header\">java</div>int a = 1; [B]comment[/B]</code></pre>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Parsed unclosed tags unwrap to plain text in output")
		void testParsedUnclosedTags() {
			BbCodeAst ast = parser.parseString("[B]Unclosed bold");
			assertEquals("[B]Unclosed bold", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Parsed multiline BBCode content converts newlines to <br/> and keeps blank spaces as standard spaces")
		void testParsedMultilineContentWithNewlinesAndSpaces() {
			String bb = "Line 1   with spaces\n\n[B]Line 2   Bold[/B]\nLine 3";
			BbCodeAst ast = parser.parseString(bb);
			assertEquals("Line 1   with spaces<br/><br/><b>Line 2   Bold</b><br/>Line 3", ast.mapToHtmlString());
		}
	}
}
