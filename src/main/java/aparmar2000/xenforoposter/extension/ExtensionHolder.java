package aparmar2000.xenforoposter.extension;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ExtensionHolder {
    @NonNull private final Extension extension;
    @NonNull private final Path dataDirectory;
    @NonNull private final ExtensionMetadata metadata;
    @NonNull private final InternalExtensionContext context;
    
    private boolean enabled = false;
    private URLClassLoader classLoader;
    private boolean initialized = false;

    public void setClassLoader(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            
            if (!initialized) {
            	return;
            }
            try {
                if (enabled) {
                    extension.onEnable();
                } else {
                    extension.onDisable();
                }
            } catch (Exception e) {
                ExtensionManager.log.error("Error toggling extension {}", extension.getId(), e);
            }
        }
    }

    public void initialize() {
    	if (initialized) {
    		return;
    	}
    	
        try {
            Files.createDirectories(dataDirectory);
            extension.initialize(context);
            context.loadSettings();
            if (enabled) {
                extension.onEnable();
            }
            initialized = true;
        } catch (Exception e) {
            ExtensionManager.log.error("Failed to initialize extension {}", extension.getId(), e);
        }
    }
}