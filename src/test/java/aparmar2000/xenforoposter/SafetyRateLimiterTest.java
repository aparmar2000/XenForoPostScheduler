package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.security.SafetyRateLimiter;

class SafetyRateLimiterTest {
    private SafetyRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new SafetyRateLimiter();
    }

    @Test
    @DisplayName("Should allow first post and then enforce global cooldown")
    void testGlobalPostCooldown() {
        String thread = "https://forum.example.com/threads/123";

        SafetyRateLimiter.RateLimitCheckResult initialCheck = rateLimiter.checkCanPost(thread);
        assertTrue(initialCheck.isAllowed());

        rateLimiter.recordPostDispatched(thread);

        SafetyRateLimiter.RateLimitCheckResult immediateCheck = rateLimiter.checkCanPost(thread);
        assertFalse(immediateCheck.isAllowed());
        assertTrue(immediateCheck.getReason().contains("cooldown active"));
    }

    @Test
    @DisplayName("Should enforce website and thread scraping intervals")
    void testScrapingIntervals() {
        String domain = "forum.example.com";
        String thread = "https://forum.example.com/threads/abc";

        assertTrue(rateLimiter.canPollWebsite(domain));
        assertTrue(rateLimiter.canPollThread(thread, 30));

        rateLimiter.recordWebsitePoll(domain);
        rateLimiter.recordThreadPoll(thread);

        assertFalse(rateLimiter.canPollWebsite(domain));
        assertFalse(rateLimiter.canPollThread(thread, 30));
        assertTrue(rateLimiter.getSecondsUntilWebsitePollReady(domain) > 0);
        assertTrue(rateLimiter.getSecondsUntilThreadPollReady(thread, 30) > 0);
    }
}
