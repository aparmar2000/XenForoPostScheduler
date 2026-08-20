package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import aparmar2000.xenforoposter.model.conditions.AntiNecropostCondition;
import aparmar2000.xenforoposter.model.conditions.DateRangeCondition;
import aparmar2000.xenforoposter.model.conditions.DayOfWeekCondition;
import aparmar2000.xenforoposter.model.conditions.PostCondition;
import aparmar2000.xenforoposter.model.conditions.PostGapCondition;
import aparmar2000.xenforoposter.model.conditions.ThreadStatusCondition;
import aparmar2000.xenforoposter.model.conditions.TimeRangeCondition;
import aparmar2000.xenforoposter.security.CredentialEncryptionService;
import aparmar2000.xenforoposter.security.SecureString;
import aparmar2000.xenforoposter.utils.GsonSupplier;

@ExtendWith(MockitoExtension.class)
class GsonSupplierTest {

    @Mock
    private CredentialEncryptionService encryptionService;

    private GsonSupplier gsonSupplier;

    @BeforeEach
    void setUp() {
        gsonSupplier = new GsonSupplier(encryptionService);
    }

    @Test
    @DisplayName("Gson should have pretty printing enabled and HTML escaping disabled")
    void testFormattingAndHtmlEscaping() {
        Gson gson = gsonSupplier.createGson();
        Map<String, String> data = Map.of("htmlSnippet", "<b>Hello & Welcome</b>");
        String json = gson.toJson(data);

        // Pretty printed output should contain newlines and indentation
        assertTrue(json.contains("\n"), "Output should contain newlines");
        assertTrue(json.contains("  \"htmlSnippet\""), "Output should be indented");

        // Unescaped HTML characters
        assertTrue(json.contains("<b>Hello & Welcome</b>"), "HTML tags and ampersands must not be escaped");
        assertFalse(json.contains("\\u003c"), "Must not contain escaped < symbol");
        assertFalse(json.contains("\\u003e"), "Must not contain escaped > symbol");
        assertFalse(json.contains("\\u0026"), "Must not contain escaped & symbol");
    }

    @Test
    @DisplayName("Instant type adapter should serialize and deserialize ISO-8601 strings")
    void testInstantAdapter() {
        Gson gson = gsonSupplier.createGson();
        Instant now = Instant.parse("2026-08-20T11:00:00Z");

        String json = gson.toJson(now);
        assertEquals("\"2026-08-20T11:00:00Z\"", json.trim());

        Instant deserialized = gson.fromJson(json, Instant.class);
        assertEquals(now, deserialized);

        assertNull(gson.fromJson("null", Instant.class));
    }

    @Test
    @DisplayName("LocalDate type adapter should serialize and deserialize ISO-8601 date strings")
    void testLocalDateAdapter() {
        Gson gson = gsonSupplier.createGson();
        LocalDate date = LocalDate.of(2026, 8, 20);

        String json = gson.toJson(date);
        assertEquals("\"2026-08-20\"", json.trim());

        LocalDate deserialized = gson.fromJson(json, LocalDate.class);
        assertEquals(date, deserialized);

        assertNull(gson.fromJson("null", LocalDate.class));
    }

    @Test
    @DisplayName("LocalTime type adapter should serialize and deserialize ISO-8601 time strings")
    void testLocalTimeAdapter() {
        Gson gson = gsonSupplier.createGson();
        LocalTime time = LocalTime.of(14, 30, 45);

        String json = gson.toJson(time);
        assertEquals("\"14:30:45\"", json.trim());

        LocalTime deserialized = gson.fromJson(json, LocalTime.class);
        assertEquals(time, deserialized);

        assertNull(gson.fromJson("null", LocalTime.class));
    }

    @Test
    @DisplayName("ZoneId type adapter should serialize and deserialize ZoneId identifiers")
    void testZoneIdAdapter() {
        Gson gson = gsonSupplier.createGson();
        ZoneId zone = ZoneId.of("America/New_York");

        String json = gson.toJson(zone);
        assertEquals("\"America/New_York\"", json.trim());

        ZoneId deserialized = gson.fromJson(json, ZoneId.class);
        assertEquals(zone, deserialized);

        assertNull(gson.fromJson("null", ZoneId.class));
    }

    @Test
    @DisplayName("PostCondition type adapter should handle polymorphic conditions accurately")
    void testPostConditionPolymorphicAdapter() {
        Gson gson = gsonSupplier.createGson();

        List<PostCondition> conditions = List.of(
                AntiNecropostCondition.builder().maxInactiveDays(14).allowAuthorExemption(true).build(),
                DateRangeCondition.builder().startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 12, 31)).build(),
                DayOfWeekCondition.builder().allowedDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)).build(),
                PostGapCondition.builder().minPostsSinceUser(3).build(),
                ThreadStatusCondition.builder().requireUnlocked(true).requireCanReply(true).build(),
                TimeRangeCondition.builder().startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0)).build()
        );

        for (PostCondition condition : conditions) {
            String json = gson.toJson(condition, PostCondition.class);
            assertTrue(json.contains("\"conditionClass\": \"" + condition.getClass().getName() + "\""),
                    "JSON must contain conditionClass metadata: " + json);

            PostCondition deserialized = gson.fromJson(json, PostCondition.class);
            assertNotNull(deserialized);
            assertEquals(condition.getClass(), deserialized.getClass());
            assertEquals(condition.getDescription(), deserialized.getDescription());
        }

        assertNull(gson.fromJson("null", PostCondition.class));
    }

    @Test
    @DisplayName("SecureString type adapter should encrypt and decrypt using injected CredentialEncryptionService")
    void testSecureStringAdapter() {
        when(encryptionService.encrypt("superSecretToken123")).thenReturn("ENC:mockedCiphertext123");
        when(encryptionService.decrypt("ENC:mockedCiphertext123")).thenReturn("superSecretToken123");

        Gson gson = gsonSupplier.createGson();
        SecureString secret = SecureString.of("superSecretToken123");

        String json = gson.toJson(secret);
        assertEquals("\"ENC:mockedCiphertext123\"", json.trim());
        verify(encryptionService).encrypt("superSecretToken123");

        SecureString deserialized = gson.fromJson(json, SecureString.class);
        assertNotNull(deserialized);
        assertEquals("superSecretToken123", deserialized.getClearText());
        verify(encryptionService).decrypt("ENC:mockedCiphertext123");

        assertNull(gson.fromJson("null", SecureString.class));
    }

    @Test
    @DisplayName("createBuilder should return a preconfigured builder that can be customized")
    void testCreateBuilderCustomization() {
        GsonBuilder builder = gsonSupplier.createBuilder();
        assertNotNull(builder);

        Gson customGson = builder.serializeNulls().create();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("nullField", null);

        String json = customGson.toJson(data);
        assertTrue(json.contains("\"nullField\": null"), "Customized builder should retain serializeNulls");
    }

    @Test
    @DisplayName("GsonSupplier should implement Provider and Supplier interfaces")
    void testProviderAndSupplierInterfaces() {
        Gson fromGet = gsonSupplier.get();
        assertNotNull(fromGet);
        Gson fromCreate = gsonSupplier.createGson();
        assertNotNull(fromCreate);
    }
}
