package aparmar2000.xenforoposter.syntax.bbcode;

import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.syntax.TagDefinitionRegistry;
import aparmar2000.xenforoposter.utils.CodePointTrie;

public class BbCodeTagDefinitionRegistry extends TagDefinitionRegistry<BbCodeTagDefinition> {
	@Nullable
	private CodePointTrie<BbCodeTagDefinition> tagTrie = null;
	

	protected void handleRegistryChange() {
		markTreeDirty();
	}

	protected void markTreeDirty() {
		registryLock.lock();
		try {
			tagTrie = null;
		} finally {
			registryLock.unlock();
		}
	}

	protected void buildTree() {
		registryLock.lock();
		try {
			final CodePointTrie.Builder<BbCodeTagDefinition> tagTrieBuilder = new CodePointTrie.Builder<>();
			for (BbCodeTagDefinition tagDefinition : activeTagNameIndex.values()) {
				tagTrieBuilder.addValue(tagDefinition.getTag().toLowerCase(), tagDefinition);
			}
			tagTrie = tagTrieBuilder.build();
		} finally {
			registryLock.unlock();
		}
	}

	public CodePointTrie<BbCodeTagDefinition> getTagTrie() {
		registryLock.lock();
		try {
			if (tagTrie == null) {
				buildTree();
			}

			return tagTrie;
		} finally {
			registryLock.unlock();
		}
	}
}
