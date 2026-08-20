package aparmar2000.xenforoposter.model.conditions;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.model.ThreadMetadata;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ThreadStatusCondition implements PostCondition {
    @NotNull @Builder.Default String id = UUID.randomUUID().toString();
    @Builder.Default boolean requireUnlocked = true;
    @Builder.Default boolean requireCanReply = true;

    @Override
    public @NotNull String getDisplayName() {
        return "Thread Status & Permissions";
    }

    @Override
    public @NotNull String getDescription() {
        return "Requires target thread to be unlocked and open for replies";
    }

    @Override
    public @NotNull ConditionType getType() {
        return ConditionType.THREAD_DEPENDENT;
    }

    @Override
    public @NotNull ConditionResult evaluate(@NotNull EvaluationContext context) {
        ThreadMetadata metadata = context.getThreadMetadata();
        if (metadata == null) {
            return ConditionResult.fail("Thread metadata is not available to verify thread status");
        }

        if (requireUnlocked && metadata.isLocked()) {
            return ConditionResult.fail("Target thread is locked / closed to new replies");
        }

        if (requireCanReply && !metadata.isCanReply()) {
            return ConditionResult.fail("User does not have permission to reply or reply form is inaccessible");
        }

        return ConditionResult.pass("Thread is open and writable");
    }
}
