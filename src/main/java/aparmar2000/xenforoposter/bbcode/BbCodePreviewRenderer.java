package aparmar2000.xenforoposter.bbcode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.utils.InternalResourceLoader;

public class BbCodePreviewRenderer {

    private static final String PREVIEW_TEMPLATE_PATH = "bbcode/preview_template.html";
    private static final String PREVIEW_DARK_CSS_PATH = "bbcode/preview_dark.css";
    private static final String PREVIEW_LIGHT_CSS_PATH = "bbcode/preview_light.css";

    private final String templateHtml;
    private final String darkCss;
    private final String lightCss;

    public BbCodePreviewRenderer() {
        this.templateHtml = InternalResourceLoader.tryGetInternalResourceAsStringSilent(PREVIEW_TEMPLATE_PATH)
                .orElse("<!DOCTYPE html>\n<html>\n<head>\n<style>\n{{CSS_STYLES}}\n</style>\n</head>\n<body>\n{{CONTENT}}\n</body>\n</html>");
        this.darkCss = InternalResourceLoader.tryGetInternalResourceAsStringSilent(PREVIEW_DARK_CSS_PATH)
                .orElse("");
        this.lightCss = InternalResourceLoader.tryGetInternalResourceAsStringSilent(PREVIEW_LIGHT_CSS_PATH)
                .orElse("");
    }

    @NotNull
    public String renderToHtml(@Nullable String bbCode, boolean darkTheme) {
        if (bbCode == null || bbCode.trim().isEmpty()) {
            return wrapInXenForoStyle("<p><i>(Empty preview)</i></p>", darkTheme);
        }

        String html = convertBbCodeToHtml(bbCode);
        return wrapInXenForoStyle(html, darkTheme);
    }

