package aparmar2000.xenforoposter.extension.hook;

import aparmar2000.xenforoposter.syntax.html.HtmlAst;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class HtmlAstEvent extends AbstractHookEvent<HtmlAst> {
	@NonNull
	private HtmlAst htmlAst;

	public HtmlAstEvent(@NonNull HookPhase phase, @NonNull HtmlAst htmlAst) {
		super(phase);
		this.htmlAst = htmlAst;
	}

	@Override
	public HtmlAst createSnapshot() {
		return this.htmlAst.clone();
	}

	@Override
	public void restoreSnapshot(HtmlAst snapshot) {
		this.htmlAst = snapshot.clone();
	}
}
