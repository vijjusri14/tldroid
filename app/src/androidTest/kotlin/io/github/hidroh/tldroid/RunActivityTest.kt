package io.github.hidroh.tldroid

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunActivityTest {
  @Rule @JvmField
  val intentsRule = IntentsTestRule<RunActivity>(RunActivity::class.java, false, false)

  @Test
  fun testRunStdOut() {
    intentsRule.launchActivity(Intent().putExtra(RunActivity.EXTRA_COMMAND, "ls"))
    onView(withId(R.id.edit_text)).perform(pressImeActionButton())
    onView(isRoot()).perform(closeSoftKeyboard())
    onView(withId(R.id.output)).check(matches(isDisplayed()))
    onView(withId(R.id.error)).check(matches(not(isDisplayed())))
  }

  @Test
  fun testRunStdErr() {
    intentsRule.launchActivity(Intent().putExtra(RunActivity.EXTRA_COMMAND, "ls"))
    onView(withId(R.id.edit_text)).perform(typeText("-la"), pressImeActionButton())
    onView(isRoot()).perform(closeSoftKeyboard())
    onView(withId(R.id.output)).check(matches(not(isDisplayed())))
    onView(withId(R.id.error)).check(matches(isDisplayed()))
  }

  @Test
  fun testStateRestoration() {
    intentsRule.launchActivity(Intent().putExtra(RunActivity.EXTRA_COMMAND, "ls"))
    onView(withId(R.id.edit_text)).perform(pressImeActionButton())
    onView(isRoot()).perform(closeSoftKeyboard())
    InstrumentationRegistry.getInstrumentation().runOnMainSync { intentsRule.activity.recreate() }
    onView(withId(R.id.output)).check(matches(isDisplayed()))
  }
}