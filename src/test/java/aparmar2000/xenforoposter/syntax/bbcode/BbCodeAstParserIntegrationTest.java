package aparmar2000.xenforoposter.syntax.bbcode;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.syntax.TagSource;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNode;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeTag;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeText;

/**
 * End-to-end parsing integration test covering the entire BBCode parsing pipeline
 * from raw input string through tokenizer and registry to the final BbCodeAst tree.
 */
class BbCodeAstParserIntegrationTest {

	private BbCodeTagDefinitionRegistry registry;
	private BbCodeTokenizer tokenizer;
	private BbCodeAstParser parser;

	private final BbCodeTagDefinition bTag = new BbCodeTagDefinition("B", true, null);
	private final BbCodeTagDefinition iTag = new BbCodeTagDefinition("I", true, null);
	private final BbCodeTagDefinition uTag = new BbCodeTagDefinition("U", true, null);
	private final BbCodeTagDefinition sTag = new BbCodeTagDefinition("S", true, null);
	private final BbCodeTagDefinition colorTag = new BbCodeTagDefinition("COLOR", true, null);
	private final BbCodeTagDefinition sizeTag = new BbCodeTagDefinition("SIZE", true, null);
	private final BbCodeTagDefinition urlTag = new BbCodeTagDefinition("URL", true, null);
	private final BbCodeTagDefinition quoteTag = new BbCodeTagDefinition("QUOTE", true, null);
	private final BbCodeTagDefinition codeTag = new BbCodeTagDefinition("CODE", false, null);
	private final BbCodeTagDefinition icodeTag = new BbCodeTagDefinition("ICODE", false, null);
	private final BbCodeTagDefinition attachTag = new BbCodeTagDefinition("ATTACH", false, null);

	@BeforeEach
	void setUp() {
		registry = new BbCodeTagDefinitionRegistry();
		registry.register(TagSource.CORE, bTag);
		registry.register(TagSource.CORE, iTag);
		registry.register(TagSource.CORE, uTag);
		registry.register(TagSource.CORE, sTag);
		registry.register(TagSource.CORE, colorTag);
		registry.register(TagSource.CORE, sizeTag);
		registry.register(TagSource.CORE, urlTag);
		registry.register(TagSource.CORE, quoteTag);
		registry.register(TagSource.CORE, codeTag);
		registry.register(TagSource.CORE, icodeTag);
		registry.register(TagSource.CORE, attachTag);

		tokenizer = new BbCodeTokenizer(registry);
		parser = new BbCodeAstParser(registry, tokenizer);
	}

	@Nested
	@DisplayName("End-to-End Plain Text and Empty Inputs")
	class PlainTextInputIntegrationTests {

		@Test
		@DisplayName("Empty string parses to empty AST")
		void testEmptyString() {
			BbCodeAst ast = parser.parseString("");
			assertNotNull(ast);
			assertNotNull(ast.getRootNode());
			assertFalse(ast.getRootNode().hasChildren());
			assertTrue(ast.getRootNode().getChildren().isEmpty());
		}

		@Test
		@DisplayName("Plain text without tags parses to a single text node under root")
		void testPlainText() {
			BbCodeAst ast = parser.parseString("This is plain text with no BBCode tags.");
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(1, children.size());
			assertTrue(children.get(0) instanceof BbCodeAstNodeText);
			assertEquals("This is plain text with no BBCode tags.", ((BbCodeAstNodeText) children.get(0)).getText());
		}

		@Test
		@DisplayName("Whitespace and multi-line strings are preserved in text node")
		void testWhitespaceAndNewlines() {
			String multiline = "Line 1\nLine 2\r\n\tTabbed Indent";
			BbCodeAst ast = parser.parseString(multiline);
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(1, children.size());
			assertEquals(multiline, ((BbCodeAstNodeText) children.get(0)).getText());
		}
	}

	@Nested
	@DisplayName("End-to-End Simple Tags and Parameters")
	class SimpleTagsIntegrationTests {

