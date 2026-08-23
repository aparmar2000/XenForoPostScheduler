package aparmar2000.xenforoposter.syntax.bbcode;

import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeTag;
import aparmar2000.xenforoposter.utils.CodePointTrie.CodePointTrieNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor(onConstructor_={@Inject})
public class BbCodeTokenizer {
	protected static final int TAG_OPEN_CODE_POINT = '[';
	protected static final int TAG_END_OPEN_CODE_POINT = '/';
	protected static final int TAG_CLOSE_CODE_POINT = ']';
	protected static final int TAG_PARAMETER_SEPERATOR_CODE_POINT = ' ';
	protected static final int TAG_PARAMETER_VALUE_CODE_POINT = '=';
	
	protected final BbCodeTagDefinitionRegistry tagDefinitionRegistry;
	
	@Data
	protected static abstract class ParsedToken {
		private final String rawString;
	}

	@EqualsAndHashCode(callSuper = true)
	protected static class TextToken extends ParsedToken {
		public TextToken(String rawString) {
			super(rawString);
		}
	}

	@Data
	@EqualsAndHashCode(callSuper = true)
	protected static class TagToken extends ParsedToken {
		private final BbCodeTagDefinition tagDefinition;
		private final boolean endingTag;
		private final ImmutableMap<String, String> parameters;
		
		public TagToken(String rawString, BbCodeTagDefinition tagDefinition, boolean endingTag, ImmutableMap<String, String> parameters) {
			super(rawString);
			this.tagDefinition = tagDefinition;
			this.endingTag = endingTag;
			this.parameters = parameters;
		}
		
		@Nullable
		public String getRootParameter() {
			return getParameterValue(BbCodeAstNodeTag.ROOT_PARAMETER_NAME);
		}
		@Nullable
		public String getParameterValue(String key) {
			return parameters.get(key);
		}
		
		@RequiredArgsConstructor
		public static class Builder {
			private final BbCodeTagDefinition tagDefinition;
			private boolean endingTag = false;
			private final ImmutableMap.Builder<String, String> parameterMapBuilder = ImmutableMap.builder();
			
			public Builder endingTag() {
				endingTag = true;
				
				return this;
			}
			
			public Builder parameter(String key, String value) {
				parameterMapBuilder.put(key, value);
				return this;
			}
			
			public TagToken build(String rawString) {
				return new TagToken(rawString, tagDefinition, endingTag, parameterMapBuilder.buildKeepingLast());
			}
		}
	}
	
	@RequiredArgsConstructor
	@Getter
	protected static class TokenListBuilder {
		private final CodePointTrieNode<BbCodeTagDefinition> tagDefinitionTrieRoot;
		
		private final List<ParsedToken> tokens = new ArrayList<>();
		private StringBuilder currentTextBuilder = new StringBuilder();
		private boolean buildingTag = false;
		private boolean buildingEndingTag = false;
		@Nullable
		private TagToken.Builder partialTagToken = null;
		@Nullable
		private StringBuilder currentParameterChunkBuilder = null;
		@Nullable
		private String parameterKey = null;
		@Nullable
		private CodePointTrieNode<BbCodeTagDefinition> currentTagNode = null;
		
		protected void startTag() {
			abortTag();
			
			buildingTag = true;
			buildingEndingTag = false;
			currentTagNode = tagDefinitionTrieRoot;
		}
		
		protected void abortTag() {
			buildingTag = false;
			buildingEndingTag = false;
			if (currentTextBuilder.length() > 0) {
				tokens.add(new TextToken(currentTextBuilder.toString()));
			}
			currentTextBuilder = new StringBuilder();
			currentParameterChunkBuilder = null;
			parameterKey = null;
			partialTagToken = null;
			currentTagNode = null;
		}
		
