package aparmar2000.xenforoposter.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aparmar2000.xenforoposter.model.conditions.AntiNecropostCondition;
import aparmar2000.xenforoposter.model.conditions.PostCondition;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ScheduledJob {
    @Getter
    @RequiredArgsConstructor
    public enum JobStatus {
        DRAFT("Draft", false),
        SCHEDULED("Scheduled", true),
        WAITING_LOCAL_CONDITIONS("Waiting (Time/Local)", true),
        WAITING_THREAD_POLL("Queued for Poll", true),
        WAITING_THREAD_CONDITIONS("Waiting (Thread Conditions)", true),
        READY_TO_POST("Ready to Post", true),
        POSTING("Posting...", false),
        COMPLETED("Completed", false),
        FAILED("Failed", false),
        CANCELLED("Cancelled", false),
        PAUSED("Paused", false);

        private final String displayName;
        private final boolean isActive;

        @Override
        public String toString() {
            return displayName;
        }
    }

    @NotNull @Builder.Default String id = UUID.randomUUID().toString();
    @NotNull String name;
    @NotNull String forumProfileId;
    @NotNull String threadUrl;
    @NotNull String bbCodeContent;

    @NotNull @Builder.Default JobPriority priority = JobPriority.NORMAL;
    @Builder.Default int customPollIntervalSeconds = 0; // 0 = use default (30s)

    @NotNull @Builder.Default List<PostCondition> conditions = defaultConditions();

    @NotNull @Builder.Default JobStatus status = JobStatus.SCHEDULED;
    @Nullable Instant lastPollTime;
    @Nullable String lastPollStatus;
    @NotNull @Builder.Default String currentStatusReason = "Initialized, awaiting scheduler evaluation";

    @NotNull @Singular("pollRecord") List<PollRecord> pollHistory;

    @Nullable Instant createdAt;
    @Nullable Instant completedAt;

    public static List<PostCondition> defaultConditions() {
        List<PostCondition> list = new ArrayList<>();
        list.add(AntiNecropostCondition.builder().build());
        return list;
    }

    public String getContent() {
        return bbCodeContent;
    }

    public List<PollRecord> getPollHistory() {
        return pollHistory != null ? Collections.unmodifiableList(pollHistory) : Collections.emptyList();
    }
}
