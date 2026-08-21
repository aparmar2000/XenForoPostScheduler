package aparmar2000.xenforoposter.di;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;

import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.scheduler.JobStorageService;
import aparmar2000.xenforoposter.scheduler.SchedulerEngine;
import aparmar2000.xenforoposter.security.CredentialEncryptionService;
import aparmar2000.xenforoposter.security.SafetyRateLimiter;
import aparmar2000.xenforoposter.security.SecureString;
import aparmar2000.xenforoposter.settings.GeneralSettings;
import aparmar2000.xenforoposter.ui.MainFrame;
import aparmar2000.xenforoposter.web.XenForoWebClient;

class GuiceInjectionTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Guice AppModule should successfully bootstrap and resolve all application components")
    void testGuiceAppModuleBootstrapping() {
        Injector injector = Guice.createInjector(new AppModule(tempDir));
        assertNotNull(injector);

        // Core singletons & services
        CredentialEncryptionService crypto = injector.getInstance(CredentialEncryptionService.class);
        assertNotNull(crypto);

        aparmar2000.xenforoposter.utils.GsonSupplier gsonSupplier = injector.getInstance(aparmar2000.xenforoposter.utils.GsonSupplier.class);
        assertNotNull(gsonSupplier);

        Gson gson = injector.getInstance(Gson.class);
        assertNotNull(gson);

        GsonBuilder builder = injector.getInstance(GsonBuilder.class);
        assertNotNull(builder);

        SafetyRateLimiter rateLimiter = injector.getInstance(SafetyRateLimiter.class);
        assertNotNull(rateLimiter);

        XenForoWebClient webClient = injector.getInstance(XenForoWebClient.class);
        assertNotNull(webClient);

        GeneralSettings settings = injector.getInstance(GeneralSettings.class);
        assertNotNull(settings);

        JobStorageService storageService = injector.getInstance(JobStorageService.class);
        assertNotNull(storageService);

        ExtensionManager extensionManager = injector.getInstance(ExtensionManager.class);
        assertNotNull(extensionManager);

        SchedulerEngine schedulerEngine = injector.getInstance(SchedulerEngine.class);
        assertNotNull(schedulerEngine);

        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            MainFrame mainFrame = injector.getInstance(MainFrame.class);
            assertNotNull(mainFrame);
        }
    }

    @Test
    @DisplayName("CredentialEncryptionService and Gson should be shared singletons across injected components")
    void testSharedSingletons() {
        Injector injector = Guice.createInjector(new AppModule(tempDir));

        CredentialEncryptionService crypto1 = injector.getInstance(CredentialEncryptionService.class);
        CredentialEncryptionService crypto2 = injector.getInstance(CredentialEncryptionService.class);
        assertSame(crypto1, crypto2, "CredentialEncryptionService must be a shared singleton");

        Gson gson1 = injector.getInstance(Gson.class);
        Gson gson2 = injector.getInstance(Gson.class);
        assertSame(gson1, gson2, "Gson must be a shared singleton");

        JobStorageService storageService = injector.getInstance(JobStorageService.class);
        assertSame(crypto1, storageService.getEncryptionService(),
                "JobStorageService must use the shared CredentialEncryptionService singleton");
    }

    @Test
    @DisplayName("Injected Gson instance should encrypt and decrypt using the shared CredentialEncryptionService")
    void testInjectedGsonUsesSharedEncryptionService() {
        Injector injector = Guice.createInjector(new AppModule(tempDir));

        CredentialEncryptionService crypto = injector.getInstance(CredentialEncryptionService.class);
        Gson gson = injector.getInstance(Gson.class);

        String secretText = "SuperSecretGuiceToken99#";
        SecureString secure = SecureString.of(secretText);

        // Serialize with injected Gson
        String json = gson.toJson(secure);
        assertTrue(json.contains("ENC:"), "JSON must be encrypted with ENC: prefix");
        assertFalse(json.contains(secretText), "JSON must not contain cleartext secret");

        // Direct decrypt with shared CredentialEncryptionService
        String rawCiphertext = json.replace("\"", "").trim();
        String decryptedDirectly = crypto.decrypt(rawCiphertext);
        assertEquals(secretText, decryptedDirectly,
                "Shared encryption service must directly decrypt ciphertext produced by injected Gson");

        // Encrypt directly with shared CredentialEncryptionService and deserialize via injected Gson
        String customEncrypted = crypto.encrypt("InjectedGsonCompatibilityCheck");
        SecureString deserialized = gson.fromJson("\"" + customEncrypted + "\"", SecureString.class);
        assertNotNull(deserialized);
        assertEquals("InjectedGsonCompatibilityCheck", deserialized.getClearText());
    }

    @Test
    @DisplayName("JobStorageService injected via Guice should persist and load credentials accurately")
    void testJobStorageServiceWithGuice() throws IOException {
        Injector injector = Guice.createInjector(new AppModule(tempDir));
        JobStorageService storageService = injector.getInstance(JobStorageService.class);

        ForumProfile profile = ForumProfile.builder()
                .id("guice-profile-1")
                .name("Guice Injected Profile")
                .baseUrl("https://forum.example.com")
                .username("guiceUser")
                .password(SecureString.of("guiceSecretPass456!"))
                .sessionCookies(Map.of("xf_user", SecureString.of("12345,cookieHashVal")))
                .build();

        storageService.saveProfiles(List.of(profile));

        Path profilesFile = tempDir.resolve("forum_profiles.json");
        assertTrue(Files.exists(profilesFile));
        String rawContent = Files.readString(profilesFile, StandardCharsets.UTF_8);
        assertFalse(rawContent.contains("guiceSecretPass456!"));
        assertTrue(rawContent.contains("ENC:"));

        List<ForumProfile> loaded = storageService.loadProfiles();
        assertEquals(1, loaded.size());
        assertEquals("guiceSecretPass456!", loaded.get(0).getPassword().getClearText());
        assertEquals("12345,cookieHashVal", loaded.get(0).getSessionCookies().get("xf_user").getClearText());
    }
}
