package aparmar2000.xenforoposter.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.ScrapedThreadData;
import aparmar2000.xenforoposter.model.ThreadMetadata;
import aparmar2000.xenforoposter.model.ThreadPost;
import aparmar2000.xenforoposter.security.SecureString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XenForoWebClient implements AutoCloseable {
    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";

    private final BasicCookieStore cookieStore;
    private final CloseableHttpClient httpClient;

    public XenForoWebClient() {
        this(new BasicCookieStore(), null);
    }

    public XenForoWebClient(@NotNull BasicCookieStore cookieStore, @Nullable CloseableHttpClient httpClient) {
        this.cookieStore = cookieStore;
        this.httpClient = httpClient != null ? httpClient : HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setUserAgent(DEFAULT_USER_AGENT)
                .build();
    }

    public void restoreCookies(@Nullable Map<String, SecureString> cookies, @NotNull String baseUrl) {
        if (cookies == null || cookies.isEmpty()) {
			return;
		}
        try {
            URI uri = URI.create(baseUrl);
            String domain = uri.getHost();
            for (Map.Entry<String, SecureString> entry : cookies.entrySet()) {
                SecureString secureVal = entry.getValue();
                if (secureVal != null && secureVal.getClearText() != null) {
                    BasicClientCookie cookie = new BasicClientCookie(entry.getKey(), secureVal.getClearText());
                    cookie.setDomain(domain);
                    cookie.setPath("/");
                    cookieStore.addCookie(cookie);
                }
            }
        } catch (Exception e) {
            log.error("Failed to restore cookies for {}", baseUrl, e);
        }
    }

    public Map<String, SecureString> exportCookies() {
        Map<String, SecureString> map = new HashMap<>();
        cookieStore.getCookies().forEach(c -> map.put(c.getName(), SecureString.of(c.getValue())));
        return map;
    }

    public String buildPageUrl(@NotNull String threadUrl, int pageNumber) {
        String cleanUrl = threadUrl.replaceAll("/page-\\d+/?$", "").replaceAll("[?&]page=\\d+", "").replaceAll("/+$", "");
        if (pageNumber <= 1) {
            return cleanUrl + "/";
        }
        return cleanUrl + "/page-" + pageNumber;
    }

    public int extractTotalPages(@NotNull Document doc) {
        int maxPage = 1;
        Elements pageElements = doc.select(".pageNav-page, .pageNav-page a, ul.pageNav-main li a, .pageNavSimple-input");
        for (Element el : pageElements) {
            if (el.hasAttr("max")) {
                try {
                    maxPage = Math.max(maxPage, Integer.parseInt(el.attr("max")));
                } catch (Exception ignored) {}
            }
            String text = el.text().trim();
            if (text.matches("\\d+")) {
                try {
                    maxPage = Math.max(maxPage, Integer.parseInt(text));
                } catch (Exception ignored) {}
            }
            String href = el.attr("href");
            if (href != null && href.contains("page-")) {
                try {
                    String numStr = href.replaceAll(".*page-([0-9]+).*", "$1");
                    maxPage = Math.max(maxPage, Integer.parseInt(numStr));
                } catch (Exception ignored) {}
            }
        }
        return Math.max(1, maxPage);
    }

    public int extractCurrentPage(@NotNull String url, @NotNull Document doc) {
        if (url.contains("page-")) {
            try {
                String numStr = url.replaceAll(".*page-([0-9]+).*", "$1");
                return Integer.parseInt(numStr);
            } catch (Exception ignored) {}
        }
        Element currentEl = doc.selectFirst(".pageNav-page--current, .pageNav-page.is-current");
        if (currentEl != null) {
            try {
                return Integer.parseInt(currentEl.text().trim());
            } catch (Exception ignored) {}
        }
        return 1;
    }

    public List<ThreadPost> parsePosts(@NotNull Document doc, int pageNumber) {
        List<ThreadPost> posts = new ArrayList<>();
        Elements postArticles = doc.select("article.message, div.message--post");
        for (Element post : postArticles) {
            String postId = post.attr("data-content");
            if (postId.isEmpty()) {
                postId = post.attr("id");
            }
            if (postId.isEmpty()) {
                Element permalink = post.selectFirst("a[href*='/post-']");
                if (permalink != null) {
                    postId = permalink.attr("href");
                } else {
                    postId = "post-" + (posts.size() + 1);
                }
            }

            String author = "Unknown";
            Element userLink = post.selectFirst("a.username, .message-name a, .message-userDetails .username");
            if (userLink != null && !userLink.text().trim().isEmpty()) {
                author = userLink.text().trim();
            }

            String authorId = post.attr("data-author-id");
            if (authorId.isEmpty() && userLink != null) {
                String userHref = userLink.attr("href");
                if (userHref.contains("members/")) {
                    authorId = userHref.replaceAll(".*members/[^.]*\\.?(\\d+).*", "$1");
                }
            }
            if (authorId != null && authorId.isEmpty()) {
                authorId = null;
            }

            Instant timestamp = Instant.now();
            Element timeEl = post.selectFirst("time[data-time], time.u-dt");
            if (timeEl != null) {
                String epochStr = timeEl.attr("data-time");
                if (!epochStr.isEmpty()) {
                    try {
                        long epoch = Long.parseLong(epochStr);
                        timestamp = Instant.ofEpochSecond(epoch);
                    } catch (Exception ignored) {}
                }
            }

            int postNumber = 0;
            Element postNumEl = post.selectFirst(".message-attribution-gadget, li.message-attribution-opposite a, a.message-attribution-opposite");
            if (postNumEl != null) {
                String postNumStr = postNumEl.text().replaceAll("[^0-9]", "");
                if (!postNumStr.isEmpty()) {
                    try {
                        postNumber = Integer.parseInt(postNumStr);
                    } catch (Exception ignored) {}
                }
            }

            String content = null;
            Element bodyEl = post.selectFirst(".message-body, .message-content, .bbWrapper");
            if (bodyEl != null) {
                content = bodyEl.text().trim();
            }

            posts.add(ThreadPost.builder()
                    .postId(postId)
                    .author(author)
                    .authorId(authorId)
                    .timestamp(timestamp)
                    .postNumber(postNumber)
                    .pageNumber(pageNumber)
                    .content(content)
                    .build());
        }
        return posts;
    }

    public ScrapedThreadData fetchThreadData(@NotNull String threadUrl) throws Exception {
        HttpGet request = new HttpGet(threadUrl);
        request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        return httpClient.execute(request, (response) -> {
            int statusCode = response.getCode();
            String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            Document doc = Jsoup.parse(html, threadUrl);

            ThreadMetadata meta = parseThreadMetadata(doc, threadUrl, statusCode);
            int curPage = extractCurrentPage(threadUrl, doc);
            List<ThreadPost> posts = parsePosts(doc, curPage);

            return ScrapedThreadData.builder()
                    .metadata(meta)
                    .page(curPage, posts)
                    .build();
        });
    }

    public List<ThreadPost> fetchThreadPage(@NotNull String threadUrl, int pageNumber) throws Exception {
        String pageUrl = buildPageUrl(threadUrl, pageNumber);
        HttpGet request = new HttpGet(pageUrl);
        request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        return httpClient.execute(request, (response) -> {
            String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            Document doc = Jsoup.parse(html, pageUrl);
            return parsePosts(doc, pageNumber);
        });
    }

    public ThreadMetadata fetchThreadMetadata(@NotNull String threadUrl) throws Exception {
        return fetchThreadData(threadUrl).getMetadata();
    }

    public ThreadMetadata parseThreadMetadata(@NotNull Document doc, @NotNull String threadUrl, int statusCode) {
        String title = "";
        Element titleEl = doc.selectFirst("h1.p-title-value");
        if (titleEl != null) {
            title = titleEl.text().trim();
        } else {
            Element docTitle = doc.selectFirst("title");
            title = docTitle != null ? docTitle.text().trim() : "Unknown Thread";
        }

        // Thread ID extraction from canonical link or URL
        String threadId = extractThreadId(threadUrl, doc);

        // Check if locked
        boolean locked = doc.selectFirst(".badge--locked, i.fa-lock, .is-locked") != null;

        // Check if can reply
        boolean canReply = doc.selectFirst("form[action*='add-reply'], .js-quickReply") != null;

        // Thread author
        String threadAuthor = null;
        Element authorEl = doc.selectFirst(".p-description a.username, .message-attribution-user a.username");
        if (authorEl != null) {
            threadAuthor = authorEl.text().trim();
        }

        // CSRF Token
        String xfToken = extractCsrfToken(doc);

        // Total pages extraction
        int totalPages = extractTotalPages(doc);

        // Reply count estimate from page posts or metadata
        Elements postArticles = doc.select("article.message, div.message--post");
        int replyCount = Math.max(0, postArticles.size() > 0 ? postArticles.size() - 1 : 0);

        return ThreadMetadata.builder()
                .threadUrl(threadUrl)
                .threadId(threadId)
                .title(title)
                .locked(locked)
                .canReply(canReply)
                .replyCount(replyCount)
                .threadAuthor(threadAuthor)
                .xfToken(xfToken)
                .totalPages(totalPages)
                .build();
    }

    public LoginResult login(@NotNull ForumProfile profile) {
        if (profile.getUsername() == null || profile.getPassword() == null || profile.getPassword().getClearText() == null) {
            return LoginResult.failure("Missing username or password in forum profile");
        }

        try {
            String loginPageUrl = joinUrl(profile.getBaseUrl(), "login/");
            HttpGet getReq = new HttpGet(loginPageUrl);
            String token = httpClient.execute(getReq, (getResp) -> {
	                String html = EntityUtils.toString(getResp.getEntity(), StandardCharsets.UTF_8);
	                Document doc = Jsoup.parse(html, loginPageUrl);
	                return extractCsrfToken(doc);
	            }
            );

            if (token == null || token.isEmpty()) {
                token = "_xfToken";
            }

            String loginPostUrl = joinUrl(profile.getBaseUrl(), "login/login");
            HttpPost postReq = new HttpPost(loginPostUrl);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("login", profile.getUsername()));
            params.add(new BasicNameValuePair("password", profile.getPassword().getClearText()));
            params.add(new BasicNameValuePair("_xfToken", token));
            params.add(new BasicNameValuePair("remember", "1"));
            params.add(new BasicNameValuePair("_xfResponseType", "json"));

            postReq.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            postReq.setHeader("X-Requested-With", "XMLHttpRequest");

            return httpClient.execute(postReq, (postResp) -> {
	                String respBody = EntityUtils.toString(postResp.getEntity(), StandardCharsets.UTF_8);
	                if (respBody.contains("\"status\":\"ok\"") || respBody.contains("\"redirect\"")) {
	                    return LoginResult.success(exportCookies());
	                } else if (respBody.contains("two_factor") || respBody.contains("two-step")) {
	                    return LoginResult.failure("Forum requires two-Factor Authentication. This is not yet supported.");
	                } else if (respBody.contains("errors")) {
	                    try {
	                        JsonObject obj = JsonParser.parseString(respBody).getAsJsonObject();
	                        String errorMsg = obj.has("errors") ? obj.get("errors").toString() : "Authentication failed";
	                        return LoginResult.failure(errorMsg);
	                    } catch (Exception e) {
	                        return LoginResult.failure("Authentication rejected: " + respBody);
	                    }
	                }
	                return LoginResult.success(exportCookies());
	            }
            );
        } catch (Exception e) {
            log.error("Login attempt failed for {}", profile.getBaseUrl(), e);
            return LoginResult.failure("Network error during login: " + e.getMessage());
        }
    }

    public PostSubmissionResult submitReply(@NotNull ForumProfile profile, @NotNull String threadUrl, @NotNull String bbCodeMessage) {
        try {
            // 1. Fetch thread to get action URL and token
            ThreadMetadata metadata = fetchThreadMetadata(threadUrl);
            if (metadata.isLocked()) {
                return PostSubmissionResult.failure("Cannot post: thread is locked");
            }
            if (!metadata.isCanReply()) {
                return PostSubmissionResult.failure("Cannot post: reply permissions not available");
            }

            String xfToken = metadata.getXfToken();
            if (xfToken == null || xfToken.isEmpty()) {
                return PostSubmissionResult.failure("Could not extract CSRF security token (_xfToken) from thread page");
            }

            // Target reply endpoint
            String replyUrl = threadUrl.replaceAll("/+$", "") + "/add-reply";
            HttpPost postReq = new HttpPost(replyUrl);

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("message", bbCodeMessage));
            params.add(new BasicNameValuePair("_xfToken", xfToken));
            params.add(new BasicNameValuePair("_xfResponseType", "json"));
            params.add(new BasicNameValuePair("_xfWithData", "1"));

            postReq.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            postReq.setHeader("X-Requested-With", "XMLHttpRequest");

            return httpClient.execute(postReq, (response) -> {
	                int status = response.getCode();
	                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

	                if (status >= 200 && status < 400) {
	                    if (body.contains("\"status\":\"ok\"") || body.contains("\"redirect\"") || body.contains("\"html\"")) {
	                        return PostSubmissionResult.success("Post successfully submitted to thread");
	                    }
	                    if (body.contains("errors")) {
	                        return PostSubmissionResult.failure("Forum error response: " + body);
	                    }
	                    return PostSubmissionResult.success("Post submitted (HTTP " + status + ")");
	                } else {
	                    return PostSubmissionResult.failure("HTTP error " + status + ": " + body);
	                }
	            }
            );
        } catch (Exception e) {
            log.error("Failed to submit reply to {}", threadUrl, e);
            return PostSubmissionResult.failure("Exception during post submission: " + e.getMessage());
        }
    }

    private String extractCsrfToken(Document doc) {
        Element tokenEl = doc.selectFirst("input[name=_xfToken], input[name=csrf_token]");
        if (tokenEl != null && !tokenEl.val().isEmpty()) {
            return tokenEl.val();
        }
        Element htmlEl = doc.selectFirst("html[data-csrf]");
        if (htmlEl != null && !htmlEl.attr("data-csrf").isEmpty()) {
            return htmlEl.attr("data-csrf");
        }
        return null;
    }

    private String extractThreadId(String url, Document doc) {
        try {
            String path = URI.create(url).getPath();
            String[] parts = path.split("/");
            for (String part : parts) {
                if (part.contains(".")) {
                    return part.substring(part.lastIndexOf('.') + 1);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String joinUrl(String base, String path) {
        if (!base.endsWith("/")) {
			base += "/";
		}
        if (path.startsWith("/")) {
			path = path.substring(1);
		}
        return base + path;
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (Exception ignored) {}
    }

    @lombok.Value
    public static class LoginResult {
        boolean successful;
        String errorMessage;
        Map<String, SecureString> cookies;

        public static LoginResult success(Map<String, SecureString> cookies) {
            return new LoginResult(true, null, cookies);
        }

        public static LoginResult failure(String errorMessage) {
            return new LoginResult(false, errorMessage, Collections.emptyMap());
        }
    }

    @lombok.Value
    public static class PostSubmissionResult {
        boolean successful;
        String message;

        public static PostSubmissionResult success(String message) {
            return new PostSubmissionResult(true, message);
        }

        public static PostSubmissionResult failure(String reason) {
            return new PostSubmissionResult(false, reason);
        }
    }
}
