# Evaluation

## Core Question

> Can LEARN-Hub process realistic K-12 computer science OER inputs and generate structurally complete and plausible teaching packages?

## Dataset

The evaluation dataset is a curated subset of the source PDFs in [`../pdfs`](../pdfs). It is used to validate the robustness and quality of LEARN-Hub content generation against a fixed set of criteria.

To keep the evaluation realistic, the subset covers multiple source-document profiles:

| Category | Description |
|---|---|
| `complete` | Source PDFs contain the expected material and should support complete teaching-package generation. |
| `incomplete` | Source PDFs reference missing or external material, testing how generation handles gaps. |
| `long` | Source PDFs are internally consistent but contain longer descriptions. |
| `short` | Source PDFs are internally consistent but contain shorter descriptions. |

## Selected PDFs

| Filename | Category |
|---|---|
| LEARN-Kit-algorithmen_und_roboter.pdf | `complete` |
| LEARN-Kit-netzwerk.pdf | `complete` |
| barefoot-howcomputerslearn.pdf | `incomplete` |
| csunplugged-colourbynumbers.pdf | `incomplete` |
| codeorg-artist.pdf | `long` |
| learnlabs-logik.pdf | `short` |

## Evaluation Criteria

| Criterion | Definition | Rating |
|---|---|---|
| Input could be processed | Whether the source PDF could be uploaded, parsed, and passed through the generation pipeline without a blocking processing error. | `yes` / `no` |
| Package was generated | Whether LEARN-Hub generated a teaching package for the input. | `yes` / `no` |
| Expected main components are present | Whether the generated package contains the expected main components (Deckblatt, Artikulationsschema, Hintergrundwissen, Tafelbild, Übung mit Lösung). | `yes` / `mostly` / `no` |
| Central topic of the source is preserved | Whether the generated package preserves the central topic and learning intent of the source material. The correct title is used, the correct age and time frame is given. The content matches the title. | `yes` / `partially` / `no` |
| Package is broadly internally consistent | Whether the generated package is broadly coherent across its sections, without major contradictions or mismatched content. Artikulationsschema integrates all different parts of the material. | `yes` / `partially` / `no` |
| Obvious fatal flaw is present | Whether there is an obvious issue. This means either incorrect information or other errors in the content. | `yes` / `no` |
| Observed issues | Short note describing relevant issues, limitations, or anomalies observed during evaluation. | Short comment |

## Results

| Filename | Input Processed | Package Generated | Main Components Present | Central Topic is preserved | Package is internally consistent | Obvious fatal flaw | Observed Issues |
| --- | --- | --- | --- | --- | --- | --- | --- |
| LEARN-Kit-algorithmen_und_roboter.pdf | yes | yes | yes | yes | yes | yes | Exercise structure is present, but generated labyrinth images are not solvable and the provided solution is incorrect. |
| codeorg-artist.pdf | yes | yes | yes | yes | partially | yes | The source's existing structure interferes with the expected LEARN-Hub package structure, and some precision-dependent exercise content is incorrect. | 
| learnlabs-logik.pdf | yes | yes | yes | yes | yes | yes | Exercise structure is present, but generated images are not usable for the intended tasks. |
| csunplugged-colourbynumbers.pdf | yes | yes | yes | yes | yes | yes | Exercise images only partially support the questions and do not map cleanly to the intended solution path. |
| barefoot-howcomputerslearn.pdf | yes | yes | yes | yes | partially | no | The source references pages or images that are not included, which leads to structural inconsistencies in the generated package. | 
| LEARN-Kit-netzwerk.pdf | yes | yes | yes | yes | yes | yes | Generated network images do not match the exercise content closely enough to support reliable solving. |

## Summary

The evaluation indicates that the LEARN-Hub generation pipeline is robust across source PDFs with different input quality. All selected documents could be processed and each run produced a teaching package with the defined material set: Deckblatt, Artikulationsschema, Hintergrundwissen, Tafelbild, and exercise material with a solution. The generated packages also reproduce the intended overall structure around the source topic and are mostly internally consistent.

The main shortcomings appear when the pipeline has to adapt to source documents with unusual or incomplete structures. Documents that already link to missing parts, external pages, or non-included images can lead to inconsistencies in the generated output structure. In addition, exercises that require exact values, paths, or counts are sometimes incorrect, and generated images are often not usable for solving the exercise, for example unsolvable labyrinths or network diagrams that do not match the task.

These findings underline the importance of the "human-in-the-loop", which is one of the main design goals in LEARN-Hub. Activities have to be approved and can be reviewed and edited before being published.
Many of these errors can quickly be eliminated through a short revision of a skilled computer science domain expert. Still the pipeline generates a solid base for the generation of activities based on both noisy & complete input.
