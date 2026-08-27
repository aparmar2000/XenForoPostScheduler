package aparmar2000.xenforoposter.extension.hook;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class PreTokenizeStringEvent extends AbstractHookEvent<String> {
	@NonNull
	private String text;

	public PreTokenizeStringEvent(@NonNull HookPhase phase, @NonNull String text) {
		super(phase);
		this.text = text;
	}

	@Override
	public String createSnapshot() {
		return this.text;
	}

	@Override
	public void restoreSnapshot(String snapshot) {
		this.text = snapshot;
	}
}
