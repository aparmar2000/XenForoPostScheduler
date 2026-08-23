package aparmar2000.xenforoposter.syntax.bbcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.AbstractAst;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition.HtmlNodeMapper.HtmlMappingException;
import aparmar2000.xenforoposter.syntax.html.HtmlAst;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeText;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNode;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeRoot;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.val;

public class BbCodeAst extends AbstractAst<BbCodeAst.BbCodeAstNodeRoot> {

    public BbCodeAst(BbCodeAstNodeRoot rootNode) {
        super(rootNode);
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
	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
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
	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
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
}
