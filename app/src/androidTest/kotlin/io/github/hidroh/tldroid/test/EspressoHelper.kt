package io.github.hidroh.tldroid.test

import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import android.view.View
import org.hamcrest.Matcher
import java.util.concurrent.TimeoutException

object EspressoHelper {
  fun waitForAtMost(millis: Long, viewMatcher: Matcher<View>): ViewAction {
    return object: ViewAction {
      override fun getDescription(): String? {
        return "Wait for $viewMatcher for at most $millis millis."
      }

      override fun getConstraints(): Matcher<View>? {
        return isRoot() // allow fall through to custom wait action
      }

      override fun perform(uiController: UiController?, view: View?) {
        var left = millis
        // loop till time out or matched
        while (left > 0) {
          for (child in TreeIterables.breadthFirstViewTraversal(view)) {
            if (viewMatcher.matches(child)) {
              return
            }
          }
          left -= 50
          uiController!!.loopMainThreadForAtLeast(50)
        }
        // time out
        throw PerformException.Builder()
          .withActionDescription(description)
          .withViewDescription(HumanReadables.describe(view))
          .withCause(TimeoutException())
          .build()
      }
    }
  }
}