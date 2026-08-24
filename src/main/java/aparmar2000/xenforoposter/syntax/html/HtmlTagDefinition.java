package aparmar2000.xenforoposter.syntax.html;

import com.google.common.collect.ImmutableMap;

import aparmar2000.xenforoposter.syntax.TagDefinition;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal=true, level=AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode(callSuper = true)
public class HtmlTagDefinition extends TagDefinition {
	boolean allowsInnerTags;
	boolean innerTextRawRequired;
	
	HtmlStringMapper stringMapper;
	
	public HtmlTagDefinition(String tag, boolean allowsInnerTags, boolean innerTextRawRequired, HtmlStringMapper stringMapper) {
		super(tag);
		this.allowsInnerTags = allowsInnerTags;
		this.innerTextRawRequired = innerTextRawRequired;
		this.stringMapper = stringMapper;
	}
	
	public String mapNode(ImmutableMap<String, String> parameters, String innerText) throws HtmlStringMapper.StringMappingException {
		return stringMapper.mapNode(this, parameters, innerText);
	}
	
	public static HtmlTagDefinition simpleHtmlSingularTag(String htmlTag) {
		return new HtmlTagDefinition(htmlTag, false, false, HtmlStringMapper.simpleHtmlSingularTag(htmlTag));
	}
	
	public static HtmlTagDefinition simpleHtmlTagWrapper(String htmlTag, boolean innerTextRawRequired) {
		return new HtmlTagDefinition(htmlTag, true, innerTextRawRequired, HtmlStringMapper.simpleHtmlTagWrapper(htmlTag));
	}
	
	@FunctionalInterface
	public static interface HtmlStringMapper {
		public static class StringMappingException extends Exception {
			private static final long serialVersionUID = -5694183233110625711L;			
		}
		
		String mapNode(HtmlTagDefinition tagDefinition, ImmutableMap<String, String> parameters, String innerText) throws StringMappingException;

		public static HtmlStringMapper simpleHtmlSingularTag(String htmlTag) {
			return (tagDef, params, innerText) -> String.format("<%s/>", htmlTag);
		}
		
		public static HtmlStringMapper simpleHtmlTagWrapper(String htmlTag) {
			return (tagDef, params, innerText) -> String.format("<%1$s>%2$s</%1$s>", htmlTag, innerText);
		}
	}
}
