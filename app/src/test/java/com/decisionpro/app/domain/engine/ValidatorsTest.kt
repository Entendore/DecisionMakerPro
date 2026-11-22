package com.decisionpro.app.domain.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `valid options pass`() {
        assertThat(
            Validators.validateOptions(listOf("Apartment A", "Apartment B"))
        ).isEqualTo(ValidationResult.Valid)
    }

    @Test
    fun `duplicate options rejected case-insensitively`() {
        val result = Validators.validateOptions(listOf("Cafe", "cafe "))
        val errors = (result as ValidationResult.Invalid).errors.joinToString()
        assertThat(errors).contains("Duplicate")
    }

    @Test
    fun `single option rejected`() {
        assertThat(Validators.validateOptions(listOf("Only")).isValid).isFalse()
    }

    @Test
    fun `score bounds enforced with tolerance`() {
        assertThat(Validators.validateScore(10f).isValid).isTrue()
        assertThat(Validators.validateScore(0f).isValid).isTrue()
        assertThat(Validators.validateScore(-0.1f).isValid).isFalse()
        assertThat(Validators.validateScore(10.05f).isValid).isTrue()
        assertThat(Validators.validateScore(11f).isValid).isFalse()
        assertThat(Validators.validateScore(Float.NaN).isValid).isFalse()
    }

    @Test
    fun `probability bounds enforced`() {
        assertThat(Validators.validateProbability(0.0).isValid).isTrue()
        assertThat(Validators.validateProbability(1.0).isValid).isTrue()
        assertThat(Validators.validateProbability(1.1).isValid).isFalse()
        assertThat(Validators.validateProbability(-0.01).isValid).isFalse()
    }

    @Test
    fun `payoff rejects NaN infinity and huge values`() {
        assertThat(Validators.validatePayoff(-500.0).isValid).isTrue()
        assertThat(Validators.validatePayoff(Double.NaN).isValid).isFalse()
        assertThat(Validators.validatePayoff(Double.POSITIVE_INFINITY).isValid).isFalse()
        assertThat(Validators.validatePayoff(1e12).isValid).isFalse()
    }

    @Test
    fun `csv injection attempts neutralized`() {
        assertThat(Validators.sanitizeForCsv("=HYPERLINK(\"x\")")).startsWith("'")
        assertThat(Validators.sanitizeForCsv("+cmd")).startsWith("'")
        assertThat(Validators.sanitizeForCsv("@import")).startsWith("'")
        assertThat(Validators.sanitizeForCsv("-2+3")).startsWith("'")
        assertThat(Validators.sanitizeForCsv("safe")).isEqualTo("safe")
        assertThat(Validators.sanitizeForCsv("")).isEmpty()
    }
}
