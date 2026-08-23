package aparmar2000.xenforoposter.syntax.html;

import java.util.Arrays;

import com.google.common.collect.ImmutableMap;
import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;

import aparmar2000.xenforoposter.syntax.AbstractAst;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition.HtmlStringMapper.StringMappingException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;

public class HtmlCodeAst extends AbstractAst<HtmlCodeAst.HtmlCodeAstNodeRoot> {

    public HtmlCodeAst(HtmlCodeAstNodeRoot rootNode) {
        super(rootNode);
    }

    public static interface HtmlCodeAstNode extends AbstractAst.AstNode<HtmlCodeAstNode> {
        @Override
        public HtmlCodeAstNode clone();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static abstract class HtmlCodeAstBranchNode extends AbstractAst.AstBranchNode<HtmlCodeAstNode> implements HtmlCodeAstNode {
        @Override
        public abstract HtmlCodeAstBranchNode clone();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static abstract class HtmlCodeAstLeafNode extends AstLeafNode<HtmlCodeAstNode> implements HtmlCodeAstNode {
        @Override
        public abstract HtmlCodeAstLeafNode clone();
    }

	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class HtmlAstNodeText extends HtmlCodeAstLeafNode {
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
	public static class HtmlAstNodeTag extends HtmlCodeAstBranchNode {
		
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
	public static class HtmlCodeAstNodeRoot extends HtmlCodeAstBranchNode {
		@Override
		public HtmlCodeAstNodeRoot clone() {
			val clonedNode = new HtmlCodeAstNodeRoot();
			clonedNode.getChildren().addAll(getChildren());
			return clonedNode;
		}
	}
	
	public String mapToHtmlString() {
		val escaper = HtmlEscapers.htmlEscaper();
		return mapNodeToHtmlString(escaper, rootNode, false);
	}
	
	protected String mapNodeToHtmlString(Escaper escaper, HtmlCodeAstNode node, boolean innerTextRawRequired) {
		if (node instanceof HtmlAstNodeText) {
			String nodeText = ((HtmlAstNodeText) node).getText();
			return innerTextRawRequired ? nodeText : escaper.escape(nodeText);
		}
		
		boolean childrenInnerTextRawRequired = innerTextRawRequired;
		if (node instanceof HtmlAstNodeTag) { childrenInnerTextRawRequired |= ((HtmlAstNodeTag)node).getTagDefinition().isInnerTextRawRequired(); }
		
		StringBuilder innerHtmlStringBuilder = new StringBuilder();
		for (HtmlCodeAstNode childNode : node.getChildren()) {
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
}
