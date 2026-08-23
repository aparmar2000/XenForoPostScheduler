package aparmar2000.xenforoposter.syntax.html;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeRoot;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeTag;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeText;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition.HtmlStringMapper.StringMappingException;

@DisplayName("HtmlCodeAst Tests")
class HtmlCodeAstTest {

	private HtmlTagDefinition bTag;
	private HtmlTagDefinition iTag;
	private HtmlTagDefinition hrTag;
	private HtmlTagDefinition codeTag;
	private HtmlTagDefinition styleTag;
	private HtmlTagDefinition errorTag;

	@BeforeEach
	void setUp() {
		bTag = HtmlTagDefinition.simpleHtmlTagWrapper("b", false);
		iTag = HtmlTagDefinition.simpleHtmlTagWrapper("i", false);
		hrTag = HtmlTagDefinition.simpleHtmlSingularTag("hr");

		codeTag = new HtmlTagDefinition("code", true, true,
				(tagDef, params, innerText) -> "<code>" + innerText + "</code>");

		styleTag = new HtmlTagDefinition("span", true, false,
				(tagDef, params, innerText) -> {
					String color = params.get("color");
					String style = color != null ? " style=\"color: " + color + ";\"" : "";
					return "<span" + style + ">" + innerText + "</span>";
				});

		errorTag = new HtmlTagDefinition("error", true, false,
				(tagDef, params, innerText) -> {
					throw new StringMappingException();
				});
	}

	@Nested
	@DisplayName("AST Node Model Tests")
	class NodeModelTests {

		@Test
		@DisplayName("HtmlAstNodeText basic properties, merge, clone, and equality")
		void testTextNode() {
			HtmlAstNodeText node1 = new HtmlAstNodeText("Hello ");
			HtmlAstNodeText node2 = new HtmlAstNodeText("World");

			assertEquals("Hello ", node1.getText());
			assertFalse(node1.hasChildren());
			assertTrue(node1.getChildren().isEmpty());

			HtmlAstNodeText merged = node1.merge(node2);
			assertEquals("Hello World", merged.getText());

			HtmlAstNodeText cloned = node1.clone();
			assertEquals(node1, cloned);
			assertEquals(node1.hashCode(), cloned.hashCode());
			assertNotSame(node1, cloned);
		}

		@Test
		@DisplayName("HtmlAstNodeTag constructor variants, rawString validation, and clone")
		void testTagNode() {
			ImmutableMap<String, String> params = ImmutableMap.of("color", "red");
			HtmlAstNodeTag defaultRawTag = new HtmlAstNodeTag(styleTag, params);
			assertArrayEquals(new String[] {"", ""}, defaultRawTag.getRawString());
			assertEquals(styleTag, defaultRawTag.getTagDefinition());
			assertEquals(params, defaultRawTag.getParameters());
			assertFalse(defaultRawTag.hasChildren());

			String[] customRaw = new String[] {"<custom>", "</custom>"};
			HtmlAstNodeTag customRawTag = new HtmlAstNodeTag(customRaw, styleTag, params);
			assertArrayEquals(customRaw, customRawTag.getRawString());

			assertThrows(IllegalArgumentException.class, () ->
					new HtmlAstNodeTag(new String[] {"<only_one>"}, styleTag, params));
			assertThrows(IllegalArgumentException.class, () ->
					new HtmlAstNodeTag(new String[] {"1", "2", "3"}, styleTag, params));

			customRawTag.getChildren().add(new HtmlAstNodeText("child text"));
			assertTrue(customRawTag.hasChildren());

			HtmlAstNodeTag clonedTag = customRawTag.clone();
			assertEquals(customRawTag, clonedTag);
			assertEquals(customRawTag.hashCode(), clonedTag.hashCode());
			assertNotSame(customRawTag, clonedTag);
			assertNotSame(customRawTag.getChildren(), clonedTag.getChildren());
			assertNotSame(customRawTag.getRawString(), clonedTag.getRawString());
			assertArrayEquals(customRawTag.getRawString(), clonedTag.getRawString());
			assertEquals(1, clonedTag.getChildren().size());
		}

		@Test
		@DisplayName("HtmlCodeAstNodeRoot children management and clone")
		void testRootNode() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			assertFalse(root.hasChildren());
			assertTrue(root.getChildren().isEmpty());

			root.getChildren().add(new HtmlAstNodeText("text"));
			assertTrue(root.hasChildren());

