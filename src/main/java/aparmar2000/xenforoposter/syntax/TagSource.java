package aparmar2000.xenforoposter.syntax;

import org.jetbrains.annotations.NotNull;

import lombok.NonNull;
import lombok.Value;

@Value
public class TagSource implements Comparable<TagSource> {
	public static final TagSource CORE = new TagSource("core");

	@NonNull
	String id;

	public static TagSource of(@NonNull String id) {
		return new TagSource(id);
	}

	@Override
	public int compareTo(@NotNull TagSource other) {
		return this.id.compareTo(other.id);
	}
}
