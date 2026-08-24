package aparmar2000.xenforoposter.syntax.bbcode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNode;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeRoot;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeTag;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeText;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.TagToken;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.TextToken;

@ExtendWith(MockitoExtension.class)
class BbCodeAstParserTest {

	@Mock
	private BbCodeTagDefinitionRegistry mockRegistry;

	@Mock
	private BbCodeTokenizer mockTokenizer;

	private BbCodeAstParser parser;

	private final BbCodeTagDefinition boldTag = new BbCodeTagDefinition("B", true, null);
	private final BbCodeTagDefinition italicTag = new BbCodeTagDefinition("I", true, null);
	private final BbCodeTagDefinition codeTag = new BbCodeTagDefinition("CODE", false, null);
	private final BbCodeTagDefinition colorTag = new BbCodeTagDefinition("COLOR", true, null);

	@BeforeEach
	void setUp() {
		parser = new BbCodeAstParser(mockRegistry, mockTokenizer);
	}

	@Nested
	@DisplayName("allowsTagChildren logic")
	class AllowsTagChildrenTests {

		@Test
		@DisplayName("null node and leaf nodes reject tag children")
		void testNullAndLeafNodes() {
			assertFalse(parser.allowsTagChildren(null));
			assertFalse(parser.allowsTagChildren(new BbCodeAstNodeText("leaf text")));
		}

		@Test
		@DisplayName("root node allows tag children")
		void testRootNode() {
			assertTrue(parser.allowsTagChildren(new BbCodeAstNodeRoot()));
		}

		@Test
		@DisplayName("tag nodes allow child tags based on tag definition")
		void testTagNodes() {
			BbCodeAstNodeTag nestableTagNode = new BbCodeAstNodeTag("[B]", boldTag, ImmutableMap.of());
			assertTrue(parser.allowsTagChildren(nestableTagNode));

			BbCodeAstNodeTag nonNestableTagNode = new BbCodeAstNodeTag("[CODE]", codeTag, ImmutableMap.of());
			assertFalse(parser.allowsTagChildren(nonNestableTagNode));
		}
	}

	@Nested
	@DisplayName("AST Tree Construction with Mocked Tokenizer")
	class MockedTokenizerParsingTests {

		@Test
		@DisplayName("Empty token list yields empty root node")
		void testEmptyTokens() {
			when(mockTokenizer.tokenizeString("")).thenReturn(ImmutableList.of());

			BbCodeAst ast = parser.parseString("");

			assertNotNull(ast);
			assertNotNull(ast.getRootNode());
			assertFalse(ast.getRootNode().hasChildren());
			verify(mockTokenizer).tokenizeString("");
		}

		@Test
		@DisplayName("Single text token creates single text child under root")
		void testSingleTextToken() {
			String input = "Hello world";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(new TextToken("Hello world")));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(1, children.size());
			assertTrue(children.get(0) instanceof BbCodeAstNodeText);
			assertEquals("Hello world", ((BbCodeAstNodeText) children.get(0)).getText());
		}

		@Test
		@DisplayName("Consecutive text tokens are merged into one text node")
		void testConsecutiveTextTokensMerged() {
			String input = "HelloWorld";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TextToken("Hello"),
					new TextToken(" "),
					new TextToken("World")
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(1, children.size());
			assertEquals("Hello World", ((BbCodeAstNodeText) children.get(0)).getText());
		}

