package aparmar2000.xenforoposter.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.syntax.AbstractAst.AstBranchNode;
import aparmar2000.xenforoposter.syntax.AbstractAst.AstNode;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNode;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeTag;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeAst.BbCodeAstNodeText;
import aparmar2000.xenforoposter.syntax.html.HtmlAst;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNode;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeTag;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeText;

public final class AstUtils {

	private AstUtils() {}

	// --- Generic AST Search

	@NotNull
	@SuppressWarnings("unchecked")
	public static <N extends AstNode<N>> List<N> findNodes(@NotNull AstNode<N> root,
			@NotNull Predicate<N> predicate) {
		List<N> result = new ArrayList<>();
		if (predicate.test((N) root)) {
			result.add((N) root);
		}
		if (root.hasChildren()) {
			for (N child : root.getChildren()) {
				result.addAll(findNodes(child, predicate));
			}
		}
		return result;
	}

	@NotNull
	@SuppressWarnings("unchecked")
	public static <N extends AstNode<N>, T extends N> List<T> findNodesOfType(@NotNull AstNode<N> root,
			@NotNull Class<T> clazz) {
		List<T> result = new ArrayList<>();
		if (clazz.isInstance(root)) {
			result.add((T) root);
		}
		if (root.hasChildren()) {
			for (N child : root.getChildren()) {
				result.addAll(findNodesOfType(child, clazz));
			}
		}
		return result;
	}

	// --- BbCodeAst Specific Search

	@NotNull
	public static List<BbCodeAstNodeTag> findBbCodeTags(@NotNull BbCodeAst ast, @NotNull String tagName) {
		return findBbCodeTags(ast.getRootNode(), tagName);
	}

	@NotNull
	public static List<BbCodeAstNodeTag> findBbCodeTags(@NotNull BbCodeAstNode root, @NotNull String tagName) {
		return findNodesOfType(root, BbCodeAstNodeTag.class).stream()
				.filter(t -> t.getTagDefinition() != null && t.getTagDefinition().getTag().equalsIgnoreCase(tagName))
				.collect(Collectors.toList());
	}

	@NotNull
	public static List<BbCodeAstNodeText> findTextNodes(@NotNull BbCodeAst ast) {
		return findNodesOfType(ast.getRootNode(), BbCodeAstNodeText.class);
	}

	@NotNull
	public static List<BbCodeAstNodeText> findTextNodesContaining(@NotNull BbCodeAst ast, @NotNull String substring) {
		return findTextNodes(ast).stream()
				.filter(t -> t.getText() != null && t.getText().contains(substring))
				.collect(Collectors.toList());
	}

	@NotNull
	public static List<BbCodeAstNodeText> findTextNodesMatching(@NotNull BbCodeAst ast, @NotNull Pattern pattern) {
		return findTextNodes(ast).stream()
				.filter(t -> t.getText() != null && pattern.matcher(t.getText()).find())
				.collect(Collectors.toList());
	}

	// --- HtmlAst Specific Search

	@NotNull
	public static List<HtmlAstNodeTag> findHtmlTags(@NotNull HtmlAst ast, @NotNull String tagName) {
		return findHtmlTags(ast.getRootNode(), tagName);
	}

	@NotNull
	public static List<HtmlAstNodeTag> findHtmlTags(@NotNull HtmlAstNode root, @NotNull String tagName) {
		return findNodesOfType(root, HtmlAstNodeTag.class).stream()
				.filter(t -> t.getTagDefinition() != null && t.getTagDefinition().getTag().equalsIgnoreCase(tagName))
				.collect(Collectors.toList());
	}

	@NotNull
	public static List<HtmlAstNodeText> findTextNodes(@NotNull HtmlAst ast) {
		return findNodesOfType(ast.getRootNode(), HtmlAstNodeText.class);
	}

	@NotNull
	public static List<HtmlAstNodeText> findTextNodesContaining(@NotNull HtmlAst ast, @NotNull String substring) {
		return findTextNodes(ast).stream()
				.filter(t -> t.getText() != null && t.getText().contains(substring))
				.collect(Collectors.toList());
	}

	@NotNull
	public static List<HtmlAstNodeText> findTextNodesMatching(@NotNull HtmlAst ast, @NotNull Pattern pattern) {
		return findTextNodes(ast).stream()
				.filter(t -> t.getText() != null && pattern.matcher(t.getText()).find())
				.collect(Collectors.toList());
	}

	// --- Generic AST Transformation & Replacement

