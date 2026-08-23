package aparmar2000.xenforoposter.syntax.bbcode;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.ParsedToken;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.TagToken;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.TextToken;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.TokenListBuilder;

class BbCodeTokenizerTest {

	private BbCodeTagDefinitionRegistry registry;
	private BbCodeTokenizer tokenizer;

	private final BbCodeTagDefinition bTag = new BbCodeTagDefinition("B", true, null);
	private final BbCodeTagDefinition iTag = new BbCodeTagDefinition("I", true, null);
	private final BbCodeTagDefinition uTag = new BbCodeTagDefinition("U", true, null);
	private final BbCodeTagDefinition cTag = new BbCodeTagDefinition("C", true, null);
	private final BbCodeTagDefinition codeTag = new BbCodeTagDefinition("CODE", false, null);
	private final BbCodeTagDefinition colorTag = new BbCodeTagDefinition("COLOR", true, null);
	private final BbCodeTagDefinition urlTag = new BbCodeTagDefinition("URL", true, null);
	private final BbCodeTagDefinition quoteTag = new BbCodeTagDefinition("QUOTE", true, null);
	private final BbCodeTagDefinition attachTag = new BbCodeTagDefinition("ATTACH", false, null);

	@BeforeEach
	void setUp() {
		registry = new BbCodeTagDefinitionRegistry();
		registry.register(bTag);
		registry.register(iTag);
		registry.register(uTag);
		registry.register(cTag);
		registry.register(codeTag);
		registry.register(colorTag);
		registry.register(urlTag);
		registry.register(quoteTag);
		registry.register(attachTag);

		tokenizer = new BbCodeTokenizer(registry);
	}

	@Test
	@DisplayName("TextToken equals, hashCode, toString, and getRawString contract")
	void testTextToken() {
		TextToken token1 = new TextToken("hello");
		TextToken token2 = new TextToken("hello");
		TextToken token3 = new TextToken("world");

		assertEquals("hello", token1.getRawString());
		assertEquals(token1, token2);
		assertEquals(token1.hashCode(), token2.hashCode());
		assertNotEquals(token1, token3);
		assertNotNull(token1.toString());
		assertTrue(token1.toString().contains("hello"));
	}

	@Test
	@DisplayName("TagToken and TagToken.Builder contract and parameter handling")
	void testTagTokenAndBuilder() {
		TagToken.Builder builder = new TagToken.Builder(urlTag);
		builder.parameter("$value", "https://example.com");
		builder.parameter("target", "_blank");
		// Duplicate key should overwrite
		builder.parameter("target", "_self");

		TagToken token1 = builder.build("[URL=https://example.com target=_self]");
		assertEquals("[URL=https://example.com target=_self]", token1.getRawString());
		assertEquals(urlTag, token1.getTagDefinition());
		assertFalse(token1.isEndingTag());
		assertEquals(Map.of("$value", "https://example.com", "target", "_self"), token1.getParameters());

		TagToken token2 = new TagToken(
				"[URL=https://example.com target=_self]",
				urlTag,
				false,
				ImmutableMap.of("$value", "https://example.com", "target", "_self")
		);
		assertEquals(token1, token2);
		assertEquals(token1.hashCode(), token2.hashCode());
		assertTrue(token1.toString().contains("target=_self"));
	}

	@Test
	@DisplayName("TagToken.Builder endingTag generates ending tag tokens properly")
	void testEndingTagTokenBuilder() {
		TagToken.Builder builder = new TagToken.Builder(bTag).endingTag();
		TagToken endingToken = builder.build("[/B]");

		assertEquals("[/B]", endingToken.getRawString());
		assertEquals(bTag, endingToken.getTagDefinition());
		assertTrue(endingToken.isEndingTag());
		assertTrue(endingToken.getParameters().isEmpty());

		TagToken openingToken = new TagToken("[B]", bTag, false, ImmutableMap.of());
		assertNotEquals(openingToken, endingToken);
	}

