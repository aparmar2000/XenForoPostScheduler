package aparmar2000.xenforoposter;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.inject.Guice;
import com.google.inject.Injector;

import aparmar2000.xenforoposter.di.AppModule;
import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.builtin.TemplateInsertExtension;
import aparmar2000.xenforoposter.ui.MainFrame;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XenForoPoster {
	public static void main(String[] args) {
		FlatDarkLaf.setup();

		Injector injector = Guice.createInjector(new AppModule());

		ExtensionManager extensionManager = injector.getInstance(ExtensionManager.class);
		extensionManager.registerInternalExtension(new TemplateInsertExtension());
		extensionManager.loadAllExtensions();

		SwingUtilities.invokeLater(() -> {
			MainFrame mainFrame = injector.getInstance(MainFrame.class);
			mainFrame.setVisible(true);
			log.info("XenForo Post Scheduler UI initialized successfully with Guice DI");
		});
	}
}
