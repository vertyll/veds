package com.vertyll.veds.shared.messaging.config

import org.springframework.boot.SpringApplication
import org.springframework.core.env.StandardEnvironment
import kotlin.test.Test
import kotlin.test.assertNotEquals

class SharedMessagingEnvironmentPostProcessorTest {
    private fun sourcesWith(vararg profiles: String) =
        StandardEnvironment()
            .apply {
                setActiveProfiles(*profiles)
                SharedMessagingEnvironmentPostProcessor().postProcessEnvironment(this, SpringApplication())
            }.propertySources
            .map { it.name }

    @Test
    fun `the local fallbacks load only on the development profiles`() {
        assertNotEquals(sourcesWith("prod").size, sourcesWith("test").size)
    }
}
