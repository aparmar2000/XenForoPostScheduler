package aparmar2000.xenforoposter.syntax.bbcode;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.DefaultTagDefinitions;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeRoot;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeTag;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeText;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;

class BbCodeAstSerializationTest {

	private BbCodeTagDefinitionRegistry bbCodeRegistry;
	private HtmlTagDefinitionRegistry htmlRegistry;
	private BbCodeTokenizer tokenizer;
	private BbCodeAstParser parser;

	@BeforeEach
	void setUp() {
		htmlRegistry = new HtmlTagDefinitionRegistry();
		bbCodeRegistry = new BbCodeTagDefinitionRegistry();
		DefaultTagDefinitions.registerBaseTags(bbCodeRegistry, htmlRegistry);
		tokenizer = new BbCodeTokenizer(bbCodeRegistry);
		parser = new BbCodeAstParser(bbCodeRegistry, tokenizer);
	}

	@Test
	@DisplayName("Should serialize simple text AST back to original string")
	void testPlainTextSerialization() {
		String input = "Hello world, this is plain text.";
		BbCodeAst ast = parser.parseString(input);
		assertEquals(input, ast.toBbCodeString());
	}

	@Test
	@DisplayName("Should serialize standard tags back to original string")
	void testStandardTagsSerialization() {
		String input = "[B]Bold[/B] and [I]Italic[/I] and [U]Underline[/U]";
		BbCodeAst ast = parser.parseString(input);
		assertEquals(input, ast.toBbCodeString());
	}

	@Test
	@DisplayName("Should serialize nested and parameterized tags back to original string")
	void testNestedAndParameterizedTagsSerialization() {
		String input = "[COLOR=#ff0000][B]Red Bold[/B][/COLOR] and [QUOTE=Author]Quoted text[/QUOTE]";
		BbCodeAst ast = parser.parseString(input);
		assertEquals(input, ast.toBbCodeString());
	}

	@Test
	@DisplayName("Should serialize programmatically constructed AST nodes with attributes")
	void testProgrammaticNodeSerialization() {
		BbCodeTagDefinition boldDef = bbCodeRegistry.getByTagString("B");
		BbCodeTagDefinition colorDef = bbCodeRegistry.getByTagString("COLOR");

		BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();

		// [COLOR=#00ff00][B]Green Bold[/B][/COLOR]
		BbCodeAstNodeTag colorTag = new BbCodeAstNodeTag(colorDef, ImmutableMap.of(BbCodeAstNodeTag.ROOT_PARAMETER_NAME, "#00ff00"));
		BbCodeAstNodeTag boldTag = new BbCodeAstNodeTag(boldDef, ImmutableMap.of());
		boldTag.getChildren().add(new BbCodeAstNodeText("Green Bold"));
		colorTag.getChildren().add(boldTag);

		root.getChildren().add(colorTag);

		BbCodeAst ast = new BbCodeAst(root);
		assertEquals("[COLOR=#00ff00][B]Green Bold[/B][/COLOR]", ast.toBbCodeString());
	}

	@Test
	@DisplayName("Should serialize programmatically constructed tag with multiple parameters")
	void testMultiParameterTagSerialization() {
		BbCodeTagDefinition urlDef = bbCodeRegistry.getByTagString("URL");
		BbCodeAstNodeRoot root = new BbCodeAstNodeRoot();

		BbCodeAstNodeTag urlTag = new BbCodeAstNodeTag(urlDef, ImmutableMap.of(
				BbCodeAstNodeTag.ROOT_PARAMETER_NAME, "https://example.com"
		));
		urlTag.getChildren().add(new BbCodeAstNodeText("Click Here"));
		root.getChildren().add(urlTag);

		BbCodeAst ast = new BbCodeAst(root);
		assertEquals("[URL=https://example.com]Click Here[/URL]", ast.toBbCodeString());
	}
}
