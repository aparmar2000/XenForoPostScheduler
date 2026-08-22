package aparmar2000.xenforoposter.bbcode;

import lombok.Value;

@Value
public class BbCodeTagDefinition {
	String tag;
	boolean allowsInnerTags;
}