	@SuppressWarnings("unchecked")
	public static <N extends AstNode<N>> void replaceNodes(@NotNull AstBranchNode<N> parent,
			@NotNull Predicate<N> predicate,
			@NotNull Function<N, List<N>> replacer) {
		List<N> children = parent.getChildren();
		List<N> newChildren = new ArrayList<>();

		for (N child : children) {
			if (predicate.test(child)) {
				List<N> replacements = replacer.apply(child);
				if (replacements != null) {
					newChildren.addAll(replacements);
				}
			} else {
				newChildren.add(child);
				if (child instanceof AstBranchNode) {
					replaceNodes((AstBranchNode<N>) child, predicate, replacer);
				}
			}
		}

		children.clear();
		children.addAll(newChildren);
	}

	public static <N extends AstNode<N>> void replaceNode(@NotNull AstBranchNode<N> parent,
			@NotNull Predicate<N> predicate,
			@NotNull Function<N, N> replacer) {
		replaceNodes(parent, predicate, node -> {
			N replacement = replacer.apply(node);
			return replacement != null ? List.of(replacement) : List.of();
		});
	}

	public static <N extends AstNode<N>> void removeNodes(@NotNull AstBranchNode<N> parent,
			@NotNull Predicate<N> predicate) {
		replaceNodes(parent, predicate, node -> List.of());
	}

	// --- BbCodeAst Text & Tag Replacements

	public static void replaceText(@NotNull BbCodeAst ast, @NotNull String target, @NotNull String replacement) {
		replaceNodes(ast.getRootNode(),
				node -> node instanceof BbCodeAstNodeText && ((BbCodeAstNodeText) node).getText().contains(target),
				node -> {
					String oldText = ((BbCodeAstNodeText) node).getText();
					return List.of(new BbCodeAstNodeText(oldText.replace(target, replacement)));
				});
	}

	public static void replaceText(@NotNull BbCodeAst ast, @NotNull Pattern pattern, @NotNull Function<Matcher, String> replacer) {
		replaceNodes(ast.getRootNode(),
				node -> node instanceof BbCodeAstNodeText && pattern.matcher(((BbCodeAstNodeText) node).getText()).find(),
				node -> {
					String oldText = ((BbCodeAstNodeText) node).getText();
					Matcher matcher = pattern.matcher(oldText);
					StringBuilder sb = new StringBuilder();
					while (matcher.find()) {
						String rep = replacer.apply(matcher);
						matcher.appendReplacement(sb, Matcher.quoteReplacement(rep != null ? rep : ""));
					}
					matcher.appendTail(sb);
					return List.of(new BbCodeAstNodeText(sb.toString()));
				});
	}

	public static void replaceBbCodeTags(@NotNull BbCodeAst ast, @NotNull String tagName,
			@NotNull Function<BbCodeAstNodeTag, BbCodeAstNode> replacer) {
		replaceNode(ast.getRootNode(),
				node -> node instanceof BbCodeAstNodeTag && ((BbCodeAstNodeTag) node).getTagDefinition() != null
						&& ((BbCodeAstNodeTag) node).getTagDefinition().getTag().equalsIgnoreCase(tagName),
				node -> replacer.apply((BbCodeAstNodeTag) node));
	}

	// --- HtmlAst Text & Tag Replacements

	public static void replaceText(@NotNull HtmlAst ast, @NotNull String target, @NotNull String replacement) {
		replaceNodes(ast.getRootNode(),
				node -> node instanceof HtmlAstNodeText && ((HtmlAstNodeText) node).getText().contains(target),
				node -> {
					String oldText = ((HtmlAstNodeText) node).getText();
					return List.of(new HtmlAstNodeText(oldText.replace(target, replacement)));
				});
	}

	public static void replaceText(@NotNull HtmlAst ast, @NotNull Pattern pattern, @NotNull Function<Matcher, String> replacer) {
		replaceNodes(ast.getRootNode(),
				node -> node instanceof HtmlAstNodeText && pattern.matcher(((HtmlAstNodeText) node).getText()).find(),
				node -> {
					String oldText = ((HtmlAstNodeText) node).getText();
					Matcher matcher = pattern.matcher(oldText);
					StringBuilder sb = new StringBuilder();
					while (matcher.find()) {
						String rep = replacer.apply(matcher);
						matcher.appendReplacement(sb, Matcher.quoteReplacement(rep != null ? rep : ""));
					}
					matcher.appendTail(sb);
					return List.of(new HtmlAstNodeText(sb.toString()));
				});
	}

	public static void replaceHtmlTags(@NotNull HtmlAst ast, @NotNull String tagName,
			@NotNull Function<HtmlAstNodeTag, HtmlAstNode> replacer) {
		replaceNode(ast.getRootNode(),
				node -> node instanceof HtmlAstNodeTag && ((HtmlAstNodeTag) node).getTagDefinition() != null
						&& ((HtmlAstNodeTag) node).getTagDefinition().getTag().equalsIgnoreCase(tagName),
				node -> replacer.apply((HtmlAstNodeTag) node));
	}
}
