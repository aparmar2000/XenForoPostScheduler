package aparmar2000.xenforoposter.extension.builtin;

import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.extension.Extension;
import aparmar2000.xenforoposter.extension.ExtensionContext;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import aparmar2000.xenforoposter.settings.defs.MultilineStringSettingDefinition;
import aparmar2000.xenforoposter.utils.InternalResourceLoader;

public class TemplateInsertExtension implements Extension {

	@Override
	public @NotNull String getId() {
		return "builtin.template_inserter";
	}

	@Override
	public @NotNull String getName() {
		return "Post Header/Footer Templates";
	}

	@Override
	public @NotNull String getVersion() {
		return "1.0.0";
	}

	@Override
	public @NotNull String getAuthor() {
		return "aparmar2000";
	}

	@Override
	public @NotNull String getDescription() {
		return "Allows configuring custom header and signature templates that can be inserted into posts with a single click.";
	}

	@Override
	public void initialize(@NotNull ExtensionContext context) {
		String defaultHeader = InternalResourceLoader
				.tryGetInternalResourceAsStringSilent("templates/default_header.bbcode")
				.orElse("[HEADING=2]Scheduled Update[/HEADING=2]\n\n");
		String defaultFooter = InternalResourceLoader
				.tryGetInternalResourceAsStringSilent("templates/default_footer.bbcode")
				.orElse("\n\n[HR][/HR]\n[I]Automated message scheduled via XenForoPostScheduler[/I]");

		MultilineStringSettingDefinition headerDef = new MultilineStringSettingDefinition(
				"header_text", "Header Template", "Text prepended to posts", defaultHeader, "Post Templates"
				);
		MultilineStringSettingDefinition footerDef = new MultilineStringSettingDefinition(
				"footer_text", "Footer / Signature Template", "Text appended to posts", defaultFooter, "Post Templates"
				);

		context.registerSetting(headerDef);
		context.registerSetting(footerDef);

		context.registerToolbarItem(BbCodeToolbarItem.builder()
				.id("insert_header")
				.label("+ Header")
				.tooltip("Insert Configured Post Header Template")
				.action(ctx -> {
					String header = context.getSettingValue("header_text", String.class);
					if (header != null && !header.isEmpty()) {
						ctx.insertAtCaret(header);
					}
				})
				.build());

		context.registerToolbarItem(BbCodeToolbarItem.builder()
				.id("insert_footer")
				.label("+ Signature")
				.tooltip("Insert Configured Post Signature Template")
				.action(ctx -> {
					String footer = context.getSettingValue("footer_text", String.class);
					if (footer != null && !footer.isEmpty()) {
						ctx.insertAtCaret(footer);
					}
				})
				.build());
	}
}
