package aparmar2000.xenforoposter.syntax.html;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;

import aparmar2000.xenforoposter.syntax.AbstractAst;
import aparmar2000.xenforoposter.syntax.AstUtils;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition.HtmlStringMapper.StringMappingException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.val;

public class HtmlAst extends AbstractAst<HtmlAst.HtmlAstNodeRoot> {

    public HtmlAst(HtmlAstNodeRoot rootNode) {
        super(rootNode);
    }

    public HtmlAst clone() {
        return new HtmlAst(rootNode.clone());
    }

    public static interface HtmlAstNode extends AbstractAst.AstNode<HtmlAstNode> {
        @Override
        public HtmlAstNode clone();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static abstract class HtmlAstBranchNode extends AbstractAst.AstBranchNode<HtmlAstNode> implements HtmlAstNode {
        @Override
        public abstract HtmlAstBranchNode clone();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static abstract class HtmlAstLeafNode extends AstLeafNode<HtmlAstNode> implements HtmlAstNode {
        @Override
        public abstract HtmlAstLeafNode clone();
    }

	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class HtmlAstNodeText extends HtmlAstLeafNode {
		private final String text;
		
		public HtmlAstNodeText merge(HtmlAstNodeText other) {
			HtmlAstNodeText mergedNode = new HtmlAstNodeText(text + other.getText());
			
			return mergedNode;
		}

		@Override
		public HtmlAstNodeText clone() {
			return new HtmlAstNodeText(text);
		}
	}

	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class HtmlAstNodeTag extends HtmlAstBranchNode {
		
		private final String[] rawString;
		private final HtmlTagDefinition tagDefinition;
		private final ImmutableMap<String, String> parameters;
		
		public HtmlAstNodeTag(HtmlTagDefinition tagDefinition, ImmutableMap<String, String> parameters) {
			rawString = new String[] {"", ""};
			this.tagDefinition = tagDefinition;
			this.parameters = parameters;
		}

		public HtmlAstNodeTag(String[] rawString, HtmlTagDefinition tagDefinition, ImmutableMap<String, String> parameters) {
			if (rawString.length != 2) {
				throw new IllegalArgumentException("rawString must be of length 2, but is length "+rawString.length);
			}
			this.rawString = rawString;
			this.tagDefinition = tagDefinition;
			this.parameters = parameters;
		}
		
		@Override
		public HtmlAstNodeTag clone() {
			val clonedNode = new HtmlAstNodeTag(Arrays.copyOf(rawString, 2), tagDefinition, parameters);
			clonedNode.getChildren().addAll(getChildren());
			return clonedNode;
		}
	}

	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class HtmlAstNodeRoot extends HtmlAstBranchNode {
		@Override
		public HtmlAstNodeRoot clone() {
			val clonedNode = new HtmlAstNodeRoot();
			clonedNode.getChildren().addAll(getChildren());
			return clonedNode;
		}
	}
	
	public String mapToHtmlString() {
		val escaper = HtmlEscapers.htmlEscaper();
		return mapNodeToHtmlString(escaper, rootNode, false);
	}
	
	protected String mapNodeToHtmlString(Escaper escaper, HtmlAstNode node, boolean innerTextRawRequired) {
		if (node instanceof HtmlAstNodeText) {
			String nodeText = ((HtmlAstNodeText) node).getText();
			if (innerTextRawRequired) {
				return nodeText;
			}
			String escaped = escaper.escape(nodeText);
			return escaped.replace("\r\n", "<br/>").replace("\r", "<br/>").replace("\n", "<br/>");
		}
		
		boolean childrenInnerTextRawRequired = innerTextRawRequired;
		if (node instanceof HtmlAstNodeTag) { childrenInnerTextRawRequired |= ((HtmlAstNodeTag)node).getTagDefinition().isInnerTextRawRequired(); }
		
		StringBuilder innerHtmlStringBuilder = new StringBuilder();
		for (HtmlAstNode childNode : node.getChildren()) {
			innerHtmlStringBuilder.append(mapNodeToHtmlString(escaper, childNode, childrenInnerTextRawRequired));
		}
		
		if (node instanceof HtmlAstNodeTag) {
			HtmlAstNodeTag tagNode = (HtmlAstNodeTag) node;
			try {
				return tagNode.getTagDefinition().mapNode(tagNode.getParameters(), innerHtmlStringBuilder.toString());
			} catch (StringMappingException e) {
				return tagNode.getRawString()[0] + innerHtmlStringBuilder.toString() + tagNode.getRawString()[1];
			}
		}
		return innerHtmlStringBuilder.toString();
	}

	// --- Convenience AST Helper Methods

	@NotNull
	public List<HtmlAstNodeTag> findTags(@NonNull String tagName) {
		return AstUtils.findHtmlTags(this, tagName);
	}

	@NotNull
	public List<HtmlAstNodeText> findTextNodes() {
		return AstUtils.findTextNodes(this);
	}

	@NotNull
	public List<HtmlAstNodeText> findTextNodesContaining(@NonNull String substring) {
		return AstUtils.findTextNodesContaining(this, substring);
	}

	@NotNull
	public List<HtmlAstNodeText> findTextNodesMatching(@NonNull Pattern pattern) {
		return AstUtils.findTextNodesMatching(this, pattern);
	}

	public void replaceText(@NotNull String target, @NonNull String replacement) {
		AstUtils.replaceText(this, target, replacement);
	}

	public void replaceText(@NotNull Pattern pattern,
			@NotNull Function<Matcher, String> replacer) {
		AstUtils.replaceText(this, pattern, replacer);
	}

	public void replaceTags(@NotNull String tagName,
			@NotNull Function<HtmlAstNodeTag, HtmlAstNode> replacer) {
		AstUtils.replaceHtmlTags(this, tagName, replacer);
	}
}
