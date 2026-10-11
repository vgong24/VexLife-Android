package com.vextreme.vexlife.android.testing.crawler

import androidx.compose.ui.test.junit4.createComposeRule
import com.vextreme.vexlife.android.testing.journey.VexTestEngine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class VexScreenCrawlerInstrumentedTest(
    private val parameter: VexScreenCrawlerParameter,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun runConfiguredScreenCrawler() {
        when (val selected = parameter) {
            is VexScreenCrawlerParameter.Disabled -> {
                println(
                    "Vex ScreenCrawler disabled: ${selected.reason}",
                )
                return
            }

            is VexScreenCrawlerParameter.Case -> {
                val case = selected.value
                val evidence =
                    VexScreenCrawlerEvidenceObserver(case)

                VexTestEngine(
                    compose = compose,
                    observer = evidence,
                ).run(
                    journey = case.journey,
                    environment = case.environment,
                    caseLabel = case.caseRef,
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            VexScreenCrawlerRunConfig
                .parameters()
                .map { parameter ->
                    arrayOf<Any>(parameter)
                }
    }
}