			HtmlAstNodeRoot clonedRoot = root.clone();
			assertEquals(root, clonedRoot);
			assertEquals(root.hashCode(), clonedRoot.hashCode());
			assertNotSame(root, clonedRoot);
			assertNotSame(root.getChildren(), clonedRoot.getChildren());
			assertEquals(1, clonedRoot.getChildren().size());
		}

		@Test
		@DisplayName("HtmlCodeAst wrapper returns root node")
		void testHtmlCodeAstWrapper() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAst ast = new HtmlAst(root);
			assertSame(root, ast.getRootNode());
		}
	}

	@Nested
	@DisplayName("HTML Escaping and Text Mapping")
	class HtmlEscapingAndTextMappingTests {

		@Test
		@DisplayName("Empty root node maps to empty string")
		void testEmptyAst() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAst ast = new HtmlAst(root);
			assertEquals("", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Plain text without special characters maps as-is")
		void testPlainText() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			root.getChildren().add(new HtmlAstNodeText("Hello world 123"));
			HtmlAst ast = new HtmlAst(root);
			assertEquals("Hello world 123", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Special HTML characters (<, >, &, \", ') are escaped in normal text nodes")
		void testSpecialCharactersEscaped() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			root.getChildren().add(new HtmlAstNodeText("AT&T <tag> \"quotes\" 'single'"));
			HtmlAst ast = new HtmlAst(root);
			assertEquals("AT&amp;T &lt;tag&gt; &quot;quotes&quot; &#39;single&#39;", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Multiple consecutive text nodes map to concatenated escaped text")
		void testMultipleTextNodes() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			root.getChildren().add(new HtmlAstNodeText("Line 1 & "));
			root.getChildren().add(new HtmlAstNodeText("Line 2 < "));
			root.getChildren().add(new HtmlAstNodeText("Line 3"));
			HtmlAst ast = new HtmlAst(root);
			assertEquals("Line 1 &amp; Line 2 &lt; Line 3", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Single newlines are converted to <br/> tags")
		void testSingleNewlineConversion() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			root.getChildren().add(new HtmlAstNodeText("First Line\nSecond Line"));
			HtmlAst ast = new HtmlAst(root);
			assertEquals("First Line<br/>Second Line", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Multiple consecutive newlines and carriage returns map to multiple <br/> tags")
		void testMultipleNewlinesAndCarriageReturns() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			root.getChildren().add(new HtmlAstNodeText("Paragraph 1\r\n\r\nParagraph 2\n\n\nParagraph 3"));
			HtmlAst ast = new HtmlAst(root);
			assertEquals("Paragraph 1<br/><br/>Paragraph 2<br/><br/><br/>Paragraph 3", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Blank spaces are preserved as normal spaces for natural HTML collapsing")
		void testBlankSpacesPreservedAsStandardSpaces() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			root.getChildren().add(new HtmlAstNodeText("Word1   Word2    Word3"));
			HtmlAst ast = new HtmlAst(root);
			assertEquals("Word1   Word2    Word3", ast.mapToHtmlString());
		}
	}

	@Nested
	@DisplayName("Tag Mapping and Nesting")
	class TagMappingTests {

		@Test
		@DisplayName("Single wrapper tag maps children inside tag")
		void testSimpleWrapperTag() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAstNodeTag tag = new HtmlAstNodeTag(bTag, ImmutableMap.of());
			tag.getChildren().add(new HtmlAstNodeText("Bold & Strong"));
			root.getChildren().add(tag);

			HtmlAst ast = new HtmlAst(root);
			assertEquals("<b>Bold &amp; Strong</b>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Singular self-closing tag maps correctly")
		void testSingularTag() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAstNodeTag tag = new HtmlAstNodeTag(hrTag, ImmutableMap.of());
			root.getChildren().add(tag);

			HtmlAst ast = new HtmlAst(root);
			assertEquals("<hr/>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Tag with parameters maps using parameters")
		void testTagWithParameters() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAstNodeTag tag = new HtmlAstNodeTag(styleTag, ImmutableMap.of("color", "blue"));
			tag.getChildren().add(new HtmlAstNodeText("Blue text"));
			root.getChildren().add(tag);

			HtmlAst ast = new HtmlAst(root);
			assertEquals("<span style=\"color: blue;\">Blue text</span>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Hierarchically nested tags map properly")
		void testNestedTags() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAstNodeTag bNode = new HtmlAstNodeTag(bTag, ImmutableMap.of());
			HtmlAstNodeTag iNode = new HtmlAstNodeTag(iTag, ImmutableMap.of());
			iNode.getChildren().add(new HtmlAstNodeText("Nested & Formatted"));
			bNode.getChildren().add(iNode);
			root.getChildren().add(bNode);

			HtmlAst ast = new HtmlAst(root);
			assertEquals("<b><i>Nested &amp; Formatted</i></b>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Mixed text and tag siblings map in sequence")
		void testMixedSiblings() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			root.getChildren().add(new HtmlAstNodeText("Start < "));
			HtmlAstNodeTag bNode = new HtmlAstNodeTag(bTag, ImmutableMap.of());
			bNode.getChildren().add(new HtmlAstNodeText("Bold"));
			root.getChildren().add(bNode);
			root.getChildren().add(new HtmlAstNodeText(" > End"));

			HtmlAst ast = new HtmlAst(root);
			assertEquals("Start &lt; <b>Bold</b> &gt; End", ast.mapToHtmlString());
		}
	}

	@Nested
	@DisplayName("Raw Text Requirement (innerTextRawRequired)")
	class InnerTextRawRequiredTests {

		@Test
		@DisplayName("Tag with innerTextRawRequired=true preserves unescaped text")
		void testRawTextPreservedInCodeTag() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAstNodeTag tag = new HtmlAstNodeTag(codeTag, ImmutableMap.of());
			tag.getChildren().add(new HtmlAstNodeText("int x = 5 & 3 < 10;"));
			root.getChildren().add(tag);

			HtmlAst ast = new HtmlAst(root);
			assertEquals("<code>int x = 5 & 3 < 10;</code>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Nested tags inside innerTextRawRequired=true propagate raw requirement to children")
		void testNestedRawRequirementPropagation() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAstNodeTag codeNode = new HtmlAstNodeTag(codeTag, ImmutableMap.of());
			HtmlAstNodeTag bNode = new HtmlAstNodeTag(bTag, ImmutableMap.of());
			bNode.getChildren().add(new HtmlAstNodeText("x < y & a > b"));
			codeNode.getChildren().add(bNode);
			root.getChildren().add(codeNode);

			HtmlAst ast = new HtmlAst(root);
			assertEquals("<code><b>x < y & a > b</b></code>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Raw requirement is isolated to innerTextRawRequired tags and does not affect siblings")
		void testRawRequirementSiblingIsolation() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAstNodeTag codeNode = new HtmlAstNodeTag(codeTag, ImmutableMap.of());
			codeNode.getChildren().add(new HtmlAstNodeText("a < b"));
			root.getChildren().add(codeNode);

			HtmlAstNodeTag bNode = new HtmlAstNodeTag(bTag, ImmutableMap.of());
			bNode.getChildren().add(new HtmlAstNodeText("c < d"));
			root.getChildren().add(bNode);

			HtmlAst ast = new HtmlAst(root);
			assertEquals("<code>a < b</code><b>c &lt; d</b>", ast.mapToHtmlString());
		}
	}

	@Nested
	@DisplayName("StringMappingException Fallback")
	class StringMappingExceptionFallbackTests {

		@Test
		@DisplayName("Tag mapping failure falls back to rawString and inner content")
		void testStringMappingExceptionFallbackWithCustomRawString() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			String[] rawString = new String[] {"<raw_open>", "</raw_close>"};
			HtmlAstNodeTag errorNode = new HtmlAstNodeTag(rawString, errorTag, ImmutableMap.of());
			errorNode.getChildren().add(new HtmlAstNodeText("Inner Text"));
			root.getChildren().add(errorNode);

			HtmlAst ast = new HtmlAst(root);
			assertEquals("<raw_open>Inner Text</raw_close>", ast.mapToHtmlString());
		}

		@Test
		@DisplayName("Tag mapping failure with default empty rawString renders only inner content")
		void testStringMappingExceptionFallbackWithDefaultRawString() {
			HtmlAstNodeRoot root = new HtmlAstNodeRoot();
			HtmlAstNodeTag errorNode = new HtmlAstNodeTag(errorTag, ImmutableMap.of());
			errorNode.getChildren().add(new HtmlAstNodeText("Fallback Text"));
			root.getChildren().add(errorNode);

			HtmlAst ast = new HtmlAst(root);
			assertEquals("Fallback Text", ast.mapToHtmlString());
		}
	}
}
