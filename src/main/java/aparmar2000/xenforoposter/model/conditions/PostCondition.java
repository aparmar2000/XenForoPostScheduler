package aparmar2000.xenforoposter.model.conditions;

import org.jetbrains.annotations.NotNull;

public interface PostCondition {
    @NotNull String getId();
    @NotNull String getDisplayName();
    @NotNull String getDescription();
    @NotNull ConditionType getType();
    @NotNull ConditionResult evaluate(@NotNull EvaluationContext context);
}