		@Test
		@DisplayName("Basic bold and italic tags enclosing text")
		void testBasicFormattingTags() {
			BbCodeAst ast = parser.parseString("[B]Bold[/B] and [I]Italic[/I]");
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(3, children.size());

			assertTrue(children.get(0) instanceof BbCodeAstNodeTag);
			BbCodeAstNodeTag bNode = (BbCodeAstNodeTag) children.get(0);
			assertEquals(bTag, bNode.getTagDefinition());
			assertArrayEquals(new String[] {"[B]", "[/B]"}, bNode.getRawString());
			assertEquals("Bold", ((BbCodeAstNodeText) bNode.getChildren().get(0)).getText());

			assertTrue(children.get(1) instanceof BbCodeAstNodeText);
			assertEquals(" and ", ((BbCodeAstNodeText) children.get(1)).getText());

			assertTrue(children.get(2) instanceof BbCodeAstNodeTag);
			BbCodeAstNodeTag iNode = (BbCodeAstNodeTag) children.get(2);
			assertEquals(iTag, iNode.getTagDefinition());
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode.getRawString());
			assertEquals("Italic", ((BbCodeAstNodeText) iNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Tag with single root parameter")
		void testTagWithRootParameter() {
			BbCodeAst ast = parser.parseString("[COLOR=red]Warning text[/COLOR]");
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(1, children.size());
			BbCodeAstNodeTag colorNode = (BbCodeAstNodeTag) children.get(0);
			assertEquals(colorTag, colorNode.getTagDefinition());
			assertArrayEquals(new String[] {"[COLOR=red]", "[/COLOR]"}, colorNode.getRawString());
			assertEquals("red", colorNode.getParameters().get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
			assertEquals(1, colorNode.getChildren().size());
			assertEquals("Warning text", ((BbCodeAstNodeText) colorNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Tag with multiple parameters and root parameter")
		void testTagWithMultipleParameters() {
			BbCodeAst ast = parser.parseString("[URL=https://example.com target=_blank]Click Here[/URL]");
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(1, children.size());
			BbCodeAstNodeTag urlNode = (BbCodeAstNodeTag) children.get(0);
			assertEquals(urlTag, urlNode.getTagDefinition());
			assertArrayEquals(new String[] {"[URL=https://example.com target=_blank]", "[/URL]"}, urlNode.getRawString());
			assertEquals("https://example.com", urlNode.getParameters().get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
			assertEquals("_blank", urlNode.getParameters().get("target"));
			assertEquals("Click Here", ((BbCodeAstNodeText) urlNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Tag with named parameters only")
		void testTagWithNamedParametersOnly() {
			BbCodeAst ast = parser.parseString("[QUOTE author=John id=99]Quoted text[/QUOTE]");
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(1, children.size());
			BbCodeAstNodeTag quoteNode = (BbCodeAstNodeTag) children.get(0);
			assertEquals(quoteTag, quoteNode.getTagDefinition());
			assertArrayEquals(new String[] {"[QUOTE author=John id=99]", "[/QUOTE]"}, quoteNode.getRawString());
			assertNull(quoteNode.getParameters().get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
			assertEquals("John", quoteNode.getParameters().get("author"));
			assertEquals("99", quoteNode.getParameters().get("id"));
			assertEquals("Quoted text", ((BbCodeAstNodeText) quoteNode.getChildren().get(0)).getText());
		}
	}

	@Nested
	@DisplayName("End-to-End Hierarchical and Non-Nesting Tags")
	class HierarchyAndNonNestingIntegrationTests {

		@Test
		@DisplayName("Nested formatting tags (B -> I -> U)")
		void testNestedFormattingTags() {
			BbCodeAst ast = parser.parseString("[B][I][U]Underlined Bold Italic[/U][/I][/B]");
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			BbCodeAstNodeTag bNode = (BbCodeAstNodeTag) rootChildren.get(0);
			assertEquals(bTag, bNode.getTagDefinition());
			assertArrayEquals(new String[] {"[B]", "[/B]"}, bNode.getRawString());

			assertEquals(1, bNode.getChildren().size());
			BbCodeAstNodeTag iNode = (BbCodeAstNodeTag) bNode.getChildren().get(0);
			assertEquals(iTag, iNode.getTagDefinition());
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode.getRawString());

			assertEquals(1, iNode.getChildren().size());
			BbCodeAstNodeTag uNode = (BbCodeAstNodeTag) iNode.getChildren().get(0);
			assertEquals(uTag, uNode.getTagDefinition());
			assertArrayEquals(new String[] {"[U]", "[/U]"}, uNode.getRawString());

			assertEquals("Underlined Bold Italic", ((BbCodeAstNodeText) uNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Non-nesting tag (CODE) ignores inner tags and preserves them as raw text")
		void testNonNestingCodeTag() {
			String bbcode = "[CODE=java]\npublic class Main {\n    [B]int x = 42;[/B]\n}\n[/CODE]";
			BbCodeAst ast = parser.parseString(bbcode);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			BbCodeAstNodeTag codeNode = (BbCodeAstNodeTag) rootChildren.get(0);
			assertEquals(codeTag, codeNode.getTagDefinition());
			assertArrayEquals(new String[] {"[CODE=java]", "[/CODE]"}, codeNode.getRawString());
			assertEquals("java", codeNode.getParameters().get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));

			assertEquals(1, codeNode.getChildren().size());
			assertTrue(codeNode.getChildren().get(0) instanceof BbCodeAstNodeText);
			assertEquals("\npublic class Main {\n    [B]int x = 42;[/B]\n}\n", ((BbCodeAstNodeText) codeNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Non-nesting tag (ATTACH) with parameters ignores inner tags")
		void testNonNestingAttachTag() {
			BbCodeAst ast = parser.parseString("[ATTACH=full size=medium][I]attachment_123[/I][/ATTACH]");
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			BbCodeAstNodeTag attachNode = (BbCodeAstNodeTag) rootChildren.get(0);
			assertEquals(attachTag, attachNode.getTagDefinition());
			assertArrayEquals(new String[] {"[ATTACH=full size=medium]", "[/ATTACH]"}, attachNode.getRawString());
			assertEquals("full", attachNode.getParameters().get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME));
			assertEquals("medium", attachNode.getParameters().get("size"));

			assertEquals(1, attachNode.getChildren().size());
			assertEquals("[I]attachment_123[/I]", ((BbCodeAstNodeText) attachNode.getChildren().get(0)).getText());
		}
	}

	@Nested
	@DisplayName("End-to-End Overlapping Tags and Splitting")
	class OverlappingTagsIntegrationTests {

		@Test
		@DisplayName("Two overlapping tags are split and continued")
		void testTwoOverlappingTags() {
			BbCodeAst ast = parser.parseString("[B][I]Bold & Italic[/B] Italic Only[/I]");
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(2, rootChildren.size());

			// Branch 1: [B] containing [I]Bold & Italic[/I]
			BbCodeAstNodeTag bNode = (BbCodeAstNodeTag) rootChildren.get(0);
			assertEquals(bTag, bNode.getTagDefinition());
			assertArrayEquals(new String[] {"[B]", "[/B]"}, bNode.getRawString());
			assertEquals(1, bNode.getChildren().size());
			BbCodeAstNodeTag iNode1 = (BbCodeAstNodeTag) bNode.getChildren().get(0);
			assertEquals(iTag, iNode1.getTagDefinition());
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode1.getRawString());
			assertEquals("Bold & Italic", ((BbCodeAstNodeText) iNode1.getChildren().get(0)).getText());

			// Branch 2: cloned [I] containing " Italic Only"
			BbCodeAstNodeTag iNode2 = (BbCodeAstNodeTag) rootChildren.get(1);
			assertEquals(iTag, iNode2.getTagDefinition());
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode2.getRawString());
			assertEquals(1, iNode2.getChildren().size());
			assertEquals(" Italic Only", ((BbCodeAstNodeText) iNode2.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Three overlapping tags preserve nesting order")
		void testThreeOverlappingTags() {
			BbCodeAst ast = parser.parseString("[B][I][U]1[/B]2[/I]3[/U]");
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(3, rootChildren.size());

			// Branch 1: [B] -> [I] -> [U] -> "1"
			BbCodeAstNodeTag bNode = (BbCodeAstNodeTag) rootChildren.get(0);
			assertEquals(bTag, bNode.getTagDefinition());
			assertArrayEquals(new String[] {"[B]", "[/B]"}, bNode.getRawString());
			BbCodeAstNodeTag iNode1 = (BbCodeAstNodeTag) bNode.getChildren().get(0);
			assertEquals(iTag, iNode1.getTagDefinition());
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode1.getRawString());
			BbCodeAstNodeTag uNode1 = (BbCodeAstNodeTag) iNode1.getChildren().get(0);
			assertEquals(uTag, uNode1.getTagDefinition());
			assertArrayEquals(new String[] {"[U]", "[/U]"}, uNode1.getRawString());
			assertEquals("1", ((BbCodeAstNodeText) uNode1.getChildren().get(0)).getText());

			// Branch 2: [I] -> [U] -> "2"
			BbCodeAstNodeTag iNode2 = (BbCodeAstNodeTag) rootChildren.get(1);
			assertEquals(iTag, iNode2.getTagDefinition());
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode2.getRawString());
			BbCodeAstNodeTag uNode2 = (BbCodeAstNodeTag) iNode2.getChildren().get(0);
			assertEquals(uTag, uNode2.getTagDefinition());
			assertArrayEquals(new String[] {"[U]", "[/U]"}, uNode2.getRawString());
			assertEquals("2", ((BbCodeAstNodeText) uNode2.getChildren().get(0)).getText());

			// Branch 3: [U] -> "3"
			BbCodeAstNodeTag uNode3 = (BbCodeAstNodeTag) rootChildren.get(2);
			assertEquals(uTag, uNode3.getTagDefinition());
			assertArrayEquals(new String[] {"[U]", "[/U]"}, uNode3.getRawString());
			assertEquals("3", ((BbCodeAstNodeText) uNode3.getChildren().get(0)).getText());
		}
	}

	@Nested
	@DisplayName("End-to-End Malformed and Edge Cases")
	class MalformedAndEdgeCasesIntegrationTests {

		@Test
		@DisplayName("Unclosed tags are unwrapped and demoted to text in final AST")
		void testUnclosedTag() {
			BbCodeAst ast = parser.parseString("[B]Unclosed bold content");
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			assertTrue(rootChildren.get(0) instanceof BbCodeAstNodeText);
			assertEquals("[B]Unclosed bold content", ((BbCodeAstNodeText) rootChildren.get(0)).getText());
		}

		@Test
		@DisplayName("Multiple nested unclosed tags are all unwrapped and merged to text")
		void testMultipleNestedUnclosedTags() {
			BbCodeAst ast = parser.parseString("[B]bold [I]italic and bold");
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			assertTrue(rootChildren.get(0) instanceof BbCodeAstNodeText);
			assertEquals("[B]bold [I]italic and bold", ((BbCodeAstNodeText) rootChildren.get(0)).getText());
		}

		@Test
		@DisplayName("Unclosed outer tag unwraps to text while preserving properly closed inner tags")
		void testUnclosedOuterTagPreservesClosedInnerTag() {
			BbCodeAst ast = parser.parseString("[B]Bold start [I]italic[/I] bold end");
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(3, rootChildren.size());
			assertTrue(rootChildren.get(0) instanceof BbCodeAstNodeText);
			assertEquals("[B]Bold start ", ((BbCodeAstNodeText) rootChildren.get(0)).getText());

			assertTrue(rootChildren.get(1) instanceof BbCodeAstNodeTag);
			BbCodeAstNodeTag iNode = (BbCodeAstNodeTag) rootChildren.get(1);
			assertEquals(iTag, iNode.getTagDefinition());
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode.getRawString());
			assertEquals("italic", ((BbCodeAstNodeText) iNode.getChildren().get(0)).getText());

			assertTrue(rootChildren.get(2) instanceof BbCodeAstNodeText);
			assertEquals(" bold end", ((BbCodeAstNodeText) rootChildren.get(2)).getText());
		}

		@Test
		@DisplayName("Unmatched closing tags at root or inside tags become plain text")
		void testUnmatchedClosingTags() {
			BbCodeAst ast = parser.parseString("Hello [/B] [I]Italic [/B] still italic[/I] [/I]");
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(3, rootChildren.size());

			assertEquals("Hello [/B] ", ((BbCodeAstNodeText) rootChildren.get(0)).getText());

			BbCodeAstNodeTag iNode = (BbCodeAstNodeTag) rootChildren.get(1);
			assertEquals(iTag, iNode.getTagDefinition());
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode.getRawString());
			assertEquals("Italic [/B] still italic", ((BbCodeAstNodeText) iNode.getChildren().get(0)).getText());

			assertEquals(" [/I]", ((BbCodeAstNodeText) rootChildren.get(2)).getText());
		}

		@Test
		@DisplayName("Unknown and incomplete tags are treated as plain text")
		void testUnknownAndIncompleteTags() {
			BbCodeAst ast = parser.parseString("This is [UNKNOWN]tag[/UNKNOWN] and [B incomplete");
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			assertEquals("This is [UNKNOWN]tag[/UNKNOWN] and [B incomplete", ((BbCodeAstNodeText) rootChildren.get(0)).getText());
		}

		@Test
		@DisplayName("Handles emojis and unicode across tags properly")
		void testUnicodeAndEmojis() {
			String input = "🎉 [B]Rocket \uD83D\uDE80 Launch[/B] 🌟 [COLOR=blue]Sky \uD83C\uDF0C[/COLOR]";
			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(4, rootChildren.size());
			assertEquals("🎉 ", ((BbCodeAstNodeText) rootChildren.get(0)).getText());

			BbCodeAstNodeTag bNode = (BbCodeAstNodeTag) rootChildren.get(1);
			assertEquals(bTag, bNode.getTagDefinition());
			assertArrayEquals(new String[] {"[B]", "[/B]"}, bNode.getRawString());
			assertEquals("Rocket \uD83D\uDE80 Launch", ((BbCodeAstNodeText) bNode.getChildren().get(0)).getText());

			assertEquals(" 🌟 ", ((BbCodeAstNodeText) rootChildren.get(2)).getText());

			BbCodeAstNodeTag colorNode = (BbCodeAstNodeTag) rootChildren.get(3);
			assertEquals(colorTag, colorNode.getTagDefinition());
			assertArrayEquals(new String[] {"[COLOR=blue]", "[/COLOR]"}, colorNode.getRawString());
			assertEquals("Sky \uD83C\uDF0C", ((BbCodeAstNodeText) colorNode.getChildren().get(0)).getText());
		}
	}
}
