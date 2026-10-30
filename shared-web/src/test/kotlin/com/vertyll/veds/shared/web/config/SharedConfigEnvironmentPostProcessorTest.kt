package com.vertyll.veds.shared.web.config

import org.springframework.boot.SpringApplication
import org.springframework.core.env.StandardEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SharedConfigEnvironmentPostProcessorTest {
    private fun environmentWith(vararg profiles: String) =
        StandardEnvironment().apply {
            setActiveProfiles(*profiles)
            SharedConfigEnvironmentPostProcessor().postProcessEnvironment(this, SpringApplication())
        }

    @Test
    fun `the local fallbacks apply on the test profile`() {
        assertEquals("veds", environmentWith("test").getProperty(REALM))
    }

    @Test
    fun `every other profile gets the defaults but no local fallback`() {
        val environment = environmentWith("prod")

        assertNotNull(environment.getProperty("spring.mvc.problemdetails.enabled"))
        assertFalse(environment.propertySources.contains(LOCAL_FALLBACKS))
    }

    private companion object {
        const val REALM = "application.shared.keycloak.realm"
        const val LOCAL_FALLBACKS = "shared-web-config-local"
    }
}
