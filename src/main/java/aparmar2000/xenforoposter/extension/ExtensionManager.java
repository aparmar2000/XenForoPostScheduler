package aparmar2000.xenforoposter.extension;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import aparmar2000.xenforoposter.extension.condition.ConditionProvider;
import aparmar2000.xenforoposter.extension.hook.AbstractHookEvent;
import aparmar2000.xenforoposter.extension.hook.HookExecutionException;
import aparmar2000.xenforoposter.extension.hook.HookPhase;
import aparmar2000.xenforoposter.extension.hook.RegisteredHook;
import aparmar2000.xenforoposter.extension.toolbar.BbCodeToolbarItem;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j(access = AccessLevel.PACKAGE)
public class ExtensionManager {
	private final Path extensionsDir;
	private final Path configFile;
	private final Gson gson;
	private final InternalExtensionContext.Factory contextFactory;

	private final Map<String, ExtensionHolder> extensions = new LinkedHashMap<>();
	private final List<ExtensionChangeListener> listeners = new ArrayList<>();

	public interface ExtensionChangeListener {
		void onExtensionsUpdated();
	}

	@Inject
	public ExtensionManager(@Named("baseDataDir") @NonNull Path baseDataDir,
			@NonNull Gson gson,
			@NonNull InternalExtensionContext.Factory contextFactory) {
		this.extensionsDir = baseDataDir.resolve("extensions");
		this.configFile = baseDataDir.resolve("extensions_config.json");
		this.gson = gson;
		this.contextFactory = contextFactory;
		try {
			Files.createDirectories(extensionsDir);
		} catch (Exception e) {
			log.error("Failed to create extensions directory: {}", extensionsDir, e);
		}
	}

	public void registerInternalExtension(@NonNull Extension extension) {
		if (extensions.containsKey(extension.getId())) {
			log.warn("Extension {} is already registered, skipping", extension.getId());
			return;
		}

		Path extDataDir = extensionsDir.resolve(extension.getId());
		ExtensionHolder holder = new ExtensionHolder(extension, extDataDir,
				ExtensionMetadata.builtIn(), contextFactory.create(extDataDir, extension.getId()));
		holder.setEnabled(true);
		extensions.put(extension.getId(), holder);
	}

	public void loadAllExtensions() {
		scanAndLoadExternalJars();
		loadManagerConfiguration();

		for (ExtensionHolder holder : extensions.values()) {
			holder.initialize();
		}

		notifyListeners();
	}

	public void scanAndLoadExternalJars() {
		if (!Files.exists(extensionsDir)) {
			return;
		}

		File[] jarFiles = extensionsDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
		if (jarFiles == null) {
			return;
		}

		for (File jarFile : jarFiles) {
			loadExtensionJar(jarFile);
		}
	}

