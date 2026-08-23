package aparmar2000.xenforoposter.syntax.html;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import lombok.Locked;
import lombok.NonNull;

public class HtmlTagDefinitionRegistry {
	private final Map<HtmlTagDefinition, HtmlTagDefinition> registeredTagDefinitions = new HashMap<>();
	private final Map<String, HtmlTagDefinition> tagNameIndex = new HashMap<>();
	
	@Locked
	public HtmlTagDefinition register(@NonNull HtmlTagDefinition tagDefinition) {
		HtmlTagDefinition definitionInst = registeredTagDefinitions.computeIfAbsent(tagDefinition, k->tagDefinition);
		tagNameIndex.put(definitionInst.getTag().toLowerCase(), definitionInst);
		
		return definitionInst;
	}
	
	@Locked
	public boolean unregister(@NonNull HtmlTagDefinition tagDefinition) {
		HtmlTagDefinition removed = registeredTagDefinitions.remove(tagDefinition);
		if (removed != null) {
			tagNameIndex.remove(removed.getTag().toLowerCase());
		}
		
		return removed != null;
	}

	@Nullable
	public HtmlTagDefinition getByTagString(String tagString) {
		return tagNameIndex.get(tagString.toLowerCase());
	}
}