	@Test
	@DisplayName("TokenListBuilder internal state transitions (startTag, abortTag, tryCloseTag)")
	void testTokenListBuilderStateTransitions() {
		TokenListBuilder builder = new TokenListBuilder(registry.getTagTrie().getRoot());

		assertNotNull(builder.getTagDefinitionTrieRoot());
		assertTrue(builder.getTokens().isEmpty());
		assertFalse(builder.isBuildingTag());
		assertFalse(builder.isBuildingEndingTag());
		assertNull(builder.getPartialTagToken());
		assertNull(builder.getCurrentParameterChunkBuilder());
		assertNull(builder.getParameterKey());
		assertNull(builder.getCurrentTagNode());

		// Cannot close tag when not building or without partial tag
		assertFalse(builder.tryCloseTag(']'));

		// Append text and start tag
		builder.getCurrentTextBuilder().append("prefix ");
		builder.startTag();
		assertTrue(builder.isBuildingTag());
		assertFalse(builder.isBuildingEndingTag());
		assertSame(registry.getTagTrie().getRoot(), builder.getCurrentTagNode());
		// Previous text "prefix " should have been flushed to tokens
		assertEquals(1, builder.getTokens().size());
		assertEquals(new TextToken("prefix "), builder.getTokens().get(0));

		// Abort tag
		builder.getCurrentTextBuilder().append("[invalid");
		builder.abortTag();
		assertFalse(builder.isBuildingTag());
		assertFalse(builder.isBuildingEndingTag());
		assertNull(builder.getCurrentTagNode());
		assertEquals(2, builder.getTokens().size());
		assertEquals(new TextToken("[invalid"), builder.getTokens().get(1));
	}

	@Test
	@DisplayName("TokenListBuilder tryCloseTag validates parameter completion")
	void testTokenListBuilderTryCloseTagValidation() {
		TokenListBuilder builder = new TokenListBuilder(registry.getTagTrie().getRoot());
		builder.startTag();

		// Feed 'B' -> reaches tag node with value
		builder.handleTagCodePoint('B');
		assertNotNull(builder.getCurrentTagNode());
		assertTrue(builder.getCurrentTagNode().hasValue());

		// Feeding '=' starts parameter without value yet
		builder.handleTagCodePoint('=');
		assertNotNull(builder.getPartialTagToken());
		// tryCloseTag should return false because parameter chunk is empty
		assertFalse(builder.tryCloseTag(']'));

		// Feed parameter value chars
		builder.handleTagCodePoint('1');
		// Now closing should succeed
		assertTrue(builder.tryCloseTag(']'));
		assertEquals(1, builder.getTokens().size());
		assertTrue(builder.getTokens().get(0) instanceof TagToken);
		assertFalse(((TagToken) builder.getTokens().get(0)).isEndingTag());
	}

	@Test
	@DisplayName("TokenListBuilder getMergedTokens concatenates consecutive TextTokens")
	void testGetMergedTokensMerging() {
		TokenListBuilder builder = new TokenListBuilder(registry.getTagTrie().getRoot());
		builder.getTokens().add(new TextToken("Hello "));
		builder.getTokens().add(new TextToken("World"));
		builder.getTokens().add(new TagToken("[B]", bTag, false, ImmutableMap.of()));
		builder.getTokens().add(new TextToken("Foo"));
		builder.getTokens().add(new TextToken("Bar"));

		ImmutableList<ParsedToken> merged = builder.getMergedTokens();
		assertEquals(3, merged.size());
		assertEquals(new TextToken("Hello World"), merged.get(0));
		assertEquals(new TagToken("[B]", bTag, false, ImmutableMap.of()), merged.get(1));
		assertEquals(new TextToken("FooBar"), merged.get(2));
	}

