package aparmar2000.xenforoposter.model.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
@Builder(toBuilder = true)
public class CompositeCondition extends PostCondition {
    public enum Operator {
        AND,
        OR
    }

    @NotNull @Builder.Default String id = UUID.randomUUID().toString();
    @NotNull @Builder.Default Operator operator = Operator.AND;
    @Singular @NotNull List<PostCondition> conditions;

    @Override
    public @NotNull String getDisplayName() {
        return operator == Operator.AND ? "All Conditions (AND)" : "Any Condition (OR)";
    }

    @Override
    public @NotNull String getDescription() {
        return conditions.stream().map(PostCondition::getDisplayName).collect(Collectors.joining(" " + operator + " "));
    }

    @Override
    public @NotNull ConditionType getType() {
        boolean hasThreadDependent = conditions.stream().anyMatch(c -> c.getType() == ConditionType.THREAD_DEPENDENT);
        return hasThreadDependent ? ConditionType.THREAD_DEPENDENT : ConditionType.LOCAL;
    }

    @Override
    protected @NotNull ConditionResult innerEvaluate(@NotNull EvaluationContext context) throws ConditionEvaluationException {
        if (conditions.isEmpty()) {
            return pass("No sub-conditions configured");
        }

        List<String> failedReasons = new ArrayList<>();
        List<String> passedMessages = new ArrayList<>();

        Integer firstPageRequest = null;
        for (PostCondition condition : conditions) {
            ConditionResult result = condition.evaluate(context);
            if (result.isSatisfied()) {
                passedMessages.add(result.getMessage());
                if (operator == Operator.OR) {
                    return pass("OR satisfied by: " + result.getMessage(), result.getExpiry());
                }
            } else {
                failedReasons.add(result.getMessage());
                if (result.hasPageRequest() && firstPageRequest == null) {
                    firstPageRequest = result.getRequestedPage();
                }
                if (operator == Operator.AND) {
                    if (result.hasPageRequest()) {
                        failWithPageRequest(
                                "AND failed on [" + condition.getDisplayName() + "]: " + result.getMessage(),
                                result.getRequestedPage(),
                                result.getExpiry());
                    }
                    fail("AND failed on [" + condition.getDisplayName() + "]: " + result.getMessage(), result.getExpiry());
                }
            }
        }

        if (operator == Operator.AND) {
            return pass("All conditions passed: " + String.join("; ", passedMessages));
        } else {
            String combinedReason = "None of the OR conditions were met: " + String.join("; ", failedReasons);
            if (firstPageRequest != null) {
                failWithPageRequest(combinedReason, firstPageRequest);
            }
            fail(combinedReason);
            return pass("OK"); // Unreachable due to fail()
        }
    }
}
