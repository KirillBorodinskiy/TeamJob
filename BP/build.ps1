# build.ps1
$containerName = "latex-builder"
$rootDir = $PSScriptRoot

# Convert Windows path to Docker-compatible format
$dockerPath = $rootDir -replace '\\', '/'

# Function to check if pygmentize is available
function Test-Pygmentize {
    docker exec $containerName bash -c "which pygmentize" 2>$null
    return $LASTEXITCODE -eq 0
}

# Function to install pygmentize with compatible version
function Install-Pygmentize {
    Write-Host "Installing pygmentize..."
    docker exec $containerName bash -c "apt-get update && apt-get install -y python3-pip && pip3 install 'Pygments<2.7'"
}

# Check if container exists
$containerExists = docker ps -a --filter "name=$containerName" --format "table {{.Names}}" | Select-String $containerName

if ($containerExists) {
    Write-Host "Container exists, starting it..."
    docker start $containerName
    
    # Check if pygmentize is available
    if (-not (Test-Pygmentize)) {
        Write-Host "pygmentize not found, installing compatible version..."
        Install-Pygmentize
    }
} else {
    Write-Host "Creating new container with pygmentize..."
    docker run --name $containerName -v "${dockerPath}:/data" -w /data -d blang/latex:ctanfull tail -f /dev/null
    
    # Install pygmentize in the new container
    Install-Pygmentize
}

# Compile the LaTeX document
Write-Host "Compiling BP.tex..."
docker exec $containerName pdflatex -shell-escape BP.tex

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful! Check BP.pdf"
} else {
    Write-Host "Compilation failed. Check BP.log for details."
}
