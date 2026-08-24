package aparmar2000.xenforoposter.syntax;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import lombok.Locked;
import lombok.NonNull;

public abstract class TagDefinitionRegistry<T extends TagDefinition> {
    protected final ReentrantLock registryLock = new ReentrantLock();
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    
	protected final Map<String, NavigableMap<TagSource, T>> tagDefinitionsByTag = new HashMap<>();
	protected final Map<String, T> activeTagNameIndex = new HashMap<>();

	@Locked("registryLock")
	public T register(@NonNull TagSource source, @NonNull T tagDefinition) {
		String tagKey = tagDefinition.getTag().toLowerCase();
		NavigableMap<TagSource, T> sourcesMap =
				tagDefinitionsByTag.computeIfAbsent(tagKey, k -> new TreeMap<>());

		T oldForSource = sourcesMap.put(source, tagDefinition);
		if (oldForSource != null) {
			log.debug("Overwriting {} '{}' for source '{}'", tagDefinition.getClass().getSimpleName(), tagDefinition.getTag(), source);
		}

		updateActiveTag(tagKey);
		handleRegistryChange();
		return activeTagNameIndex.get(tagKey);
	}

	@Locked("registryLock")
	public T registerIfAbsent(@NonNull TagSource source, @NonNull String tag,
			@NonNull Supplier<T> definitionSupplier) {
		String tagKey = tag.toLowerCase();
		T existing = activeTagNameIndex.get(tagKey);
		if (existing != null) {
			return existing;
		}

		T newDef = Objects.requireNonNull(definitionSupplier.get(), "Supplied definition cannot be null");
		return register(source, newDef);
	}

	@Locked("registryLock")
	public boolean unregister(@NonNull TagSource source, @NonNull T tagDefinition) {
		return unregister(source, tagDefinition.getTag());
	}

	@Locked("registryLock")
	public boolean unregister(@NonNull TagSource source, @NonNull String tag) {
		String tagKey = tag.toLowerCase();
		NavigableMap<TagSource, T> sourcesMap = tagDefinitionsByTag.get(tagKey);
		if (sourcesMap == null) {
			return false;
		}

		T removed = sourcesMap.remove(source);
		if (removed == null) {
			return false;
		}

		if (sourcesMap.isEmpty()) {
			tagDefinitionsByTag.remove(tagKey);
		}
		updateActiveTag(tagKey);
		handleRegistryChange();
		return true;
	}

	protected void updateActiveTag(String tagKey) {
		NavigableMap<TagSource, T> sourcesMap = tagDefinitionsByTag.get(tagKey);
		if (sourcesMap == null || sourcesMap.isEmpty()) {
			activeTagNameIndex.remove(tagKey);
		} else {
			activeTagNameIndex.put(tagKey, sourcesMap.lastEntry().getValue());
		}
	}
	
	protected void handleRegistryChange() {}

	@Locked("registryLock")
	public @Nullable T getByTagString(@NonNull String tagString) {
		return activeTagNameIndex.get(tagString.toLowerCase());
	}

	@Locked("registryLock")
	public @NotNull ImmutableSet<T> getRegisteredTagDefinitions() {
		return ImmutableSet.copyOf(activeTagNameIndex.values());
	}

	@Locked("registryLock")
	public @Nullable T getDefinition(@NonNull TagSource source, @NonNull String tag) {
		NavigableMap<TagSource, T> sourcesMap = tagDefinitionsByTag.get(tag.toLowerCase());
		return sourcesMap != null ? sourcesMap.get(source) : null;
	}

	@Locked("registryLock")
	public @NotNull ImmutableMap<TagSource, T> getDefinitionsForTag(@NonNull String tag) {
		NavigableMap<TagSource, T> sourcesMap = tagDefinitionsByTag.get(tag.toLowerCase());
		return sourcesMap != null ? ImmutableMap.copyOf(sourcesMap) : ImmutableMap.of();
	}

	@Locked("registryLock")
	public boolean hasTag(@NonNull String tag) {
		return activeTagNameIndex.containsKey(tag.toLowerCase());
	}

	@Locked("registryLock")
	public boolean hasTag(@NonNull TagSource source, @NonNull String tag) {
		NavigableMap<TagSource, T> sourcesMap = tagDefinitionsByTag.get(tag.toLowerCase());
		return sourcesMap != null && sourcesMap.containsKey(source);
	}
}
