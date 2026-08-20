package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.StudyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Study Timetable", appName)
    }

    @Test
    fun `admin pin constant is 9044`() {
        assertEquals("9044", StudyViewModel.ADMIN_SECURITY_PIN)
    }
}
