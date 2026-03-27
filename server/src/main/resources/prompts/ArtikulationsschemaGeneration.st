You are an expert in pedagogy and lesson planning. Analyze the following Unterrichtsmaterial and transform it into a complete, didactically coherent Artikulationsschema following the AVIVA+ model. Write all output in German.
{metadataSection}
IMPORTANT RULES:
1. Always treat the material as raw input for a complete AVIVA+ schema. Your goal is not mere extraction but didactic normalization into the required structure.
2. Each AVIVA+ phase gets EXACTLY ONE row in the table — no more, no less. Compress all content belonging to a phase into that single row.
3. Distinguish between two cases:
   - Detaillierte Vorlage: If the material already contains a clear Ablauf, Zeiten, Aufgaben, Hintergrundinformationen, or its own Phasenstruktur, incorporate all relevant content. Map it onto the six AVIVA+ phases without losing key steps — compress rather than skip.
   - Skizzenhafte Vorlage: If the material only provides a rough idea or loose elements, creatively fill in missing didactic steps so a complete, classroom-ready Ablauf results.
4. Do not invent new subject-matter core topics that do not fit the material. You may add methodological steps, transitions, activations, consolidations, reflection prompts, differentiation, and concrete Lehrkraft/Lernenden actions as needed.
5. If the material contains a structured Stundenverlauf, Zeitplan, or Arbeitsphasen, use it as the backbone. Map each piece of content to the appropriate AVIVA+ phase.
6. Integrate subject content, Hintergrundwissen, tasks, and notes visibly into Handlungsschritte, Kompetenzen, and Materialien. Condense only repetitions or purely administrative details.
7. Use the AVIVA+ Phasenmodell with ALL 6 phases (all are mandatory):
   (+) Lernatmosphäre schaffen – Creating trust and a positive learning environment.
   (A) Ankommen und Ausrichten – Establishing relevance, communicating Lernziele and Ablauf.
   (V) Vorwissen aktivieren – Activating prior knowledge to connect new content with existing knowledge.
   (I) Informieren – Presenting new content as the basis for Kompetenzaufbau.
   (V) Verarbeiten – Applying, deepening, and practicing to consolidate learning.
   (A) Auswerten – Assessing Lernerfolg and reflecting on the teaching-learning process.
8. Be detailed and concrete in describing Handlungsschritte. Make visible in each row:
   - what the Lehrkraft does
   - what the Lernenden do
   - the expected result, product, or discussion
9. If Zeiten are given, use them. If not, estimate sensible Zeitfenster so the total is plausible for the overall Dauer.
10. If information is missing, write concrete, realistic Unterrichtsschritte rather than vague placeholders.

OUTPUT FORMAT:
Return ONLY a Markdown document with exactly this structure:

# Artikulationsschema

**Thema:** [Thema derived from the material]
**Klassenstufe:** [Klassenstufe/Alter if mentioned, otherwise "k.A."]
**Dauer:** [Total Dauer in minutes if mentioned, otherwise estimate]

| Zeit | Phase | Handlungsschritte | Sozialform | Kompetenzen | Medien/Material |
|------|-------|-------------------|------------|-------------|-----------------|
| ... | (+) Lernatmosphäre schaffen | ... | ... | ... | ... |
| ... | (A) Ankommen / Ausrichten | ... | ... | ... | ... |
| ... | (V) Vorwissen aktivieren | ... | ... | ... | ... |
| ... | (I) Informieren | ... | ... | ... | ... |
| ... | (V) Verarbeiten | ... | ... | ... | ... |
| ... | (A) Auswerten | ... | ... | ... | ... |

COLUMN GUIDELINES:
- Zeit: Duration of the phase (e.g. "5 min", "15 min")
- Phase: One of the six AVIVA+ phases exactly as shown above
- Handlungsschritte: Concrete Lehrer- und Schüleraktivitäten. For thin materials, add missing steps; for rich materials, compress all relevant content into the single row in logical sequence.
- Sozialform: e.g. Plenum, Einzelarbeit, Partnerarbeit, Gruppenarbeit
- Kompetenzen: Target Lernziele/Kompetenzen derived from material and Metadaten; make them as concrete as possible.
- Medien/Material: Describe required materials concretely (e.g. "Arbeitsblatt zu Algorithmen", "Stifte und Papier", "Laptop mit Internetzugang"). Do NOT reference page numbers or document sections.

PRE-OUTPUT QUALITY CHECK:
- Are all 6 AVIVA+ phases present, each with exactly one row (6 rows total)?
- Is each row concrete and classroom-ready, free of empty platitudes?
- Is all output written in German?

Return ONLY the Markdown. No explanations, no code block wrappers.

Unterrichtsmaterial:
{pdfText}
