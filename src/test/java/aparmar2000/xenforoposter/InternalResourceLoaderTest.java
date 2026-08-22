package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.utils.InternalResourceLoader;

class InternalResourceLoaderTest {

	@Test
	@DisplayName("Should successfully load preview template and stylesheets as strings")
	void testLoadPreviewResources() throws Exception {
		String template = InternalResourceLoader.getInternalResourceAsString("bbcode/preview_template.html");
		assertNotNull(template);
		assertTrue(template.contains("<!DOCTYPE html>"));
		assertTrue(template.contains("{{CSS_STYLES}}"));
		assertTrue(template.contains("{{CONTENT}}"));

		String darkCss = InternalResourceLoader.getInternalResourceAsString("bbcode/preview_dark.css");
		assertNotNull(darkCss);
		assertTrue(darkCss.contains("#1e2227"));

		String lightCss = InternalResourceLoader.getInternalResourceAsString("bbcode/preview_light.css");
		assertNotNull(lightCss);
		assertTrue(lightCss.contains("#ffffff"));
	}

	@Test
	@DisplayName("Should successfully load resources with or without leading slashes")
	void testLeadingSlashResolution() throws Exception {
		String withSlash = InternalResourceLoader.getInternalResourceAsString("/bbcode/preview_template.html");
		String withoutSlash = InternalResourceLoader.getInternalResourceAsString("bbcode/preview_template.html");
		assertEquals(withoutSlash, withSlash);
	}

	@Test
	@DisplayName("Should successfully load templates as byte array and stream")
	void testLoadBytesAndStream() throws Exception {
		byte[] bytes = InternalResourceLoader.getInternalResourceAsByteArray("bbcode/preview_template.html");
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);

		try (InputStream stream = InternalResourceLoader.getInternalResourceAsStream("bbcode/preview_template.html")) {
			assertNotNull(stream);
			assertTrue(stream.available() > 0);
		}
	}

	@Test
	@DisplayName("Should return empty optional on missing resource with silent loader")
	void testSilentLoaderMissing() {
		Optional<String> result = InternalResourceLoader.tryGetInternalResourceAsStringSilent("non_existent_file.txt");
		assertFalse(result.isPresent());

		Optional<byte[]> byteResult = InternalResourceLoader.tryGetInternalResourceAsByteArraySilent("non_existent_file.txt");
		assertFalse(byteResult.isPresent());
	}

	@Test
	@DisplayName("Should throw FileNotFoundException for missing resource when requested directly")
	void testDirectLoaderMissing() {
		assertThrows(FileNotFoundException.class, () -> {
			InternalResourceLoader.getInternalResourceAsString("non_existent_file.txt");
		});
		assertThrows(FileNotFoundException.class, () -> {
			InternalResourceLoader.getInternalResourceAsStream("non_existent_file.txt");
		});
	}
}
