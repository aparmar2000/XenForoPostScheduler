package aparmar2000.xenforoposter.extension.hook;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Hook {
	HookPriority priority() default HookPriority.NORMAL;
	HookPhase[] phases() default {HookPhase.PREVIEW, HookPhase.POST};
}
