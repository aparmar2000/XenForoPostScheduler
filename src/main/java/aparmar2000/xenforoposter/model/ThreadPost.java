package aparmar2000.xenforoposter.model;

import java.time.Instant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ThreadPost {
    @NotNull String postId;
    @NotNull String author;
    @Nullable String authorId;
    @NotNull Instant timestamp;
    int postNumber;
    int pageNumber;
    @Nullable String content;
}