    @NotNull
    public String convertBbCodeToHtml(@NotNull String bbCode) {
        String text = bbCode.replace("\r\n", "\n").replace("\r", "\n");

        // Extract code blocks
        List<String> codePlaceholders = new ArrayList<>();
        Pattern codeBlockPattern = Pattern.compile("(?is)\\[CODE(?:=([^\\]]+))?\\](.*?)\\[/CODE\\]");
        Matcher codeMatcher = codeBlockPattern.matcher(text);
        StringBuffer codeSb = new StringBuffer();
        while (codeMatcher.find()) {
            String lang = codeMatcher.group(1);
            String content = codeMatcher.group(2);
            if (lang != null) {
                lang = lang.replace("'", "").replace("\"", "").trim();
            }
            String escapedCode = escapeHtml(content != null ? content.trim() : "");
            String header = (lang != null && !lang.isEmpty())
                    ? "<div class=\"code-header\">Code (" + escapeHtml(lang) + "):</div>"
                    : "<div class=\"code-header\">Code:</div>";
            String replacement = "<pre class=\"bbcode-code\">" + header + "<code>" + escapedCode + "</code></pre>";
            codePlaceholders.add(replacement);
            codeMatcher.appendReplacement(codeSb, "___CODE_BLOCK_PLACEHOLDER_" + (codePlaceholders.size() - 1) + "___");
        }
        codeMatcher.appendTail(codeSb);
        text = codeSb.toString();

        // Extract inline code
        List<String> icodePlaceholders = new ArrayList<>();
        Pattern icodePattern = Pattern.compile("(?is)\\[ICODE\\](.*?)\\[/ICODE\\]");
        Matcher icodeMatcher = icodePattern.matcher(text);
        StringBuffer icodeSb = new StringBuffer();
        while (icodeMatcher.find()) {
            String content = icodeMatcher.group(1);
            String escaped = escapeHtml(content != null ? content : "");
            String replacement = "<code class=\"bbcode-icode\">" + escaped + "</code>";
            icodePlaceholders.add(replacement);
            icodeMatcher.appendReplacement(icodeSb, "___ICODE_PLACEHOLDER_" + (icodePlaceholders.size() - 1) + "___");
        }
        icodeMatcher.appendTail(icodeSb);
        text = icodeSb.toString();

        // HTML escape text
        text = escapeHtml(text);

        // Restore placeholders temporary token format for subsequent tag matching

        // Headings
        text = text.replaceAll("(?is)\\[HEADING=1\\](.*?)\\[/HEADING=1\\]", "<h1 class=\"bbcode-heading\">$1</h1>");
        text = text.replaceAll("(?is)\\[HEADING=2\\](.*?)\\[/HEADING=2\\]", "<h2 class=\"bbcode-heading\">$1</h2>");
        text = text.replaceAll("(?is)\\[HEADING=3\\](.*?)\\[/HEADING=3\\]", "<h3 class=\"bbcode-heading\">$1</h3>");
        text = text.replaceAll("(?is)\\[HEADING=([1-6])\\](.*?)\\[/HEADING=\\1\\]", "<h$1 class=\"bbcode-heading\">$2</h$1>");

        // Inline formatting
        text = text.replaceAll("(?is)\\[B\\](.*?)\\[/B\\]", "<b>$1</b>");
        text = text.replaceAll("(?is)\\[I\\](.*?)\\[/I\\]", "<i>$1</i>");
        text = text.replaceAll("(?is)\\[U\\](.*?)\\[/U\\]", "<u>$1</u>");
        text = text.replaceAll("(?is)\\[S\\](.*?)\\[/S\\]", "<s>$1</s>");

        // Color, Size, Font
        text = text.replaceAll("(?is)\\[COLOR=['\"]?([#a-zA-Z0-9_-]+)['\"]?\\](.*?)\\[/COLOR\\]", "<span style=\"color: $1;\">$2</span>");
        text = text.replaceAll("(?is)\\[FONT=['\"]?([^'\"]+)['\"]?\\](.*?)\\[/FONT\\]", "<span style=\"font-family: $1;\">$2</span>");
        text = text.replaceAll("(?is)\\[SIZE=['\"]?([0-9]+(?:px|pt)?)['\"]?\\](.*?)\\[/SIZE\\]", "<span style=\"font-size: $1;\">$2</span>");

        // Alignment
        text = text.replaceAll("(?is)\\[CENTER\\](.*?)\\[/CENTER\\]", "<div style=\"text-align: center;\">$1</div>");
        text = text.replaceAll("(?is)\\[LEFT\\](.*?)\\[/LEFT\\]", "<div style=\"text-align: left;\">$1</div>");
        text = text.replaceAll("(?is)\\[RIGHT\\](.*?)\\[/RIGHT\\]", "<div style=\"text-align: right;\">$1</div>");
        text = text.replaceAll("(?is)\\[ALIGN=['\"]?(left|center|right|justify)['\"]?\\](.*?)\\[/ALIGN\\]", "<div style=\"text-align: $1;\">$2</div>");

        // URLs and Images
        text = text.replaceAll("(?is)\\[URL=['\"]?([^'\"]+?)['\"]?\\](.*?)\\[/URL\\]", "<a href=\"$1\" target=\"_blank\">$2</a>");
        text = text.replaceAll("(?is)\\[URL\\](.*?)\\[/URL\\]", "<a href=\"$1\" target=\"_blank\">$1</a>");
        text = text.replaceAll("(?is)\\[IMG\\](.*?)\\[/IMG\\]", "<img src=\"$1\" style=\"max-width: 100%; height: auto; border-radius: 4px;\" />");

        // User Mentions
        text = text.replaceAll("(?is)\\[USER=['\"]?([0-9]+)['\"]?\\](.*?)\\[/USER\\]", "<span class=\"bbcode-user\">@$2</span>");

        // Horizontal Rules
        text = text.replaceAll("(?is)\\[HR\\]\\[/HR\\]", "<hr class=\"bbcode-hr\" />");
        text = text.replaceAll("(?is)\\[HR\\]", "<hr class=\"bbcode-hr\" />");

        // Spoilers
        text = text.replaceAll("(?is)\\[SPOILER=['\"]?([^'\"]*?)['\"]?\\](.*?)\\[/SPOILER\\]",
                "<div class=\"bbcode-spoiler\"><div class=\"spoiler-title\">Spoiler: $1</div><div class=\"spoiler-body\">$2</div></div>");
        text = text.replaceAll("(?is)\\[SPOILER\\](.*?)\\[/SPOILER\\]",
                "<div class=\"bbcode-spoiler\"><div class=\"spoiler-title\">Spoiler</div><div class=\"spoiler-body\">$1</div></div>");

        // Quotes (process multiple times to handle nesting)
        for (int i = 0; i < 3; i++) {
            text = text.replaceAll("(?is)\\[QUOTE=['\"]?([^'\"]+?)['\"]?\\](.*?)\\[/QUOTE\\]",
                    "<blockquote class=\"bbcode-quote\"><div class=\"quote-author\">$1 said:</div>$2</blockquote>");
            text = text.replaceAll("(?is)\\[QUOTE\\](.*?)\\[/QUOTE\\]",
                    "<blockquote class=\"bbcode-quote\">$1</blockquote>");
        }

        // Tables
        text = text.replaceAll("(?is)\\[TABLE\\](.*?)\\[/TABLE\\]", "<table class=\"bbcode-table\">$1</table>");
        text = text.replaceAll("(?is)\\[TR\\](.*?)\\[/TR\\]", "<tr>$1</tr>");
        text = text.replaceAll("(?is)\\[TH\\](.*?)\\[/TH\\]", "<th>$1</th>");
        text = text.replaceAll("(?is)\\[TD\\](.*?)\\[/TD\\]", "<td>$1</td>");

        // Lists
        text = processLists(text);

        // Convert newlines outside of preformatted blocks to <br> or paragraphs
        text = text.replace("\n", "<br>\n");

        // Restore code placeholders
        for (int i = 0; i < icodePlaceholders.size(); i++) {
            text = text.replace("___ICODE_PLACEHOLDER_" + i + "___", icodePlaceholders.get(i));
        }
        for (int i = 0; i < codePlaceholders.size(); i++) {
            text = text.replace("___CODE_BLOCK_PLACEHOLDER_" + i + "___", codePlaceholders.get(i));
        }

        return text;
    }

