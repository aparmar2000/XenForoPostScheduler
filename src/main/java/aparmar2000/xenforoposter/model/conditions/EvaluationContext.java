package aparmar2000.xenforoposter.model.conditions;

import java.time.Instant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.ScrapedThreadData;
import aparmar2000.xenforoposter.model.ThreadMetadata;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class EvaluationContext {
	@NotNull Instant evaluationTime;
	@NotNull ForumProfile forumProfile;
	@Nullable ThreadMetadata threadMetadata;
	@Nullable ScrapedThreadData threadData;

	@Nullable
	public ThreadMetadata getThreadMetadata() {
		if (threadMetadata != null) {
			return threadMetadata;
		}
		return threadData != null ? threadData.getMetadata() : null;
	}
}

