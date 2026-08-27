package aparmar2000.xenforoposter.extension.hook;

import java.util.ArrayList;
import java.util.List;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.ParsedToken;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class TokenizedBbCodeEvent extends AbstractHookEvent<List<ParsedToken>> {
	@NonNull
	private List<ParsedToken> tokens;

	public TokenizedBbCodeEvent(@NonNull HookPhase phase, @NonNull List<ParsedToken> tokens) {
		super(phase);
		this.tokens = new ArrayList<>(tokens);
	}

	public void setTokens(@NonNull List<ParsedToken> tokens) {
		this.tokens = new ArrayList<>(tokens);
	}

	@Override
	public List<ParsedToken> createSnapshot() {
		return new ArrayList<>(this.tokens);
	}

	@Override
	public void restoreSnapshot(List<ParsedToken> snapshot) {
		this.tokens = new ArrayList<>(snapshot);
	}
}