	@Test
	@DisplayName("tokenizeString on empty or blank text without tags")
	void testTokenizePlainText() {
		ImmutableList<ParsedToken> emptyResult = tokenizer.tokenizeString("");
		assertTrue(emptyResult.isEmpty());

		ImmutableList<ParsedToken> plainResult = tokenizer.tokenizeString("Just normal text here!");
		assertEquals(1, plainResult.size());
		assertTrue(plainResult.get(0) instanceof TextToken);
		assertEquals("Just normal text here!", plainResult.get(0).getRawString());
	}

	@Test
	@DisplayName("tokenizeString parses opening and ending tags and surrounding text")
	void testTokenizeSimpleTags() {
		ImmutableList<ParsedToken> tokens = tokenizer.tokenizeString("Before [B]bold content[/B] after");

		assertEquals(5, tokens.size());
		assertEquals(new TextToken("Before "), tokens.get(0));

		assertTrue(tokens.get(1) instanceof TagToken);
		TagToken bOpen = (TagToken) tokens.get(1);
		assertEquals("[B]", bOpen.getRawString());
		assertEquals(bTag, bOpen.getTagDefinition());
		assertFalse(bOpen.isEndingTag());
		assertTrue(bOpen.getParameters().isEmpty());

		assertEquals(new TextToken("bold content"), tokens.get(2));

		assertTrue(tokens.get(3) instanceof TagToken);
		TagToken bClose = (TagToken) tokens.get(3);
		assertEquals("[/B]", bClose.getRawString());
		assertEquals(bTag, bClose.getTagDefinition());
		assertTrue(bClose.isEndingTag());
		assertTrue(bClose.getParameters().isEmpty());

		assertEquals(new TextToken(" after"), tokens.get(4));
	}

	@Test
	@DisplayName("tokenizeString parses tags with root value parameter and ending tags")
	void testTokenizeRootParameter() {
		ImmutableList<ParsedToken> tokens = tokenizer.tokenizeString("[COLOR=red]Colored text[/COLOR]");

		assertEquals(3, tokens.size());
		assertTrue(tokens.get(0) instanceof TagToken);
		TagToken colorToken = (TagToken) tokens.get(0);
		assertEquals("[COLOR=red]", colorToken.getRawString());
		assertEquals(colorTag, colorToken.getTagDefinition());
		assertFalse(colorToken.isEndingTag());
		assertEquals("red", colorToken.getRootParameter());

		assertEquals(new TextToken("Colored text"), tokens.get(1));

		assertTrue(tokens.get(2) instanceof TagToken);
		TagToken colorEndToken = (TagToken) tokens.get(2);
		assertEquals("[/COLOR]", colorEndToken.getRawString());
		assertEquals(colorTag, colorEndToken.getTagDefinition());
		assertTrue(colorEndToken.isEndingTag());
	}

	@Test
	@DisplayName("tokenizeString parses tags with multiple named parameters and root parameter")
	void testTokenizeMultipleParameters() {
		ImmutableList<ParsedToken> tokens = tokenizer.tokenizeString("[ATTACH=full size=large align=center]img[/ATTACH]");

		assertEquals(3, tokens.size());
		assertTrue(tokens.get(0) instanceof TagToken);
		TagToken attachToken = (TagToken) tokens.get(0);
		assertEquals("[ATTACH=full size=large align=center]", attachToken.getRawString());
		assertEquals(attachTag, attachToken.getTagDefinition());
		assertFalse(attachToken.isEndingTag());
		assertEquals("full", attachToken.getRootParameter());
		assertEquals("large", attachToken.getParameterValue("size"));
		assertEquals("center", attachToken.getParameterValue("align"));

		assertEquals(new TextToken("img"), tokens.get(1));

		assertTrue(tokens.get(2) instanceof TagToken);
		TagToken attachEndToken = (TagToken) tokens.get(2);
		assertEquals("[/ATTACH]", attachEndToken.getRawString());
		assertEquals(attachTag, attachEndToken.getTagDefinition());
		assertTrue(attachEndToken.isEndingTag());
	}

