package aparmar2000.xenforoposter.model;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ForumProfile {
    @NotNull @Builder.Default String id = UUID.randomUUID().toString();
    @NotNull String name;
    @NotNull String baseUrl;
    @Nullable String username;
    @Nullable String password;
    @Nullable Map<String, String> sessionCookies;
    @Nullable String customUserAgent;

    public String getDomain() {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : baseUrl;
        } catch (Exception e) {
            return baseUrl;
        }
    }
}
