package aparmar2000.xenforoposter.syntax;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeTag;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeText;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAstParser;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinitionRegistry;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer;
import aparmar2000.xenforoposter.syntax.html.HtmlAst;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeTag;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeText;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;

class AstUtilsTest {

	private BbCodeTagDefinitionRegistry bbRegistry;
	private HtmlTagDefinitionRegistry htmlRegistry;
	private BbCodeAstParser parser;

	@BeforeEach
	void setUp() {
		htmlRegistry = new HtmlTagDefinitionRegistry();
		bbRegistry = new BbCodeTagDefinitionRegistry();
		DefaultTagDefinitions.registerBaseTags(bbRegistry, htmlRegistry);
		BbCodeTokenizer tokenizer = new BbCodeTokenizer(bbRegistry);
		parser = new BbCodeAstParser(bbRegistry, tokenizer);
	}

	@Test
	@DisplayName("Should find BBCode tags and text nodes in AST")
	void testBbCodeAstFind() {
		BbCodeAst ast = parser.parseString("[B]Bold [I]Italic[/I][/B] and trailing text");

		List<BbCodeAstNodeTag> bTags = ast.findTags("B");
		assertEquals(1, bTags.size());

		List<BbCodeAstNodeTag> iTags = ast.findTags("I");
		assertEquals(1, iTags.size());

		List<BbCodeAstNodeText> textNodes = ast.findTextNodes();
		assertEquals(3, textNodes.size()); // "Bold ", "Italic", " and trailing text"

		List<BbCodeAstNodeText> matching = ast.findTextNodesContaining("trailing");
		assertEquals(1, matching.size());
	}

	@Test
	@DisplayName("Should replace text across BBCode AST text nodes")
	void testBbCodeAstReplaceText() {
		BbCodeAst ast = parser.parseString("Hello foo, [B]welcome foo![/B]");
		ast.replaceText("foo", "bar");

		assertEquals("Hello bar, [B]welcome bar![/B]", ast.toBbCodeString());
	}

	@Test
	@DisplayName("Should replace text using regex in BBCode AST")
	void testBbCodeAstReplaceTextRegex() {
		BbCodeAst ast = parser.parseString("Order #100 and [I]Order #200[/I]");
		ast.replaceText(Pattern.compile("#(\\d+)"), m -> "ID-" + m.group(1));

		assertEquals("Order ID-100 and [I]Order ID-200[/I]", ast.toBbCodeString());
	}

	@Test
	@DisplayName("Should replace tags in BBCode AST")
	void testBbCodeAstReplaceTags() {
		BbCodeAst ast = parser.parseString("[B]Important Notice[/B]");
		BbCodeTagDefinition iDef = bbRegistry.getByTagString("I");

		ast.replaceTags("B", tagNode -> {
			BbCodeAstNodeTag newTag = new BbCodeAstNodeTag(iDef, tagNode.getParameters());
			newTag.getChildren().addAll(tagNode.getChildren());
			return newTag;
		});

		assertEquals("[I]Important Notice[/I]", ast.toBbCodeString());
	}

	@Test
	@DisplayName("Should find and replace text and tags in HtmlAst")
	void testHtmlAstFindAndReplace() {
		BbCodeAst bbAst = parser.parseString("[B]Hello HTML[/B]");
		HtmlAst htmlAst = bbAst.mapToHtmlAst();

		List<HtmlAstNodeTag> bTags = htmlAst.findTags("b");
		assertEquals(1, bTags.size());

		List<HtmlAstNodeText> textNodes = htmlAst.findTextNodes();
		assertEquals(1, textNodes.size());
		assertEquals("Hello HTML", textNodes.get(0).getText());

		htmlAst.replaceText("Hello", "Greetings");
		assertTrue(htmlAst.mapToHtmlString().contains("Greetings HTML"));
	}
}
