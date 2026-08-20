package aparmar2000.xenforoposter.model;

import java.time.Instant;

import org.jetbrains.annotations.NotNull;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class PollRecord {
    public enum PollType {
        LOCAL_EVALUATION,
        THREAD_FETCH,
        POST_SUBMISSION
    }

    @NotNull
    Instant timestamp;
    @NotNull
    PollType pollType;
    boolean success;
    @NotNull
    String summary;
    String details;
}
