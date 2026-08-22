package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.ScheduledJob;
import aparmar2000.xenforoposter.scheduler.JobStorageService;
import aparmar2000.xenforoposter.security.CredentialEncryptionService;
import aparmar2000.xenforoposter.security.SecureString;
import aparmar2000.xenforoposter.utils.GsonSupplier;

class JobStorageCredentialSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("JobStorageService should save sensitive credentials in encrypted form and load them accurately")
    void testSensitiveCredentialsAreEncryptedOnDisk() throws IOException {
        CredentialEncryptionService crypto = new CredentialEncryptionService(tempDir.resolve(".credentials.key"));
        Gson gson = new GsonSupplier(crypto).createGson();
        JobStorageService storageService = new JobStorageService(tempDir, crypto, gson);

        String secretPassword = "plainTextPasswordNeverOnDisk!#99";
        String sessionCookieVal = "sensitive_session_token_xyz_8877";
        String userCookieVal = "105,secret_user_hash_1122";

        Map<String, SecureString> cookies = new HashMap<>();
        cookies.put("xf_session", SecureString.of(sessionCookieVal));
        cookies.put("xf_user", SecureString.of(userCookieVal));

        ForumProfile profile = ForumProfile.builder()
                .id("profile-sec-test")
                .name("Encrypted Profile Test")
                .baseUrl("https://forum.example.com")
                .username("testAdmin")
                .password(SecureString.of(secretPassword))
                .sessionCookies(cookies)
                .customUserAgent("CustomAgent/1.0")
                .build();

        // ---
        
        storageService.saveProfiles(List.of(profile));

        Path profilesFile = tempDir.resolve("forum_profiles.json");
        assertTrue(Files.exists(profilesFile), "forum_profiles.json should be created");

        String rawJson = Files.readString(profilesFile, StandardCharsets.UTF_8);

        // ---

        assertFalse(rawJson.contains(secretPassword), "JSON file on disk must not contain raw plaintext password");
        assertFalse(rawJson.contains(sessionCookieVal), "JSON file on disk must not contain raw session cookie value");
        assertFalse(rawJson.contains(userCookieVal), "JSON file on disk must not contain raw user cookie value");
        assertTrue(rawJson.contains("ENC:"), "JSON file on disk should store credentials as ENC: ciphertexts");

        // ---

        CredentialEncryptionService secondCrypto = new CredentialEncryptionService(tempDir.resolve(".credentials.key"));
        Gson secondGson = new GsonSupplier(secondCrypto).createGson();
        JobStorageService secondStorageService = new JobStorageService(tempDir, secondCrypto, secondGson);
        List<ForumProfile> loadedProfiles = secondStorageService.loadProfiles();

        assertEquals(1, loadedProfiles.size());
        ForumProfile loaded = loadedProfiles.get(0);

        assertEquals("profile-sec-test", loaded.getId());
        assertEquals("Encrypted Profile Test", loaded.getName());
        assertEquals("https://forum.example.com", loaded.getBaseUrl());
        assertEquals("testAdmin", loaded.getUsername());
        assertEquals("CustomAgent/1.0", loaded.getCustomUserAgent());

        assertNotNull(loaded.getPassword());
        assertEquals(secretPassword, loaded.getPassword().getClearText());

        assertNotNull(loaded.getSessionCookies());
        assertEquals(2, loaded.getSessionCookies().size());
        assertNotNull(loaded.getSessionCookies().get("xf_session"));
        assertEquals(sessionCookieVal, loaded.getSessionCookies().get("xf_session").getClearText());
        assertNotNull(loaded.getSessionCookies().get("xf_user"));
        assertEquals(userCookieVal, loaded.getSessionCookies().get("xf_user").getClearText());
    }

    @Test
    @DisplayName("JobStorageService unit test with mocked Gson should invoke serialization and deserialization")
    void testJobStorageServiceWithMockedGson() {
        Gson mockGson = mock(Gson.class);
        CredentialEncryptionService mockCrypto = mock(CredentialEncryptionService.class);

        JobStorageService storage = new JobStorageService(tempDir, mockCrypto, mockGson);

        List<ScheduledJob> jobs = List.of(
                ScheduledJob.builder()
                        .id("mock-job-1")
                        .name("Mock Job")
                        .forumProfileId("prof-1")
                        .threadUrl("https://forum.example.com/threads/1")
                        .bbCodeContent("content")
                        .build()
        );

        storage.saveJobs(jobs);
        verify(mockGson).toJson(eq(jobs), any(Writer.class));

        List<ForumProfile> profiles = List.of(
                ForumProfile.builder()
                        .id("mock-prof-1")
                        .name("Mock Profile")
                        .baseUrl("https://forum.example.com")
                        .username("user")
                        .build()
        );

        storage.saveProfiles(profiles);
        verify(mockGson).toJson(eq(profiles), any(Writer.class));

        when(mockGson.fromJson(any(Reader.class), any(Type.class))).thenReturn(jobs);
        List<ScheduledJob> loadedJobs = storage.loadJobs();
        assertFalse(loadedJobs.isEmpty());
        verify(mockGson).fromJson(any(Reader.class), any(Type.class));
    }

    @Test
    @DisplayName("ForumProfile toString should exclude sensitive password and sessionCookies")
    void testForumProfileToStringExclusion() {
        ForumProfile profile = ForumProfile.builder()
                .name("Secure Profile")
                .baseUrl("https://forum.example.com")
                .username("testUser")
                .password(SecureString.of("sensitivePassword123"))
                .sessionCookies(Map.of("xf_session", SecureString.of("sessionToken999")))
                .build();

        String profileString = profile.toString();
        assertFalse(profileString.contains("sensitivePassword123"), "ForumProfile.toString() must not contain password");
        assertFalse(profileString.contains("sessionToken999"), "ForumProfile.toString() must not contain session tokens");
        assertFalse(profileString.contains("password="), "ForumProfile.toString() should exclude password field");
        assertFalse(profileString.contains("sessionCookies="), "ForumProfile.toString() should exclude sessionCookies field");
    }
}
