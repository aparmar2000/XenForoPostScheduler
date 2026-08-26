package aparmar2000.xenforoposter.syntax.bbcode;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.syntax.TagSource;
import aparmar2000.xenforoposter.utils.CodePointTrie;
import aparmar2000.xenforoposter.utils.CodePointTrie.CodePointTrieNode;

class BbCodeTagDefinitionRegistryTest {

	private static final TagSource SOURCE_CORE = TagSource.of("core");
	private static final TagSource SOURCE_EXT_A = TagSource.of("ext-a");
	private static final TagSource SOURCE_EXT_B = TagSource.of("ext-b");

	private BbCodeTagDefinitionRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new BbCodeTagDefinitionRegistry();
	}

	@Test
	@DisplayName("Initial state should have empty definitions and empty trie")
	void testInitialState() {
		assertTrue(registry.getRegisteredTagDefinitions().isEmpty());

		CodePointTrie<BbCodeTagDefinition> trie = registry.getTagTrie();
		assertNotNull(trie);
		assertNotNull(trie.getRoot());
		assertFalse(trie.getRoot().hasValue());
		assertTrue(trie.getRoot().getChildren().isEmpty());
	}

	@Test
	@DisplayName("Registering a new tag definition updates registered set and trie")
	void testRegisterTag() {
		BbCodeTagDefinition bTag = new BbCodeTagDefinition("B", true, null);
		BbCodeTagDefinition registered = registry.register(SOURCE_CORE, bTag);

		assertSame(bTag, registered);
		assertEquals(1, registry.getRegisteredTagDefinitions().size());
		assertTrue(registry.getRegisteredTagDefinitions().contains(bTag));

		CodePointTrie<BbCodeTagDefinition> trie = registry.getTagTrie();
		CodePointTrieNode<BbCodeTagDefinition> nodeB = trie.getRoot().getChild('b');
		assertNotNull(nodeB);
		assertTrue(nodeB.hasValue());
		assertEquals(bTag, nodeB.getValue());
	}

	@Test
	@DisplayName("Overwriting tag for the same source updates definition")
	void testRegisterOverwriteSameSource() {
		BbCodeTagDefinition bTag1 = new BbCodeTagDefinition("B", true, null);
		BbCodeTagDefinition bTag2 = new BbCodeTagDefinition("B", false, null);

		registry.register(SOURCE_CORE, bTag1);
		BbCodeTagDefinition result = registry.register(SOURCE_CORE, bTag2);

		assertSame(bTag2, result);
		assertEquals(1, registry.getRegisteredTagDefinitions().size());
		assertTrue(registry.getRegisteredTagDefinitions().contains(bTag2));
		assertFalse(registry.getRegisteredTagDefinitions().contains(bTag1));
	}

	@Test
	@DisplayName("Multiple sources registering same tag follow canonical ordering")
	void testMultipleSourcesCanonicalOrdering() {
		// "core" < "ext-a" < "ext-b"
		BbCodeTagDefinition coreTag = new BbCodeTagDefinition("B", true, null);
		BbCodeTagDefinition extATag = new BbCodeTagDefinition("B", false, null);
		BbCodeTagDefinition extBTag = new BbCodeTagDefinition("B", true, null);

		registry.register(SOURCE_CORE, coreTag);
		assertSame(coreTag, registry.getByTagString("B"));

		// ext-a is greater than core in canonical order -> ext-a becomes active
		registry.register(SOURCE_EXT_A, extATag);
		assertSame(extATag, registry.getByTagString("B"));

		// ext-b is greater than ext-a -> ext-b becomes active
		registry.register(SOURCE_EXT_B, extBTag);
		assertSame(extBTag, registry.getByTagString("B"));
		assertEquals(1, registry.getRegisteredTagDefinitions().size());

		// Unregister ext-b -> falls back to ext-a
		assertTrue(registry.unregister(SOURCE_EXT_B, "B"));
		assertSame(extATag, registry.getByTagString("B"));

		// Unregister ext-a -> falls back to core
		assertTrue(registry.unregister(SOURCE_EXT_A, extATag));
		assertSame(coreTag, registry.getByTagString("B"));

		// Unregister core -> tag removed completely
		assertTrue(registry.unregister(SOURCE_CORE, "B"));
		assertNull(registry.getByTagString("B"));
		assertTrue(registry.getRegisteredTagDefinitions().isEmpty());
	}

	@Test
	@DisplayName("registerIfAbsent evaluates supplier when tag is absent")
	void testRegisterIfAbsentWhenAbsent() {
		AtomicBoolean supplierCalled = new AtomicBoolean(false);
		BbCodeTagDefinition newDef = new BbCodeTagDefinition("CUSTOM", true, null);

		BbCodeTagDefinition result = registry.registerIfAbsent(SOURCE_EXT_A, "CUSTOM", () -> {
			supplierCalled.set(true);
			return newDef;
		});

		assertTrue(supplierCalled.get());
		assertSame(newDef, result);
		assertSame(newDef, registry.getByTagString("CUSTOM"));
	}

	@Test
	@DisplayName("registerIfAbsent does not evaluate supplier when tag is already present")
	void testRegisterIfAbsentWhenPresent() {
		BbCodeTagDefinition existing = new BbCodeTagDefinition("B", true, null);
		registry.register(SOURCE_CORE, existing);

		AtomicBoolean supplierCalled = new AtomicBoolean(false);
		BbCodeTagDefinition result = registry.registerIfAbsent(SOURCE_EXT_A, "B", () -> {
			supplierCalled.set(true);
			return new BbCodeTagDefinition("B", false, null);
		});

		assertFalse(supplierCalled.get());
		assertSame(existing, result);
	}

	@Test
	@DisplayName("Unregistering existing tag removes from set and invalidates trie")
	void testUnregisterTag() {
		BbCodeTagDefinition bTag = new BbCodeTagDefinition("B", true, null);
		BbCodeTagDefinition iTag = new BbCodeTagDefinition("I", true, null);

		registry.register(SOURCE_CORE, bTag);
		registry.register(SOURCE_CORE, iTag);
		assertEquals(2, registry.getRegisteredTagDefinitions().size());

		assertTrue(registry.unregister(SOURCE_CORE, bTag));
		assertEquals(1, registry.getRegisteredTagDefinitions().size());
		assertFalse(registry.getRegisteredTagDefinitions().contains(bTag));
		assertTrue(registry.getRegisteredTagDefinitions().contains(iTag));

		CodePointTrie<BbCodeTagDefinition> trie = registry.getTagTrie();
		assertNull(trie.getRoot().getChild('b'));
		assertNotNull(trie.getRoot().getChild('i'));
	}

	@Test
	@DisplayName("Unregistering non-existent tag returns false")
	void testUnregisterNonExistentTag() {
		BbCodeTagDefinition bTag = new BbCodeTagDefinition("B", true, null);
		assertFalse(registry.unregister(SOURCE_CORE, bTag));
		assertFalse(registry.unregister(SOURCE_CORE, "NONEXISTENT"));
	}

	@Test
	@DisplayName("markTreeDirty resets cached trie and forces rebuild on next getTagTrie")
	void testMarkTreeDirty() {
		BbCodeTagDefinition bTag = new BbCodeTagDefinition("B", true, null);
		registry.register(SOURCE_CORE, bTag);

		CodePointTrie<BbCodeTagDefinition> firstTrie = registry.getTagTrie();
		assertNotNull(firstTrie);

		// Same instance returned if not dirty
		assertSame(firstTrie, registry.getTagTrie());

		registry.markTreeDirty();
		CodePointTrie<BbCodeTagDefinition> secondTrie = registry.getTagTrie();
		assertNotNull(secondTrie);
		assertNotSame(firstTrie, secondTrie);
	}

	@Test
	@DisplayName("buildTree builds trie for multiple overlapping and multi-character tags")
	void testMultipleAndOverlappingTags() {
		BbCodeTagDefinition cTag = new BbCodeTagDefinition("C", true, null);
		BbCodeTagDefinition codeTag = new BbCodeTagDefinition("CODE", false, null);
		BbCodeTagDefinition colorTag = new BbCodeTagDefinition("COLOR", true, null);
		BbCodeTagDefinition quoteTag = new BbCodeTagDefinition("QUOTE", true, null);

		registry.register(SOURCE_CORE, cTag);
		registry.register(SOURCE_CORE, codeTag);
		registry.register(SOURCE_CORE, colorTag);
		registry.register(SOURCE_CORE, quoteTag);

		CodePointTrie<BbCodeTagDefinition> trie = registry.getTagTrie();

		// Check 'c'
		CodePointTrieNode<BbCodeTagDefinition> nodeC = trie.getRoot().getChild('c');
		assertNotNull(nodeC);
		assertTrue(nodeC.hasValue());
		assertEquals(cTag, nodeC.getValue());

		// Check 'c' -> 'o' -> 'd' -> 'e'
		CodePointTrieNode<BbCodeTagDefinition> nodeO = nodeC.getChild('o');
		assertNotNull(nodeO);
		assertFalse(nodeO.hasValue());

		CodePointTrieNode<BbCodeTagDefinition> nodeD = nodeO.getChild('d');
		assertNotNull(nodeD);
		assertFalse(nodeD.hasValue());

		CodePointTrieNode<BbCodeTagDefinition> nodeE = nodeD.getChild('e');
		assertNotNull(nodeE);
		assertTrue(nodeE.hasValue());
		assertEquals(codeTag, nodeE.getValue());

		// Check 'c' -> 'o' -> 'l' -> 'o' -> 'r'
		CodePointTrieNode<BbCodeTagDefinition> nodeL = nodeO.getChild('l');
		assertNotNull(nodeL);
		CodePointTrieNode<BbCodeTagDefinition> nodeO2 = nodeL.getChild('o');
		assertNotNull(nodeO2);
		CodePointTrieNode<BbCodeTagDefinition> nodeR = nodeO2.getChild('r');
		assertNotNull(nodeR);
		assertTrue(nodeR.hasValue());
		assertEquals(colorTag, nodeR.getValue());

		// Check 'q' -> 'u' -> 'o' -> 't' -> 'e'
		CodePointTrieNode<BbCodeTagDefinition> nodeQ = trie.getRoot().getChild('q');
		assertNotNull(nodeQ);
		CodePointTrieNode<BbCodeTagDefinition> nodeQuoteE = nodeQ.getChild('u').getChild('o').getChild('t').getChild('e');
		assertNotNull(nodeQuoteE);
		assertTrue(nodeQuoteE.hasValue());
		assertEquals(quoteTag, nodeQuoteE.getValue());
	}

	@Test
	@DisplayName("Concurrent registration and trie retrieval is thread-safe")
	void testThreadSafety() throws Exception {
		int threadCount = 10;
		int iterationsPerThread = 50;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch startLatch = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			futures.add(executor.submit(() -> {
				try {
					startLatch.await();
					TagSource source = TagSource.of("thread-" + threadId);
					for (int j = 0; j < iterationsPerThread; j++) {
						BbCodeTagDefinition tag = new BbCodeTagDefinition("TAG_" + threadId + "_" + j, true, null);
						registry.register(source, tag);
						assertNotNull(registry.getTagTrie());
						assertNotNull(registry.getRegisteredTagDefinitions());
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
		}

		startLatch.countDown();
		for (Future<?> future : futures) {
			future.get(10, TimeUnit.SECONDS);
		}
		executor.shutdown();

		assertEquals(threadCount * iterationsPerThread, registry.getRegisteredTagDefinitions().size());
		assertNotNull(registry.getTagTrie());
	}

	@Nested
	@DisplayName("Case-Insensitive Registry Operations")
	class CaseInsensitiveRegistryTests {

		@Test
		@DisplayName("Tag lookup by string is case-insensitive")
		void testCaseInsensitiveGetByTagString() {
			BbCodeTagDefinition boldTag = new BbCodeTagDefinition("B", true, null);
			BbCodeTagDefinition colorTag = new BbCodeTagDefinition("COLOR", true, null);
			registry.register(SOURCE_CORE, boldTag);
			registry.register(SOURCE_CORE, colorTag);

			assertSame(boldTag, registry.getByTagString("b"));
			assertSame(boldTag, registry.getByTagString("B"));
			assertSame(colorTag, registry.getByTagString("color"));
			assertSame(colorTag, registry.getByTagString("COLOR"));
			assertSame(colorTag, registry.getByTagString("Color"));
			assertSame(colorTag, registry.getByTagString("cOlOr"));
		}

		@Test
		@DisplayName("hasTag check is case-insensitive")
		void testCaseInsensitiveHasTag() {
			BbCodeTagDefinition quoteTag = new BbCodeTagDefinition("QUOTE", true, null);
			registry.register(SOURCE_CORE, quoteTag);

			assertTrue(registry.hasTag("quote"));
			assertTrue(registry.hasTag("QUOTE"));
			assertTrue(registry.hasTag("Quote"));
			assertTrue(registry.hasTag(SOURCE_CORE, "quote"));
			assertTrue(registry.hasTag(SOURCE_CORE, "QUOTE"));
			assertTrue(registry.hasTag(SOURCE_CORE, "QuOtE"));
			assertFalse(registry.hasTag("unknown"));
			assertFalse(registry.hasTag(SOURCE_CORE, "unknown"));
		}

		@Test
		@DisplayName("getDefinition and getDefinitionsForTag are case-insensitive")
		void testCaseInsensitiveGetDefinitions() {
			BbCodeTagDefinition coreDef = new BbCodeTagDefinition("CODE", false, null);
			BbCodeTagDefinition extDef = new BbCodeTagDefinition("CODE", true, null);
			registry.register(SOURCE_CORE, coreDef);
			registry.register(SOURCE_EXT_A, extDef);

			assertSame(coreDef, registry.getDefinition(SOURCE_CORE, "code"));
			assertSame(coreDef, registry.getDefinition(SOURCE_CORE, "CODE"));
			assertSame(coreDef, registry.getDefinition(SOURCE_CORE, "CoDe"));
			assertSame(extDef, registry.getDefinition(SOURCE_EXT_A, "code"));
			assertSame(extDef, registry.getDefinition(SOURCE_EXT_A, "CODE"));

			assertEquals(2, registry.getDefinitionsForTag("code").size());
			assertEquals(2, registry.getDefinitionsForTag("CODE").size());
			assertEquals(2, registry.getDefinitionsForTag("CoDe").size());
		}

		@Test
		@DisplayName("Unregistering with different casing removes tag definition and updates trie")
		void testCaseInsensitiveUnregister() {
			BbCodeTagDefinition bTag = new BbCodeTagDefinition("B", true, null);
			BbCodeTagDefinition colorTag = new BbCodeTagDefinition("COLOR", true, null);
			registry.register(SOURCE_CORE, bTag);
			registry.register(SOURCE_CORE, colorTag);

			// Unregister "b" using lowercase string when registered as uppercase "B"
			assertTrue(registry.unregister(SOURCE_CORE, "b"));
			assertNull(registry.getByTagString("B"));
			assertNull(registry.getByTagString("b"));
			assertNull(registry.getTagTrie().getRoot().getChild('b'));

			// Unregister "COLOR" using mixed-case string "Color"
			assertTrue(registry.unregister(SOURCE_CORE, "Color"));
			assertNull(registry.getByTagString("COLOR"));
			assertNull(registry.getByTagString("color"));
		}

		@Test
		@DisplayName("registerIfAbsent respects case-insensitivity for existing and new tags")
		void testCaseInsensitiveRegisterIfAbsent() {
			BbCodeTagDefinition existing = new BbCodeTagDefinition("B", true, null);
			registry.register(SOURCE_CORE, existing);

			AtomicBoolean supplierCalled = new AtomicBoolean(false);
			BbCodeTagDefinition result = registry.registerIfAbsent(SOURCE_EXT_A, "b", () -> {
				supplierCalled.set(true);
				return new BbCodeTagDefinition("b", false, null);
			});

			assertFalse(supplierCalled.get());
			assertSame(existing, result);

			// Now register new tag with mixed case key
			BbCodeTagDefinition newDef = new BbCodeTagDefinition("Spoiler", true, null);
			BbCodeTagDefinition registered = registry.registerIfAbsent(SOURCE_CORE, "sPoIlEr", () -> newDef);
			assertSame(newDef, registered);
			assertSame(newDef, registry.getByTagString("SPOILER"));
			assertSame(newDef, registry.getByTagString("spoiler"));
			assertNotNull(registry.getTagTrie().getRoot().getChild('s'));
		}

		@Test
		@DisplayName("Trie builder builds lowercase trie nodes for mixed-case registered tags")
		void testTrieLowercaseConstruction() {
			BbCodeTagDefinition mixedTag = new BbCodeTagDefinition("MyCustomTag", true, null);
			registry.register(SOURCE_CORE, mixedTag);

			CodePointTrie<BbCodeTagDefinition> trie = registry.getTagTrie();
			// Walk 'm' -> 'y' -> 'c' -> 'u' -> 's' -> 't' -> 'o' -> 'm' -> 't' -> 'a' -> 'g'
			CodePointTrieNode<BbCodeTagDefinition> node = trie.getRoot();
			for (char c : "mycustomtag".toCharArray()) {
				node = node.getChild(c);
				assertNotNull(node, "Trie should contain lowercase child for char: " + c);
			}
			assertTrue(node.hasValue());
			assertSame(mixedTag, node.getValue());

			// Trie root should not have uppercase 'M'
			assertNull(trie.getRoot().getChild('M'));
		}
	}
}
