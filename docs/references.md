# References and data notes

This repository is a computer science portfolio project, not a biological model. The included data is a starter dataset intended for software demonstration.

## HGNC interleukin catalog

The `data/hgnc_interleukins.csv` file is a manually prepared starter catalog based on the HGNC Interleukins gene group concept. Before using the dataset outside a portfolio demonstration, verify symbols directly against the current HGNC download page or API.

Useful source:

- HGNC gene group report for Interleukins: https://www.genenames.org/data/genegroup/

## C17orf99 / IL-40 note

The starter catalog uses the approved symbol `C17orf99` and lists `IL40` / `IL-40` as aliases because IL-40 is commonly described as encoded by C17orf99 rather than using `IL40` as the approved gene symbol.

Useful source:

- HGNC symbol report for C17orf99: https://www.genenames.org/data/gene-symbol-report/

## Synthetic rules

The rules in `rules/th17_core.icirc` are simplified teaching rules. They are not validated quantitative immunology and should not be used for medical or research inference.
