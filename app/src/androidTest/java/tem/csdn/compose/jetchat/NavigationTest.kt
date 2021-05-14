package tem.csdn.compose.jetchat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Checks that the navigation flows in the app are correct.
 */
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<NavActivity>()

    @Test
    fun app_launches() {
        // Check app launches at the correct destination
        assertEquals(getNavController().currentDestination?.id, R.id.nav_home)
    }

    @Test
    @Ignore("Issue with keyboard sync https://issuetracker.google.com/169235317")
    fun profileScreen_back_conversationScreen() {
        val navController = getNavController()
        // Navigate to profile
        composeTestRule.runOnUiThread {
            navController.navigate(R.id.nav_profile)
        }
        // Check profile is displayed
        assertEquals(navController.currentDestination?.id, R.id.nav_profile)
        // Extra UI check
        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(R.string.display_name))
            .assertIsDisplayed()

        // Press back
        Espresso.pressBack()

        // Check that we're home
        assertEquals(navController.currentDestination?.id, R.id.nav_home)
    }

    private fun getNavController(): NavController {
        return composeTestRule.activity.findNavController(R.id.nav_host_fragment)
    }
}