		@Test
		@DisplayName("Opening and matching ending tag token creates tag node hierarchy")
		void testMatchedTagTokens() {
			String input = "[B]content[/B]";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[B]", boldTag, false, ImmutableMap.of()),
					new TextToken("content"),
					new TagToken("[/B]", boldTag, true, ImmutableMap.of())
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(1, children.size());
			assertTrue(children.get(0) instanceof BbCodeAstNodeTag);

			BbCodeAstNodeTag bNode = (BbCodeAstNodeTag) children.get(0);
			assertArrayEquals(new String[] {"[B]", "[/B]"}, bNode.getRawString());
			assertEquals(boldTag, bNode.getTagDefinition());
			assertEquals(1, bNode.getChildren().size());
			assertEquals("content", ((BbCodeAstNodeText) bNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Tag tokens with parameters are attached correctly")
		void testTagTokensWithParameters() {
			String input = "[COLOR=red]Text[/COLOR]";
			ImmutableMap<String, String> params = ImmutableMap.of("$value", "red");
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[COLOR=red]", colorTag, false, params),
					new TextToken("Text"),
					new TagToken("[/COLOR]", colorTag, true, ImmutableMap.of())
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> children = ast.getRootNode().getChildren();

			assertEquals(1, children.size());
			BbCodeAstNodeTag colorNode = (BbCodeAstNodeTag) children.get(0);
			assertArrayEquals(new String[] {"[COLOR=red]", "[/COLOR]"}, colorNode.getRawString());
			assertEquals(params, colorNode.getParameters());
			assertEquals("Text", ((BbCodeAstNodeText) colorNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Nested tag tokens create parent-child tag hierarchy")
		void testNestedTagTokens() {
			String input = "[B][I]Inner[/I][/B]";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[B]", boldTag, false, ImmutableMap.of()),
					new TagToken("[I]", italicTag, false, ImmutableMap.of()),
					new TextToken("Inner"),
					new TagToken("[/I]", italicTag, true, ImmutableMap.of()),
					new TagToken("[/B]", boldTag, true, ImmutableMap.of())
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			BbCodeAstNodeTag bNode = (BbCodeAstNodeTag) rootChildren.get(0);
			assertArrayEquals(new String[] {"[B]", "[/B]"}, bNode.getRawString());
			assertEquals(boldTag, bNode.getTagDefinition());

			assertEquals(1, bNode.getChildren().size());
			BbCodeAstNodeTag iNode = (BbCodeAstNodeTag) bNode.getChildren().get(0);
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode.getRawString());
			assertEquals(italicTag, iNode.getTagDefinition());

			assertEquals(1, iNode.getChildren().size());
			assertEquals("Inner", ((BbCodeAstNodeText) iNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Tag token inside a non-nesting tag is converted to raw text node")
		void testTagInsideNonNestingTagConvertedToText() {
			String input = "[CODE][B]literal[/B][/CODE]";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[CODE]", codeTag, false, ImmutableMap.of()),
					new TagToken("[B]", boldTag, false, ImmutableMap.of()),
					new TextToken("literal"),
					new TagToken("[/B]", boldTag, true, ImmutableMap.of()),
					new TagToken("[/CODE]", codeTag, true, ImmutableMap.of())
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			BbCodeAstNodeTag codeNode = (BbCodeAstNodeTag) rootChildren.get(0);
			assertArrayEquals(new String[] {"[CODE]", "[/CODE]"}, codeNode.getRawString());
			assertEquals(codeTag, codeNode.getTagDefinition());

			// Inside CODE, the tokens should be merged as raw text
			assertEquals(1, codeNode.getChildren().size());
			assertTrue(codeNode.getChildren().get(0) instanceof BbCodeAstNodeText);
			assertEquals("[B]literal[/B]", ((BbCodeAstNodeText) codeNode.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Overlapping tag tokens split inner tag and clone continuation node")
		void testOverlappingTagTokensSplitting() {
			String input = "[B][I]bi[/B]i[/I]";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[B]", boldTag, false, ImmutableMap.of()),
					new TagToken("[I]", italicTag, false, ImmutableMap.of()),
					new TextToken("bi"),
					new TagToken("[/B]", boldTag, true, ImmutableMap.of()),
					new TextToken("i"),
					new TagToken("[/I]", italicTag, true, ImmutableMap.of())
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(2, rootChildren.size());

			// Branch 1: [B] containing [I]bi[/I]
			BbCodeAstNodeTag bNode = (BbCodeAstNodeTag) rootChildren.get(0);
			assertArrayEquals(new String[] {"[B]", "[/B]"}, bNode.getRawString());
			assertEquals(boldTag, bNode.getTagDefinition());
			BbCodeAstNodeTag iNode1 = (BbCodeAstNodeTag) bNode.getChildren().get(0);
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode1.getRawString());
			assertEquals(italicTag, iNode1.getTagDefinition());
			assertEquals("bi", ((BbCodeAstNodeText) iNode1.getChildren().get(0)).getText());

			// Branch 2: cloned [I] containing i
			BbCodeAstNodeTag iNode2 = (BbCodeAstNodeTag) rootChildren.get(1);
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode2.getRawString());
			assertEquals(italicTag, iNode2.getTagDefinition());
			assertEquals("i", ((BbCodeAstNodeText) iNode2.getChildren().get(0)).getText());
		}

		@Test
		@DisplayName("Unclosed tag tokens unwrap into raw text node under parent")
		void testUnclosedTagTokens() {
			String input = "[B]unclosed";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[B]", boldTag, false, ImmutableMap.of()),
					new TextToken("unclosed")
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			assertTrue(rootChildren.get(0) instanceof BbCodeAstNodeText);
			assertEquals("[B]unclosed", ((BbCodeAstNodeText) rootChildren.get(0)).getText());
		}

		@Test
		@DisplayName("Nested unclosed tags unwrap and merge hierarchically into text")
		void testNestedUnclosedTagsUnwrap() {
			String input = "[B]bold [I]italic";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[B]", boldTag, false, ImmutableMap.of()),
					new TextToken("bold "),
					new TagToken("[I]", italicTag, false, ImmutableMap.of()),
					new TextToken("italic")
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			assertTrue(rootChildren.get(0) instanceof BbCodeAstNodeText);
			assertEquals("[B]bold [I]italic", ((BbCodeAstNodeText) rootChildren.get(0)).getText());
		}

		@Test
		@DisplayName("Unclosed outer tag unwraps while preserving closed child tag nodes")
		void testUnclosedOuterTagWithClosedChildTag() {
			String input = "[B]prefix [I]italic[/I] suffix";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[B]", boldTag, false, ImmutableMap.of()),
					new TextToken("prefix "),
					new TagToken("[I]", italicTag, false, ImmutableMap.of()),
					new TextToken("italic"),
					new TagToken("[/I]", italicTag, true, ImmutableMap.of()),
					new TextToken(" suffix")
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(3, rootChildren.size());
			assertTrue(rootChildren.get(0) instanceof BbCodeAstNodeText);
			assertEquals("[B]prefix ", ((BbCodeAstNodeText) rootChildren.get(0)).getText());

			assertTrue(rootChildren.get(1) instanceof BbCodeAstNodeTag);
			BbCodeAstNodeTag iNode = (BbCodeAstNodeTag) rootChildren.get(1);
			assertArrayEquals(new String[] {"[I]", "[/I]"}, iNode.getRawString());
			assertEquals(italicTag, iNode.getTagDefinition());
			assertEquals("italic", ((BbCodeAstNodeText) iNode.getChildren().get(0)).getText());

			assertTrue(rootChildren.get(2) instanceof BbCodeAstNodeText);
			assertEquals(" suffix", ((BbCodeAstNodeText) rootChildren.get(2)).getText());
		}

		@Test
		@DisplayName("Unmatched closing tag at root level falls through as text")
		void testUnmatchedEndingTagAtRoot() {
			String input = "[/B]";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[/B]", boldTag, true, ImmutableMap.of())
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			assertEquals("[/B]", ((BbCodeAstNodeText) rootChildren.get(0)).getText());
		}

		@Test
		@DisplayName("Unmatched closing tag inside an open tag falls through as text")
		void testUnmatchedEndingTagInsideTag() {
			String input = "[B][/I][/B]";
			when(mockTokenizer.tokenizeString(input)).thenReturn(ImmutableList.of(
					new TagToken("[B]", boldTag, false, ImmutableMap.of()),
					new TagToken("[/I]", italicTag, true, ImmutableMap.of()),
					new TagToken("[/B]", boldTag, true, ImmutableMap.of())
			));

			BbCodeAst ast = parser.parseString(input);
			List<BbCodeAstNode> rootChildren = ast.getRootNode().getChildren();

			assertEquals(1, rootChildren.size());
			BbCodeAstNodeTag bNode = (BbCodeAstNodeTag) rootChildren.get(0);
			assertArrayEquals(new String[] {"[B]", "[/B]"}, bNode.getRawString());
			assertEquals(boldTag, bNode.getTagDefinition());
			assertEquals(1, bNode.getChildren().size());
			assertEquals("[/I]", ((BbCodeAstNodeText) bNode.getChildren().get(0)).getText());
		}
	}

	@Nested
	@DisplayName("AST Node Model Contracts")
	class AstNodeModelTests {

		@Test
		@DisplayName("BbCodeAstNodeText merge, clone, and properties")
		void testBbCodeAstNodeText() {
			BbCodeAstNodeText node1 = new BbCodeAstNodeText("Hello ");
			BbCodeAstNodeText node2 = new BbCodeAstNodeText("World");

			BbCodeAstNodeText merged = node1.merge(node2);
			assertEquals("Hello World", merged.getText());
			assertFalse(merged.hasChildren());
			assertTrue(merged.getChildren().isEmpty());

			BbCodeAstNodeText cloned = node1.clone();
			assertEquals(node1, cloned);
			assertEquals(node1.hashCode(), cloned.hashCode());
			assertNotSame(node1, cloned);
		}

		@Test
		@DisplayName("BbCodeAstNodeTag clone, children, and properties")
		void testBbCodeAstNodeTag() {
			ImmutableMap<String, String> params = ImmutableMap.of("$value", "red", "key", "val");
			BbCodeAstNodeTag tagNode = new BbCodeAstNodeTag(new String[]{"[COLOR=red key=val]", "[/COLOR]"}, colorTag, params);

			assertArrayEquals(new String[] {"[COLOR=red key=val]", "[/COLOR]"}, tagNode.getRawString());
			assertEquals(colorTag, tagNode.getTagDefinition());
			assertEquals(params, tagNode.getParameters());
			assertFalse(tagNode.hasChildren());

			tagNode.getChildren().add(new BbCodeAstNodeText("text"));
			assertTrue(tagNode.hasChildren());

			BbCodeAstNodeTag cloned = tagNode.clone();
			assertEquals(tagNode, cloned);
			assertEquals(tagNode.hashCode(), cloned.hashCode());
			assertNotSame(tagNode, cloned);
			assertNotSame(tagNode.getChildren(), cloned.getChildren());
			assertArrayEquals(tagNode.getRawString(), cloned.getRawString());
			assertEquals(1, cloned.getChildren().size());
		}

		@Test
		@DisplayName("BbCodeAstNodeRoot clone and properties")
		void testBbCodeAstNodeRoot() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			assertFalse(root.hasChildren());

			root.getChildren().add(new BbCodeAstNodeText("child"));
			assertTrue(root.hasChildren());

			BbCodeAstNodeRoot cloned = root.clone();
			assertEquals(root, cloned);
			assertEquals(root.hashCode(), cloned.hashCode());
			assertNotSame(root, cloned);
			assertNotSame(root.getChildren(), cloned.getChildren());
			assertEquals(1, cloned.getChildren().size());
		}

		@Test
		@DisplayName("BbCodeAst wrapper getRootNode")
		void testBbCodeAstWrapper() {
			BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();
			BbCodeAst ast = new BbCodeAst(root);
			assertSame(root, ast.getRootNode());
		}
	}
}
