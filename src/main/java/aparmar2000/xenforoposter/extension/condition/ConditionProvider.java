package aparmar2000.xenforoposter.extension.condition;

import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.model.conditions.PostCondition;

public interface ConditionProvider {
	@NotNull String getConditionTypeId();
	@NotNull String getDisplayName();
	@NotNull String getDescription();
	@NotNull PostCondition createDefaultInstance();
	@NotNull JPanel createConfigurationPanel(@NotNull PostCondition currentInstance, @NotNull ConditionUpdateCallback callback);

	@FunctionalInterface
	interface ConditionUpdateCallback {
		void onConditionUpdated(@NotNull PostCondition updatedCondition);
	}
}
