$ErrorActionPreference = "Stop"

$root = "D:\moniC\project\learn"
$matlabDir = Join-Path $root "01-topics\image-restoration-lab6\matlab"
$pythonExe = "C:\Users\rainhotle\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
$reportScript = Join-Path $root "01-topics\image-restoration-lab6\generate_lab6_report.py"

Write-Host "Running Matlab experiment automation..."
& "D:\matlab\bin\matlab.exe" -batch "addpath('$matlabDir'); run_lab6_image_restoration"

Write-Host "Generating DOCX report draft..."
& $pythonExe $reportScript

Write-Host "Automation completed."
