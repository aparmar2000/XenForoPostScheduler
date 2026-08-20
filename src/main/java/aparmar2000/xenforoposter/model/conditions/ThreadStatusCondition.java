package aparmar2000.xenforoposter.model.conditions;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.model.ThreadMetadata;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
@Builder(toBuilder = true)
public class ThreadStatusCondition extends PostCondition {
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
    protected @NotNull ConditionResult innerEvaluate(@NotNull EvaluationContext context) throws ConditionEvaluationException {
        ThreadMetadata metadata = getThreadMetadataOrFail(context);

        if (requireUnlocked && metadata.isLocked()) {
            fail("Target thread is locked / closed to new replies");
        }

        if (requireCanReply && !metadata.isCanReply()) {
            fail("User does not have permission to reply or reply form is inaccessible");
        }

        return pass("Thread is open and writable");
    }
}
