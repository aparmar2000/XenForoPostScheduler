package aparmar2000.xenforoposter.syntax.html;

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
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.syntax.TagSource;

class HtmlTagDefinitionRegistryTest {

	private static final TagSource SOURCE_CORE = TagSource.of("core");
	private static final TagSource SOURCE_EXT_A = TagSource.of("ext-a");
	private static final TagSource SOURCE_EXT_B = TagSource.of("ext-b");

	private HtmlTagDefinitionRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new HtmlTagDefinitionRegistry();
	}

	@Test
	@DisplayName("Initial state should have empty definitions")
	void testInitialState() {
		assertTrue(registry.getRegisteredTagDefinitions().isEmpty());
		assertNull(registry.getByTagString("b"));
	}

	@Test
	@DisplayName("Registering a new tag definition updates registered set and lookup")
	void testRegisterTag() {
		HtmlTagDefinition bTag = HtmlTagDefinition.simpleHtmlTagWrapper("b", false);
		HtmlTagDefinition registered = registry.register(SOURCE_CORE, bTag);

		assertSame(bTag, registered);
		assertEquals(1, registry.getRegisteredTagDefinitions().size());
		assertTrue(registry.getRegisteredTagDefinitions().contains(bTag));
		assertSame(bTag, registry.getByTagString("b"));
		assertSame(bTag, registry.getByTagString("B")); // Case-insensitive
	}

	@Test
	@DisplayName("Overwriting tag for same source updates definition")
	void testRegisterOverwriteSameSource() {
		HtmlTagDefinition bTag1 = HtmlTagDefinition.simpleHtmlTagWrapper("b", false);
		HtmlTagDefinition bTag2 = HtmlTagDefinition.simpleHtmlTagWrapper("b", true);

		registry.register(SOURCE_CORE, bTag1);
		HtmlTagDefinition result = registry.register(SOURCE_CORE, bTag2);

		assertSame(bTag2, result);
		assertEquals(1, registry.getRegisteredTagDefinitions().size());
		assertTrue(registry.getRegisteredTagDefinitions().contains(bTag2));
		assertFalse(registry.getRegisteredTagDefinitions().contains(bTag1));
	}

	@Test
	@DisplayName("Multiple sources registering same tag follow canonical ordering")
	void testMultipleSourcesCanonicalOrdering() {
		// "core" < "ext-a" < "ext-b"
		HtmlTagDefinition coreTag = HtmlTagDefinition.simpleHtmlTagWrapper("div", false);
		HtmlTagDefinition extATag = HtmlTagDefinition.simpleHtmlTagWrapper("div", true);
		HtmlTagDefinition extBTag = HtmlTagDefinition.simpleHtmlSingularTag("div");

		registry.register(SOURCE_CORE, coreTag);
		assertSame(coreTag, registry.getByTagString("div"));

		// ext-a is greater than core in canonical order -> ext-a becomes active
		registry.register(SOURCE_EXT_A, extATag);
		assertSame(extATag, registry.getByTagString("div"));

		// ext-b is greater than ext-a -> ext-b becomes active
		registry.register(SOURCE_EXT_B, extBTag);
		assertSame(extBTag, registry.getByTagString("div"));
		assertEquals(1, registry.getRegisteredTagDefinitions().size());

		// Unregister ext-b -> falls back to ext-a
		assertTrue(registry.unregister(SOURCE_EXT_B, "div"));
		assertSame(extATag, registry.getByTagString("div"));

		// Unregister ext-a -> falls back to core
		assertTrue(registry.unregister(SOURCE_EXT_A, extATag));
		assertSame(coreTag, registry.getByTagString("div"));

		// Unregister core -> tag removed completely
		assertTrue(registry.unregister(SOURCE_CORE, "div"));
		assertNull(registry.getByTagString("div"));
		assertTrue(registry.getRegisteredTagDefinitions().isEmpty());
	}

	@Test
	@DisplayName("registerIfAbsent evaluates supplier when tag is absent")
	void testRegisterIfAbsentWhenAbsent() {
		AtomicBoolean supplierCalled = new AtomicBoolean(false);
		HtmlTagDefinition newDef = HtmlTagDefinition.simpleHtmlTagWrapper("custom", false);

		HtmlTagDefinition result = registry.registerIfAbsent(SOURCE_EXT_A, "custom", () -> {
			supplierCalled.set(true);
			return newDef;
		});

		assertTrue(supplierCalled.get());
		assertSame(newDef, result);
		assertSame(newDef, registry.getByTagString("custom"));
	}

	@Test
	@DisplayName("registerIfAbsent does not evaluate supplier when tag is already present")
	void testRegisterIfAbsentWhenPresent() {
		HtmlTagDefinition existing = HtmlTagDefinition.simpleHtmlTagWrapper("b", false);
		registry.register(SOURCE_CORE, existing);

		AtomicBoolean supplierCalled = new AtomicBoolean(false);
		HtmlTagDefinition result = registry.registerIfAbsent(SOURCE_EXT_A, "b", () -> {
			supplierCalled.set(true);
			return HtmlTagDefinition.simpleHtmlTagWrapper("b", true);
		});

		assertFalse(supplierCalled.get());
		assertSame(existing, result);
	}

	@Test
	@DisplayName("Unregistering non-existent tag returns false")
	void testUnregisterNonExistentTag() {
		HtmlTagDefinition bTag = HtmlTagDefinition.simpleHtmlTagWrapper("b", false);
		assertFalse(registry.unregister(SOURCE_CORE, bTag));
		assertFalse(registry.unregister(SOURCE_CORE, "nonexistent"));
	}

	@Test
	@DisplayName("Concurrent registration and retrieval is thread-safe")
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
						HtmlTagDefinition tag = HtmlTagDefinition.simpleHtmlTagWrapper("tag_" + threadId + "_" + j, false);
						registry.register(source, tag);
						assertNotNull(registry.getByTagString(tag.getTag()));
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
	}
}
