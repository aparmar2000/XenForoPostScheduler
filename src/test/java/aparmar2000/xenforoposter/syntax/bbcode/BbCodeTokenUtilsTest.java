package aparmar2000.xenforoposter.syntax.bbcode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.syntax.DefaultTagDefinitions;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.ParsedToken;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.TagToken;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.TextToken;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinitionRegistry;

class BbCodeTokenUtilsTest {

	private BbCodeTokenizer tokenizer;
	private BbCodeTagDefinitionRegistry bbRegistry;

	@BeforeEach
	void setUp() {
		HtmlTagDefinitionRegistry htmlRegistry = new HtmlTagDefinitionRegistry();
		bbRegistry = new BbCodeTagDefinitionRegistry();
		DefaultTagDefinitions.registerBaseTags(bbRegistry, htmlRegistry);
		tokenizer = new BbCodeTokenizer(bbRegistry);
	}

	@Test
	@DisplayName("Should find tokens by predicate, type, tag name, and text pattern")
	void testTokenSearch() {
		String input = "[B]Hello[/B] world [I]foo bar[/I]";
		List<ParsedToken> tokens = tokenizer.tokenizeString(input);

		List<TagToken> bTags = BbCodeTokenUtils.findTagTokens(tokens, "B");
		assertEquals(2, bTags.size()); // [B] and [/B]

		List<TextToken> textTokens = BbCodeTokenUtils.findTextTokens(tokens);
		assertEquals(3, textTokens.size()); // "Hello", " world ", "foo bar"

		List<TextToken> matchedText = BbCodeTokenUtils.findTextTokensMatching(tokens, Pattern.compile("world"));
		assertEquals(1, matchedText.size());
		assertEquals(" world ", matchedText.get(0).getRawString());

		int idx = BbCodeTokenUtils.indexOf(tokens, t -> t instanceof TagToken && ((TagToken) t).getTagDefinition().getTag().equals("I"));
		assertEquals(4, idx);
	}

	@Test
	@DisplayName("Should replace text within text tokens")
	void testReplaceText() {
		String input = "[B]Hello foo[/B] and foo";
		List<ParsedToken> tokens = tokenizer.tokenizeString(input);

		List<ParsedToken> replaced = BbCodeTokenUtils.replaceText(tokens, "foo", "bar");
		assertEquals("[B]Hello bar[/B] and bar", BbCodeTokenUtils.tokensToString(replaced));
	}

	@Test
	@DisplayName("Should replace text using regex in text tokens")
	void testReplaceTextRegex() {
		String input = "[B]User123[/B] met User456";
		List<ParsedToken> tokens = tokenizer.tokenizeString(input);

		List<ParsedToken> replaced = BbCodeTokenUtils.replaceText(tokens, Pattern.compile("User(\\d+)"), m -> "Player" + m.group(1));
		assertEquals("[B]Player123[/B] met Player456", BbCodeTokenUtils.tokensToString(replaced));
	}

	@Test
	@DisplayName("Should replace tag name and tag definition across token list")
	void testReplaceTag() {
		String input = "[B]Bold Text[/B]";
		List<ParsedToken> tokens = tokenizer.tokenizeString(input);

		BbCodeTagDefinition iDef = bbRegistry.getByTagString("I");
		List<ParsedToken> replaced = BbCodeTokenUtils.replaceTag(tokens, "B", "I", iDef);

		assertEquals("[I]Bold Text[/I]", BbCodeTokenUtils.tokensToString(replaced));
	}

	@Test
	@DisplayName("Should remove tokens matching predicate")
	void testRemoveTokens() {
		String input = "[B]Bold[/B]";
		List<ParsedToken> tokens = tokenizer.tokenizeString(input);

		List<ParsedToken> stripped = BbCodeTokenUtils.removeTokens(tokens, t -> t instanceof TagToken);
		assertEquals(1, stripped.size());
		assertEquals("Bold", BbCodeTokenUtils.tokensToString(stripped));
	}
}
