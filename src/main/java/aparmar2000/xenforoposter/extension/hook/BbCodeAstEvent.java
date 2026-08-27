package aparmar2000.xenforoposter.extension.hook;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class BbCodeAstEvent extends AbstractHookEvent<BbCodeAst> {
	@NonNull
	private BbCodeAst bbCodeAst;

	public BbCodeAstEvent(@NonNull HookPhase phase, @NonNull BbCodeAst bbCodeAst) {
		super(phase);
		this.bbCodeAst = bbCodeAst;
	}

	@Override
	public BbCodeAst createSnapshot() {
		return this.bbCodeAst.clone();
	}

	@Override
	public void restoreSnapshot(BbCodeAst snapshot) {
		this.bbCodeAst = snapshot.clone();
	}
}
