package aparmar2000.xenforoposter.syntax.bbcode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableSet;

import aparmar2000.xenforoposter.utils.CodePointTrie;
import lombok.Locked;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BbCodeTagDefinitionRegistry {
	private final Set<BbCodeTagDefinition> registeredTagDefinitions = new HashSet<>();
	private final Map<String, BbCodeTagDefinition> tagNameIndex = new HashMap<>();
	@Nullable
	private CodePointTrie<BbCodeTagDefinition> tagTrie = null;
	
	@Locked
	public boolean register(BbCodeTagDefinition tagDefinition) {		
		if (tagNameIndex.containsKey(tagDefinition.getTag().toLowerCase())) {
			log.warn("Duplicate regsistration of BBCode tag '{}'", tagDefinition.getTag());
			return false;
		}
		
		boolean change = registeredTagDefinitions.add(tagDefinition);
		if (change) {
			markTreeDirty();
			tagNameIndex.put(tagDefinition.getTag().toLowerCase(), tagDefinition);
		}
		
		return change;
	}
	
	@Locked
	public boolean unregister(BbCodeTagDefinition tagDefinition) {
		boolean change = registeredTagDefinitions.remove(tagDefinition);
		if (change) {
			markTreeDirty();
			tagNameIndex.remove(tagDefinition.getTag().toLowerCase());
		}
		
		return change;
	}
	
	@Locked
	protected void markTreeDirty() {
		tagTrie = null;
	}
	
	@Locked
	protected void buildTree() {
		final CodePointTrie.Builder<BbCodeTagDefinition> tagTrieBuilder = new CodePointTrie.Builder<>();
		for (BbCodeTagDefinition tagDefinition : registeredTagDefinitions) {
			tagTrieBuilder.addValue(tagDefinition.getTag(), tagDefinition);
		}
		tagTrie = tagTrieBuilder.build();
	}

	@Locked
	public ImmutableSet<BbCodeTagDefinition> getRegisteredTagDefinitions() {
		return ImmutableSet.copyOf(registeredTagDefinitions);
	}
	
	@Locked
	public CodePointTrie<BbCodeTagDefinition> getTagTrie() {
		if (tagTrie == null) {
			buildTree();
		}
		
		return tagTrie;
	}
}
