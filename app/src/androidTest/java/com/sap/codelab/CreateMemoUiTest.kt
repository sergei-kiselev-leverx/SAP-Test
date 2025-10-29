package com.sap.codelab

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.textfield.TextInputLayout
import com.sap.codelab.model.MemoLocation
import com.sap.codelab.utils.permissions.isAllPermissionsGranted
import com.sap.codelab.utils.permissions.isPostNotificationsGranted
import com.sap.codelab.view.create.CreateMemoActivity
import com.sap.codelab.view.location.ChooseLocationActivity
import com.sap.codelab.view.location.ChooseLocationContract
import com.sap.codelab.view.location.ChooseLocationResult
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After

@RunWith(AndroidJUnit4::class)
class CreateMemoUiTest {

    @get:Rule(order = 0)
    val intentsRule = IntentsRule()

    @Before
    fun setUp() {
        mockkStatic("com.sap.codelab.utils.permissions.PermissionsUtilsKt")
        mockkStatic(ContextCompat::class)

        every { any<Context>().isAllPermissionsGranted() } returns true
        every { any<Context>().isAllPermissionsGranted() } returns true
        every { any<Context>().isPostNotificationsGranted() } returns true

        every { ContextCompat.startForegroundService(any(), any()) } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic("com.sap.codelab.utils.permissions.PermissionsUtilsKt")
    }

    /**
     * 1) Basic validation: empty fields → should show errors for Title/Text and a message for location.
     */
    @Test
    fun save_withEmptyFields_showsErrors() {
        ActivityScenario.launch(CreateMemoActivity::class.java).use {
            // Click Save
            onView(withId(R.id.action_save)).perform(click())

            // Check Title error
            onView(withId(R.id.memo_title_container))
                .check(matches(hasTextInputLayoutErrorText(getString(R.string.memo_title_empty_error))))

            // Check Description error
            onView(withId(R.id.memo_description_container))
                .check(matches(hasTextInputLayoutErrorText(getString(R.string.memo_text_empty_error))))

            // Check location block error text
            onView(withId(R.id.memo_location_container))
                .check(matches(hasTextInputLayoutErrorText(getString(R.string.memo_location_empty_error))))
        }
    }

    /**
     * 2) When Title and Description are filled but no location is selected → only location error should appear.
     */
    @Test
    fun save_withoutLocation_showsOnlyLocationError() {
        ActivityScenario.launch(CreateMemoActivity::class.java).use {
            onView(withId(R.id.memo_title)).perform(replaceText("Test title"), closeSoftKeyboard())
            onView(withId(R.id.memo_description)).perform(
                replaceText("Test description"),
                closeSoftKeyboard()
            )

            onView(withId(R.id.action_save)).perform(click())

            onView(withId(R.id.memo_title_container))
                .check(matches(hasTextInputLayoutErrorText("")))
            onView(withId(R.id.memo_description_container))
                .check(matches(hasTextInputLayoutErrorText("")))
            onView(withId(R.id.memo_location_container))
                .check(matches(hasTextInputLayoutErrorText(getString(R.string.memo_location_empty_error))))
        }
    }

    /**
     * 3) Select location via Activity Result API, input valid data and save → should finish with RESULT_OK.
     *    Use launchActivityForResult() here so we can read resultCode.
     */
    @Test
    fun save_afterChoosingLocation_finishesWithOk() {
        // 1) Prepare intent stub for choosing location before performing any actions
        Intents.intending(hasComponent(ChooseLocationActivity::class.java.name))
            .respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    ChooseLocationContract.createResult(
                        ChooseLocationResult(MemoLocation(41.6413, 41.6359))
                    )
                )
            )

        // 2) Launch Activity for result
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, CreateMemoActivity::class.java)
        val scenario = ActivityScenario.launchActivityForResult<CreateMemoActivity>(intent)

        // 3) Input data and choose location
        onView(withId(R.id.memo_title)).perform(replaceText("Test title"), closeSoftKeyboard())
        onView(withId(R.id.memo_description)).perform(
            replaceText("Test description"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.memo_location)).perform(click())

        // Check that coordinates are displayed
        onView(withId(R.id.memo_location))
            .check(matches(withText("41.6413, 41.6359")))

        // 4) Save memo and check RESULT_OK
        onView(withId(R.id.action_save)).perform(click())
        val result = scenario.result
        Assert.assertEquals(Activity.RESULT_OK, result.resultCode)
    }

    /**
     * 4) Reopening location picker should replace coordinates with new ones.
     */
    @Test
    fun chooseLocation_multipleTimes_updatesText() {
        val first = ChooseLocationContract.createResult(
            ChooseLocationResult(MemoLocation(41.0, 41.1))
        )
        val second = ChooseLocationContract.createResult(
            ChooseLocationResult(MemoLocation(42.2, 42.3))
        )

        // First response: initial coordinates
        Intents.intending(hasComponent(ChooseLocationActivity::class.java.name))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, first))

        ActivityScenario.launch(CreateMemoActivity::class.java).use {
            onView(withId(R.id.memo_location)).perform(click())
            onView(withId(R.id.memo_location))
                .check(matches(withText("41.0, 41.1")))

            // Now replace with the second coordinates
            Intents.intending(hasComponent(ChooseLocationActivity::class.java.name))
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, second))

            onView(withId(R.id.memo_location)).perform(click())
            onView(withId(R.id.memo_location))
                .check(matches(withText("42.2, 42.3")))
        }
    }

    private fun getString(resId: Int): String =
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(resId)

    private fun hasTextInputLayoutErrorText(expected: String): Matcher<in android.view.View> {
        return object : TypeSafeMatcher<android.view.View>() {
            override fun describeTo(description: Description) {
                description.appendText("with TextInputLayout error text: $expected")
            }

            override fun matchesSafely(view: android.view.View): Boolean {
                if (view !is TextInputLayout) return false
                val error = view.error?.toString() ?: ""
                return error == expected
            }
        }
    }
}