	@Test
	@DisplayName("tokenizeString parses tags with named parameters only without root parameter")
	void testTokenizeNamedParametersOnly() {
		ImmutableList<ParsedToken> tokens = tokenizer.tokenizeString("[QUOTE author=Admin id=42]Quote body[/QUOTE]");

		assertEquals(3, tokens.size());
		assertTrue(tokens.get(0) instanceof TagToken);
		TagToken quoteToken = (TagToken) tokens.get(0);
		assertEquals("[QUOTE author=Admin id=42]", quoteToken.getRawString());
		assertEquals(quoteTag, quoteToken.getTagDefinition());
		assertFalse(quoteToken.isEndingTag());
		assertNull(quoteToken.getRootParameter());
		assertEquals("Admin", quoteToken.getParameterValue("author"));
		assertEquals("42", quoteToken.getParameterValue("id"));

		assertEquals(new TextToken("Quote body"), tokens.get(1));

		assertTrue(tokens.get(2) instanceof TagToken);
		TagToken quoteEndToken = (TagToken) tokens.get(2);
		assertEquals("[/QUOTE]", quoteEndToken.getRawString());
		assertEquals(quoteTag, quoteEndToken.getTagDefinition());
		assertTrue(quoteEndToken.isEndingTag());
	}

	@Test
	@DisplayName("tokenizeString properly disambiguates overlapping tag definitions")
	void testOverlappingTagsTokenization() {
		// Tags registered: C, CODE, COLOR
		ImmutableList<ParsedToken> tokens = tokenizer.tokenizeString("[C]short[/C] [CODE=java]code[/CODE] [COLOR=blue]color[/COLOR]");

		assertEquals(11, tokens.size());
		// [C]
		assertTrue(tokens.get(0) instanceof TagToken);
		TagToken cToken = (TagToken) tokens.get(0);
		assertEquals("[C]", cToken.getRawString());
		assertEquals(cTag, cToken.getTagDefinition());
		assertFalse(cToken.isEndingTag());

		assertEquals(new TextToken("short"), tokens.get(1));

		// [/C]
		assertTrue(tokens.get(2) instanceof TagToken);
		TagToken cEndToken = (TagToken) tokens.get(2);
		assertEquals("[/C]", cEndToken.getRawString());
		assertEquals(cTag, cEndToken.getTagDefinition());
		assertTrue(cEndToken.isEndingTag());

		assertEquals(new TextToken(" "), tokens.get(3));

		// [CODE=java]
		assertTrue(tokens.get(4) instanceof TagToken);
		TagToken codeToken = (TagToken) tokens.get(4);
		assertEquals("[CODE=java]", codeToken.getRawString());
		assertEquals(codeTag, codeToken.getTagDefinition());
		assertFalse(codeToken.isEndingTag());
		assertEquals("java", codeToken.getRootParameter());

		assertEquals(new TextToken("code"), tokens.get(5));

		// [/CODE]
		assertTrue(tokens.get(6) instanceof TagToken);
		TagToken codeEndToken = (TagToken) tokens.get(6);
		assertEquals("[/CODE]", codeEndToken.getRawString());
		assertEquals(codeTag, codeEndToken.getTagDefinition());
		assertTrue(codeEndToken.isEndingTag());

		assertEquals(new TextToken(" "), tokens.get(7));

		// [COLOR=blue]
		assertTrue(tokens.get(8) instanceof TagToken);
		TagToken colorToken = (TagToken) tokens.get(8);
		assertEquals("[COLOR=blue]", colorToken.getRawString());
		assertEquals(colorTag, colorToken.getTagDefinition());
		assertFalse(colorToken.isEndingTag());
		assertEquals("blue", colorToken.getRootParameter());

		assertEquals(new TextToken("color"), tokens.get(9));

		// [/COLOR]
		assertTrue(tokens.get(10) instanceof TagToken);
		TagToken colorEndToken = (TagToken) tokens.get(10);
		assertEquals("[/COLOR]", colorEndToken.getRawString());
		assertEquals(colorTag, colorEndToken.getTagDefinition());
		assertTrue(colorEndToken.isEndingTag());
	}

