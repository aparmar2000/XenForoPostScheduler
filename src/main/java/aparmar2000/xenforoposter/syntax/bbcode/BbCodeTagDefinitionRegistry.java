package aparmar2000.xenforoposter.syntax.bbcode;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableSet;

import aparmar2000.xenforoposter.utils.CodePointTrie;
import lombok.Locked;

public class BbCodeTagDefinitionRegistry {
	private final Set<BbCodeTagDefinition> registeredTagDefinitions = new HashSet<>();
	@Nullable
	private CodePointTrie<BbCodeTagDefinition> tagTrie = null;
	
	@Locked
	public boolean register(BbCodeTagDefinition tagDefinition) {		
		boolean change = registeredTagDefinitions.add(tagDefinition);
		if (change) {
			markTreeDirty();
		}
		
		return change;
	}
	
	@Locked
	public boolean unregister(BbCodeTagDefinition tagDefinition) {
		boolean change = registeredTagDefinitions.remove(tagDefinition);
		if (change) {
			markTreeDirty();
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
