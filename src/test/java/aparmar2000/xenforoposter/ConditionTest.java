package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.ScrapedThreadData;
import aparmar2000.xenforoposter.model.ThreadMetadata;
import aparmar2000.xenforoposter.model.ThreadPost;
import aparmar2000.xenforoposter.model.conditions.AntiNecropostCondition;
import aparmar2000.xenforoposter.model.conditions.CompositeCondition;
import aparmar2000.xenforoposter.model.conditions.ConditionResult;
import aparmar2000.xenforoposter.model.conditions.ConditionType;
import aparmar2000.xenforoposter.model.conditions.DateRangeCondition;
import aparmar2000.xenforoposter.model.conditions.DayOfWeekCondition;
import aparmar2000.xenforoposter.model.conditions.EvaluationContext;
import aparmar2000.xenforoposter.model.conditions.PostCondition;
import aparmar2000.xenforoposter.model.conditions.PostGapCondition;
import aparmar2000.xenforoposter.model.conditions.TimeRangeCondition;

class ConditionTest {
    private ForumProfile profile;
    private ZoneId zone;

    @BeforeEach
    void setUp() {
        zone = ZoneId.systemDefault();
        profile = ForumProfile.builder()
                .name("Test Forum")
                .baseUrl("https://forum.example.com")
                .username("MyUsername")
                .build();
    }

    @Test
    @DisplayName("DateRangeCondition should pass inside date window and fail with proper expiry before start")
    void testDateRangeCondition() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusDays(7);

        // Condition 1: Future start date -> should fail with expiry at start of that date
        DateRangeCondition futureCond = DateRangeCondition.builder()
                .startDate(tomorrow)
                .endDate(nextWeek)
                .build();

        assertEquals(ConditionType.LOCAL, futureCond.getType());

        EvaluationContext ctx = EvaluationContext.builder()
                .evaluationTime(Instant.now())
                .forumProfile(profile)
                .build();

        ConditionResult futureRes = futureCond.evaluate(ctx);
        assertFalse(futureRes.isSatisfied());
        assertTrue(futureRes.hasExpiry());
        assertEquals(tomorrow.atStartOfDay(zone).toInstant(), futureRes.getExpiry());
        assertTrue(futureRes.getMessage().contains("Waiting for start date"));

        // Condition 2: Active date window (yesterday to tomorrow) -> should pass
        DateRangeCondition activeCond = DateRangeCondition.builder()
                .startDate(yesterday)
                .endDate(tomorrow)
                .build();

        ConditionResult activeRes = activeCond.evaluate(ctx);
        assertTrue(activeRes.isSatisfied());

        // Condition 3: Single date match (today only) -> should pass
        DateRangeCondition singleDateCond = DateRangeCondition.builder()
                .startDate(today)
                .endDate(today)
                .build();
        assertTrue(singleDateCond.evaluate(ctx).isSatisfied());
        assertTrue(singleDateCond.getDescription().contains("Only on"));

        // Condition 4: Expired date window (past) -> should fail without future expiry
        DateRangeCondition pastCond = DateRangeCondition.builder()
                .startDate(yesterday.minusDays(5))
                .endDate(yesterday)
                .build();