		protected boolean tryCloseTag(int codePoint) {
			if (currentTagNode != null) {
				if (currentTagNode.hasValue()) {
					partialTagToken = new TagToken.Builder(currentTagNode.getValue());
					if (buildingEndingTag) {
						partialTagToken.endingTag();
					}
					currentTagNode = null;
				} else {
					return false;
				}
			}
			if (partialTagToken == null) { return false; }
			if (parameterKey != null && (currentParameterChunkBuilder == null || currentParameterChunkBuilder.isEmpty())) { return false; }

			if (parameterKey != null) {
				partialTagToken.parameter(parameterKey, currentParameterChunkBuilder.toString());
			}

			currentTextBuilder.appendCodePoint(codePoint);
			tokens.add(partialTagToken.build(currentTextBuilder.toString()));

			buildingTag = false;
			buildingEndingTag = false;
			currentTextBuilder = new StringBuilder();
			currentParameterChunkBuilder = null;
			parameterKey = null;
			partialTagToken = null;
			currentTagNode = null;
			return true;
		}
		
		protected boolean handleTagCodePoint(int codePoint) {			
			if (currentTagNode != null) {
				if (codePoint == TAG_PARAMETER_SEPERATOR_CODE_POINT || codePoint == TAG_PARAMETER_VALUE_CODE_POINT) {
					if (currentTagNode.hasValue()) {
						
						partialTagToken = new TagToken.Builder(currentTagNode.getValue());
						if (buildingEndingTag) {
							partialTagToken.endingTag();
						}
						currentTagNode = null;
						
					} else {
						
						abortTag();
						return false;
						
					}
				} else if (codePoint == TAG_END_OPEN_CODE_POINT) {
					buildingEndingTag = true;
				} else {
					currentTagNode = currentTagNode.getChild(codePoint);
					if (currentTagNode == null) {
						abortTag();
					}
					return false;
				}
			}
			
			if (partialTagToken != null) {

				if (codePoint == TAG_PARAMETER_SEPERATOR_CODE_POINT) {
					if (parameterKey != null && currentParameterChunkBuilder != null && !currentParameterChunkBuilder.isEmpty()) {
						partialTagToken.parameter(parameterKey, currentParameterChunkBuilder.toString());
					}
					
					parameterKey = null;
					currentParameterChunkBuilder = new StringBuilder();
				} else if (codePoint == TAG_PARAMETER_VALUE_CODE_POINT) {
					if (currentParameterChunkBuilder == null || currentParameterChunkBuilder.isEmpty()) {
						parameterKey = BbCodeAstNodeTag.ROOT_PARAMETER_NAME;
					} else {
						parameterKey = currentParameterChunkBuilder.toString();
					}
					currentParameterChunkBuilder = new StringBuilder();
				} else if (currentParameterChunkBuilder != null) {
					currentParameterChunkBuilder.appendCodePoint(codePoint);
				}
			}
			
			return false;
		}
		
		public void appendCodePoint(int codePoint) {
			boolean codePointConsumed = false;
			
			if (buildingTag && codePoint == TAG_CLOSE_CODE_POINT) {
				if (tryCloseTag(codePoint)) {
					codePointConsumed |= true;
				} else {
					abortTag();
				}
			}
			
			if (buildingTag) {
				codePointConsumed |= handleTagCodePoint(codePoint);
			}
			
			if (codePoint == TAG_OPEN_CODE_POINT) {
				startTag();
			}
			
			if (!codePointConsumed) {
				currentTextBuilder.appendCodePoint(codePoint);
			}
		}
		
		public ImmutableList<ParsedToken> getMergedTokens() {
			List<ParsedToken> mergedTokens = new ArrayList<>();
			for (ParsedToken token : tokens) {
				if (mergedTokens.size()>0 && token instanceof TextToken && mergedTokens.get(mergedTokens.size()-1) instanceof TextToken) {
					val prevToken = mergedTokens.get(mergedTokens.size()-1);
					mergedTokens.set(mergedTokens.size()-1, new TextToken(prevToken.getRawString() + token.getRawString()));
				} else {
					mergedTokens.add(token);
				}
			}			
			
			return ImmutableList.copyOf(mergedTokens);
		}
	}
	
	public ImmutableList<ParsedToken> tokenizeString(String bbcodeString) {
		TokenListBuilder tokenListBuilder = new TokenListBuilder(tagDefinitionRegistry.getTagTrie().getRoot());
		
		for (PrimitiveIterator.OfInt iterator = bbcodeString.codePoints().iterator(); iterator.hasNext();) {
			tokenListBuilder.appendCodePoint(iterator.nextInt());
		}
		tokenListBuilder.abortTag();

		return tokenListBuilder.getMergedTokens();
	}

}
