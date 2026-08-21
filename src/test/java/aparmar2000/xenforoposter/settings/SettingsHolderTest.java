package aparmar2000.xenforoposter.settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import aparmar2000.xenforoposter.settings.defs.BooleanSettingDefinition;
import aparmar2000.xenforoposter.settings.defs.ChoiceSettingDefinition;
import aparmar2000.xenforoposter.settings.defs.IntegerSettingDefinition;
import aparmar2000.xenforoposter.settings.defs.StringSettingDefinition;

class SettingsHolderTest {

    @TempDir
    Path tempDir;

    private SettingsHolder holder;
    private StringSettingDefinition stringDef;
    private IntegerSettingDefinition intDef;
    private BooleanSettingDefinition boolDef;
    private ChoiceSettingDefinition choiceDef;

    @BeforeEach
    void setUp() {
        holder = new SettingsHolder(null, null);
        stringDef = new StringSettingDefinition("app.name", "App Name", "The name of the app", "DefaultApp");
        intDef = new IntegerSettingDefinition("app.port", "Port", "Server Port", 8080, 1024, 65535);
        boolDef = new BooleanSettingDefinition("app.debug", "Debug Mode", "Enable debug", false);
        choiceDef = new ChoiceSettingDefinition("app.env", "Environment", "Target env", "dev", List.of("dev", "stage", "prod"));
    }

    @Test
    @DisplayName("Registration should return memoized suppliers that dynamically track updates and resets")
    void testRegistrationAndMemoizedSuppliers() {
        Supplier<String> nameSupplier = holder.register(stringDef);
        Supplier<Integer> portSupplier = holder.register(intDef);
        Supplier<Boolean> debugSupplier = holder.register(boolDef);

        // Initial default values
        assertEquals("DefaultApp", nameSupplier.get());
        assertEquals(8080, portSupplier.get());
        assertEquals(false, debugSupplier.get());

        // Update via holder key-based setter
        assertTrue(holder.setSettingValue("app.name", "CustomApp"));
        assertEquals("CustomApp", nameSupplier.get());
        assertEquals("CustomApp", stringDef.getUiValue());

        // Update via holder definition setter
        assertTrue(holder.setSettingValue(intDef, 9000));
        assertEquals(9000, portSupplier.get());
        assertEquals(9000, intDef.getUiValue());

        // Update boolean
        assertTrue(holder.setSettingValue(boolDef, true));
        assertEquals(true, debugSupplier.get());

        // Reset single setting
        holder.resetSetting(stringDef);
        assertEquals("DefaultApp", nameSupplier.get());
        assertEquals("DefaultApp", stringDef.getUiValue());
        assertEquals(9000, portSupplier.get()); // int remains changed

        // Reset all settings
        holder.resetAllToDefaults();
        assertEquals("DefaultApp", nameSupplier.get());
        assertEquals(8080, portSupplier.get());
        assertEquals(false, debugSupplier.get());
    }

    @Test
    @DisplayName("Generic getters and setters should validate input and handle type casting safely")
    void testGenericGettersAndSettersWithValidation() {
        holder.register(stringDef);
        holder.register(intDef);
        holder.register(choiceDef);

        // Key-based typed getters
        assertEquals("DefaultApp", holder.getSettingValue("app.name", String.class));
        assertEquals(8080, holder.getSettingValue("app.port", Integer.class));
        assertEquals("dev", holder.getSettingValue("app.env", String.class));

        // Definition-based getters
        assertEquals("DefaultApp", holder.getSettingValue(stringDef));
        assertEquals(8080, holder.getSettingValue(intDef));

        // Invalid integer (out of range: min 1024, max 65535)
        assertFalse(holder.setSettingValue("app.port", 100));
        assertEquals(8080, holder.getSettingValue("app.port", Integer.class)); // unchanged

        // Invalid choice (not in options list)
        assertFalse(holder.setSettingValue("app.env", "invalid_env"));
        assertEquals("dev", holder.getSettingValue("app.env", String.class)); // unchanged

        // Valid updates
        assertTrue(holder.setSettingValue("app.port", 3000));
        assertEquals(3000, holder.getSettingValue("app.port", Integer.class));

        assertTrue(holder.setSettingValue("app.env", "prod"));
        assertEquals("prod", holder.getSettingValue("app.env", String.class));

        // Unknown key query returns null
        assertNull(holder.getSettingValue("unknown.key", String.class));
    }

    @Test
    @DisplayName("Reset operations should properly restore default values and update UI components")
    void testResetOperations() {
        holder.register(stringDef);
        holder.register(intDef);
        holder.register(boolDef);

        holder.setSettingValue("app.name", "UpdatedName");
        holder.setSettingValue("app.port", 5000);
        holder.setSettingValue("app.debug", true);

        assertEquals("UpdatedName", stringDef.getUiValue());
        assertEquals(5000, intDef.getUiValue());
        assertTrue(boolDef.getUiValue());

        // Reset by key
        holder.resetSetting("app.name");
        assertEquals("DefaultApp", holder.getSettingValue(stringDef));
        assertEquals("DefaultApp", stringDef.getUiValue());

        // Reset all
        holder.resetAllToDefaults();
        assertEquals(8080, holder.getSettingValue(intDef));
        assertEquals(8080, intDef.getUiValue());
        assertFalse(holder.getSettingValue(boolDef));
        assertFalse(boolDef.getUiValue());
    }

