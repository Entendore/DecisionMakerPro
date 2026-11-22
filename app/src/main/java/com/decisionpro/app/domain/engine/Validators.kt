package com.decisionpro.app.domain.engine

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult() {
        val firstError: String get() = errors.first()
    }
    val isValid: Boolean get() = this is Valid
}

object Validators {

    const val MAX_OPTION_COUNT = 8
    const val MAX_CRITERION_COUNT = 7
    const val MAX_LABEL_LENGTH = 100
    const val MAX_SCORE = 10f
    const val MAX_WEIGHT = 10f

    fun validateOptions(options: List<String>): ValidationResult {
        val errors = mutableListOf<String>()

        if (options.size < 2) errors += "Add at least 2 options"
        if (options.size > MAX_OPTION_COUNT)
            errors += "Maximum $MAX_OPTION_COUNT options"

        val normalized = options.map { it.trim().lowercase() }
        if (normalized.size != normalized.distinct().size)
            errors += "Duplicate option names aren't allowed"

        options.forEachIndexed { index, option ->
            when {
                option.isBlank() -> errors += "Option ${index + 1} is empty"
                option.length > MAX_LABEL_LENGTH ->
                    errors += "Option ${index + 1} is too long (max $MAX_LABEL_LENGTH)"
                option.any { it.isISOControl() } ->
                    errors += "Option ${index + 1} contains invalid characters"
            }
        }

        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    fun validateCriteria(criteria: List<String>): ValidationResult {
        val errors = mutableListOf<String>()
        if (criteria.isEmpty()) errors += "Add at least 1 criterion"
        if (criteria.size > MAX_CRITERION_COUNT)
            errors += "Keep it to $MAX_CRITERION_COUNT criteria"
        criteria.forEachIndexed { index, criterion ->
            if (criterion.isBlank()) errors += "Criterion ${index + 1} is empty"
            if (criterion.length > MAX_LABEL_LENGTH)
                errors += "Criterion ${index + 1} is too long"
        }
        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    fun validateScore(score: Float): ValidationResult =
        if (score.isNaN() || score < 0f || score > MAX_SCORE + 0.01f)
            ValidationResult.Invalid(listOf("Score must be 0–10, was $score"))
        else ValidationResult.Valid

    fun validateWeight(weight: Float): ValidationResult =
        if (weight.isNaN() || weight < 0f || weight > MAX_WEIGHT + 0.01f)
            ValidationResult.Invalid(listOf("Weight must be 0–10, was $weight"))
        else ValidationResult.Valid

    fun validatePayoff(value: Double): ValidationResult = when {
        value.isNaN() || value.isInfinite() ->
            ValidationResult.Invalid(listOf("Payoff must be a finite number"))
        kotlin.math.abs(value) > MAX_PAYOFF ->
            ValidationResult.Invalid(listOf("Payoff too large (max ±$MAX_PAYOFF)"))
        else -> ValidationResult.Valid
    }

    fun validateProbability(p: Double): ValidationResult =
        if (p.isNaN() || p < 0.0 || p > 1.0)
            ValidationResult.Invalid(listOf("Probability must be 0–100%"))
        else ValidationResult.Valid

    fun validateQuestion(text: String): ValidationResult = when {
        text.isBlank() -> ValidationResult.Invalid(listOf("Question can't be empty"))
        text.length > 500 ->
            ValidationResult.Invalid(listOf("Question too long (max 500)"))
        text.any { it.isISOControl() } ->
            ValidationResult.Invalid(listOf("Question contains invalid characters"))
        else -> ValidationResult.Valid
    }

    /** Excel/LibreOffice execute cells starting with = + - @ as formulas. */
    fun sanitizeForCsv(value: String): String =
        if (value.isNotEmpty() && value.first() in "=+-@") "'$value" else value

    private const val MAX_PAYOFF = 1_000_000_000.0
}
