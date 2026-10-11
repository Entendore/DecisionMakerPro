package com.decisionpro.app.domain.engine.tree

/** Golden-master fixtures: hand-verified EMVs double as analyzer tests. */
object TreeTemplates {

    /** EMV(job) = 0.6×130 + 0.4×40 = 94 > 70. Breakeven vs 70: p* = ⅓. */
    fun jobDecision(): Decision {
        val stay = Outcome(newId(), "Stay put", 70.0)
        return Decision(
            label = "Career decision",
            children = listOf(
                Branch(
                    label = "Take the new job",
                    probability = null,
                    child = Chance(
                        label = "How the startup does",
                        children = listOf(
                            Branch("It takes off", 0.6, Outcome(newId(), "Company thrives", 130.0)),
                            Branch("It struggles", 0.4, Outcome(newId(), "Company struggles", 40.0))
                        )
                    )
                ),
                Branch("Stay at current job", null, stay)
            )
        )
    }

    /** EMV = 0.30×500 + 0.45×150 + 0.25×(−200) = 167.5. Range −200…+500. */
    fun productLaunch(): Decision =
        Decision(
            label = "Product strategy",
            children = listOf(
                Branch(
                    "Launch ourselves", null,
                    Chance(
                        label = "Market reception",
                        children = listOf(
                            Branch("Big hit", 0.30, Outcome(newId(), "Big hit", 500.0)),
                            Branch("Modest", 0.45, Outcome(newId(), "Modest sales", 150.0)),
                            Branch("Flops", 0.25, Outcome(newId(), "Flop", -200.0))
                        )
                    )
                ),
                Branch("License the tech", null, Outcome(newId(), "License deal", 120.0)),
                Branch("Shelve it", null, Outcome(newId(), "Shelved", 0.0))
            )
        )

    private fun newId(): String =
        java.util.UUID.randomUUID().toString().take(8)
}