	public boolean loadExtensionJar(@NonNull File jarFile) {
		try {
			URL jarUrl = jarFile.toURI().toURL();
			URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());

			// Scan JAR entries to find classes implementing Extension
			try (JarFile jar = new JarFile(jarFile)) {
				for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
					JarEntry entry = entries.nextElement();

					if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
						String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
						try {
							Class<?> clazz = Class.forName(className, false, classLoader);

							if (Extension.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
								Extension extInstance = (Extension) clazz.getDeclaredConstructor().newInstance();

								if (!extensions.containsKey(extInstance.getId())) {
									Path extDataDir = extensionsDir.resolve(extInstance.getId());
									ExtensionHolder holder = new ExtensionHolder(extInstance,
											extDataDir,
											ExtensionMetadata.fromJar(jarFile),
											contextFactory.create(extDataDir, extInstance.getId()));
									holder.setClassLoader(classLoader);
									extensions.put(extInstance.getId(), holder);
									holder.initialize();
									log.info("Loaded external extension: {} ({}) from {}",
											extInstance.getName(), extInstance.getId(), jarFile.getName());
								}
							}
						} catch (Throwable t) {
							// Ignored: not an instantiable Extension class
						}
					}
				}
			}
			saveManagerConfiguration();
			notifyListeners();
			return true;
		} catch (Exception e) {
			log.error("Failed to load extension jar from {}", jarFile.getAbsolutePath(), e);
			return false;
		}
	}

	public void setExtensionEnabled(@NonNull String extensionId, boolean enabled) {
		ExtensionHolder holder = extensions.get(extensionId);
		if (holder != null && holder.isEnabled() != enabled) {
			holder.setEnabled(enabled);
			saveManagerConfiguration();
			notifyListeners();
		}
	}

	public ImmutableList<ExtensionHolder> getAllExtensions() {
		return ImmutableList.sortedCopyOf(Comparator.comparing(h->h.getExtension().getId()), extensions.values());
	}

	@Nullable
	public ExtensionHolder getExtensionHolder(@NotNull String id) {
		return extensions.get(id);
	}

	public List<BbCodeToolbarItem> getActiveToolbarItems() {
		List<BbCodeToolbarItem> items = new ArrayList<>();
		for (ExtensionHolder holder : extensions.values()) {
			if (holder.isEnabled()) {
				items.addAll(holder.getContext().getRegisteredToolbarItems());
			}
		}
		return items;
	}

	public List<ConditionProvider> getActiveConditionProviders() {
		List<ConditionProvider> providers = new ArrayList<>();
		for (ExtensionHolder holder : extensions.values()) {
			if (holder.isEnabled()) {
				providers.addAll(holder.getContext().getRegisteredConditions());
			}
		}
		return providers;
	}

	public List<RegisteredHook<?,?>> getActiveHooks() {
		List<RegisteredHook<?,?>> hooks = new ArrayList<>();
		for (ExtensionHolder holder : extensions.values()) {
			if (holder.isEnabled()) {
				hooks.addAll(holder.getContext().getRegisteredHooks());
			}
		}
		
		Collections.sort(hooks);
		return hooks;
	}

	@SuppressWarnings("unchecked")
	public <H extends AbstractHookEvent<S>, S> List<RegisteredHook<H,S>> getActiveHooksForEvent(@NonNull H event) {
		List<RegisteredHook<H,S>> matching = new ArrayList<>();
		for (RegisteredHook<?,?> hook : getActiveHooks()) {
			if (hook.appliesToEvent(event)) {
				matching.add((RegisteredHook<H, S>) hook);
			}
		}
		return matching;
	}

	@NonNull
	public <H extends AbstractHookEvent<S>, S> H fireHookEvent(@NonNull H event) {
		List<RegisteredHook<H,S>> hooks = getActiveHooksForEvent(event);
		for (RegisteredHook<H,S> hook : hooks) {
			S snapshot = event.createSnapshot();
			
			try {
				hook.execute(event);
			} catch (Throwable t) {
				log.error("Error executing hook {} in extension {}", hook.getMethodName(), hook.getExtensionId(), t);
				if (event.getPhase() == HookPhase.POST) {
					throw new HookExecutionException(
							hook.getExtensionId(),
							hook.getMethodName(),
							"Hook " + hook.getMethodName() + " in extension " + hook.getExtensionId()
									+ " threw exception during POST phase: " + t.getMessage(),
							t);
				} else {
					// In PREVIEW, revert event to clean snapshot
					event.restoreSnapshot(snapshot);
				}
			}
		}
		return event;
	}

	public void addChangeListener(@NonNull ExtensionChangeListener listener) {
		listeners.add(listener);
	}

	private void notifyListeners() {
		for (ExtensionChangeListener listener : listeners) {
			try {
				listener.onExtensionsUpdated();
			} catch (Exception e) {
				log.error("Error invoking extension change listener", e);
			}
		}
	}

	private void loadManagerConfiguration() {
		try {
			Map<String, Boolean> enabledStates = new HashMap<>();
			if (configFile.toFile().canRead()) {
				try (FileReader reader = new FileReader(configFile.toFile())) {
					val loadedEnabledStates = gson.fromJson(reader, new TypeToken<Map<String, Boolean>>() {});
					if (loadedEnabledStates != null) {
						enabledStates.putAll(loadedEnabledStates);
					}
				}
			}

			for (Entry<String, ExtensionHolder> entry : extensions.entrySet()) {
				boolean defaultEnabled = entry.getValue().getMetadata().isBuiltIn();
				entry.getValue().setEnabled(enabledStates.getOrDefault(entry.getKey(), defaultEnabled));
			}
		} catch (Exception e) {
			log.error("Failed to load extension manager configuration", e);
		}
	}

	public void saveManagerConfiguration() {
		try {
			Map<String, Boolean> enabledStates = new HashMap<>();
			for (ExtensionHolder holder : extensions.values()) {
				enabledStates.put(holder.getExtension().getId(), holder.isEnabled());
			}

			try (FileWriter writer = new FileWriter(configFile.toFile())) {
				gson.toJson(enabledStates, writer);
			}
		} catch (Exception e) {
			log.error("Failed to save extension manager configuration", e);
		}
	}
}
