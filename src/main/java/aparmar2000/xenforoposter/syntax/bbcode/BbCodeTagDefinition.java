package aparmar2000.xenforoposter.syntax.bbcode;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTagDefinition.HtmlNodeMapper.HtmlMappingException;
import lombok.Value;

@Value
public class BbCodeTagDefinition {
	String tag;
	boolean allowsInnerTags;
	
	HtmlNodeMapper htmlMapper;
	
	public String mapNode(ImmutableMap<String, String> parameters, String innerText) throws HtmlMappingException {
		return htmlMapper.mapNode(this, parameters, innerText);
	}
	
	public static BbCodeTagDefinition simpleHtmlSingularTag(String bbcodeTag, String htmlTag) {
		return new BbCodeTagDefinition(bbcodeTag, false, HtmlNodeMapper.simpleHtmlSingularTag(htmlTag));
	}
	
	public static BbCodeTagDefinition simpleHtmlTagWrapper(String bbcodeTag, String htmlTag) {
		return new BbCodeTagDefinition(bbcodeTag, true, HtmlNodeMapper.simpleHtmlTagWrapper(htmlTag));
	}
	
	@FunctionalInterface
	public static interface HtmlNodeMapper {
		public static class HtmlMappingException extends Exception {
			private static final long serialVersionUID = -5694183233110625711L;			
		}
		
		String mapNode(BbCodeTagDefinition tagDefinition, ImmutableMap<String, String> parameters, String innerText) throws HtmlMappingException;

		public static HtmlNodeMapper simpleHtmlSingularTag(String htmlTag) {
			return (tagDef, params, innerText) -> String.format("<%s/>", htmlTag);
		}
		
		public static HtmlNodeMapper simpleHtmlTagWrapper(String htmlTag) {
			return (tagDef, params, innerText) -> String.format("<%1$s>%2$s</%1$s>", htmlTag, innerText);
		}
	}
}
