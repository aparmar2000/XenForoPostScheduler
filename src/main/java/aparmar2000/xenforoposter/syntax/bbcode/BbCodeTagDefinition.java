package aparmar2000.xenforoposter.syntax.bbcode;

import java.util.List;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.AbstractAst;
import aparmar2000.xenforoposter.syntax.TagDefinition;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition.HtmlNodeMapper.HtmlMappingException;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNode;
import aparmar2000.xenforoposter.syntax.html.HtmlAst.HtmlAstNodeTag;
import aparmar2000.xenforoposter.syntax.html.HtmlTagDefinition;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal=true, level=AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode(callSuper = true)
public class BbCodeTagDefinition extends TagDefinition {
	boolean allowsInnerTags;
	
	HtmlNodeMapper htmlMapper;
	
	public BbCodeTagDefinition(String tag, boolean allowsInnerTags, HtmlNodeMapper htmlMapper) {
		super(tag);
		this.allowsInnerTags = allowsInnerTags;
		this.htmlMapper = htmlMapper;
	}
	
	public HtmlAstNode mapNode(ImmutableMap<String, String> parameters, List<HtmlAstNode> htmlChildNodes) throws HtmlMappingException {
		return htmlMapper.mapNode(this, parameters, htmlChildNodes);
	}
	
	public static BbCodeTagDefinition simpleHtmlSingularTag(String bbcodeTag, HtmlTagDefinition htmlTag) {
		return new BbCodeTagDefinition(bbcodeTag, false, HtmlNodeMapper.simpleHtmlSingularTag(htmlTag));
	}
	
	public static BbCodeTagDefinition simpleHtmlTagWrapper(String bbcodeTag, HtmlTagDefinition htmlTag) {
		return new BbCodeTagDefinition(bbcodeTag, true, HtmlNodeMapper.simpleHtmlTagWrapper(htmlTag));
	}
	
	@FunctionalInterface
	public static interface HtmlNodeMapper {
		public static class HtmlMappingException extends Exception {
			private static final long serialVersionUID = -5694183233110625711L;			
		}
		
		HtmlAstNode mapNode(BbCodeTagDefinition tagDefinition, ImmutableMap<String, String> parameters, List<HtmlAstNode> htmlChildNodes) throws HtmlMappingException;

		public static HtmlNodeMapper simpleHtmlSingularTag(HtmlTagDefinition htmlTag) {
			return (tagDef, params, htmlChildNodes) -> new HtmlAstNodeTag(htmlTag, ImmutableMap.of());
		}
		
		public static HtmlNodeMapper simpleHtmlTagWrapper(HtmlTagDefinition htmlTag) {
			return (tagDef, params, htmlChildNodes) -> AbstractAst.wrap(new HtmlAstNodeTag(htmlTag, ImmutableMap.of()), htmlChildNodes);
		}
	}
}
