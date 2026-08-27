package aparmar2000.xenforoposter.extension.hook;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class AbstractHookEvent<S> {
	@NonNull
	private final HookPhase phase;

	public boolean isPreview() {
		return phase == HookPhase.PREVIEW;
	}

	public boolean isPost() {
		return phase == HookPhase.POST;
	}

	public abstract S createSnapshot();
	public abstract void restoreSnapshot(S snapshot);
}
