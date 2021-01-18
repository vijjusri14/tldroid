package io.github.hidroh.tldroid

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.ComponentNameMatchers.hasClassName
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions.webContent
import androidx.test.espresso.web.matcher.DomMatchers.containingTextInBody
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommandActivityTest {
  @Rule @JvmField
  val intentsRule = IntentsTestRule<CommandActivity>(CommandActivity::class.java, false, false)

  @Test
  fun testRender() {
    MarkdownProcessor.markdown = "<div>Blah</div>"
    intentsRule.launchActivity(Intent().putExtra(CommandActivity.EXTRA_QUERY, "ls"))
    onWebView(withId(R.id.web_view))
        .forceJavascriptEnabled()
        .check(webContent(containingTextInBody("Blah")))
  }

  @Test
  fun testRunClick() {
    MarkdownProcessor.markdown = "<div>Blah</div>"
    intentsRule.launchActivity(Intent().putExtra(CommandActivity.EXTRA_QUERY, "ls"))
    onView(withId(R.id.menu_run)).perform(click())
    intended(hasComponent(hasClassName(RunActivity::class.java.name)))
  }

  @Test
  fun testHomeClick() {
    intentsRule.launchActivity(Intent().putExtra(CommandActivity.EXTRA_QUERY, "ls"))
    onView(withContentDescription(InstrumentationRegistry.getInstrumentation().targetContext
        .getString(R.string.abc_action_bar_up_description)))
        .perform(click())
    assertThat(intentsRule.activity.isFinishing || intentsRule.activity.isDestroyed)
        .isTrue()
  }

  @Test
  fun testRenderNoContent() {
    MarkdownProcessor.markdown = null
    intentsRule.launchActivity(Intent().putExtra(CommandActivity.EXTRA_QUERY, "ls"))
    onWebView(withId(R.id.web_view))
        .forceJavascriptEnabled()
        .check(webContent(containingTextInBody("This command is not yet available.")))
  }

  @Test
  fun testStateRestoration() {
    MarkdownProcessor.markdown = "<div>Blah</div>"
    intentsRule.launchActivity(Intent().putExtra(CommandActivity.EXTRA_QUERY, "ls"))
    MarkdownProcessor.markdown = ""
    InstrumentationRegistry.getInstrumentation().runOnMainSync { intentsRule.activity.recreate() }
    onWebView(withId(R.id.web_view))
        .forceJavascriptEnabled()
        .check(webContent(containingTextInBody("Blah")))
  }

  @After
  fun tearDown() {
    MarkdownProcessor.markdown = null
  }
}