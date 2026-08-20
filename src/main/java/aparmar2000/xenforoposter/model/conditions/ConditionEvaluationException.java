package aparmar2000.xenforoposter.model.conditions;

import lombok.Getter;
import lombok.NonNull;


/**
 * Unchecked exception representing condition failure or prerequisite requirement violation.
 * Thrown during {@link PostCondition#innerEvaluate(EvaluationContext)} to immediately short-circuit
 * condition evaluation.
 */
public class ConditionEvaluationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    @Getter
	private final ConditionResult result;

    public ConditionEvaluationException(@NonNull ConditionResult result) {
        super(result.getMessage());
    	if (result.isSatisfied()) {
    		throw new IllegalArgumentException("Cannot construct evaluation exception from successful result!");
    	}
        this.result = result;
    }
}
