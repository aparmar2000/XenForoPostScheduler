package aparmar2000.xenforoposter.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ScrapedThreadData {
    @NotNull ThreadMetadata metadata;
    @NotNull @Singular("page") Map<Integer, List<ThreadPost>> pages;

    public int getTotalPages() {
        return metadata != null ? metadata.getTotalPages() : 1;
    }

    public boolean hasPage(int pageNumber) {
        return pages != null && pages.containsKey(pageNumber);
    }

    public @NotNull List<ThreadPost> getPostsOnPage(int pageNumber) {
        if (pages == null || !pages.containsKey(pageNumber)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(pages.get(pageNumber));
    }

    public @NotNull Set<Integer> getLoadedPageNumbers() {
        if (pages == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(pages.keySet());
    }

    public @NotNull List<ThreadPost> getAllLoadedPosts() {
        if (pages == null || pages.isEmpty()) {
            return Collections.emptyList();
        }
        List<ThreadPost> all = new ArrayList<>();
        List<Integer> sortedPages = new ArrayList<>(pages.keySet());
        Collections.sort(sortedPages);
        for (Integer pageNum : sortedPages) {
            List<ThreadPost> pagePosts = pages.get(pageNum);
            if (pagePosts != null) {
                all.addAll(pagePosts);
            }
        }
        return Collections.unmodifiableList(all);
    }

    public @NotNull Optional<ThreadPost> getLatestPost() {
        if (pages == null || pages.isEmpty()) {
            return Optional.empty();
        }
        List<Integer> sortedPages = new ArrayList<>(pages.keySet());
        Collections.sort(sortedPages);
        for (int i = sortedPages.size() - 1; i >= 0; i--) {
            List<ThreadPost> pagePosts = pages.get(sortedPages.get(i));
            if (pagePosts != null && !pagePosts.isEmpty()) {
                return Optional.of(pagePosts.get(pagePosts.size() - 1));
            }
        }
        return Optional.empty();
    }

    public @NotNull Optional<Instant> getLatestPostTimestamp() {
        return getLatestPost().map(ThreadPost::getTimestamp);
    }

    public @NotNull Optional<ThreadPost> getLatestPostByAuthor(@NotNull String author) {
        List<ThreadPost> posts = getAllLoadedPosts();
        for (int i = posts.size() - 1; i >= 0; i--) {
            ThreadPost p = posts.get(i);
            if (p.getAuthor().equalsIgnoreCase(author)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public int countPostsAfterLatestByAuthor(@NotNull String author) {
        List<ThreadPost> posts = getAllLoadedPosts();
        int count = 0;
        boolean found = false;
        for (int i = posts.size() - 1; i >= 0; i--) {
            ThreadPost p = posts.get(i);
            if (p.getAuthor().equalsIgnoreCase(author)) {
                found = true;
                break;
            }
            count++;
        }
        return found ? count : posts.size();
    }

    public @NotNull ScrapedThreadData withPage(int pageNumber, @NotNull List<ThreadPost> posts) {
        Map<Integer, List<ThreadPost>> updated = new TreeMap<>();
        if (pages != null) {
            updated.putAll(pages);
        }
        updated.put(pageNumber, new ArrayList<>(posts));
        return toBuilder().clearPages().pages(updated).build();
    }

    public @NotNull ScrapedThreadData withMetadata(@NotNull ThreadMetadata newMetadata) {
        return toBuilder().metadata(newMetadata).build();
    }
}
