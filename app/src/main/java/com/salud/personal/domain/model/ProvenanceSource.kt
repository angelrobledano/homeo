package com.salud.personal.domain.model

enum class ProvenanceSource(
    val identifier: String,
    val displayName: String
) {
    RENPHO_ELIS_1("RENPHO_ELIS_1", "RENPHO Elis 1"),
    AMAZFIT_GTR_3("AMAZFIT_GTR_3", "Amazfit GTR 3"),
    MANUAL_INPUT("MANUAL_INPUT", "Manual"),
    HEALTH_CONNECT("HEALTH_CONNECT", "Health Connect")
}