	@Test
	@DisplayName("tokenizeString treats unknown and incomplete tags as plain text")
	void testUnknownAndIncompleteTags() {
		// Unknown tag
		ImmutableList<ParsedToken> unknownTokens = tokenizer.tokenizeString("This is [UNKNOWN]tag[/UNKNOWN] test");
		assertEquals(1, unknownTokens.size());
		assertEquals("This is [UNKNOWN]tag[/UNKNOWN] test", unknownTokens.get(0).getRawString());

		// Incomplete tag (missing closing bracket)
		ImmutableList<ParsedToken> incompleteTokens1 = tokenizer.tokenizeString("Incomplete [B bold text here");
		assertEquals(1, incompleteTokens1.size());
		assertEquals("Incomplete [B bold text here", incompleteTokens1.get(0).getRawString());

		// Incomplete ending tag
		ImmutableList<ParsedToken> incompleteEnding = tokenizer.tokenizeString("Incomplete [/B bold text");
		assertEquals(1, incompleteEnding.size());
		assertEquals("Incomplete [/B bold text", incompleteEnding.get(0).getRawString());

		// Incomplete parameter (ends with '=')
		ImmutableList<ParsedToken> incompleteTokens2 = tokenizer.tokenizeString("Incomplete [COLOR=]text");
		assertEquals(1, incompleteTokens2.size());
		assertEquals("Incomplete [COLOR=]text", incompleteTokens2.get(0).getRawString());
	}

	@Test
	@DisplayName("tokenizeString handles unicode and surrogate pairs properly")
	void testUnicodeCharacters() {
		String unicodeText = "Emoji \uD83D\uDE00 [B]Bold \uD83D\uDE80 Rocket[/B] End \uD83C\uDF89";
		ImmutableList<ParsedToken> tokens = tokenizer.tokenizeString(unicodeText);

		assertEquals(5, tokens.size());
		assertEquals(new TextToken("Emoji \uD83D\uDE00 "), tokens.get(0));

		assertTrue(tokens.get(1) instanceof TagToken);
		assertEquals(bTag, ((TagToken) tokens.get(1)).getTagDefinition());
		assertFalse(((TagToken) tokens.get(1)).isEndingTag());

		assertEquals(new TextToken("Bold \uD83D\uDE80 Rocket"), tokens.get(2));

		assertTrue(tokens.get(3) instanceof TagToken);
		assertEquals(bTag, ((TagToken) tokens.get(3)).getTagDefinition());
		assertTrue(((TagToken) tokens.get(3)).isEndingTag());

		assertEquals(new TextToken(" End \uD83C\uDF89"), tokens.get(4));
	}

	@Test
	@DisplayName("tokenizeString handles nested and adjacent opening/ending tags")
	void testAdjacentAndNestedTags() {
		ImmutableList<ParsedToken> tokens = tokenizer.tokenizeString("[B][I]Bold & Italic[/I][/B]");

		assertEquals(5, tokens.size());
		assertTrue(tokens.get(0) instanceof TagToken);
		assertEquals(bTag, ((TagToken) tokens.get(0)).getTagDefinition());
		assertFalse(((TagToken) tokens.get(0)).isEndingTag());

		assertTrue(tokens.get(1) instanceof TagToken);
		assertEquals(iTag, ((TagToken) tokens.get(1)).getTagDefinition());
		assertFalse(((TagToken) tokens.get(1)).isEndingTag());

		assertEquals(new TextToken("Bold & Italic"), tokens.get(2));

		assertTrue(tokens.get(3) instanceof TagToken);
		assertEquals(iTag, ((TagToken) tokens.get(3)).getTagDefinition());
		assertTrue(((TagToken) tokens.get(3)).isEndingTag());

		assertTrue(tokens.get(4) instanceof TagToken);
		assertEquals(bTag, ((TagToken) tokens.get(4)).getTagDefinition());
		assertTrue(((TagToken) tokens.get(4)).isEndingTag());
	}
}