        ConditionResult pastRes = pastCond.evaluate(ctx);
        assertFalse(pastRes.isSatisfied());
        assertFalse(pastRes.hasExpiry());
        assertTrue(pastRes.getMessage().contains("Date range expired"));
    }

    @Test
    @DisplayName("DayOfWeekCondition should validate allowed days and compute next day start for expiry")
    void testDayOfWeekCondition() {
        Instant now = Instant.now();
        DayOfWeek currentDay = now.atZone(zone).getDayOfWeek();

        // 1. Condition containing today -> pass
        DayOfWeekCondition passCond = DayOfWeekCondition.builder()
                .allowedDays(Set.of(currentDay))
                .build();

        assertEquals(ConditionType.LOCAL, passCond.getType());

        EvaluationContext ctx = EvaluationContext.builder()
                .evaluationTime(now)
                .forumProfile(profile)
                .build();

        assertTrue(passCond.evaluate(ctx).isSatisfied());

        // 2. Condition excluding today -> fail, with expiry pointing to the next allowed day
        DayOfWeek otherDay = currentDay.plus(2); // 2 days ahead
        DayOfWeekCondition failCond = DayOfWeekCondition.builder()
                .allowedDays(Set.of(otherDay))
                .build();

        ConditionResult failRes = failCond.evaluate(ctx);
        assertFalse(failRes.isSatisfied());
        assertTrue(failRes.hasExpiry());
        LocalDate expectedDate = now.atZone(zone).toLocalDate().plusDays(2);
        assertEquals(expectedDate.atStartOfDay(zone).toInstant(), failRes.getExpiry());

        // 3. Preset descriptions
        DayOfWeekCondition weekdays = DayOfWeekCondition.builder()
                .allowedDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY))
                .build();
        assertEquals("Weekdays only (Mon - Fri)", weekdays.getDescription());

        DayOfWeekCondition weekends = DayOfWeekCondition.builder()
                .allowedDays(EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
                .build();
        assertEquals("Weekends only (Sat - Sun)", weekends.getDescription());
    }

    @Test
    @DisplayName("TimeRangeCondition should evaluate daytime and overnight ranges correctly")
    void testTimeRangeCondition() {
        ZonedDateTime nowZdt = ZonedDateTime.now(zone);
        LocalTime nowTime = nowZdt.toLocalTime();

        // 1. Daytime range covering now
        LocalTime start = nowTime.minusMinutes(10);
        LocalTime end = nowTime.plusMinutes(10);

        // Adjust if crossing midnight
        if (start.isBefore(end)) {
            TimeRangeCondition activeCond = TimeRangeCondition.builder()
                    .startTime(start)
                    .endTime(end)
                    .build();

            EvaluationContext ctx = EvaluationContext.builder()
                    .evaluationTime(nowZdt.toInstant())
                    .forumProfile(profile)
                    .build();

            assertTrue(activeCond.evaluate(ctx).isSatisfied());
        }

        // 2. Fixed daytime test with synthetic evaluation time
        ZonedDateTime noon = LocalDate.of(2026, 8, 20).atTime(12, 0).atZone(zone);
        EvaluationContext noonCtx = EvaluationContext.builder()
                .evaluationTime(noon.toInstant())
                .forumProfile(profile)
                .build();

        TimeRangeCondition businessHours = TimeRangeCondition.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build();

        assertEquals(ConditionType.LOCAL, businessHours.getType());
        assertTrue(businessHours.evaluate(noonCtx).isSatisfied());

        // Evaluation before business hours (e.g. 07:00) -> should fail with expiry today at 09:00
        ZonedDateTime earlyMorning = LocalDate.of(2026, 8, 20).atTime(7, 0).atZone(zone);
        EvaluationContext earlyCtx = EvaluationContext.builder()
                .evaluationTime(earlyMorning.toInstant())
                .forumProfile(profile)
                .build();

        ConditionResult earlyRes = businessHours.evaluate(earlyCtx);
        assertFalse(earlyRes.isSatisfied());
        assertTrue(earlyRes.hasExpiry());
        assertEquals(LocalDate.of(2026, 8, 20).atTime(9, 0).atZone(zone).toInstant(), earlyRes.getExpiry());

        // 3. Overnight range test (22:00 to 06:00)
        TimeRangeCondition overnight = TimeRangeCondition.builder()
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .build();

        assertTrue(overnight.getDescription().contains("overnight"));

        // At 23:00 -> should pass
        ZonedDateTime lateNight = LocalDate.of(2026, 8, 20).atTime(23, 0).atZone(zone);
        assertTrue(overnight.evaluate(EvaluationContext.builder().evaluationTime(lateNight.toInstant()).forumProfile(profile).build()).isSatisfied());

        // At 03:00 -> should pass
        ZonedDateTime earlyHours = LocalDate.of(2026, 8, 20).atTime(3, 0).atZone(zone);
        assertTrue(overnight.evaluate(EvaluationContext.builder().evaluationTime(earlyHours.toInstant()).forumProfile(profile).build()).isSatisfied());

        // At 14:00 -> should fail with expiry today at 22:00
        ZonedDateTime afternoon = LocalDate.of(2026, 8, 20).atTime(14, 0).atZone(zone);
        ConditionResult afternoonRes = overnight.evaluate(EvaluationContext.builder().evaluationTime(afternoon.toInstant()).forumProfile(profile).build());
        assertFalse(afternoonRes.isSatisfied());
        assertTrue(afternoonRes.hasExpiry());
        assertEquals(LocalDate.of(2026, 8, 20).atTime(22, 0).atZone(zone).toInstant(), afternoonRes.getExpiry());
    }

    @Test
    @DisplayName("PostGapCondition should request unpolled pages and evaluate gaps based on username")
    void testPostGapCondition() {
        PostGapCondition gapCondition = PostGapCondition.builder()
                .minPostsSinceUser(2)
                .useBaselineCount(false)
                .build();

        assertEquals(ConditionType.THREAD_DEPENDENT, gapCondition.getType());

        ThreadMetadata meta = ThreadMetadata.builder()
                .threadUrl("https://forum.example.com/threads/1")
                .title("Test")
                .canReply(true)
                .totalPages(2)
                .build();

        // Scenario 0: Latest page (2) is not loaded -> Request page 2
        ScrapedThreadData threadDataPage1Only = ScrapedThreadData.builder()
                .metadata(meta)
                .page(1, List.of(
                        ThreadPost.builder().postId("1").author("Alice").timestamp(Instant.now()).pageNumber(1).build()
                ))
                .build();

        EvaluationContext ctx0 = EvaluationContext.builder()
                .evaluationTime(Instant.now())
                .forumProfile(profile)
                .threadData(threadDataPage1Only)
                .build();

        ConditionResult res0 = gapCondition.evaluate(ctx0);
        assertFalse(res0.isSatisfied());
        assertTrue(res0.hasPageRequest());
        assertEquals(2, res0.getRequestedPage());

        // Scenario 1: Page 2 loaded, but only 1 post since MyUsername -> Fail
        ScrapedThreadData threadData1 = ScrapedThreadData.builder()
                .metadata(meta)
                .page(2, List.of(
                        ThreadPost.builder().postId("10").author("Alice").timestamp(Instant.now()).pageNumber(2).build(),
                        ThreadPost.builder().postId("11").author("MyUsername").timestamp(Instant.now()).pageNumber(2).build(),
                        ThreadPost.builder().postId("12").author("Bob").timestamp(Instant.now()).pageNumber(2).build()
                ))
                .build();

        EvaluationContext ctx1 = EvaluationContext.builder()
                .evaluationTime(Instant.now())
                .forumProfile(profile)
                .threadData(threadData1)
                .build();

        ConditionResult res1 = gapCondition.evaluate(ctx1);
        assertFalse(res1.isSatisfied());
        assertTrue(res1.getMessage().contains("only 1 post(s) since your last post"));

        // Scenario 2: 2 posts since MyUsername on page 2 -> Pass
        ScrapedThreadData threadData2 = ScrapedThreadData.builder()
                .metadata(meta)
                .page(2, List.of(
                        ThreadPost.builder().postId("10").author("Alice").timestamp(Instant.now()).pageNumber(2).build(),
                        ThreadPost.builder().postId("11").author("MyUsername").timestamp(Instant.now()).pageNumber(2).build(),
                        ThreadPost.builder().postId("12").author("Bob").timestamp(Instant.now()).pageNumber(2).build(),
                        ThreadPost.builder().postId("13").author("Charlie").timestamp(Instant.now()).pageNumber(2).build()
                ))
                .build();

        EvaluationContext ctx2 = EvaluationContext.builder()
                .evaluationTime(Instant.now())
                .forumProfile(profile)
                .threadData(threadData2)
                .build();

        assertTrue(gapCondition.evaluate(ctx2).isSatisfied());
    }

    @Test
    @DisplayName("AntiNecropostCondition should request unpolled pages and block inactive threads unless user is author")
    void testAntiNecropostCondition() {
        AntiNecropostCondition necroCondition = AntiNecropostCondition.builder()
                .maxInactiveDays(30)
                .allowAuthorExemption(true)
                .build();

        Instant oldTime = Instant.now().minus(Duration.ofDays(45));

        // Scenario 0: Multi-page thread where last page is not loaded -> Request last page
        ThreadMetadata multiPageMeta = ThreadMetadata.builder()
                .threadUrl("https://forum.example.com/threads/1")
                .title("Multi-page Thread")
                .threadAuthor("OriginalPoster")
                .canReply(true)
                .totalPages(5)
                .build();

        ScrapedThreadData dataPage1Only = ScrapedThreadData.builder()
                .metadata(multiPageMeta)
                .page(1, List.of(
                        ThreadPost.builder().postId("1").author("OriginalPoster").timestamp(oldTime).pageNumber(1).build()
                ))
                .build();

        EvaluationContext ctxPage1 = EvaluationContext.builder()
                .evaluationTime(Instant.now())
                .forumProfile(profile)
                .threadData(dataPage1Only)
                .build();

        ConditionResult pageReqRes = necroCondition.evaluate(ctxPage1);
        assertFalse(pageReqRes.isSatisfied());
        assertTrue(pageReqRes.hasPageRequest());
        assertEquals(5, pageReqRes.getRequestedPage());

        // Scenario 1: Inactive thread with non-author user and loaded last page -> Fail
        ScrapedThreadData metaOtherAuthor = ScrapedThreadData.builder()
                .metadata(ThreadMetadata.builder()
                        .threadUrl("https://forum.example.com/threads/1")
                        .title("Old Thread")
                        .threadAuthor("OriginalPoster")
                        .canReply(true)
                        .totalPages(1)
                        .build())
                .page(1, List.of(
                        ThreadPost.builder().postId("1").author("OriginalPoster").timestamp(oldTime).pageNumber(1).build()
                ))
                .build();

        EvaluationContext ctxOther = EvaluationContext.builder()
                .evaluationTime(Instant.now())
                .forumProfile(profile)
                .threadData(metaOtherAuthor)
                .build();

        ConditionResult resOther = necroCondition.evaluate(ctxOther);
        assertFalse(resOther.isSatisfied());
        assertTrue(resOther.getMessage().contains("Anti-necropost trigger"));

        // Scenario 2: Inactive thread where user IS the thread author -> Pass (Exemption)
        ScrapedThreadData metaSameAuthor = ScrapedThreadData.builder()
                .metadata(ThreadMetadata.builder()
                        .threadUrl("https://forum.example.com/threads/1")
                        .title("My Old Thread")
                        .threadAuthor("MyUsername")
                        .canReply(true)
                        .totalPages(1)
                        .build())
                .page(1, List.of(
                        ThreadPost.builder().postId("1").author("MyUsername").timestamp(oldTime).pageNumber(1).build()
                ))
                .build();

        EvaluationContext ctxAuthor = EvaluationContext.builder()
                .evaluationTime(Instant.now())
                .forumProfile(profile)
                .threadData(metaSameAuthor)
                .build();

        ConditionResult resAuthor = necroCondition.evaluate(ctxAuthor);
        assertTrue(resAuthor.isSatisfied());
        assertTrue(resAuthor.getMessage().contains("Necropost exemption"));
    }

    @Test
    @DisplayName("CompositeCondition should evaluate AND / OR logic and propagate page requests properly")
    void testCompositeCondition() {
        EvaluationContext ctx = EvaluationContext.builder()
                .evaluationTime(Instant.now())
                .forumProfile(profile)
                .build();

        PostCondition passCond = mock(PostCondition.class);
        when(passCond.evaluate(ctx)).thenReturn(ConditionResult.pass("Pass condition"));

        PostCondition failCond = mock(PostCondition.class);
        when(failCond.evaluate(ctx)).thenReturn(ConditionResult.fail("Fail condition"));

        CompositeCondition andComposite = CompositeCondition.builder()
                .operator(CompositeCondition.Operator.AND)
                .condition(passCond)
                .condition(failCond)
                .build();

        CompositeCondition orComposite = CompositeCondition.builder()
                .operator(CompositeCondition.Operator.OR)
                .condition(passCond)
                .condition(failCond)
                .build();

        assertFalse(andComposite.evaluate(ctx).isSatisfied());
        assertTrue(orComposite.evaluate(ctx).isSatisfied());

        // Propagate page request from child condition
        PostCondition pageReqCond = mock(PostCondition.class);
        when(pageReqCond.evaluate(ctx)).thenReturn(ConditionResult.failWithPageRequest("Need page 3", 3));

        CompositeCondition andWithPageReq = CompositeCondition.builder()
                .operator(CompositeCondition.Operator.AND)
                .condition(passCond)
                .condition(pageReqCond)
                .build();

        ConditionResult andRes = andWithPageReq.evaluate(ctx);
        assertFalse(andRes.isSatisfied());
        assertTrue(andRes.hasPageRequest());
        assertEquals(3, andRes.getRequestedPage());
    }

    @Test
    @DisplayName("ConditionResult expiry and re-test eligibility checks")
    void testConditionResultExpiryAndEligibility() {
        Instant now = Instant.now();
        Instant futureExpiry = now.plus(Duration.ofMinutes(10));
        Instant pastExpiry = now.minus(Duration.ofMinutes(10));

        // Result with no expiry (never expires)
        ConditionResult noExpiry = ConditionResult.pass("No expiry set");
        assertFalse(noExpiry.hasExpiry());
        assertFalse(noExpiry.isExpired(now));
        assertTrue(noExpiry.isEligibleForRetest(now));

        // Result with future expiry (non-expired -> eligible for re-test)
        ConditionResult futureResult = ConditionResult.fail("Cooldown active", futureExpiry);
        assertTrue(futureResult.hasExpiry());
        assertEquals(futureExpiry, futureResult.getExpiry());
        assertFalse(futureResult.isExpired(now));
        assertTrue(futureResult.isEligibleForRetest(now));

        // Result with past expiry (expired -> discarded, not eligible for re-test)
        ConditionResult pastResult = ConditionResult.fail("Expired check", pastExpiry);
        assertTrue(pastResult.hasExpiry());
        assertEquals(pastExpiry, pastResult.getExpiry());
        assertTrue(pastResult.isExpired(now));
        assertFalse(pastResult.isEligibleForRetest(now));

        // Testing duration-based helper
        ConditionResult durationResult = ConditionResult.pass("Valid for 1 hour", Duration.ofHours(1));
        assertTrue(durationResult.hasExpiry());
        assertFalse(durationResult.isExpired(now));
        assertTrue(durationResult.isEligibleForRetest(now));

        // Page request helpers
        ConditionResult pageReq = ConditionResult.failWithPageRequest("Need page 4", 4);
        assertTrue(pageReq.hasPageRequest());
        assertEquals(4, pageReq.getRequestedPage());
    }
}
