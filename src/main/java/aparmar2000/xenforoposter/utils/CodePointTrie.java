package aparmar2000.xenforoposter.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMap;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class CodePointTrie<T> {
	@Value
	public static class CodePointTrieNode<T> {
		ImmutableMap<Integer, CodePointTrieNode<T>> children;
		@Nullable T value;
		
		public boolean hasValue() {
			return value != null;
		}
		
		@Nullable
		public CodePointTrieNode<T> getChild(int codePoint) {
			return children.get(codePoint);
		}
	}
	
	@Getter
	private final CodePointTrieNode<T> root;
	
	public static class Builder<T> {
		@Data
		protected static class MutableCodePointTrieNode<T> {
			private final Map<Integer, MutableCodePointTrieNode<T>> children = new HashMap<>();
			@Nullable private T value;
		}
		
		private final MutableCodePointTrieNode<T> root = new MutableCodePointTrieNode<>();
		
		public Builder<T> addValue(String key, T value) {
			return addValue(key.codePoints(), value);
		}

		public Builder<T> addValue(IntStream keyCodePoints, T value) {
			MutableCodePointTrieNode<T> currentNode = root;
			for (PrimitiveIterator.OfInt iterator = keyCodePoints.iterator(); iterator.hasNext();) {
				int codePoint = iterator.nextInt();
				currentNode = currentNode.getChildren()
						.computeIfAbsent(codePoint, k -> new MutableCodePointTrieNode<>());
			}
			currentNode.value = value;
			
			return this;
		}
		
		public CodePointTrie<T> build() {
			return new CodePointTrie<T>(getImmutableNode(root));
		}
		
		protected CodePointTrieNode<T> getImmutableNode(MutableCodePointTrieNode<T> mutableNode) {
			ImmutableMap.Builder<Integer, CodePointTrieNode<T>> childMapBuilder = ImmutableMap.builderWithExpectedSize(mutableNode.getChildren().size());
			for (Entry<Integer, MutableCodePointTrieNode<T>> mutableChild : mutableNode.getChildren().entrySet()) {
				childMapBuilder.put(mutableChild.getKey(), getImmutableNode(mutableChild.getValue()));
			}
			
			return new CodePointTrieNode<T>(childMapBuilder.build(), mutableNode.getValue());
		}
	}
}
