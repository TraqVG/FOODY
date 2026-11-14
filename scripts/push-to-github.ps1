# Run only after creating an EMPTY private repository named TraqVG/FOODY on GitHub.
# Requires Git and GitHub authorization in Git Credential Manager.
$ErrorActionPreference = 'Stop'
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..'))
$origin = 'https://github.com/TraqVG/FOODY.git'
if (-not (git remote | Select-String '^origin$')) { git remote add origin $origin }
else { git remote set-url origin $origin }
git push -u origin main
