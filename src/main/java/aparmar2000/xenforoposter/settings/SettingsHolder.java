package aparmar2000.xenforoposter.settings;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import aparmar2000.xenforoposter.settings.defs.SettingDefinition;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SettingsHolder {
	@Getter
	private final Path settingsFile;
	private final Gson gson;

	private final Map<String, SettingDefinition<?>> definitions = new LinkedHashMap<>();
	private final Map<String, Object> values = new ConcurrentHashMap<>();
	private final Map<String, String> unparsedRawValues = new ConcurrentHashMap<>();
	private final Map<String, Supplier<?>> memoizedSuppliers = new ConcurrentHashMap<>();

	@Inject
	SettingsHolder(@Assisted @Nullable Path settingsFile, @Nullable Gson gson) {
		this.settingsFile = settingsFile;
		this.gson = gson;
	}

	public static interface Factory {
		public SettingsHolder create(Path settingsFile);
	}

	/**
	 * Registers a {@link SettingDefinition} and returns a memoized {@link Supplier}
	 * providing continuous, thread-safe access to the current typed value.
	 *
	 * @param definition the setting definition to register
	 * @param <T>        the setting value type
	 * @return a memoized supplier returning the latest valid setting value
	 */
	public synchronized <T> @NonNull Supplier<T> register(@NonNull SettingDefinition<T> definition) {
		Objects.requireNonNull(definition, "Setting definition cannot be null");
		String key = definition.getKey();
		definitions.put(key, definition);

		// If raw values were loaded before definition registration, hydrate now
		if (unparsedRawValues.containsKey(key)) {
			String raw = unparsedRawValues.remove(key);
			try {
				T deserialized = definition.deserialize(raw);
				if (deserialized != null && definition.validate(deserialized)) {
					values.put(key, deserialized);
					definition.setUiValue(deserialized);
				} else {
					values.put(key, definition.getDefaultValue());
					definition.resetToDefault();
				}
			} catch (Exception e) {
				log.warn("Failed to deserialize setting {} from raw value '{}': {}", key, raw, e.getMessage());
				values.put(key, definition.getDefaultValue());
				definition.resetToDefault();
			}
		} else if (!values.containsKey(key)) {
			values.put(key, definition.getDefaultValue());
			definition.setUiValue(definition.getDefaultValue());
		} else {
			// Value already present, synchronize UI component
			T currentVal = getSettingValue(definition);
			definition.setUiValue(currentVal);
		}

		Supplier<T> supplier = () -> getSettingValue(definition);
		memoizedSuppliers.put(key, supplier);
		return supplier;
	}

	public synchronized void registerAll(@NonNull SettingDefinition<?>... settingDefinitions) {
		for (SettingDefinition<?> def : settingDefinitions) {
			if (def != null) {
				register(def);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable Supplier<T> getSupplier(@NonNull String key, @NonNull Class<T> type) {
		Supplier<?> supplier = memoizedSuppliers.get(key);
		if (supplier != null) {
			return (Supplier<T>) supplier;
		}
		SettingDefinition<?> def = definitions.get(key);
		if (def != null && type.isAssignableFrom(def.getValueType())) {
			return register((SettingDefinition<T>) def);
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	public <T> @NonNull Supplier<T> getSupplier(@NonNull SettingDefinition<T> definition) {
		Supplier<?> existing = memoizedSuppliers.get(definition.getKey());
		if (existing != null) {
			return (Supplier<T>) existing;
		}
		return register(definition);
	}

	public synchronized @NonNull ImmutableList<SettingDefinition<?>> getRegisteredSettings() {
		return ImmutableList.copyOf(definitions.values());
	}

	public synchronized @Nullable SettingDefinition<?> getSettingDefinition(@NonNull String key) {
		return definitions.get(key);
	}

	public synchronized boolean hasSetting(@NonNull String key) {
		return definitions.containsKey(key);
	}

	/**
	 * Retrieves the typed value of a registered setting definition, falling back to default if unset or invalid.
	 *
	 * @param definition the setting definition
	 * @param <T>        the value type
	 * @return the current valid value
	 */
	public <T> @NonNull T getSettingValue(@NonNull SettingDefinition<T> definition) {
		Object val = values.get(definition.getKey());
		if (val != null && definition.getValueType().isInstance(val)) {
			T typed = definition.getValueType().cast(val);
			if (definition.validate(typed)) {
				return typed;
			}
		}
		return definition.getDefaultValue();
	}

	/**
	 * Retrieves the value of a setting by its key and expected type.
	 *
	 * @param key  the setting key
	 * @param type the target class type
	 * @param <T>  the value type
	 * @return the current setting value, or null if not found
	 */
	public <T> @Nullable T getSettingValue(@NonNull String key, @NonNull Class<T> type) {
		SettingDefinition<?> def;
		synchronized (this) {
			def = definitions.get(key);
		}

		if (def != null) {
			Object val = values.get(key);
			if (val != null && type.isInstance(val)) {
				return type.cast(val);
			}
			if (val instanceof String && !type.equals(String.class)) {
				try {
					Object parsed = def.deserialize((String) val);
					if (type.isInstance(parsed)) {
						return type.cast(parsed);
					}
				} catch (Exception ignored) {
				}
			}
			Object defaultVal = def.getDefaultValue();
			return type.isInstance(defaultVal) ? type.cast(defaultVal) : null;
		}

		Object rawVal = values.get(key);
		if (rawVal != null && type.isInstance(rawVal)) {
			return type.cast(rawVal);
		}

		String unparsed = unparsedRawValues.get(key);
		if (unparsed != null && type.equals(String.class)) {
			return type.cast(unparsed);
		}

		return null;
	}

	/**
	 * Sets the value for a setting definition after validating it.
	 *
	 * @param definition the setting definition
	 * @param value      the new value
	 * @param <T>        the value type
	 * @return true if valid and updated, false if validation failed
	 */
	public <T> boolean setSettingValue(@NonNull SettingDefinition<T> definition, @NonNull T value) {
		if (!definition.validate(value)) {
			log.warn("Validation failed for setting {} with value '{}'", definition.getKey(), value);
			return false;
		}

		values.put(definition.getKey(), value);
		definition.setUiValue(value);
		return true;
	}

	/**
	 * Sets the value for a setting by key after validating against its registered definition (if present).
	 *
	 * @param key   the setting key
	 * @param value the new value
	 * @param <T>   the value type
	 * @return true if updated, false if validation failed
	 */
	@SuppressWarnings("unchecked")
	public <T> boolean setSettingValue(@NonNull String key, @NonNull T value) {
		SettingDefinition<?> def;
		synchronized (this) {
			def = definitions.get(key);
		}

		if (def != null) {
			if (def.getValueType().isInstance(value)) {
				SettingDefinition<T> typedDef = (SettingDefinition<T>) def;
				return setSettingValue(typedDef, value);
			}
			if (value instanceof String) {
				try {
					T parsed = (T) def.deserialize((String) value);
					if (parsed != null) {
						return setSettingValue((SettingDefinition<T>) def, parsed);
					}
				} catch (Exception e) {
					log.warn("Failed to parse value '{}' for setting {}: {}", value, key, e.getMessage());
				}
			}
			return false;
		}

		values.put(key, value);
		return true;
	}

	public void resetSetting(@NonNull String key) {
		SettingDefinition<?> def;
		synchronized (this) {
			def = definitions.get(key);
		}

		if (def != null) {
			resetSettingInternal(def);
		} else {
			values.remove(key);
			unparsedRawValues.remove(key);
		}
	}
	public <T> void resetSetting(@NonNull SettingDefinition<T> definition) {
		resetSettingInternal(definition);
	}
	private <T> void resetSettingInternal(@NonNull SettingDefinition<T> def) {
		T defaultVal = def.getDefaultValue();
		values.put(def.getKey(), defaultVal);
		def.resetToDefault();
	}

	public synchronized void resetAllToDefaults() {
		unparsedRawValues.clear();
		for (SettingDefinition<?> def : definitions.values()) {
			resetSettingInternal(def);
		}
	}

	public @NonNull ImmutableMap<String, Object> getAllValues() {
		ImmutableMap.Builder<String, Object> snapshotBuilder = ImmutableMap.builder();
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			snapshotBuilder.put(entry.getKey(), entry.getValue());
		}
		for (SettingDefinition<?> def : getRegisteredSettings()) {
			snapshotBuilder.put(def.getKey(), getSettingValue(def));
		}
		return snapshotBuilder.buildKeepingLast();
	}

	public @NonNull ImmutableMap<String, String> getUnparsedRawValues() {
		return ImmutableMap.copyOf(unparsedRawValues);
	}


	public synchronized void load() {
		if (settingsFile == null || !Files.exists(settingsFile)) {
			return;
		}
		load(settingsFile);
	}
	public synchronized void load(@NonNull Path path) {
		Objects.requireNonNull(path, "Path cannot be null");
		if (!Files.exists(path)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			load(reader);
			log.info("Loaded settings from {}", path);
		} catch (Exception e) {
			log.error("Failed to load settings from {}", path, e);
		}
	}
	public synchronized void load(@NonNull Reader reader) {
		Objects.requireNonNull(reader, "Reader cannot be null");
		try {
			JsonElement rootElement = JsonParser.parseReader(reader);
			if (!rootElement.isJsonObject()) {
				return;
			}
			JsonObject jsonObject = rootElement.getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
				String key = entry.getKey();
				JsonElement element = entry.getValue();
				loadJsonElement(key, element);
			}
		} catch (Exception e) {
			log.error("Error reading settings JSON stream", e);
		}
	}

	public synchronized void save() {
		if (settingsFile == null) {
			return;
		}
		save(settingsFile);
	}
	public synchronized void save(@NonNull Path path) {
		Objects.requireNonNull(path, "Path cannot be null");
		try {
			if (path.getParent() != null) {
				Files.createDirectories(path.getParent());
			}
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				save(writer);
			}
			log.info("Saved settings to {}", path);
		} catch (Exception e) {
			log.error("Failed to save settings to {}", path, e);
		}
	}
	public synchronized void save(@NonNull Writer writer) {
		Objects.requireNonNull(writer, "Writer cannot be null");
		try {
			JsonObject rootObject = new JsonObject();

			// 1. Write registered setting values
			for (SettingDefinition<?> def : definitions.values()) {
				String key = def.getKey();
				Object value = getSettingValue(def);
				if (value != null) {
					if (value instanceof Boolean) {
						rootObject.addProperty(key, (Boolean) value);
					} else if (value instanceof Number) {
						rootObject.addProperty(key, (Number) value);
					} else if (value instanceof String) {
						rootObject.addProperty(key, (String) value);
					} else if (gson != null) {
						rootObject.add(key, gson.toJsonTree(value));
					} else {
						rootObject.addProperty(key, value.toString());
					}
				}
			}

			// 2. Write unparsed / passthrough values
			for (Map.Entry<String, String> entry : unparsedRawValues.entrySet()) {
				if (!definitions.containsKey(entry.getKey())) {
					rootObject.addProperty(entry.getKey(), entry.getValue());
				}
			}

			if (gson != null) {
				gson.toJson(rootObject, writer);
			} else {
				JsonWriter jsonWriter = new JsonWriter(writer);
				jsonWriter.setIndent("  ");
				com.google.gson.internal.Streams.write(rootObject, jsonWriter);
			}
			writer.flush();
		} catch (Exception e) {
			log.error("Error writing settings JSON stream", e);
		}
	}


	@SuppressWarnings("unchecked")

	void loadJsonElement(@NonNull String key, @NonNull JsonElement element) {
		SettingDefinition<?> def = definitions.get(key);
		if (def != null) {
			try {
				if (element.isJsonNull()) {
					resetSetting(key);
					return;
				}

				if (element.isJsonPrimitive()) {
					var prim = element.getAsJsonPrimitive();
					if (def.getValueType() == Boolean.class && prim.isBoolean()) {
						setSettingValue((SettingDefinition<Boolean>) def, prim.getAsBoolean());
						return;
					}
					if (def.getValueType() == Integer.class && prim.isNumber()) {
						setSettingValue((SettingDefinition<Integer>) def, prim.getAsInt());
						return;
					} else if (def.getValueType() == String.class && prim.isString()) {
						setSettingValue((SettingDefinition<String>) def, prim.getAsString());
						return;
					}
				}

				// Fallback to definition deserialize or Gson parsing
				String rawString = element.isJsonPrimitive() ? element.getAsString() : element.toString();
				Object deserialized = def.deserialize(rawString);
				if (deserialized != null && def.validate(cast(deserialized))) {
					values.put(key, deserialized);
					def.setUiValue(cast(deserialized));
				} else if (gson != null) {
					// Try Gson deserialization
					Object gsonParsed = gson.fromJson(element, def.getValueType());
					if (gsonParsed != null && def.validate(cast(gsonParsed))) {
						values.put(key, gsonParsed);
						def.setUiValue(cast(gsonParsed));
					}
				}
			} catch (Exception e) {
				log.warn("Failed to load setting {} with value {}: {}", key, element, e.getMessage());
			}
		} else {
			String raw = element.isJsonPrimitive() ? element.getAsString() : element.toString();
			unparsedRawValues.put(key, raw);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object obj) {
		return (T) obj;
	}
}
