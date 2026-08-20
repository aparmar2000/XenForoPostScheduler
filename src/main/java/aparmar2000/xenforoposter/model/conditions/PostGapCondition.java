package aparmar2000.xenforoposter.model.conditions;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import aparmar2000.xenforoposter.model.ScrapedThreadData;
import aparmar2000.xenforoposter.model.ThreadMetadata;
import aparmar2000.xenforoposter.model.ThreadPost;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
@Builder(toBuilder = true)
public class PostGapCondition extends PostCondition {
    @NotNull @Builder.Default String id = UUID.randomUUID().toString();
    int minPostsSinceUser;
    int baselineReplyCount;
    boolean useBaselineCount;

    @Override
    public @NotNull String getDisplayName() {
        return "Post Gap / Activity";
    }

    @Override
    public @NotNull String getDescription() {
        if (useBaselineCount) {
            return String.format("Requires at least %d new replies since baseline count (%d)",
                    minPostsSinceUser, baselineReplyCount);
        }
        return String.format("Requires at least %d reply(ies) from other users since your last post",
                    minPostsSinceUser);
    }

    @Override
    public @NotNull ConditionType getType() {
        return ConditionType.THREAD_DEPENDENT;
    }

    @Override
    protected @NotNull ConditionResult innerEvaluate(@NotNull EvaluationContext context) throws ConditionEvaluationException {
        ThreadMetadata metadata = getThreadMetadataOrFail(context);

        if (useBaselineCount) {
            int newReplies = metadata.getReplyCount() - baselineReplyCount;
            if (newReplies < minPostsSinceUser) {
                fail(String.format(
                        "Insufficient new replies: %d received since baseline (%d total now), requires %d",
                        Math.max(0, newReplies), metadata.getReplyCount(), minPostsSinceUser));
            }
            return pass(String.format("%d new replies received (required %d)", newReplies, minPostsSinceUser));
        }

        // Count posts by others since last post by user
        String username = getUsernameOrFail(context);

        ScrapedThreadData threadData = getThreadDataOrFail(context);

        int totalPages = threadData.getTotalPages();
        requireLatestPageLoadedOrFail(threadData);

        int countSinceUser = 0;
        boolean userFound = false;

        // Traverse backwards from totalPages down to 1
        for (int p = totalPages; p >= 1; p--) {
            if (!threadData.hasPage(p)) {
                if (countSinceUser >= minPostsSinceUser) {
                    // Already enough posts accumulated after user or end of thread
                    break;
                }
                requirePageLoadedOrFail(threadData, p);
            }

            List<ThreadPost> pagePosts = threadData.getPostsOnPage(p);
            for (int i = pagePosts.size() - 1; i >= 0; i--) {
                ThreadPost post = pagePosts.get(i);
                if (post.getAuthor().equalsIgnoreCase(username)) {
                    userFound = true;
                    break;
                }
                countSinceUser++;
            }

            if (userFound) {
                break;
            }
        }

        int effectiveGap = userFound ? countSinceUser : Math.max(countSinceUser, minPostsSinceUser);

        if (effectiveGap < minPostsSinceUser) {
            fail(String.format(
                    "Post gap not met: only %d post(s) since your last post (%s), requires %d",
                    effectiveGap, username, minPostsSinceUser));
        }

        return pass(String.format("Post gap satisfied (%d >= %d posts since %s)",
                effectiveGap, minPostsSinceUser, username));
    }
}
