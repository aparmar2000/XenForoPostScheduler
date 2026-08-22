package aparmar2000.xenforoposter.bbcode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.Inject;

import aparmar2000.xenforoposter.bbcode.BbCodeAst.BbCodeAstBranchNode;
import aparmar2000.xenforoposter.bbcode.BbCodeAst.BbCodeAstLeafNode;
import aparmar2000.xenforoposter.bbcode.BbCodeAst.BbCodeAstNode;
import aparmar2000.xenforoposter.bbcode.BbCodeAst.BbCodeAstNodeRoot;
import aparmar2000.xenforoposter.bbcode.BbCodeAst.BbCodeAstNodeTag;
import aparmar2000.xenforoposter.bbcode.BbCodeAst.BbCodeAstNodeText;
import aparmar2000.xenforoposter.bbcode.BbCodeTokenizer.ParsedToken;
import aparmar2000.xenforoposter.bbcode.BbCodeTokenizer.TagToken;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor(onConstructor_={@Inject})
public class BbCodeAstParser {	
	protected final BbCodeTagDefinitionRegistry tagDefinitionRegistry;
	protected final BbCodeTokenizer tokenizer;
	
	protected boolean allowsTagChildren(BbCodeAstNode node) {
		if (node == null) { return false; }
		if (node instanceof BbCodeAstLeafNode) { return false; }
		if ( node instanceof BbCodeAstNodeTag && !((BbCodeAstNodeTag)node).getTagDefinition().isAllowsInnerTags() ) { return false; }
		return true;
	}
	
	public BbCodeAst parseString(String bbcodeString) {
		val bbcodeTokens = tokenizer.tokenizeString(bbcodeString);
		final BbCodeAstNodeRoot rootNode = new BbCodeAstNodeRoot();
		LinkedList<BbCodeAstBranchNode> currentNodeParents = new LinkedList<>();
		currentNodeParents.add(rootNode);
		
		for (ParsedToken bbcodeToken : bbcodeTokens) {
			BbCodeAstBranchNode lastParent = currentNodeParents.peekLast();
			BbCodeAstNodeTag parentTagNode = lastParent instanceof BbCodeAstNodeTag ? (BbCodeAstNodeTag) lastParent : null;
			boolean parentAllowsTagChildren = allowsTagChildren(lastParent);
			
			// Tag nodes
			if (bbcodeToken instanceof TagToken) {
				TagToken bbcodeTagToken = (TagToken) bbcodeToken;
				
				if (bbcodeTagToken.isEndingTag()) {
					if (parentTagNode != null) {
						if (parentTagNode.getTagDefinition() == bbcodeTagToken.getTagDefinition()) {
							
							currentNodeParents.pollLast();
							continue;
							
						} else if (parentAllowsTagChildren) {
							
							List<BbCodeAstBranchNode> nodesToSplit = new ArrayList<>();
							BbCodeAstBranchNode foundMatchingNode = null;
							for (Iterator<BbCodeAstBranchNode> parentIteratorReversed = currentNodeParents.descendingIterator(); parentIteratorReversed.hasNext();) {
								val nextNode = parentIteratorReversed.next();
								
								if ( nextNode instanceof BbCodeAstNodeTag && ((BbCodeAstNodeTag)nextNode).getTagDefinition()==bbcodeTagToken.getTagDefinition() ) {
									foundMatchingNode = nextNode;
									break;
								}
								
								nodesToSplit.add(0, nextNode);
							}
							
							if (foundMatchingNode != null) {
								while (currentNodeParents.peekLast() != foundMatchingNode) { currentNodeParents.pollLast(); }
								currentNodeParents.pollLast();
								
								for (BbCodeAstBranchNode nodeToSplit : nodesToSplit) {
									lastParent = currentNodeParents.peekLast();
									BbCodeAstBranchNode clonedNode = nodeToSplit.clone();
									
									clonedNode.getChildren().clear();
									lastParent.getChildren().add(clonedNode);
									currentNodeParents.add(clonedNode);
								}
								continue;
							}
							
						}
					}
				} else if (!bbcodeTagToken.isEndingTag() && parentAllowsTagChildren) {
					BbCodeAstNodeTag newTagNode = new BbCodeAstNodeTag(bbcodeTagToken.getRawString(), bbcodeTagToken.getTagDefinition(), bbcodeTagToken.getParameters());
					lastParent.getChildren().add(newTagNode);
					currentNodeParents.add(newTagNode);
					continue;
				}
			}
			
			// Text node merging
			BbCodeAstBranchNode parentNode = currentNodeParents.peekLast();
			val parentNodeChildren = parentNode.getChildren();
			if (!parentNodeChildren.isEmpty() && parentNodeChildren.get(parentNodeChildren.size()-1) instanceof BbCodeAstNodeText) {
				BbCodeAstNodeText prevTextNode = (BbCodeAstNodeText) parentNodeChildren.get(parentNodeChildren.size()-1);
				parentNodeChildren.set( parentNodeChildren.size()-1, new BbCodeAstNodeText(prevTextNode.getText() + bbcodeToken.getRawString()) );
			} else {
				parentNodeChildren.add( new BbCodeAstNodeText(bbcodeToken.getRawString()) );
			}
		}
		
		// Handle unclosed tags
		while (currentNodeParents.size() > 1) {
			BbCodeAstBranchNode mergingParent = currentNodeParents.pollLast();
			BbCodeAstBranchNode lastParent = currentNodeParents.peekLast();
			
			String mergingRawText = "";
			if (mergingParent instanceof BbCodeAstNodeTag) {
				mergingRawText = ((BbCodeAstNodeTag) mergingParent).getRawString();
			}
			
			lastParent.getChildren().remove(mergingParent);
			val parentNodeChildren = lastParent.getChildren();
			List<BbCodeAstNode> mergingChildren = new ArrayList<>();
			mergingChildren.add(new BbCodeAstNodeText(mergingRawText));
			mergingChildren.addAll(mergingParent.getChildren());
			for (BbCodeAstNode mergingChild : mergingChildren) {
				if (mergingChild instanceof BbCodeAstNodeText && !parentNodeChildren.isEmpty() && parentNodeChildren.get(parentNodeChildren.size()-1) instanceof BbCodeAstNodeText) {
					BbCodeAstNodeText prevTextNode = (BbCodeAstNodeText) parentNodeChildren.get(parentNodeChildren.size()-1);
					parentNodeChildren.set( parentNodeChildren.size()-1, new BbCodeAstNodeText(prevTextNode.getText() + ((BbCodeAstNodeText) mergingChild).getText()) );
				} else {
					parentNodeChildren.add(mergingChild);
				}
			}
		}
		
		return new BbCodeAst(rootNode);
	}
}