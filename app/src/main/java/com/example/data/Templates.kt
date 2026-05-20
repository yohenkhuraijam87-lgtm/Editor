package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object Templates {

    fun getDefaultBlocks(templateType: String, userName: String = "John Doe"): List<ContentBlock> {
        val blocks = mutableListOf<ContentBlock>()
        when (templateType.uppercase()) {
            "RESUME" -> {
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_1", userName))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "ALIGN_CENTER", "Email: john.doe@email.com | Phone: (555) 019-2834 | LinkedIn: linkedin.com/in/johndoe"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Professional Summary"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "Highly motivated professional content designer and document editor specialist with 5+ years of experience drafting high-impact essays and business summaries. Expert in structured layout systems, publishing standards, and proofreading."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Professional Experience"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Lead Document Analyst — WriteCorp (2022 - Present)"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "BULLET_LIST", "Managed rich typography alignments and structure reviews for over 200 corporate journals."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "BULLET_LIST", "Redefined document visual architecture strategies, lowering layout errors by 40%."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Junior Content Coordinator — EditTech (2020 - 2022)"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "BULLET_LIST", "Drafted clean templates, user manuals, and client-facing summaries under rigorous time bounds."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Education"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "Bachelor of Arts in English and Professional Editing - State University (2016 - 2020)"))
            }
            "ESSAY" -> {
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_1", "The Architecture of Linear Layout Structures"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "ALIGN_CENTER", "A Study on Typography Pairings and Visual Rhythm"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "ALIGN_CENTER", "By Academic Specialist & Writer"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Abstract"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "This research explores formatting alignments and the ergonomics of reading mobile documents. By analyzing the optimal proportions of the A4 layout (1 to 1.414 ratio), we evaluate the visual fatigue rates of students during high-concentration deep focus states on mobile reading devices versus paper documents."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Introduction"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "The advancement of portable computers has shifted reader expectations. While traditional books are bound to dynamic line counts, professional documents are formatted to standard dimensions. A4 represents the gold standard for institutional distribution. This paper tests if mobile-first responsive scaling can deliver the exact mental focus equivalent of the physical paper page."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "References"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "[1] L. Bernstein, 'Visual Typography Dynamics', Journal of Screen Studies, vol. 12, pp. 45-89, 2024."))
            }
            "LETTER" -> {
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "May 20, 2026"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "Hiring Committee\nDocument Design Inc.\n100 Creative Avenue"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Subject: Application for Document Design Specialist Position"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "Dear Hiring Committee,"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "I am writing to express my enthusiastic interest in the Document Design Specialist opening at your company. With a deep admiration for your team's contributions to layout precision and my robust technical background in typesetting, I am confident I would be a stellar addition to your roster."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "In my prior roles, I successfully developed standard formatting frameworks for professional whitepapers and customized brand guidelines. Having spent years refining line bounds, spacing grids, and typographic pairings, I understand how critical visual layout is to effective material comprehension."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "Thank you sincerely for your valuable time and consideration. I would be thrilled to discuss my alignment with this role in more detail during an interview."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", "Sincerely,"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", userName))
            }
            "NOTES" -> {
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_1", "Focus Session: Project Milestones"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Key Takeaways"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "BULLET_LIST", "Design clean minimalist layouts that avoid complex visual clutter."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "BULLET_LIST", "Prioritize responsive container widths on larger screen tablets."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "BULLET_LIST", "Review grammar suggestions from the intelligent editor with a single click."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "HEADING_2", "Immediate Action Items"))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "BULLET_LIST", "Set up local Room Database persistence models."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "BULLET_LIST", "Incorporate Material 3 adaptive color schemes and responsive font sizing."))
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "BULLET_LIST", "Add PDF print capabilities.") )
            }
            else -> {
                blocks.add(ContentBlock(UUID.randomUUID().toString(), "PARAGRAPH", ""))
            }
        }
        return blocks
    }
}
