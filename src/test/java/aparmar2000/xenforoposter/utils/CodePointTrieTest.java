package aparmar2000.xenforoposter.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CodePointTrieTest {

	@Test
	@DisplayName("Empty trie should have empty root with no value or children")
	void testEmptyTrie() {
		CodePointTrie<String> trie = new CodePointTrie.Builder<String>().build();

		assertNotNull(trie.getRoot());
		assertNull(trie.getRoot().getValue());
		assertFalse(trie.getRoot().hasValue());
		assertTrue(trie.getRoot().getChildren().isEmpty());
		assertNull(trie.getRoot().getChild('a'));
	}

	@Test
	@DisplayName("Single key traversal and retrieval")
	void testSingleKey() {
		CodePointTrie<Integer> trie = new CodePointTrie.Builder<Integer>()
				.addValue("cat", 42)
				.build();

		CodePointTrie.CodePointTrieNode<Integer> nodeC = trie.getRoot().getChild('c');
		assertNotNull(nodeC);
		assertFalse(nodeC.hasValue());

		CodePointTrie.CodePointTrieNode<Integer> nodeA = nodeC.getChild('a');
		assertNotNull(nodeA);
		assertFalse(nodeA.hasValue());

		CodePointTrie.CodePointTrieNode<Integer> nodeT = nodeA.getChild('t');
		assertNotNull(nodeT);
		assertTrue(nodeT.hasValue());
		assertEquals(42, nodeT.getValue());
		assertTrue(nodeT.getChildren().isEmpty());

		assertNull(trie.getRoot().getChild('d'));
		assertNull(nodeC.getChild('o'));
	}

	@Test
	@DisplayName("Branching and overlapping prefixes")
	void testBranchingAndOverlappingPrefixes() {
		CodePointTrie<String> trie = new CodePointTrie.Builder<String>()
				.addValue("car", "vehicle")
				.addValue("cart", "shopping")
				.addValue("cat", "feline")
				.addValue("dog", "canine")
				.build();

		// Root checks
		assertFalse(trie.getRoot().hasValue());
		assertEquals(2, trie.getRoot().getChildren().size()); // 'c' and 'd'

		// 'c' -> 'a'
		CodePointTrie.CodePointTrieNode<String> nodeC = trie.getRoot().getChild('c');
		assertNotNull(nodeC);
		CodePointTrie.CodePointTrieNode<String> nodeA = nodeC.getChild('a');
		assertNotNull(nodeA);

		// 'c' -> 'a' -> 't' ("cat")
		CodePointTrie.CodePointTrieNode<String> nodeCat = nodeA.getChild('t');
		assertNotNull(nodeCat);
		assertTrue(nodeCat.hasValue());
		assertEquals("feline", nodeCat.getValue());

		// 'c' -> 'a' -> 'r' ("car")
		CodePointTrie.CodePointTrieNode<String> nodeCar = nodeA.getChild('r');
		assertNotNull(nodeCar);
		assertTrue(nodeCar.hasValue());
		assertEquals("vehicle", nodeCar.getValue());

		// 'c' -> 'a' -> 'r' -> 't' ("cart")
		CodePointTrie.CodePointTrieNode<String> nodeCart = nodeCar.getChild('t');
		assertNotNull(nodeCart);
		assertTrue(nodeCart.hasValue());
		assertEquals("shopping", nodeCart.getValue());

		// 'd' -> 'o' -> 'g' ("dog")
		CodePointTrie.CodePointTrieNode<String> nodeDog = trie.getRoot()
				.getChild('d')
				.getChild('o')
				.getChild('g');
		assertNotNull(nodeDog);
		assertTrue(nodeDog.hasValue());
		assertEquals("canine", nodeDog.getValue());
	}

	@Test
	@DisplayName("Overwriting existing key value")
	void testOverwriteExistingKey() {
		CodePointTrie<String> trie = new CodePointTrie.Builder<String>()
				.addValue("key", "initial")
				.addValue("key", "updated")
				.build();

		CodePointTrie.CodePointTrieNode<String> terminal = trie.getRoot()
				.getChild('k')
				.getChild('e')
				.getChild('y');

		assertNotNull(terminal);
		assertTrue(terminal.hasValue());
		assertEquals("updated", terminal.getValue());
	}

	@Test
	@DisplayName("Unicode supplementary code points (emojis / surrogate pairs)")
	void testUnicodeSupplementaryCodePoints() {
		String grinningFace = "\uD83D\uDE00"; // 😀 (U+1F600)
		int grinningCodePoint = 0x1F600;
		String rocket = "\uD83D\uDE80"; // 🚀 (U+1F680)
		int rocketCodePoint = 0x1F680;

		CodePointTrie<String> trie = new CodePointTrie.Builder<String>()
				.addValue(grinningFace + "abc", "emojiPrefix")
				.addValue(rocket, "singleEmoji")
				.build();

		// Grinning face test: should match single integer code point rather than char surrogates
		CodePointTrie.CodePointTrieNode<String> emojiNode = trie.getRoot().getChild(grinningCodePoint);
		assertNotNull(emojiNode);
		assertFalse(emojiNode.hasValue());

		CodePointTrie.CodePointTrieNode<String> abcTerminal = emojiNode
				.getChild('a')
				.getChild('b')
				.getChild('c');
		assertNotNull(abcTerminal);
		assertTrue(abcTerminal.hasValue());
		assertEquals("emojiPrefix", abcTerminal.getValue());

		// Rocket test
		CodePointTrie.CodePointTrieNode<String> rocketNode = trie.getRoot().getChild(rocketCodePoint);
		assertNotNull(rocketNode);
		assertTrue(rocketNode.hasValue());
		assertEquals("singleEmoji", rocketNode.getValue());
	}

	@Test
	@DisplayName("Adding values using IntStream directly")
	void testAddValueIntStream() {
		IntStream codePoints = IntStream.of(100, 200, 300);

		CodePointTrie<String> trie = new CodePointTrie.Builder<String>()
				.addValue(codePoints, "streamValue")
				.build();

		CodePointTrie.CodePointTrieNode<String> node100 = trie.getRoot().getChild(100);
		assertNotNull(node100);
		CodePointTrie.CodePointTrieNode<String> node200 = node100.getChild(200);
		assertNotNull(node200);
		CodePointTrie.CodePointTrieNode<String> node300 = node200.getChild(300);
		assertNotNull(node300);

		assertTrue(node300.hasValue());
		assertEquals("streamValue", node300.getValue());
	}

	@Test
	@DisplayName("CodePointTrieNode equals, hashCode, and toString contracts")
	void testNodeValueContracts() {
		CodePointTrie<String> trie1 = new CodePointTrie.Builder<String>()
				.addValue("a", "val")
				.build();
		CodePointTrie<String> trie2 = new CodePointTrie.Builder<String>()
				.addValue("a", "val")
				.build();
		CodePointTrie<String> trie3 = new CodePointTrie.Builder<String>()
				.addValue("a", "diff")
				.build();

		assertEquals(trie1.getRoot(), trie2.getRoot());
		assertEquals(trie1.getRoot().hashCode(), trie2.getRoot().hashCode());
		assertNotEquals(trie1.getRoot(), trie3.getRoot());

		assertNotNull(trie1.getRoot().toString());
		assertTrue(trie1.getRoot().toString().contains("CodePointTrieNode"));
	}
}
