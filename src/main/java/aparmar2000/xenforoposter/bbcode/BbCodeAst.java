package aparmar2000.xenforoposter.bbcode;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class BbCodeAst {	
	public static abstract class BbCodeAstNode implements Cloneable {
		public abstract boolean hasChildren();
		@NonNull
		public abstract List<BbCodeAstNode> getChildren();
		
		public abstract BbCodeAstNode clone();
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static abstract class BbCodeAstBranchNode extends BbCodeAstNode {
		private final List<BbCodeAstNode> children = new ArrayList<>();
		
		public boolean hasChildren() {
			return !children.isEmpty();
		}
		
		public abstract BbCodeAstBranchNode clone();
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	public static abstract class BbCodeAstLeafNode extends BbCodeAstNode {
		public boolean hasChildren() {
			return false;
		}
		
		public List<BbCodeAstNode> getChildren() {
			return List.of();
		}
		
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
	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
	@EqualsAndHashCode(callSuper = true)
	public static class BbCodeAstNodeTag extends BbCodeAstBranchNode {
		/**
		 * The name used for 'root parameters', i.e. [tag=value]
		 */
		public static final String ROOT_PARAMETER_NAME = "$value";

		private final String rawString;
		private final BbCodeTagDefinition tagDefinition;
		private final ImmutableMap<String, String> parameters;
		
		@Override
		public BbCodeAstNodeTag clone() {
			val clonedNode = new BbCodeAstNodeTag(rawString, tagDefinition, parameters);
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
	
	@Getter
	private final BbCodeAstNodeRoot rootNode;
}
