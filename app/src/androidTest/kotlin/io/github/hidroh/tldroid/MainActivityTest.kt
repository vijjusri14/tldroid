package io.github.hidroh.tldroid

import androidx.appcompat.app.AppCompatActivity
import android.content.ContentValues
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.ComponentNameMatchers.hasClassName
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.CursorMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.web.assertion.WebViewAssertions.webContent
import androidx.test.espresso.web.matcher.DomMatchers.containingTextInBody
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.hidroh.tldroid.test.EspressoHelper.waitForAtMost
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
  @Rule @JvmField
  val intentsRule = CustomTestRule(MainActivity::class.java)

  @Test
  fun testInfo() {
    onView(isRoot()).perform(closeSoftKeyboard())
    onView(withId(R.id.info_button)).perform(click())
    onView(isRoot())
        .inRoot(isDialog())
        .perform(waitForAtMost(1000, withId(R.id.web_view)))
    onWebView(withId(R.id.web_view))
        .forceJavascriptEnabled()
        .check(webContent(containingTextInBody("License")))
    onView(isRoot()).perform(pressBack())
    onView(withId(R.id.web_view))
        .check(doesNotExist())
  }

  @Test
  fun testBrowse() {
    onView(isRoot()).perform(closeSoftKeyboard())
    onView(withId(R.id.list_button)).perform(click())
    onData(CursorMatchers.withRowString("name", `is`("ls")))
        .inRoot(isDialog())
        .check(matches(isDisplayed()))
        .perform(click())
    intended(hasComponent(hasClassName(CommandActivity::class.java.name)))
  }

  @Test
  fun testSearch() {
    onView(withId(R.id.edit_text)).perform(typeText("l"), closeSoftKeyboard())
    onView(withText("ls"))
        .inRoot(withDecorView(not(intentsRule.activity.window.decorView)))
        .check(matches(isDisplayed()))
        .perform(click())
    intended(hasComponent(hasClassName(CommandActivity::class.java.name)))
  }

  @Test
  fun testEmptySearch() {
    onView(withId(R.id.edit_text)).perform(typeText("git"), pressImeActionButton())
    intended(hasComponent(hasClassName(CommandActivity::class.java.name)))
  }

  class CustomTestRule<T : AppCompatActivity>(activityClass: Class<T>?) : IntentsTestRule<T>(activityClass) {
    override fun beforeActivityLaunched() {
      super.beforeActivityLaunched()
      val cv = ContentValues()
      cv.put(TldrProvider.CommandEntry.COLUMN_NAME, "ls")
      cv.put(TldrProvider.CommandEntry.COLUMN_PLATFORM, "common")
      InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
          .insert(TldrProvider.URI_COMMAND, cv);
    }

    override fun afterActivityFinished() {
      super.afterActivityFinished()
      InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
          .delete(TldrProvider.URI_COMMAND, null, null)
    }
  }
}
