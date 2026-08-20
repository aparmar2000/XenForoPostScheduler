package aparmar2000.xenforoposter.model.conditions;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.model.ScrapedThreadData;
import aparmar2000.xenforoposter.model.ThreadMetadata;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
@Builder(toBuilder = true)
public class AntiNecropostCondition extends PostCondition {
    public static final int DEFAULT_MAX_INACTIVE_DAYS = 30;

    @NotNull @Builder.Default String id = UUID.randomUUID().toString();
    @Builder.Default int maxInactiveDays = DEFAULT_MAX_INACTIVE_DAYS;
    @Builder.Default boolean allowAuthorExemption = true;

    @Override
    public @NotNull String getDisplayName() {
        return "Anti-Necropost Protection";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Prevents posting if thread has been inactive for >%d days%s",
                maxInactiveDays, allowAuthorExemption ? " (unless you are the thread author)" : "");
    }

    @Override
    public @NotNull ConditionType getType() {
        return ConditionType.THREAD_DEPENDENT;
    }

    @Override
    protected @NotNull ConditionResult innerEvaluate(@NotNull EvaluationContext context) throws ConditionEvaluationException {
        ThreadMetadata metadata = getThreadMetadataOrFail(context);

        String username = context.getForumProfile().getUsername();
        if (allowAuthorExemption && username != null && !username.trim().isEmpty()) {
            if (metadata.getThreadAuthor() != null && metadata.getThreadAuthor().equalsIgnoreCase(username)) {
                return pass(String.format("Necropost exemption: you (%s) are the thread author", username));
            }
        }

        ScrapedThreadData threadData = getThreadDataOrFail(context);
        requireLatestPageLoadedOrFail(threadData);

        Optional<Instant> lastPostTimeOpt = threadData.getLatestPostTimestamp();
        if (lastPostTimeOpt.isEmpty()) {
            // If timestamp unavailable, pass with warning
            return pass("Last post timestamp not available; necropost check skipped");
        }

        Instant lastPostTime = lastPostTimeOpt.get();
        Duration inactiveDuration = Duration.between(lastPostTime, context.getEvaluationTime());
        long inactiveDays = Math.max(0, inactiveDuration.toDays());

        if (inactiveDays > maxInactiveDays) {
            fail(String.format(
                    "Anti-necropost trigger: thread inactive for %d days (limit: %d days) and user is not thread author",
                    inactiveDays, maxInactiveDays));
        }

        return pass(String.format("Thread is active (last post %d days ago <= %d days limit)",
                inactiveDays, maxInactiveDays));
    }
}
