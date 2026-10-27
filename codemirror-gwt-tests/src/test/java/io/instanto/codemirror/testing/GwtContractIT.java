package io.instanto.codemirror.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;

public class GwtContractIT {
  @Test
  public void sharedApiRunsOnGwtInBothBrowsers() throws Exception {
    Path site = Path.of("target/site").toAbsolutePath();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          Path file =
              site.resolve(
                      exchange.getRequestURI().getPath().equals("/")
                          ? "index.html"
                          : exchange.getRequestURI().getPath().substring(1))
                  .normalize();
          if (!file.startsWith(site) || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
          }
          byte[] data = Files.readAllBytes(file);
          exchange
              .getResponseHeaders()
              .set(
                  "Content-Type",
                  file.toString().endsWith(".js") ? "text/javascript" : "text/html");
          exchange.sendResponseHeaders(200, data.length);
          try (var out = exchange.getResponseBody()) {
            out.write(data);
          }
        });
    server.start();
    try (var pw = Playwright.create()) {
      for (var engine : List.of(pw.chromium(), pw.webkit()))
        try (var browser = engine.launch()) {
          var page = browser.newPage();
          List<String> errors = new ArrayList<>();
          page.onPageError(errors::add);
          int[] requests = {0};
          page.onRequest(
              r -> {
                if (r.url().endsWith("/codemirror-teavm.js?gwt-contract")) requests[0]++;
              });
          page.navigate("http://127.0.0.1:" + server.getAddress().getPort() + "/");
          page.waitForFunction(
              "document.body.dataset.ready==='true' || document.body.dataset.error");
          assertNull(page.locator("body").getAttribute("data-error"));
          page.waitForFunction("document.body.dataset.sharedLoad === 'true'");
          assertEquals(1, requests[0]);
          page.waitForSelector(".cm-tooltip-autocomplete");
          page.locator(".cm-tooltip-autocomplete li")
              .filter(new Locator.FilterOptions().setHasText("println"))
              .click();
          assertTrue(page.locator(".cm-content").textContent().contains("System.out.println()"));
          page.locator("#dispose").click();
          assertEquals("true", page.locator("body").getAttribute("data-disposed"));
          FeatureBrowserChecks.run(page);
          assertTrue(errors.toString(), errors.isEmpty());
        }
    } finally {
      server.stop(0);
    }
  }
}
