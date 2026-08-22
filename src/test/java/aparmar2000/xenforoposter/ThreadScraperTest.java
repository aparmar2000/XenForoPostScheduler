package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import aparmar2000.xenforoposter.model.ForumProfile;
import aparmar2000.xenforoposter.model.ScrapedThreadData;
import aparmar2000.xenforoposter.model.ThreadMetadata;
import aparmar2000.xenforoposter.model.ThreadPost;
import aparmar2000.xenforoposter.web.XenForoWebClient;

class ThreadScraperTest {

	@Test
	@DisplayName("URL builder should generate correct XenForo page URLs")
	void testBuildPageUrl() {
		try (XenForoWebClient client = new XenForoWebClient()) {
			String base = "https://forum.example.com/threads/my-topic.123/";
			assertEquals("https://forum.example.com/threads/my-topic.123/", client.buildPageUrl(base, 1));
			assertEquals("https://forum.example.com/threads/my-topic.123/page-2", client.buildPageUrl(base, 2));
			assertEquals("https://forum.example.com/threads/my-topic.123/page-15", client.buildPageUrl(base, 15));

			// URL already containing page-2
			String page2Url = "https://forum.example.com/threads/my-topic.123/page-2";
			assertEquals("https://forum.example.com/threads/my-topic.123/", client.buildPageUrl(page2Url, 1));
			assertEquals("https://forum.example.com/threads/my-topic.123/page-5", client.buildPageUrl(page2Url, 5));
		}
	}

	@Test
	@DisplayName("WebClient should parse pagination, thread metadata, and posts from HTML")
	void testParsePaginationAndPosts() {
		String html = """
				<!DOCTYPE html>
				<html>
				<head>
				    <title>Test Discussion Topic | XenForo Community</title>
				</head>
				<body>
				    <h1 class="p-title-value">Test Discussion Topic</h1>
				    <div class="p-description">Started by <a class="username" href="/members/alice.101/">Alice</a></div>
				    <form action="/threads/my-topic.123/add-reply" method="post">
				        <input type="hidden" name="_xfToken" value="sample_csrf_token_123" />
				    </form>
				    <nav class="pageNav">
				        <ul class="pageNav-main">
				            <li class="pageNav-page pageNav-page--current"><a href="/threads/my-topic.123/">1</a></li>
				            <li class="pageNav-page"><a href="/threads/my-topic.123/page-2">2</a></li>
				            <li class="pageNav-page"><a href="/threads/my-topic.123/page-7">7</a></li>
				        </ul>
				    </nav>
				    <article class="message" data-content="post-1001" data-author-id="101">
				        <div class="message-name"><a class="username" href="/members/alice.101/">Alice</a></div>
				        <time data-time="1700000000" class="u-dt">Nov 14, 2023</time>
				        <a class="message-attribution-gadget">#1</a>
				        <div class="message-body bbWrapper">First post in thread!</div>
				    </article>
				    <article class="message" data-content="post-1002" data-author-id="202">
				        <div class="message-name"><a class="username" href="/members/bob.202/">Bob</a></div>
				        <time data-time="1700003600" class="u-dt">Nov 14, 2023</time>
				        <a class="message-attribution-gadget">#2</a>
				        <div class="message-body bbWrapper">Hello Alice, nice topic!</div>
				    </article>
				</body>
				</html>
				""";

		try (XenForoWebClient client = new XenForoWebClient()) {
			Document doc = Jsoup.parse(html, "https://forum.example.com/threads/my-topic.123/");

			ThreadMetadata meta = client.parseThreadMetadata(doc, "https://forum.example.com/threads/my-topic.123/", 200);
			assertEquals("Test Discussion Topic", meta.getTitle());
			assertEquals("Alice", meta.getThreadAuthor());
			assertEquals("sample_csrf_token_123", meta.getXfToken());
			assertTrue(meta.isCanReply());
			assertFalse(meta.isLocked());
			assertEquals(7, meta.getTotalPages());

			List<ThreadPost> posts = client.parsePosts(doc, 1);
			assertEquals(2, posts.size());

			ThreadPost post1 = posts.get(0);
			assertEquals("post-1001", post1.getPostId());
			assertEquals("Alice", post1.getAuthor());
			assertEquals("101", post1.getAuthorId());
			assertEquals(Instant.ofEpochSecond(1700000000), post1.getTimestamp());
			assertEquals(1, post1.getPostNumber());
			assertEquals(1, post1.getPageNumber());
			assertEquals("First post in thread!", post1.getContent());

			ThreadPost post2 = posts.get(1);
			assertEquals("post-1002", post2.getPostId());
			assertEquals("Bob", post2.getAuthor());
			assertEquals("202", post2.getAuthorId());
			assertEquals(Instant.ofEpochSecond(1700003600), post2.getTimestamp());
			assertEquals(2, post2.getPostNumber());
			assertEquals(1, post2.getPageNumber());
			assertEquals("Hello Alice, nice topic!", post2.getContent());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	@DisplayName("fetchThreadData should execute HTTP request using mocked CloseableHttpClient and parse response")
	void testFetchThreadDataWithMockedHttpClient() throws Exception {
		String html = """
				<!DOCTYPE html>
				<html>
				<head><title>Mocked Topic</title></head>
				<body>
				    <h1 class="p-title-value">Mocked Topic</h1>
				    <form action="/threads/mock.1/add-reply" method="post">
				        <input type="hidden" name="_xfToken" value="csrf_val" />
				    </form>
				    <article class="message" data-content="post-500">
				        <div class="message-name"><a>MockUser</a></div>
				        <time data-time="1700000000" class="u-dt"></time>
				        <div class="message-body bbWrapper">Hello Mock</div>
				    </article>
				</body>
				</html>
				""";

		CloseableHttpClient mockHttp = mock(CloseableHttpClient.class);
		doAnswer(invocation -> {
			HttpClientResponseHandler<?> handler = invocation.getArgument(1);
			ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);
			when(mockResponse.getCode()).thenReturn(200);
			when(mockResponse.getEntity()).thenReturn(new StringEntity(html, StandardCharsets.UTF_8));
			return handler.handleResponse(mockResponse);
		}).when(mockHttp).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));

