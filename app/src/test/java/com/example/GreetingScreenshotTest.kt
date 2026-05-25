package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.material3.Text
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { 
      MyApplicationTheme { 
        Text("Study Echo Applet Simulator Live Preview Screenshot")
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun app_startup_screenshot() {
    val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodels.StudyViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.MainStudyDashboard(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/app_startup.png")
  }
}
