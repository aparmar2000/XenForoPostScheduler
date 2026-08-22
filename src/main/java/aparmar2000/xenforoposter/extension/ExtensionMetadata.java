package aparmar2000.xenforoposter.extension;

import java.io.File;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ExtensionMetadata {
	@NonNull String source;
	@Builder.Default boolean builtIn = false;
	@Nullable String jarFileName;

	@NotNull
	public static ExtensionMetadata builtIn() {
		return ExtensionMetadata.builder()
				.source("Built-in")
				.builtIn(true)
				.build();
	}

	@NotNull
	public static ExtensionMetadata fromJar(@NotNull File jarFile) {
		return ExtensionMetadata.builder()
				.source(jarFile.getName())
				.builtIn(false)
				.jarFileName(jarFile.getName())
				.build();
	}

	@NotNull
	public static ExtensionMetadata fromJar(@NotNull Path jarPath) {
		String fileName = jarPath.getFileName() != null ? jarPath.getFileName().toString() : jarPath.toString();
		return ExtensionMetadata.builder()
				.source(fileName)
				.builtIn(false)
				.jarFileName(fileName)
				.build();
	}
}
