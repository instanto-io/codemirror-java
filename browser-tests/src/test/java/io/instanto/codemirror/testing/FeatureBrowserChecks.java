package io.instanto.codemirror.testing;

import static org.junit.Assert.*;

import com.microsoft.playwright.*;

/** The same UI assertions exercise compiled GWT and TeaVM callbacks. */
public final class FeatureBrowserChecks {
  private FeatureBrowserChecks() {}

  public static void run(Page page) {
    page.evaluate("document.querySelector('#feature-launcher').click()");
    Locator fixture = page.locator("#feature-fixture");
    page.locator("#feature-start").click();
    page.waitForFunction("Number(document.querySelector('#feature-fixture').dataset.requests) > 0");
    page.evaluate("document.querySelector('#feature-deliver').click()");
    page.waitForSelector("#feature-editor .cm-tooltip-autocomplete");
    page.waitForSelector("#feature-editor .cm-completionInfo");
    assertTrue(
        page.locator("#feature-editor .cm-completionInfo")
            .textContent()
            .contains("<b>literally</b>"));
    assertEquals(0, page.locator("#feature-editor .cm-completionInfo b").count());
    page.locator("#feature-editor .cm-tooltip-autocomplete li").first().click();
    assertTrue(
        page.locator("#feature-editor .cm-content")
            .textContent()
            .contains("System.out.println(value);"));
    assertEquals("value", fixture.getAttribute("data-selection"));
    page.keyboard().press("Tab");
    assertEquals("", fixture.getAttribute("data-selection"));

    // Wait for a request to start, then edit before it delivers its result.
    int requests = Integer.parseInt(fixture.getAttribute("data-requests"));
    page.locator("#feature-start").click();
    page.waitForFunction(
        "n => Number(document.querySelector('#feature-fixture').dataset.requests) > n", requests);
    int finished = Integer.parseInt(fixture.getAttribute("data-finished"));
    page.locator("#feature-edit").click();
    page.evaluate("document.querySelector('#feature-deliver').click()");
    page.waitForFunction(
        "n => Number(document.querySelector('#feature-fixture').dataset.finished) > n", finished);
    assertTrue(Integer.parseInt(fixture.getAttribute("data-cancelled")) > 0);
    assertEquals("changed", page.locator("#feature-editor .cm-content").textContent());
    assertEquals(0, page.locator("#feature-editor .cm-tooltip-autocomplete").count());

    // Detach with work pending; a late response must not touch the new view.
    requests = Integer.parseInt(fixture.getAttribute("data-requests"));
    page.locator("#feature-start").click();
    page.waitForFunction(
        "n => Number(document.querySelector('#feature-fixture').dataset.requests) > n", requests);
    finished = Integer.parseInt(fixture.getAttribute("data-finished"));
    page.locator("#feature-detach").click();
    page.locator("#feature-attach").click();
    page.evaluate("document.querySelector('#feature-deliver').click()");
    page.waitForFunction(
        "n => Number(document.querySelector('#feature-fixture').dataset.finished) > n", finished);
    assertEquals(0, page.locator("#feature-editor .cm-tooltip-autocomplete").count());
    page.locator("#feature-editor .cm-content").click();
    page.keyboard().press("F8");
    assertEquals("pri", fixture.getAttribute("data-selection"));
    page.keyboard().press("F9");
    assertEquals("1", fixture.getAttribute("data-keys"));

    requests = Integer.parseInt(fixture.getAttribute("data-requests"));
    page.locator("#feature-start").click();
    page.waitForFunction(
        "n => Number(document.querySelector('#feature-fixture').dataset.requests) > n", requests);
    finished = Integer.parseInt(fixture.getAttribute("data-finished"));
    page.locator("#feature-replace-source").click();
    page.evaluate("document.querySelector('#feature-deliver').click()");
    page.waitForFunction(
        "n => Number(document.querySelector('#feature-fixture').dataset.finished) > n", finished);
    assertEquals(0, page.locator("#feature-editor .cm-tooltip-autocomplete").count());

    page.locator("#feature-diagnostic").click();
    page.locator("#feature-editor .cm-lintRange-error").hover();
    page.locator("#feature-editor .cm-diagnosticAction").click();
    assertEquals("good", page.locator("#feature-editor .cm-content").textContent());
    assertEquals(0, page.locator("#feature-editor .cm-lintRange-error").count());
    page.locator("#feature-presentation").click();
    assertEquals(
        "rgb(240, 244, 255)",
        page.locator("#feature-editor .cm-editor")
            .evaluate("e => getComputedStyle(e).backgroundColor"));
    assertEquals(
        "rgb(255, 255, 0)",
        page.locator("#feature-editor .cm-javaHighlight")
            .evaluate("e => getComputedStyle(e).backgroundColor"));
    assertTrue(page.locator("#feature-editor .cm-javaMarkers").textContent().contains("●"));
    page.locator("#feature-editor .cm-javaHighlight").hover();
    page.waitForSelector("#feature-editor .cm-javaHover");
    assertEquals(
        "Helpful <b>plain text</b>", page.locator("#feature-editor .cm-javaHover").textContent());
    assertEquals(0, page.locator("#feature-editor .cm-javaHover b").count());
    page.locator("#feature-detach").click();
    page.locator("#feature-attach").click();
    assertEquals(
        "rgb(240, 244, 255)",
        page.locator("#feature-editor .cm-editor")
            .evaluate("e => getComputedStyle(e).backgroundColor"));
  }
}