		BasicCookieStore cookieStore = new BasicCookieStore();
		try (XenForoWebClient client = new XenForoWebClient(cookieStore, mockHttp)) {
			ScrapedThreadData data = client.fetchThreadData("https://forum.example.com/threads/mock.1/");
			assertNotNull(data);
			assertEquals("Mocked Topic", data.getMetadata().getTitle());
			assertEquals("csrf_val", data.getMetadata().getXfToken());
			assertEquals(1, data.getAllLoadedPosts().size());
			verify(mockHttp).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	@DisplayName("submitReply should perform fetchThreadMetadata and POST reply using mocked CloseableHttpClient")
	void testSubmitReplyWithMockedHttpClient() throws Exception {
		String threadHtml = """
				<html>
				<body>
				    <h1 class="p-title-value">Reply Thread</h1>
				    <form action="/threads/topic.99/add-reply" method="post">
				        <input type="hidden" name="_xfToken" value="tok_999" />
				    </form>
				</body>
				</html>
				""";

		CloseableHttpClient mockHttp = mock(CloseableHttpClient.class);
		// GET thread metadata
		doAnswer(invocation -> {
			HttpClientResponseHandler<?> handler = invocation.getArgument(1);
			ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);
			when(mockResponse.getCode()).thenReturn(200);
			when(mockResponse.getEntity()).thenReturn(new StringEntity(threadHtml, StandardCharsets.UTF_8));
			return handler.handleResponse(mockResponse);
		}).when(mockHttp).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));

		// POST reply
		doAnswer(invocation -> {
			HttpClientResponseHandler<?> handler = invocation.getArgument(1);
			ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);
			when(mockResponse.getCode()).thenReturn(200);
			when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"status\":\"ok\"}", StandardCharsets.UTF_8));
			return handler.handleResponse(mockResponse);
		}).when(mockHttp).execute(any(HttpPost.class), any(HttpClientResponseHandler.class));

		BasicCookieStore cookieStore = new BasicCookieStore();
		try (XenForoWebClient client = new XenForoWebClient(cookieStore, mockHttp)) {
			ForumProfile profile = ForumProfile.builder()
					.name("Test Forum")
					.baseUrl("https://forum.example.com")
					.username("tester")
					.build();

			XenForoWebClient.PostSubmissionResult result = client.submitReply(
					profile, "https://forum.example.com/threads/topic.99/", "Sample reply body");

			assertTrue(result.isSuccessful());
			verify(mockHttp).execute(any(HttpGet.class), any(HttpClientResponseHandler.class));
			verify(mockHttp).execute(any(HttpPost.class), any(HttpClientResponseHandler.class));
		}
	}

	@Test
	@DisplayName("ScrapedThreadData querying methods should work across pages")
	void testScrapedThreadDataQueries() {
		ThreadMetadata meta = ThreadMetadata.builder()
				.threadUrl("https://forum.example.com/threads/my-topic.123/")
				.title("Topic")
				.canReply(true)
				.totalPages(2)
				.build();

		ThreadPost p1 = ThreadPost.builder().postId("1").author("Alice").timestamp(Instant.ofEpochSecond(100)).pageNumber(1).build();
		ThreadPost p2 = ThreadPost.builder().postId("2").author("Bob").timestamp(Instant.ofEpochSecond(200)).pageNumber(1).build();
		ThreadPost p3 = ThreadPost.builder().postId("3").author("Charlie").timestamp(Instant.ofEpochSecond(300)).pageNumber(2).build();
		ThreadPost p4 = ThreadPost.builder().postId("4").author("Alice").timestamp(Instant.ofEpochSecond(400)).pageNumber(2).build();
		ThreadPost p5 = ThreadPost.builder().postId("5").author("David").timestamp(Instant.ofEpochSecond(500)).pageNumber(2).build();

		ScrapedThreadData data = ScrapedThreadData.builder()
				.metadata(meta)
				.page(1, List.of(p1, p2))
				.page(2, List.of(p3, p4, p5))
				.build();

		assertEquals(2, data.getTotalPages());
		assertTrue(data.hasPage(1));
		assertTrue(data.hasPage(2));
		assertFalse(data.hasPage(3));
		assertEquals(5, data.getAllLoadedPosts().size());

		// Latest post and timestamp
		assertTrue(data.getLatestPost().isPresent());
		assertEquals("5", data.getLatestPost().get().getPostId());
		assertEquals(Instant.ofEpochSecond(500), data.getLatestPostTimestamp().orElse(null));

		// Query by author
		assertTrue(data.getLatestPostByAuthor("Alice").isPresent());
		assertEquals("4", data.getLatestPostByAuthor("Alice").get().getPostId());

		// Post gap count after author
		assertEquals(1, data.countPostsAfterLatestByAuthor("Alice")); // p5 after p4
		assertEquals(0, data.countPostsAfterLatestByAuthor("David")); // none after p5
		assertEquals(5, data.countPostsAfterLatestByAuthor("UnknownUser")); // 5 if user never posted
	}
}
