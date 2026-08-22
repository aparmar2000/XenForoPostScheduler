package aparmar2000.xenforoposter.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jetbrains.annotations.NotNull;

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.inject.Guice;
import com.google.inject.Injector;

import aparmar2000.xenforoposter.di.AppModule;
import aparmar2000.xenforoposter.extension.ExtensionManager;
import aparmar2000.xenforoposter.extension.builtin.FormattingToolsExtension;
import aparmar2000.xenforoposter.extension.builtin.TemplateInsertExtension;
import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.JobPriority;
import aparmar2000.xenforoposter.model.PollRecord;
import aparmar2000.xenforoposter.model.ScheduledJob;
import aparmar2000.xenforoposter.model.conditions.AntiNecropostCondition;
import aparmar2000.xenforoposter.model.conditions.DateRangeCondition;
import aparmar2000.xenforoposter.model.conditions.DayOfWeekCondition;
import aparmar2000.xenforoposter.model.conditions.PostGapCondition;
import aparmar2000.xenforoposter.model.conditions.TimeRangeCondition;
import aparmar2000.xenforoposter.scheduler.SchedulerEngine;
import aparmar2000.xenforoposter.security.SecureString;
import aparmar2000.xenforoposter.settings.GeneralSettings;
import aparmar2000.xenforoposter.web.XenForoWebClient;

/**
 * Helper utility to supply mock data and services for WindowBuilder design-time UI previewing.
 */
public final class UiPreviewHelper {

    private UiPreviewHelper() {}

    /**
     * Creates a populated in-memory {@link SchedulerEngine} with sample profiles and jobs
     * for visual inspection in WindowBuilder design mode.
     */
	@NotNull
    public static SchedulerEngine createPreviewSchedulerEngine() {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("xf_design_preview");
            tempDir.toFile().deleteOnExit();
        } catch (IOException e) {
            tempDir = new File(System.getProperty("java.io.tmpdir"), "xf_design_preview").toPath();
        }

        @SuppressWarnings("deprecation")
        Injector injector = Guice.createInjector(new AppModule(tempDir));
        SchedulerEngine engine = injector.getInstance(SchedulerEngine.class);

        // Add sample forum profiles
        ForumProfile profile1 = ForumProfile.builder()
                .id("prof-1")
                .name("Official Community (Admin)")
                .baseUrl("https://community.example.com/")
                .username("AdminUser")
                .sessionCookies(Collections.singletonMap("xf_session", SecureString.of("preview_token_12345")))
                .build();

        ForumProfile profile2 = ForumProfile.builder()
                .id("prof-2")
                .name("Gaming Subforum (Moderator)")
                .baseUrl("https://gaming.example.org/")
                .username("ModGamer")
                .build();

        engine.addOrUpdateProfile(profile1);
        engine.addOrUpdateProfile(profile2);

        // Sample Poll Records
        List<PollRecord> samplePollHistory = new ArrayList<>();
        samplePollHistory.add(PollRecord.builder()
                .timestamp(Instant.now().minus(10, ChronoUnit.MINUTES))
                .pollType(PollRecord.PollType.LOCAL_EVALUATION)
                .success(true)
                .summary("Local conditions passed (TimeRangeCondition met)")
                .details("Evaluated at target timestamp. Condition is ready.")
                .build());
        samplePollHistory.add(PollRecord.builder()
                .timestamp(Instant.now().minus(2, ChronoUnit.MINUTES))
                .pollType(PollRecord.PollType.THREAD_FETCH)
                .success(true)
                .summary("Thread status: Open | Reply count: 48 (Required: >= 2)")
                .details("Fetched page 1 of thread. Anti-necropost checks satisfied. Post gap satisfied.")
                .build());

