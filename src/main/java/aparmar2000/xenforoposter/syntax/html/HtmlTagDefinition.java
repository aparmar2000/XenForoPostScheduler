package aparmar2000.xenforoposter.syntax.html;

import lombok.Value;

@Value
public class HtmlTagDefinition {
	String tag;
	boolean allowsInnerTags;
}