    private String processLists(String input) {
        // Handle [LIST=1]...[/LIST] (Ordered)
        Pattern olPattern = Pattern.compile("(?is)\\[LIST=1\\](.*?)\\[/LIST\\]");
        Matcher olMatcher = olPattern.matcher(input);
        StringBuffer olSb = new StringBuffer();
        while (olMatcher.find()) {
            String listContent = olMatcher.group(1);
            String renderedList = "<ol class=\"bbcode-list\">" + renderListItems(listContent) + "</ol>";
            olMatcher.appendReplacement(olSb, Matcher.quoteReplacement(renderedList));
        }
        olMatcher.appendTail(olSb);
        String text = olSb.toString();

        // Handle [LIST]...[/LIST] (Unordered)
        Pattern ulPattern = Pattern.compile("(?is)\\[LIST\\](.*?)\\[/LIST\\]");
        Matcher ulMatcher = ulPattern.matcher(text);
        StringBuffer ulSb = new StringBuffer();
        while (ulMatcher.find()) {
            String listContent = ulMatcher.group(1);
            String renderedList = "<ul class=\"bbcode-list\">" + renderListItems(listContent) + "</ul>";
            ulMatcher.appendReplacement(ulSb, Matcher.quoteReplacement(renderedList));
        }
        ulMatcher.appendTail(ulSb);
        return ulSb.toString();
    }

    private String renderListItems(String content) {
        String[] items = content.split("\\[\\*\\]");
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                sb.append("<li>").append(trimmed).append("</li>");
            }
        }
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) { return ""; }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private String wrapInXenForoStyle(String contentHtml, boolean darkTheme) {
        String css = darkTheme ? darkCss : lightCss;
        return templateHtml
                .replace("{{CSS_STYLES}}", css)
                .replace("{{CONTENT}}", contentHtml != null ? contentHtml : "");
    }
}
