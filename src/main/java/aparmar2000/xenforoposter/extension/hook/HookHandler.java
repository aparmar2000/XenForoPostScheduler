package aparmar2000.xenforoposter.extension.hook;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface HookHandler<H extends AbstractHookEvent<S>, S> {
	void handle(@NotNull H event) throws Exception;
}
