package aparmar2000.xenforoposter.syntax.html;

import java.util.HashMap;
import java.util.Map;

import lombok.Locked;
import lombok.NonNull;

public class HtmlCodeTagDefinitionRegistry {
	private final Map<HtmlTagDefinition, HtmlTagDefinition> registeredTagDefinitions = new HashMap<>();
	private final Map<String, HtmlTagDefinition> tagNameIndex = new HashMap<>();
	
	@Locked
	public HtmlTagDefinition register(@NonNull HtmlTagDefinition tagDefinition) {
		HtmlTagDefinition definitionInst = registeredTagDefinitions.computeIfAbsent(tagDefinition, k->tagDefinition);
		tagNameIndex.put(definitionInst.getTag(), definitionInst);
		
		return definitionInst;
	}
	
	@Locked
	public boolean unregister(@NonNull HtmlTagDefinition tagDefinition) {
		HtmlTagDefinition removed = registeredTagDefinitions.remove(tagDefinition);
		if (removed != null) {
			tagNameIndex.remove(removed.getTag());
		}
		
		return removed != null;
	}
}
