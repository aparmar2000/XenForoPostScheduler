package aparmar2000.xenforoposter.syntax.html;

import java.util.Arrays;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.AbstractAst;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
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
	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
	@EqualsAndHashCode(callSuper = true)
	public static class HtmlCodeAstNodeText extends HtmlCodeAstLeafNode {
		private final String text;
		
		public HtmlCodeAstNodeText merge(HtmlCodeAstNodeText other) {
			HtmlCodeAstNodeText mergedNode = new HtmlCodeAstNodeText(text + other.getText());
			
			return mergedNode;
		}

		@Override
		public HtmlCodeAstNodeText clone() {
			return new HtmlCodeAstNodeText(text);
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
	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
	@EqualsAndHashCode(callSuper = true)
	public static class HtmlCodeAstNodeRoot extends HtmlCodeAstBranchNode {
		@Override
		public HtmlCodeAstNodeRoot clone() {
			val clonedNode = new HtmlCodeAstNodeRoot();
			clonedNode.getChildren().addAll(getChildren());
			return clonedNode;
		}
	}
}
