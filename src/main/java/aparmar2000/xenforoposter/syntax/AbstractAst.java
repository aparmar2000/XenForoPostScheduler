package aparmar2000.xenforoposter.syntax;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractAst<N extends AbstractAst.AstNode<?>> {
	
    public static interface AstNode<N extends AstNode<N>> extends Cloneable {
        public boolean hasChildren();

        @NonNull
        public List<N> getChildren();

        public AstNode<N> clone();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static abstract class AstBranchNode<N extends AstNode<N>> implements AstNode<N> {
        private final List<N> children = new ArrayList<>();

        @Override
        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public abstract AstBranchNode<N> clone();
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public static abstract class AstLeafNode<N extends AstNode<N>> implements AstNode<N> {
        @Override
        public boolean hasChildren() {
            return false;
        }

        @Override
        public List<N> getChildren() {
            return List.of();
        }

        public abstract AstLeafNode<N> clone();
    }

    @Getter
    protected final N rootNode;
}