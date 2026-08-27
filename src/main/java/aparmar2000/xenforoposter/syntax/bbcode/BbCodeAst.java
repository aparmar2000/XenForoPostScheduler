package aparmar2000.xenforoposter.syntax.bbcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.AbstractAst;
import aparmar2000.xenforoposter.syntax.AstUtils;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition.HtmlNodeMapper.HtmlMappingException;
import aparmar2000.xenforoposter.syntax.html.HtmlAst;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNode;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeRoot;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeText;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

public class BbCodeAst extends AbstractAst<BbCodeAst.BbCodeAstNodeRoot> {

    public BbCodeAst(BbCodeAstNodeRoot rootNode) {
        super(rootNode);
    }

    public BbCodeAst clone() {
        return new BbCodeAst(rootNode.clone());
    }

    public static interface BbCodeAstNode extends AbstractAst.AstNode<BbCodeAstNode> {
        @Override
        public BbCodeAstNode clone();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static abstract class BbCodeAstBranchNode extends AbstractAst.AstBranchNode<BbCodeAstNode> implements BbCodeAstNode {
        @Override
        public abstract BbCodeAstBranchNode clone();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static abstract class BbCodeAstLeafNode extends AstLeafNode<BbCodeAstNode> implements BbCodeAstNode {
        @Override
        public abstract BbCodeAstLeafNode clone();
    }

	@Data
	@RequiredArgsConstructor
	@EqualsAndHashCode(callSuper = true)
	public static class BbCodeAstNodeText extends BbCodeAstLeafNode {
		private final String text;
		
		public BbCodeAstNodeText merge(BbCodeAstNodeText other) {
			BbCodeAstNodeText mergedNode = new BbCodeAstNodeText(text + other.getText());
			
			return mergedNode;
		}

		@Override
		public BbCodeAstNodeText clone() {
			return new BbCodeAstNodeText(text);
		}
	}

	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class BbCodeAstNodeTag extends BbCodeAstBranchNode {
		/**
		 * The name used for 'root parameters', i.e. [tag=value]
		 */
		public static final String ROOT_PARAMETER_NAME = "$value";

		private final String[] rawString;
		private final BbCodeTagDefinition tagDefinition;
		private final ImmutableMap<String, String> parameters;
		
		public BbCodeAstNodeTag(BbCodeTagDefinition tagDefinition, ImmutableMap<String, String> parameters) {
			rawString = new String[] {"", ""};
			this.tagDefinition = tagDefinition;
			this.parameters = parameters;
		}
		
		public BbCodeAstNodeTag(String openingTag, BbCodeTagDefinition tagDefinition, ImmutableMap<String, String> parameters) {
			rawString = new String[] {openingTag, ""};
			this.tagDefinition = tagDefinition;
			this.parameters = parameters;
		}

		public BbCodeAstNodeTag(String[] rawString, BbCodeTagDefinition tagDefinition, ImmutableMap<String, String> parameters) {
			if (rawString.length != 2) {
				throw new IllegalArgumentException("rawString must be of length 2, but is length "+rawString.length);
			}
			this.rawString = rawString;
			this.tagDefinition = tagDefinition;
			this.parameters = parameters;
		}

		@Override
		public BbCodeAstNodeTag clone() {
			val clonedNode = new BbCodeAstNodeTag(Arrays.copyOf(rawString, 2), tagDefinition, parameters);
			clonedNode.getChildren().addAll(getChildren());
			return clonedNode;
		}
	}

	@Data
	@RequiredArgsConstructor
	@EqualsAndHashCode(callSuper = true)
	public static class BbCodeAstNodeRoot extends BbCodeAstBranchNode {
		@Override
		public BbCodeAstNodeRoot clone() {
			val clonedNode = new BbCodeAstNodeRoot();
			clonedNode.getChildren().addAll(getChildren());
			return clonedNode;
		}
	}
	
	public String mapToHtmlString() {
		return mapToHtmlAst().mapToHtmlString();
	}
	
	public HtmlAst mapToHtmlAst() {
		return new HtmlAst((HtmlAstNodeRoot) mapNodeToHtmlNode(rootNode));
	}
	
	protected HtmlAstNodeRoot mapNodeToHtmlNode(BbCodeAstNodeRoot node) {
		HtmlAstNodeRoot newRoot = new HtmlAstNodeRoot();

		for (BbCodeAstNode childNode : node.getChildren()) {
			newRoot.getChildren().addAll(mapNodeToHtmlNode(childNode));
		}
		
		return newRoot;
	}
	
	protected List<HtmlAstNode> mapNodeToHtmlNode(BbCodeAstNode node) {
		if (node instanceof BbCodeAstNodeText) {
			return List.of( new HtmlAstNodeText(((BbCodeAstNodeText)node).getText()) );
		}
		
		List<HtmlAstNode> childNodes = new ArrayList<>();
		for (BbCodeAstNode childNode : node.getChildren()) {
			childNodes.addAll(mapNodeToHtmlNode(childNode));
		}
		
		if (node instanceof BbCodeAstNodeTag) {
			BbCodeAstNodeTag tagNode = (BbCodeAstNodeTag) node;
			try {
				return List.of( tagNode.getTagDefinition().mapNode(tagNode.getParameters(), childNodes) );
			} catch (HtmlMappingException e) {
				childNodes.add(0, new HtmlAstNodeText(tagNode.getRawString()[0]));
				childNodes.add(new HtmlAstNodeText(tagNode.getRawString()[1]));
				return childNodes;
			}
		}
		
		return childNodes;
	}

	@NotNull
	public String toBbCodeString() {
		return mapNodeToBbCodeString(rootNode);
	}

	@NotNull
	public static String mapNodeToBbCodeString(BbCodeAstNode node) {
		if (node == null) {
			return "";
		}
		
		if (node instanceof BbCodeAstNodeText) {
			String text = ((BbCodeAstNodeText) node).getText();
			return text != null ? text : "";
		}
		
		if (node instanceof BbCodeAstNodeTag) {
			BbCodeAstNodeTag tagNode = (BbCodeAstNodeTag) node;
			String openingTag;
			if (tagNode.getRawString() != null && tagNode.getRawString().length > 0 && !tagNode.getRawString()[0].isEmpty()) {
				openingTag = tagNode.getRawString()[0];
			} else if (tagNode.getTagDefinition() != null) {
				StringBuilder sb = new StringBuilder("[");
				sb.append(tagNode.getTagDefinition().getTag());
				ImmutableMap<String, String> params = tagNode.getParameters();
				if (params != null && !params.isEmpty()) {
					if (params.containsKey(BbCodeAstNodeTag.ROOT_PARAMETER_NAME)) {
						sb.append("=").append(formatParameterValue(params.get(BbCodeAstNodeTag.ROOT_PARAMETER_NAME)));
					}
					for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
						if (!entry.getKey().equals(BbCodeAstNodeTag.ROOT_PARAMETER_NAME)) {
							sb.append(" ").append(entry.getKey()).append("=").append(formatParameterValue(entry.getValue()));
						}
					}
				}
				sb.append("]");
				openingTag = sb.toString();
			} else {
				openingTag = "";
			}

			StringBuilder childrenSb = new StringBuilder();
			for (BbCodeAstNode child : tagNode.getChildren()) {
				childrenSb.append(mapNodeToBbCodeString(child));
			}

			String closingTag;
			if (tagNode.getRawString() != null && tagNode.getRawString().length > 1 && !tagNode.getRawString()[1].isEmpty()) {
				closingTag = tagNode.getRawString()[1];
			} else if (tagNode.getTagDefinition() != null) {
				closingTag = "[/" + tagNode.getTagDefinition().getTag() + "]";
			} else {
				closingTag = "";
			}

			return openingTag + childrenSb.toString() + closingTag;
		}

		if (node instanceof BbCodeAstNodeRoot) {
			StringBuilder sb = new StringBuilder();
			for (BbCodeAstNode child : ((BbCodeAstNodeRoot) node).getChildren()) {
				sb.append(mapNodeToBbCodeString(child));
			}
			return sb.toString();
		}

		return "";
	}

