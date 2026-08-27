package aparmar2000.xenforoposter.syntax.bbcode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.ParsedToken;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.TagToken;
import aparmar2000.xenforoposter.syntax.bbcode.BbCodeTokenizer.TextToken;
import lombok.NonNull;

public final class BbCodeTokenUtils {

	private BbCodeTokenUtils() {}

	@NotNull
	public static List<ParsedToken> findTokens(@NonNull List<ParsedToken> tokens,
			@NonNull Predicate<ParsedToken> predicate) {
		return tokens.stream().filter(predicate).collect(Collectors.toList());
	}

	@NotNull
	@SuppressWarnings("unchecked")
	public static <T extends ParsedToken> List<T> findTokensOfType(@NonNull List<ParsedToken> tokens,
			@NonNull Class<T> clazz) {
		return tokens.stream()
				.filter(clazz::isInstance)
				.map(t -> (T) t)
				.collect(Collectors.toList());
	}

	@NotNull
	public static List<TagToken> findTagTokens(@NonNull List<ParsedToken> tokens,
			@NonNull String tagName) {
		return tokens.stream()
				.filter(TagToken.class::isInstance)
				.map(TagToken.class::cast)
				.filter(t -> t.getTagDefinition() != null && t.getTagDefinition().getTag().equalsIgnoreCase(tagName))
				.collect(Collectors.toList());
	}

	@NotNull
	public static List<TextToken> findTextTokens(@NonNull List<ParsedToken> tokens) {
		return findTokensOfType(tokens, TextToken.class);
	}

	@NotNull
	public static List<TextToken> findTextTokensContaining(@NonNull List<ParsedToken> tokens,
			@NonNull String substring) {
		return tokens.stream()
				.filter(TextToken.class::isInstance)
				.map(TextToken.class::cast)
				.filter(t -> t.getRawString().contains(substring))
				.collect(Collectors.toList());
	}

	@NotNull
	public static List<TextToken> findTextTokensMatching(@NonNull List<ParsedToken> tokens,
			@NonNull Pattern pattern) {
		return tokens.stream()
				.filter(TextToken.class::isInstance)
				.map(TextToken.class::cast)
				.filter(t -> pattern.matcher(t.getRawString()).find())
				.collect(Collectors.toList());
	}

	public static int indexOf(@NonNull List<ParsedToken> tokens, @NonNull Predicate<ParsedToken> predicate) {
		return indexOf(tokens, predicate, 0);
	}

	public static int indexOf(@NonNull List<ParsedToken> tokens, @NonNull Predicate<ParsedToken> predicate, int fromIndex) {
		for (int i = Math.max(0, fromIndex); i < tokens.size(); i++) {
			if (predicate.test(tokens.get(i))) {
				return i;
			}
		}
		return -1;
	}

	@NotNull
	public static List<ParsedToken> replaceTokens(@NonNull List<ParsedToken> tokens,
			@NonNull Predicate<ParsedToken> predicate,
			@NonNull Function<ParsedToken, List<ParsedToken>> replacer) {
		List<ParsedToken> result = new ArrayList<>();
		for (ParsedToken token : tokens) {
			if (predicate.test(token)) {
				List<ParsedToken> replacements = replacer.apply(token);
				if (replacements != null) {
					result.addAll(replacements);
				}
			} else {
				result.add(token);
			}
		}
		return result;
	}

	@NotNull
	public static List<ParsedToken> replaceSingleToken(@NonNull List<ParsedToken> tokens,
			@NonNull Predicate<ParsedToken> predicate,
			@NonNull Function<ParsedToken, ParsedToken> replacer) {
		List<ParsedToken> result = new ArrayList<>();
		for (ParsedToken token : tokens) {
			if (predicate.test(token)) {
				ParsedToken replacement = replacer.apply(token);
				if (replacement != null) {
					result.add(replacement);
				}
			} else {
				result.add(token);
			}
		}
		return result;
	}

	@NotNull
	public static List<ParsedToken> replaceText(@NonNull List<ParsedToken> tokens,
			@NonNull String target,
			@NonNull String replacement) {
		List<ParsedToken> result = new ArrayList<>();
		for (ParsedToken token : tokens) {
			if (token instanceof TextToken) {
				String newText = token.getRawString().replace(target, replacement);
				result.add(new TextToken(newText));
			} else {
				result.add(token);
			}
		}
		return result;
	}

	@NotNull
	public static List<ParsedToken> replaceText(@NonNull List<ParsedToken> tokens,
			@NonNull Pattern pattern,
			@NonNull Function<Matcher, String> replacer) {
		List<ParsedToken> result = new ArrayList<>();
		for (ParsedToken token : tokens) {
			if (token instanceof TextToken) {
				Matcher matcher = pattern.matcher(token.getRawString());
				StringBuilder sb = new StringBuilder();
				while (matcher.find()) {
					String rep = replacer.apply(matcher);
					matcher.appendReplacement(sb, Matcher.quoteReplacement(rep != null ? rep : ""));
				}
				matcher.appendTail(sb);
				result.add(new TextToken(sb.toString()));
			} else {
				result.add(token);
			}
		}
		return result;
	}

	@NotNull
	public static List<ParsedToken> replaceTag(@NonNull List<ParsedToken> tokens,
			@NonNull String oldTagName,
			@NonNull String newTagName,
			@NonNull BbCodeTagDefinition newTagDefinition) {
		List<ParsedToken> result = new ArrayList<>();
		for (ParsedToken token : tokens) {
			if (token instanceof TagToken) {
				TagToken tagToken = (TagToken) token;
				if (tagToken.getTagDefinition() != null && tagToken.getTagDefinition().getTag().equalsIgnoreCase(oldTagName)) {
					BbCodeTagDefinition def = newTagDefinition != null ? newTagDefinition : tagToken.getTagDefinition();
					String oldRaw = tagToken.getRawString();
					String newRaw = tagToken.isEndingTag()
							? "[/" + newTagName + "]"
							: oldRaw.replaceFirst("(?i)^\\[" + Pattern.quote(oldTagName), "[" + newTagName);
					result.add(new TagToken(newRaw, def, tagToken.isEndingTag(), tagToken.getParameters()));
					continue;
				}
			}
			result.add(token);
		}
		return result;
	}

	@NotNull
	public static List<ParsedToken> removeTokens(@NonNull List<ParsedToken> tokens,
			@NonNull Predicate<ParsedToken> predicate) {
		return tokens.stream().filter(predicate.negate()).collect(Collectors.toList());
	}

	@NotNull
	public static String tokensToString(@NonNull List<ParsedToken> tokens) {
		StringBuilder sb = new StringBuilder();
		for (ParsedToken token : tokens) {
			sb.append(token.getRawString());
		}
		return sb.toString();
	}
}
