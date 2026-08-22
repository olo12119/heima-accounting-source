param(
    [string]$Source = "$PSScriptRoot\source-assets\category_3d_atlas.png",
    [string]$Destination = "$PSScriptRoot\..\app\src\main\res\drawable-nodpi\category_3d_atlas_v2.png"
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -TypeDefinition (Get-Content -Raw "$PSScriptRoot\CategoryAtlasNormalizer.cs") -ReferencedAssemblies System.Drawing

$sourcePath = [System.IO.Path]::GetFullPath($Source)
$destinationPath = [System.IO.Path]::GetFullPath($Destination)
[CategoryAtlasNormalizer]::Normalize($sourcePath, $destinationPath)
Write-Output "Normalized category atlas: $destinationPath"
