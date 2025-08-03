#!/bin/bash
# 1. Delete auxiliary and temporary files
rm -f *.aux *.bbl *.bcf *.blg *.lof *.log *.lot *.out *.toc

# 2. Compile the document
lualatex -shell-escape BP.tex
biber BP
lualatex -shell-escape BP.tex
lualatex -shell-escape BP.tex


