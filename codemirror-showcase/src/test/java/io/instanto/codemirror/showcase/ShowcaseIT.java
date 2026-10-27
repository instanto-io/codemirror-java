package io.instanto.codemirror.showcase;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ShowcaseIT {
  @Parameterized.Parameters(name = "{0}")
  public static Object[] engines() {
    return new Object[] {"chromium", "webkit"};
  }

  private final String engine;
  private HttpServer server;
  private Playwright playwright;
  private Browser browser;
  private Page page;
  private final List<String> errors = new ArrayList<>();
  private int bundleRequests;

  public ShowcaseIT(String engine) {
    this.engine = engine;
  }

  @Before
  public void start() throws Exception {
    Path site = Path.of("target/site").toAbsolutePath().normalize();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/project/",
        e -> {
          String request = e.getRequestURI().getPath().substring("/project/".length());
          Path file = site.resolve(request.isEmpty() ? "index.html" : request).normalize();
          if (!file.startsWith(site) || !Files.isRegularFile(file)) {
            e.sendResponseHeaders(404, -1);
            e.close();
            return;
          }
          e.getResponseHeaders()
              .set(
                  "Content-Type",
                  file.toString().endsWith(".js")
                      ? "text/javascript"
                      : file.toString().endsWith(".css")
                          ? "text/css"
                          : file.toString().endsWith(".html") ? "text/html" : "text/plain");
          byte[] content = Files.readAllBytes(file);
          e.sendResponseHeaders(200, content.length);
          try (var out = e.getResponseBody()) {
            out.write(content);
          }
        });
    server.start();
    playwright = Playwright.create();
    browser = (engine.equals("webkit") ? playwright.webkit() : playwright.chromium()).launch();
    page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1280, 1050));
    page.onPageError(errors::add);
    page.onRequest(
        r -> {
          if (r.url().endsWith("/codemirror-teavm.js")) bundleRequests++;
          if (!r.url().startsWith("http://127.0.0.1:"))
            errors.add("Unexpected external request: " + r.url());
        });
  }

  private void load() {
    page.navigate("http://127.0.0.1:" + server.getAddress().getPort() + "/project/");
    page.waitForFunction(
        "document.body.dataset.ready === 'true' && document.body.dataset.sharedLoad === 'true'");
  }

  @After
  public void stop() {
    if (browser != null) browser.close();
    if (playwright != null) playwright.close();
    if (server != null) server.stop(0);
  }

  @Test
  public void compiledJavaContracts() {
    load();
    page.evaluate("document.querySelector('#contracts').click()");
    assertEquals("passed", page.locator("#contract-result").textContent());
    assertEquals(1, page.locator(".cm-editor").count());
    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void keyboardHistorySearchCompletionAndLifecycle() {
    load();
    page.locator(".cm-content").click();
    page.keyboard().press("ControlOrMeta+A");
    page.keyboard().insertText("alpha beta alpha");
    page.locator("#demo-actions summary").click();
    page.locator("#save").click();
    assertTrue(page.locator("#status").textContent().contains("16 UTF-16"));
    page.locator("#undo").click();
    assertTrue(text().contains("public class"));
    page.locator("#redo").click();
    assertEquals("alpha beta alpha", text());
    page.locator("#search").click();
    page.locator(".cm-search input[name=search]").fill("beta");
    page.locator(".cm-search button[name=next]").click();
    assertEquals(
        "beta",
        page.evaluate(
            "showcaseView.state.sliceDoc(showcaseView.state.selection.main.from,showcaseView.state.selection.main.to)"));
    page.locator(".cm-search input[name=replace]").fill("gamma");
    page.locator(".cm-search button[name=replace]").click();
    assertEquals("alpha gamma alpha", text());
    page.locator(".cm-search button[name=close]").click();
    page.evaluate("document.querySelector('#fixture').click()");
    page.locator("#complete").click();
    page.waitForSelector(".cm-tooltip-autocomplete");
    assertTrue(page.locator(".cm-tooltip-autocomplete").textContent().contains("println"));
    page.locator(".cm-tooltip-autocomplete li")
        .filter(new Locator.FilterOptions().setHasText("println"))
        .click();
    assertEquals("System.out.println()", text());
    page.locator("#diagnostics").click();
    page.waitForSelector(".cm-lintRange-warning");
    page.locator("#clear").click();
    assertEquals(0, page.locator(".cm-lintRange-warning").count());
    page.locator("#json").click();
    assertEquals("project.json", page.locator("#filename").textContent());
    assertTrue(page.locator(".cm-content span").count() > 0);
    page.locator("#select").click();
    assertEquals(12, ((Number) page.evaluate("showcaseView.state.selection.main.to")).intValue());
    page.locator("#readonly").click();
    String before = text();
    assertEquals("false", page.locator(".cm-content").getAttribute("contenteditable"));
    page.locator(".cm-content").click();
    page.keyboard().insertText("forbidden");
    assertEquals(before, text());
    page.locator("#readonly").click();
    for (int i = 0; i < 3; i++) {
      page.evaluate("() => { window.oldView=showcaseView; }");
      page.locator("#recreate").click();
      assertEquals(true, page.evaluate("oldView !== showcaseView && oldView.destroyed"));
      assertEquals(1, page.locator(".cm-editor").count());
      assertEquals(before, text());
      assertEquals(
          0,
          ((Number) page.evaluate("showcaseView.state.languageDataAt('commentTokens',0).length"))
              .intValue());
    }
    int changes = Integer.parseInt(page.locator("#changes").textContent());
    page.locator(".cm-content").click();
    page.keyboard().insertText("x");
    assertEquals(changes + 1, Integer.parseInt(page.locator("#changes").textContent()));
    assertEquals(1, bundleRequests);
    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void responsiveShowcaseAndLicenses() throws Exception {
    load();
    assertFalse(page.locator("#recreate").isVisible());
    page.locator("#demo-actions summary").click();
    assertTrue(page.locator("#recreate").isVisible());
    page.locator("#demo-actions summary").click();
    assertEquals(1, bundleRequests);
    assertTrue(
        ((Number)
                    page.evaluate(
                        "document.querySelector('.cm-editor').getBoundingClientRect().height"))
                .doubleValue()
            > 300);

    page.screenshot(
        new Page.ScreenshotOptions()
            .setPath(Path.of("target/desktop-" + engine + ".png"))
            .setFullPage(true));
    page.locator("#theme").click();
    assertEquals("light", page.locator("body").getAttribute("data-theme"));
    page.setViewportSize(390, 844);
    page.screenshot(
        new Page.ScreenshotOptions()
            .setPath(Path.of("target/mobile-" + engine + ".png"))
            .setFullPage(true));
    assertEquals(true, page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
    page.locator("a[href='legal/index.html']").click();
    assertEquals("Licenses & attribution", page.locator("h1").textContent());
    for (var link : page.locator("a").all()) {
      String href = link.getAttribute("href");
      if (!href.startsWith("https:")) {
        var response =
            page.request().get(page.url().substring(0, page.url().lastIndexOf('/') + 1) + href);
        assertEquals(href, 200, response.status());
      }
    }
    assertEquals(
        Files.readString(Path.of("../LICENSE")),
        Files.readString(Path.of("target/site/legal/LICENSE")));
    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void missingAssetsProduceHelpfulFailure() {
    page.route(
        "**/codemirror-teavm.js",
        r -> r.fulfill(new Route.FulfillOptions().setStatus(404).setBody("missing")));
    page.navigate("http://127.0.0.1:" + server.getAddress().getPort() + "/project/");
    page.waitForFunction(
        "document.querySelector('#status').textContent.includes('Unable to load editor assets')");
    assertEquals(0, page.locator(".cm-editor").count());
    page.unroute("**/codemirror-teavm.js");
    page.locator("#retry-load").click();
    page.waitForFunction("document.body.dataset.ready === 'true'");
    assertEquals(1, page.locator(".cm-editor").count());
    assertEquals(2, bundleRequests);
    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void incompatibleBundleReportsFailure() {
    page.route(
        "**/codemirror-teavm.js",
        r ->
            r.fulfill(
                new Route.FulfillOptions()
                    .setContentType("text/javascript")
                    .setBody("window.CodeMirrorTea = {apiVersion: 99};")));
    page.navigate("http://127.0.0.1:" + server.getAddress().getPort() + "/project/");
    page.waitForFunction(
        "document.querySelector('#status').textContent.includes('Unable to load editor assets')");
    assertEquals(0, page.locator(".cm-editor").count());
    assertTrue(page.locator("#retry-load").isVisible());
    assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void comparisonWidgetInteractions() {
    load();
    page.locator("#compare-mode").click();
    assertEquals(2, page.locator("#compare .cm-editor").count());
    assertEquals(
        "false", page.locator("#compare .cm-content").first().getAttribute("contenteditable"));
    assertEquals(
        "true", page.locator("#compare .cm-content").last().getAttribute("contenteditable"));
    page.locator("#compare .cm-merge-revert button").first().press("Enter");
    assertTrue(
        page.locator("#compare .cm-content").last().textContent().contains("Hello from TeaVM"));
    page.locator("#reset-compare").click();
    page.locator("#unified").click();
    assertEquals(1, page.locator("#compare .cm-editor").count());
    page.locator("#compare button[name=accept]").first().click();
    page.locator("#compare button[name=reject]").first().click();
    assertEquals(0, page.locator("#compare button[name=accept]").count());
    page.locator("#reset-compare").click();
    page.locator("#compare .cm-content").click();
    page.keyboard().press("ControlOrMeta+A");
    page.keyboard().insertText("// Edited through the merge widget");
    page.locator("#editor-mode").click();
    page.locator("#compare-mode").click();
    assertTrue(
        page.locator("#compare .cm-content")
            .textContent()
            .contains("Edited through the merge widget"));
    page.locator("#reset-compare").click();
    page.locator("#compare-readonly").click();
    assertEquals("false", page.locator("#compare .cm-content").getAttribute("contenteditable"));
    assertEquals(0, page.locator("#compare button[name=accept]").count());
    page.locator("#compare-readonly").click();
    page.locator("#collapse").click();
    page.locator("#compare-theme").click();
    assertEquals(
        "rgb(255, 255, 255)",
        page.locator("#compare .cm-editor").evaluate("e => getComputedStyle(e).backgroundColor"));
    page.locator("#next-change").click();
    page.locator("#previous-change").click();
    for (int i = 0; i < 3; i++) {
      page.locator("#editor-mode").click();
      assertEquals(0, page.locator("#compare .cm-editor").count());
      page.locator("#compare-mode").click();
      assertEquals(1, page.locator("#compare .cm-editor").count());
    }
    page.screenshot(
        new Page.ScreenshotOptions()
            .setPath(Path.of("target/compare-" + engine + ".png"))
            .setFullPage(true));
    page.setViewportSize(390, 844);
    assertEquals(true, page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
    page.locator("#split").click();
    page.locator("#compare .cm-merge-revert button").first().waitFor();
    assertEquals(
        "rgb(32, 41, 55)",
        page.locator("#compare .cm-merge-revert button")
            .first()
            .evaluate("e => getComputedStyle(e).color"));
    assertEquals(true, page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
    page.screenshot(
        new Page.ScreenshotOptions()
            .setPath(Path.of("target/compare-mobile-" + engine + ".png"))
            .setFullPage(true));
    assertEquals(1, bundleRequests);
    assertTrue(errors.toString(), errors.isEmpty());
  }

  private String text() {
    return (String) page.evaluate("showcaseView.state.doc.toString()");
  }
}
