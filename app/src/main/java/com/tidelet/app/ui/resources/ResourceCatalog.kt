package com.tidelet.app.ui.resources

import com.tidelet.app.R
import java.util.Locale

data class ResourceLink(
    val titleRes: Int,
    val descriptionRes: Int,
    val url: String,
    val countryFilter: List<String> = emptyList(),
)

enum class ResourceSection(val labelRes: Int) {
    CRISIS(R.string.resources_section_crisis),
    COMMUNITIES(R.string.resources_section_communities),
    READING(R.string.resources_section_reading),
}

object ResourceCatalog {

    fun linksForSection(
        section: ResourceSection,
        country: String = Locale.getDefault().country,
    ): List<ResourceLink> {
        val all = catalog[section] ?: return emptyList()
        return all.filter { it.countryFilter.isEmpty() || country in it.countryFilter }
    }

    private val catalog: Map<ResourceSection, List<ResourceLink>> = mapOf(
        ResourceSection.CRISIS to listOf(
            ResourceLink(
                titleRes = R.string.resources_samhsa_title,
                descriptionRes = R.string.resources_samhsa_desc,
                url = "tel:18006624357",
                countryFilter = listOf("US"),
            ),
            ResourceLink(
                titleRes = R.string.resources_988_title,
                descriptionRes = R.string.resources_988_desc,
                url = "tel:988",
                countryFilter = listOf("US"),
            ),
            ResourceLink(
                titleRes = R.string.resources_samaritans_title,
                descriptionRes = R.string.resources_samaritans_desc,
                url = "tel:116123",
                countryFilter = listOf("GB"),
            ),
            ResourceLink(
                titleRes = R.string.resources_icall_title,
                descriptionRes = R.string.resources_icall_desc,
                url = "tel:9152987821",
                countryFilter = listOf("IN"),
            ),
            ResourceLink(
                titleRes = R.string.resources_lifeline_au_title,
                descriptionRes = R.string.resources_lifeline_au_desc,
                url = "tel:131114",
                countryFilter = listOf("AU"),
            ),
        ),
        ResourceSection.COMMUNITIES to listOf(
            ResourceLink(
                titleRes = R.string.resources_aa_title,
                descriptionRes = R.string.resources_aa_desc,
                url = "https://www.aa.org",
            ),
            ResourceLink(
                titleRes = R.string.resources_mm_title,
                descriptionRes = R.string.resources_mm_desc,
                url = "https://www.moderation.org",
            ),
            ResourceLink(
                titleRes = R.string.resources_smart_title,
                descriptionRes = R.string.resources_smart_desc,
                url = "https://www.smartrecovery.org",
            ),
            ResourceLink(
                titleRes = R.string.resources_stopdrinking_title,
                descriptionRes = R.string.resources_stopdrinking_desc,
                url = "https://www.reddit.com/r/stopdrinking",
            ),
        ),
        ResourceSection.READING to listOf(
            ResourceLink(
                titleRes = R.string.resources_alcohol_explained_title,
                descriptionRes = R.string.resources_alcohol_explained_desc,
                url = "https://alcoholexplained.com",
            ),
            ResourceLink(
                titleRes = R.string.resources_niaaa_rethinking_title,
                descriptionRes = R.string.resources_niaaa_rethinking_desc,
                url = "https://rethinkingdrinking.niaaa.nih.gov",
            ),
            ResourceLink(
                titleRes = R.string.resources_niaaa_navigator_title,
                descriptionRes = R.string.resources_niaaa_navigator_desc,
                url = "https://alcoholtreatment.niaaa.nih.gov",
            ),
            ResourceLink(
                titleRes = R.string.resources_naked_mind_title,
                descriptionRes = R.string.resources_naked_mind_desc,
                url = "https://thisnakedmind.com",
            ),
        ),
    )

    fun needsFallbackNote(country: String = Locale.getDefault().country): Boolean =
        country !in listOf("US", "GB", "IN", "AU")
}
