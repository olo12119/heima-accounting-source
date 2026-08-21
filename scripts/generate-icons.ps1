Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $projectRoot 'build'
New-Item -ItemType Directory -Path $buildDir -Force | Out-Null

$size = 512
$bitmap = New-Object System.Drawing.Bitmap($size, $size)
$bitmap.SetResolution(96, 96)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.Clear([System.Drawing.Color]::Transparent)

$background = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#19342d'))
$cream = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#f4f1e8'))
$goldPen = New-Object System.Drawing.Pen([System.Drawing.ColorTranslator]::FromHtml('#d5a85e'), 24)
$goldPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$goldPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round

$roundRect = New-Object System.Drawing.Drawing2D.GraphicsPath
$radius = 136
$roundRect.AddArc(0, 0, $radius, $radius, 180, 90)
$roundRect.AddArc($size-$radius, 0, $radius, $radius, 270, 90)
$roundRect.AddArc($size-$radius, $size-$radius, $radius, $radius, 0, 90)
$roundRect.AddArc(0, $size-$radius, $radius, $radius, 90, 90)
$roundRect.CloseFigure()
$graphics.FillPath($background, $roundRect)

$horse = New-Object System.Drawing.Drawing2D.GraphicsPath
$horse.StartFigure()
$horse.AddPolygon([System.Drawing.Point[]]@(
  [System.Drawing.Point]::new(144, 360),
  [System.Drawing.Point]::new(144, 180),
  [System.Drawing.Point]::new(200, 180),
  [System.Drawing.Point]::new(256, 104),
  [System.Drawing.Point]::new(300, 184),
  [System.Drawing.Point]::new(342, 202),
  [System.Drawing.Point]::new(382, 240),
  [System.Drawing.Point]::new(382, 360),
  [System.Drawing.Point]::new(326, 360),
  [System.Drawing.Point]::new(326, 282),
  [System.Drawing.Point]::new(292, 242),
  [System.Drawing.Point]::new(200, 242),
  [System.Drawing.Point]::new(200, 360)
))
$horse.CloseFigure()
$graphics.FillPath($cream, $horse)
$graphics.DrawLine($goldPen, 248, 274, 312, 274)
$graphics.DrawLine($goldPen, 248, 322, 312, 322)
$graphics.FillEllipse((New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#d5a85e'))), 318, 202, 20, 20)

$output = Join-Path $buildDir 'icon.png'
$bitmap.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
$goldPen.Dispose()
$cream.Dispose()
$background.Dispose()
$horse.Dispose()
$roundRect.Dispose()
$graphics.Dispose()
$bitmap.Dispose()
Write-Output "Generated $output"
