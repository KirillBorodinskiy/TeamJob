# build.ps1
$containerName = "latex-builder"
$rootDir = $PSScriptRoot

# Try to start an existing container
docker start -a -i $containerName

if ($LASTEXITCODE -ne 0) {
    # If the container doesn't exist, create and run it, then execute build.sh
    docker run --name $containerName -v "${rootDir}:/data" -w /data -it blang/latex:ctanfull pdflatex -shell-escape BP.tex
}
