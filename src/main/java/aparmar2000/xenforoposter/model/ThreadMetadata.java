package aparmar2000.xenforoposter.model;

import java.time.Instant;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ThreadMetadata {
    @NotNull String threadUrl;
    @Nullable String threadId;
    @NotNull String title;
    boolean locked;
    boolean canReply;
    int replyCount;
    @Nullable String threadAuthor;
    @Nullable String lastPostAuthor;
    @Nullable Instant lastPostTimestamp;
    @Nullable String xfToken;
    @Singular List<String> recentPostAuthors;
}