        // Sample Scheduled Jobs
        ScheduledJob job1 = ScheduledJob.builder()
                .id("job-sample-1")
                .name("Weekly Maintenance Announcement")
                .forumProfileId("prof-1")
                .threadUrl("https://community.example.com/threads/maintenance-updates.101/")
                .bbCodeContent("[B]Scheduled Server Maintenance Notice[/B]\n\nPlease note that our servers will be undergoing scheduled updates tonight.\n\n[QUOTE=SysAdmin]Estimated downtime is ~30 minutes.[/QUOTE]\n\n[COLOR=#28a745]Thank you for your patience![/COLOR]")
                .priority(JobPriority.HIGH)
                .customPollIntervalSeconds(30)
                .status(ScheduledJob.JobStatus.SCHEDULED)
                .currentStatusReason("Awaiting scheduled window (TimeRangeCondition)")
                .conditions(List.of(
                        DateRangeCondition.builder().startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(7)).build(),
                        DayOfWeekCondition.builder().allowedDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)).build(),
                        TimeRangeCondition.builder().startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0)).build(),
                        AntiNecropostCondition.builder().maxInactiveDays(30).allowAuthorExemption(true).build()
                ))
                .pollHistory(samplePollHistory)
                .createdAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .lastPollTime(Instant.now().minus(2, ChronoUnit.MINUTES))
                .build();

        ScheduledJob job2 = ScheduledJob.builder()
                .id("job-sample-2")
                .name("Community Thread Bump & FAQ Reply")
                .forumProfileId("prof-2")
                .threadUrl("https://gaming.example.org/threads/faq-and-rules.202/")
                .bbCodeContent("[B]Frequently Asked Questions (Updated)[/B]\n\n[LIST]\n[*]Rule 1: Be respectful\n[*]Rule 2: No spamming\n[/LIST]")
                .priority(JobPriority.NORMAL)
                .customPollIntervalSeconds(60)
                .status(ScheduledJob.JobStatus.WAITING_THREAD_CONDITIONS)
                .currentStatusReason("Waiting for at least 3 replies since user's last post (PostGapCondition)")
                .conditions(List.of(
                        PostGapCondition.builder().minPostsSinceUser(3).useBaselineCount(false).build(),
                        AntiNecropostCondition.builder().maxInactiveDays(14).allowAuthorExemption(false).build()
                ))
                .pollHistory(samplePollHistory)
                .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .lastPollTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        ScheduledJob job3 = ScheduledJob.builder()
                .id("job-sample-3")
                .name("Draft: Event Sign-up Thread")
                .forumProfileId("prof-1")
                .threadUrl("https://community.example.com/threads/summer-tournament.303/")
                .bbCodeContent("[CENTER][SIZE=5][B]Summer Championship Sign-ups[/B][/SIZE][/CENTER]\n\nReply to this thread to register your team!")
                .priority(JobPriority.LOW)
                .status(ScheduledJob.JobStatus.DRAFT)
                .currentStatusReason("Draft saved")
                .conditions(List.of(AntiNecropostCondition.builder().build()))
                .createdAt(Instant.now().minus(3, ChronoUnit.HOURS))
                .build();

        engine.addOrUpdateJob(job1);
        engine.addOrUpdateJob(job2);
        engine.addOrUpdateJob(job3);

        return engine;
    }

    /**
     * Creates a populated in-memory {@link ExtensionManager} with built-in extensions loaded
     * for WindowBuilder design preview.
     */
    @NotNull
    public static ExtensionManager createPreviewExtensionManager() {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("xf_design_ext");
            tempDir.toFile().deleteOnExit();
        } catch (IOException e) {
            tempDir = new File(System.getProperty("java.io.tmpdir"), "xf_design_ext").toPath();
        }

        @SuppressWarnings("deprecation")
        Injector injector = Guice.createInjector(new AppModule(tempDir));
        ExtensionManager extensionManager = injector.getInstance(ExtensionManager.class);
        extensionManager.registerInternalExtension(new FormattingToolsExtension());
        extensionManager.registerInternalExtension(new TemplateInsertExtension());
        extensionManager.loadAllExtensions();
        return extensionManager;
    }

    /**
     * Creates a default {@link aparmar2000.xenforoposter.settings.GeneralSettings} for design preview.
     */
    @NotNull
    public static GeneralSettings createPreviewGeneralSettings() {
        return new GeneralSettings(null, null);
    }

    /**
     * Creates a default {@link XenForoWebClient} for design preview.
     */
    @NotNull
    public static XenForoWebClient createPreviewWebClient() {
        return new XenForoWebClient();
    }

    /**
     * Sample preview post job for component initializations.
     */
    @NotNull
    public static ScheduledJob createPreviewSampleJob() {
        return ScheduledJob.builder()
                .id("sample-job-preview")
                .name("Sample Scheduled Post")
                .forumProfileId("prof-1")
                .threadUrl("https://forum.example.com/threads/sample.123/")
                .bbCodeContent("[B]Sample Headline[/B]\n\nThis is a preview of the [COLOR=#007bff]BBCode editor[/COLOR] and live HTML renderer.")
                .priority(JobPriority.NORMAL)
                .status(ScheduledJob.JobStatus.SCHEDULED)
                .currentStatusReason("Awaiting conditions")
                .conditions(List.of(AntiNecropostCondition.builder().build()))
                .pollHistory(List.of(
                        PollRecord.builder()
                                .timestamp(Instant.now().minus(15, ChronoUnit.MINUTES))
                                .pollType(PollRecord.PollType.LOCAL_EVALUATION)
                                .success(true)
                                .summary("Local condition passed")
                                .build(),
                        PollRecord.builder()
                                .timestamp(Instant.now().minus(5, ChronoUnit.MINUTES))
                                .pollType(PollRecord.PollType.THREAD_FETCH)
                                .success(true)
                                .summary("Thread scraped: 24 replies found")
                                .details("All anti-necropost checks satisfied.")
                                .build()
                ))
                .build();
    }

    /**
     * Launches a standalone test frame to preview any JPanel component with FlatDarkLaf theme.
     */
    public static void showPreviewFrame(@NotNull JPanel panel, @NotNull String title, int width, int height) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            JFrame frame = new JFrame("[Design Preview] " + title);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setSize(width, height);
            frame.setLocationRelativeTo(null);
            frame.getContentPane().add(panel);
            frame.setVisible(true);
        });
    }
}
