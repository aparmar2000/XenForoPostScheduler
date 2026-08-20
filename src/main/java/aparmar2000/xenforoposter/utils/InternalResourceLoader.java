package aparmar2000.xenforoposter.utils;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InternalResourceLoader {
	public static InputStream getInternalResourceAsStream(@NonNull String filename) throws FileNotFoundException {
		InputStream foundResourceStream = nullableGetInternalResourceAsStream(filename);
		if (foundResourceStream == null) {
			throw new FileNotFoundException(filename + " was not found!");
		}

		return foundResourceStream;
	}
	public static boolean internalResourceExists(@NonNull String filename) {
		return nullableGetInternalResourceAsStream(filename) != null;
	}

	@Nullable
	private static InputStream nullableGetInternalResourceAsStream(String filename) {
		String cleanPath = filename.startsWith("/") ? filename.substring(1) : filename;

		InputStream foundResourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(cleanPath);
		if (foundResourceStream == null) {
			foundResourceStream = InternalResourceLoader.class.getClassLoader().getResourceAsStream(cleanPath);
		}
		if (foundResourceStream == null) {
			foundResourceStream = InternalResourceLoader.class.getResourceAsStream("/" + cleanPath);
		}
		return foundResourceStream;
	}

	@FunctionalInterface
	private static interface InternalResourceLoadFunction<T> {
		public T load(String filename) throws IOException;
	}

	private static <T> Optional<T> silentWrapper(InternalResourceLoadFunction<T> loader, String filename) {
		try {
			return Optional.ofNullable(loader.load(filename));
		} catch (IOException e) {
			log.error(String.format("Exception loading internal resource %s", filename), e);
		}

		return Optional.empty();
	}

	public static byte[] getInternalResourceAsByteArray(String filename) throws IOException {
		return getInternalResourceAsStream(filename).readAllBytes();
	}
	public static Optional<byte[]> tryGetInternalResourceAsByteArraySilent(String filename) {
		return silentWrapper(InternalResourceLoader::getInternalResourceAsByteArray, filename);
	}

	public static BufferedImage getInternalResourceAsImage(String filename) throws IOException {
		return ImageIO.read(getInternalResourceAsStream(filename));
	}
	public static Optional<BufferedImage> tryGetInternalResourceAsImageSilent(String filename) {
		return silentWrapper(InternalResourceLoader::getInternalResourceAsImage, filename);
	}

	public static String getInternalResourceAsString(String filename) throws IOException {
		return new String(getInternalResourceAsByteArray(filename), StandardCharsets.UTF_8);
	}
	public static Optional<String> tryGetInternalResourceAsStringSilent(String filename) {
		return silentWrapper(InternalResourceLoader::getInternalResourceAsString, filename);
	}
}
