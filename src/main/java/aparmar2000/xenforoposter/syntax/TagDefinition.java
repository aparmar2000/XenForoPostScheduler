package aparmar2000.xenforoposter.syntax;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(makeFinal=true, level=AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public abstract class TagDefinition {
	String tag;
}