	private static String formatParameterValue(String val) {
		if (val == null) {
			return "";
		}
		if (val.contains(" ") || val.contains("'") || val.contains("\"") || val.contains("]") || val.contains("[")) {
			if (!val.contains("\"")) {
				return "\"" + val + "\"";
			} else if (!val.contains("'")) {
				return "'" + val + "'";
			} else {
				return "\"" + val.replace("\"", "\\\"") + "\"";
			}
		}
		return val;
	}

	// --- Convenience AST Helper Methods

	@NotNull
	public List<BbCodeAstNodeTag> findTags(@NonNull String tagName) {
		return AstUtils.findBbCodeTags(this, tagName);
	}

	@NotNull
	public List<BbCodeAstNodeText> findTextNodes() {
		return AstUtils.findTextNodes(this);
	}

	@NotNull
	public List<BbCodeAstNodeText> findTextNodesContaining(@NonNull String substring) {
		return AstUtils.findTextNodesContaining(this, substring);
	}

	@NotNull
	public List<BbCodeAstNodeText> findTextNodesMatching(@NonNull Pattern pattern) {
		return AstUtils.findTextNodesMatching(this, pattern);
	}

	public void replaceText(@NonNull String target, @NonNull String replacement) {
		AstUtils.replaceText(this, target, replacement);
	}

	public void replaceText(@NonNull Pattern pattern,
			@NonNull Function<Matcher, String> replacer) {
		AstUtils.replaceText(this, pattern, replacer);
	}

	public void replaceTags(@NonNull String tagName,
			@NonNull Function<BbCodeAstNodeTag, BbCodeAstNode> replacer) {
		AstUtils.replaceBbCodeTags(this, tagName, replacer);
	}
}
