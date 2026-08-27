package aparmar2000.xenforoposter.extension.hook;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class RegisteredHook<H extends AbstractHookEvent<S>, S> implements Comparable<RegisteredHook<?,?>> {
	@NonNull
	private final String extensionId;
	@NonNull
	private final String methodName;
	@NonNull
	private final Class<H> eventType;
	@NonNull
	private final HookPriority priority;
	@NonNull
	private final Set<HookPhase> phases;
	@NonNull
	private final HookHandler<H, S> handler;

	@Builder
	public RegisteredHook(@NonNull String extensionId,
			@NonNull String methodName,
			@NonNull Class<H> eventType,
			@NonNull HookPriority priority,
			Set<HookPhase> phases,
			@NonNull HookHandler<H, S> handler) {
		this.extensionId = extensionId;
		this.methodName = methodName;
		this.eventType = eventType;
		this.priority = priority;
		if (phases == null || phases.isEmpty()) {
			this.phases = Collections.unmodifiableSet(EnumSet.allOf(HookPhase.class));
		} else {
			this.phases = Collections.unmodifiableSet(EnumSet.copyOf(phases));
		}
		this.handler = handler;
	}

	public boolean appliesToPhase(@NonNull HookPhase phase) {
		return phases.contains(phase);
	}

	public boolean appliesToEvent(@NonNull AbstractHookEvent<?> event) {
		return eventType.isInstance(event) && appliesToPhase(event.getPhase());
	}

	@SuppressWarnings("unchecked")
	public void execute(@NonNull AbstractHookEvent<?> event) throws Exception {
		if (appliesToEvent(event)) {
			handler.handle((H) event);
		}
	}

	@Override
	public int compareTo(@NotNull RegisteredHook<?,?> o) {
		int pCmp = Integer.compare(this.priority.getWeight(), o.priority.getWeight());
		if (pCmp != 0) {
			return pCmp;
		}
		
		int extCmp = this.extensionId.compareTo(o.extensionId);
		if (extCmp != 0) {
			return extCmp;
		}
		
		return this.methodName.compareTo(o.methodName);
	}
}
