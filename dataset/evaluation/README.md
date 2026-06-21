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
| `long` | Source PDFs are internally consistent and contain longer descriptions. |
| `short` | Source PDFs are internally consistent but contain only shorter descriptions. |

## Selected PDFs

| Filename | Category |
|---|---|
| LEARN-Kit-algorithmen_und_roboter.pdf | `complete` |
| LEARN-Kit-netzwerk.pdf | `complete` |
| barefoot-howcomputerslearn.pdf | `incomplete` |
| csunplugged-colourbynumbers.pdf | `incomplete` |
| codeorg-artist.pdf | `long` |
| learnlabs-logik.pdf | `short` |
| LEARN-Kit-binaersystem.pdf | `complete` |
| learnlabs-baeume.pdf | `short` |
| barefoot-worldmap.pdf | `incomplete` |
| learnlabs-itsicherheit.pdf | `short` |
| barefoot-patternsunplugges.pdf | `incomplete` |
| csunplugged-squeezingpictures.pdf | `incomplete` |

## Evaluation Criteria

| Criterion | Definition | Rating |
|---|---|---|
| Input could be processed | Whether the source PDF could be uploaded, parsed, and passed through the generation pipeline without a blocking processing error. | `yes` / `no` |
| Package was generated | Whether LEARN-Hub generated a teaching package for the input. | `yes` / `no` |
| Expected main components are present | Whether the generated package contains the expected main components (Deckblatt, Artikulationsschema, Hintergrundwissen, Tafelbild, Übung mit Lösung). | `yes` / `mostly` / `no` |
| Central topic of the source is preserved | Whether the generated package preserves the central topic and learning intent of the source material, including title, target age, lesson duration, and topic-specific content. | `yes` / `partially` / `no` |
| Package is broadly internally consistent | Whether the generated package is coherent across its sections, without major contradictions or mismatched content, and whether the Artikulationsschema integrates the generated materials appropriately. | `yes` / `partially` / `no` |
| Obvious fatal flaw is present | Whether the generated package contains an obvious content or media issue that would prevent direct classroom use without revision. | `yes` / `no` |
| Observed issues | Short note describing relevant issues, limitations, or anomalies observed during evaluation. | Short comment |

## Results

| Filename | Input Processed | Package Generated | Main Components Present | Central Topic Preserved | Package Internally Consistent | Obvious Fatal Flaw | Observed Issues |
| --- | --- | --- | --- | --- | --- | --- | --- |
| LEARN-Kit-algorithmen_und_roboter.pdf | yes | yes | yes | yes | yes | yes | Exercise structure is present, but generated labyrinth images are not solvable and the provided solution is incorrect. |
| codeorg-artist.pdf | yes | yes | yes | yes | partially | yes | The source's existing structure interferes with the expected LEARN-Hub package structure, and some precision-dependent exercise content is incorrect. |
| learnlabs-logik.pdf | yes | yes | yes | yes | yes | yes | Exercise structure is present, but the generated images are not usable for the intended tasks. |
| csunplugged-colourbynumbers.pdf | yes | yes | yes | yes | yes | yes | Exercise images only partially support the questions and do not map cleanly to the intended solution path. |
| barefoot-howcomputerslearn.pdf | yes | yes | yes | yes | partially | no | The source references pages or images that are not included, which leads to structural inconsistencies in the generated package. |
| LEARN-Kit-netzwerk.pdf | yes | yes | yes | yes | yes | yes | Generated network images do not match the exercise content closely enough to support reliable solving. |
| LEARN-Kit-binaersystem.pdf | yes | yes | yes | yes | yes | yes | One exercise image does not match its question precisely enough. |
| learnlabs-baeume.pdf | yes | yes | yes | yes | yes | yes | Some generated tree images are semantically incorrect. |
| barefoot-worldmap.pdf | yes | yes | yes | yes | yes | yes | The world-map images do not include a grid, so the robot's target position cannot be identified unambiguously. |
| learnlabs-itsicherheit.pdf | yes | yes | yes | yes | yes | no | No major issues observed. |
| barefoot-patternsunplugges.pdf | yes | yes | yes | yes | yes | no | No major issues observed. |
| csunplugged-squeezingpictures.pdf | yes | yes | yes | yes | yes | yes | An exercise image described as an `8x1` grid contains only seven boxes. |

## Summary

The evaluation indicates that the LEARN-Hub generation pipeline is robust at the processing and package-assembly level across source PDFs with different input quality. All selected documents could be processed, and each run produced a teaching package with the defined material set: Deckblatt, Artikulationsschema, Hintergrundwissen, Tafelbild, and exercise material with a solution. The generated packages preserve the central source topic in all evaluated cases and are mostly internally consistent.

The main shortcomings appear in exercise media and precision-dependent tasks. Several generated packages contain images that are visually present but do not support reliable solving, for example unsolvable labyrinths, network diagrams that do not match the task, grids with the wrong number of cells, or illustrations that are semantically inconsistent with the question. Documents that reference missing pages, external material, or non-included images can also lead to structural inconsistencies in the generated package.

These findings underline the importance of the human-in-the-loop workflow, which is one of the main design goals in LEARN-Hub. Activities have to be approved and can be reviewed and edited before being published. Many of the observed issues could be resolved through a short revision by a computer science domain expert. Overall, the pipeline generates a solid draft package from both complete and noisy inputs, but exercise images and exact solution paths require careful review before classroom use.