    @Test
    @DisplayName("Load and save should serialize strictly setting values and deserialize properly")
    void testLoadAndSaveSerialization() throws Exception {
        Path savePath = tempDir.resolve("settings.json");
        SettingsHolder saverHolder = new SettingsHolder(savePath, null);
        saverHolder.register(stringDef);
        saverHolder.register(intDef);
        saverHolder.register(boolDef);
        saverHolder.register(choiceDef);

        saverHolder.setSettingValue("app.name", "PersistedApp");
        saverHolder.setSettingValue("app.port", 8443);
        saverHolder.setSettingValue("app.debug", true);
        saverHolder.setSettingValue("app.env", "prod");

        saverHolder.save();
        assertTrue(Files.exists(savePath));

        // Read saved raw JSON to inspect structure
        String jsonContent = Files.readString(savePath);
        JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();

        // Verify that only keys and actual values exist (no definition metadata)
        assertEquals("PersistedApp", jsonObject.get("app.name").getAsString());
        assertEquals(8443, jsonObject.get("app.port").getAsInt());
        assertTrue(jsonObject.get("app.debug").getAsBoolean());
        assertEquals("prod", jsonObject.get("app.env").getAsString());

        assertFalse(jsonObject.has("label"));
        assertFalse(jsonObject.has("description"));
        assertFalse(jsonObject.has("uiComponent"));
        assertFalse(jsonObject.has("changeListeners"));

        // Reload into a fresh SettingsHolder instance
        SettingsHolder loaderHolder = new SettingsHolder(savePath, null);
        StringSettingDefinition newStringDef = new StringSettingDefinition("app.name", "App Name", "", "DefaultApp");
        IntegerSettingDefinition newIntDef = new IntegerSettingDefinition("app.port", "Port", "", 8080, 1024, 65535);
        BooleanSettingDefinition newBoolDef = new BooleanSettingDefinition("app.debug", "Debug Mode", "", false);
        ChoiceSettingDefinition newChoiceDef = new ChoiceSettingDefinition("app.env", "Environment", "", "dev", List.of("dev", "stage", "prod"));

        loaderHolder.register(newStringDef);
        loaderHolder.register(newIntDef);
        loaderHolder.register(newBoolDef);
        loaderHolder.register(newChoiceDef);

        loaderHolder.load();

        assertEquals("PersistedApp", loaderHolder.getSettingValue(newStringDef));
        assertEquals(8443, loaderHolder.getSettingValue(newIntDef));
        assertTrue(loaderHolder.getSettingValue(newBoolDef));
        assertEquals("prod", loaderHolder.getSettingValue(newChoiceDef));
    }

    @Test
    @DisplayName("SettingsHolder should format clean JSON and read/write from streams")
    void testStreamingLoadAndSave() {
        holder.register(stringDef);
        holder.register(intDef);
        holder.setSettingValue("app.name", "StreamingApp");
        holder.setSettingValue("app.port", 8443);

        StringWriter writer = new StringWriter();
        holder.save(writer);
        String json = writer.toString();

        assertTrue(json.contains("\"app.name\": \"StreamingApp\""));
        assertTrue(json.contains("\"app.port\": 8443"));

        // Load into new holder via Reader
        SettingsHolder reloaded = new SettingsHolder(null, null);
        StringSettingDefinition rStringDef = new StringSettingDefinition("app.name", "App Name", "", "DefaultApp");
        IntegerSettingDefinition rIntDef = new IntegerSettingDefinition("app.port", "Port", "", 8080, 1024, 65535);

        reloaded.register(rStringDef);
        reloaded.register(rIntDef);

        reloaded.load(new StringReader(json));
        assertEquals("StreamingApp", reloaded.getSettingValue(rStringDef));
        assertEquals(8443, reloaded.getSettingValue(rIntDef));
    }

    @Test
    @DisplayName("Settings loaded before definition registration should hydrate when registered later")
    void testEarlyLoadBeforeDefinitionRegistration() {
        String json = "{\"app.name\": \"PreloadedApp\", \"app.port\": 7070, \"custom.unregistered\": \"value123\"}";

        holder.load(new StringReader(json));

        // Before definition is registered, raw value is in unparsed map
        assertEquals("value123", holder.getSettingValue("custom.unregistered", String.class));

        // Now register definition
        Supplier<String> nameSupplier = holder.register(stringDef);
        Supplier<Integer> portSupplier = holder.register(intDef);

        // Should be hydrated with preloaded values
        assertEquals("PreloadedApp", nameSupplier.get());
        assertEquals(7070, portSupplier.get());
        assertEquals("PreloadedApp", stringDef.getUiValue());
        assertEquals(7070, intDef.getUiValue());
    }

    @Test
    @DisplayName("getSupplier should return suppliers by key and definition")
    void testGetSupplierMethods() {
        Supplier<String> regSupplier = holder.register(stringDef);
        Supplier<String> fetchedByName = holder.getSupplier("app.name", String.class);
        Supplier<String> fetchedByDef = holder.getSupplier(stringDef);

        assertNotNull(fetchedByName);
        assertNotNull(fetchedByDef);
        assertEquals("DefaultApp", fetchedByName.get());
        assertEquals("DefaultApp", fetchedByDef.get());

        holder.setSettingValue(stringDef, "NewName");
        assertEquals("NewName", regSupplier.get());
        assertEquals("NewName", fetchedByName.get());
        assertEquals("NewName", fetchedByDef.get());
    }

    @Test
    @DisplayName("SettingDefinition should trigger mocked change listener upon UI value modification")
    void testSettingDefinitionChangeListenerNotification() {
        Runnable mockListener = mock(Runnable.class);
        stringDef.addChangeListener(mockListener);

        stringDef.setUiValue("ModifiedValue");
        verify(mockListener, atLeastOnce()).run();

        stringDef.removeChangeListener(mockListener);
        stringDef.setUiValue("AnotherValue");
    }
}
