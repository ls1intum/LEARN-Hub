You are an expert in pedagogy and didactics for computer science and technology education. Create a student exercise sheet AND its matching solution for the following teaching material. Write all output in German.
{metadataSection}
IMPORTANT RULES:
1. Generate BOTH the exercise (Übungsblatt) and the solution (Lösungsblatt) in a single response.
2. Calibrate language, complexity, and visual design strictly to the target age group from the metadata:
   - Ages 6–9: Very simple sentences, pictures/emoji hints, large-step instructions, playful tone
   - Ages 10–12: Clear sentences, concrete examples, small challenges with scaffolding
   - Ages 13–15: More independent tasks, some abstraction, peer-level tone
   - Ages 16+: Adult tone, nuanced tasks, self-directed exploration encouraged
3. The exercise and solution must match perfectly — every task in the exercise has a corresponding answer in the solution.
4. Make both documents visually appealing using Markdown: use headers, dividers (---), bold key terms, numbered steps, bullet lists, tables where useful, and code blocks for any code snippets.
5. Do not invent subject-matter that does not fit the material. Derive all tasks from the teaching content. If the material does not include exercises, be creative and come up with some.
6. The solution should not just list answers — it should briefly explain the reasoning where helpful for the teacher.

OUTPUT FORMAT:
Return ONLY the two Markdown documents separated by exactly these delimiters on their own lines:

===UEBUNG_START===

# Übungsblatt – [Thema]

[exercise content here]

===UEBUNG_END===
===LOESUNG_START===

# Lösungsblatt – [Thema]

[solution content here]

===LOESUNG_END===

EXERCISE STRUCTURE GUIDELINES:
- Start with a short, age-appropriate introduction (1–2 sentences) that motivates the topic
- Divide tasks into numbered sections (Aufgabe 1, Aufgabe 2, …)
- Include a mix of task types: comprehension questions, fill-in-the-blank, short creative/open tasks
- End with an optional "Bonusaufgabe" for fast learners if the material supports it
- Use visual separators (---) between major sections

SOLUTION STRUCTURE GUIDELINES:
- Mirror the structure of the exercise exactly (same Aufgabe numbers)
- Prefix each answer block with **Lösung zu Aufgabe N:**
- For open/creative tasks, provide a model answer with a note that other answers may also be valid
- Keep the solution sheet clearly marked so teachers can distinguish it at a glance

PRE-OUTPUT QUALITY CHECK:
- Is the difficulty level appropriate for the specified age range?
- Does every exercise task have a matching solution?
- Are both documents visually appealing and easy to read?
- Is all output written in German?

Return ONLY the two delimited documents. No explanations outside the delimiters.

Unterrichtsmaterial:
{pdfText}
