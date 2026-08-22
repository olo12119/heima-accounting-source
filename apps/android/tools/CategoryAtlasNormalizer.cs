using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Linq;

public static class CategoryAtlasNormalizer
{
    private sealed class Component
    {
        public readonly List<int> Pixels = new List<int>();
        public int MinX = int.MaxValue;
        public int MinY = int.MaxValue;
        public int MaxX = int.MinValue;
        public int MaxY = int.MinValue;
        public double WeightedX;
        public double WeightedY;
        public double AlphaMass;
        public double CenterX { get { return WeightedX / AlphaMass; } }
        public double CenterY { get { return WeightedY / AlphaMass; } }
    }

    public static void Normalize(string sourcePath, string destinationPath)
    {
        const int columns = 7;
        const int rows = 4;
        const int cellSize = 240;
        const int safeSize = 178;
        const byte alphaThreshold = 12;

        using (var source = new Bitmap(sourcePath))
        using (var target = new Bitmap(columns * cellSize, rows * cellSize, PixelFormat.Format32bppArgb))
        {
            var components = FindComponents(source, alphaThreshold);
            using (var graphics = Graphics.FromImage(target))
            {
                graphics.Clear(Color.Transparent);
                graphics.CompositingMode = CompositingMode.SourceCopy;
                graphics.CompositingQuality = CompositingQuality.HighQuality;
                graphics.InterpolationMode = InterpolationMode.HighQualityBicubic;
                graphics.PixelOffsetMode = PixelOffsetMode.HighQuality;
                graphics.SmoothingMode = SmoothingMode.HighQuality;

                for (var row = 0; row < rows; row++)
                for (var column = 0; column < columns; column++)
                {
                    var left = column * source.Width / columns;
                    var right = (column + 1) * source.Width / columns;
                    var top = row * source.Height / rows;
                    var bottom = (row + 1) * source.Height / rows;

                    // The main illustrated object is the largest connected alpha
                    // component whose visual centre belongs to this grid cell. This
                    // automatically rejects detached shadows, fringe specks and bleed
                    // from a neighbouring cell without per-icon offsets.
                    var component = components
                        .Where(item => item.CenterX >= left && item.CenterX < right && item.CenterY >= top && item.CenterY < bottom)
                        .OrderByDescending(item => item.Pixels.Count)
                        .FirstOrDefault();
                    if (component == null) continue;

                    var padding = 2;
                    var cropLeft = Math.Max(0, component.MinX - padding);
                    var cropTop = Math.Max(0, component.MinY - padding);
                    var cropRight = Math.Min(source.Width - 1, component.MaxX + padding);
                    var cropBottom = Math.Min(source.Height - 1, component.MaxY + padding);
                    var cropWidth = cropRight - cropLeft + 1;
                    var cropHeight = cropBottom - cropTop + 1;

                    using (var isolated = new Bitmap(cropWidth, cropHeight, PixelFormat.Format32bppArgb))
                    {
                        var keep = new HashSet<int>(component.Pixels);
                        foreach (var pixelIndex in component.Pixels)
                        {
                            var pixelX = pixelIndex % source.Width;
                            var pixelY = pixelIndex / source.Width;
                            for (var offsetY = -1; offsetY <= 1; offsetY++)
                            for (var offsetX = -1; offsetX <= 1; offsetX++)
                            {
                                var neighbourX = pixelX + offsetX;
                                var neighbourY = pixelY + offsetY;
                                if (neighbourX < 0 || neighbourX >= source.Width || neighbourY < 0 || neighbourY >= source.Height) continue;
                                if (source.GetPixel(neighbourX, neighbourY).A > 0)
                                    keep.Add(neighbourY * source.Width + neighbourX);
                            }
                        }

                        foreach (var pixelIndex in keep)
                        {
                            var pixelX = pixelIndex % source.Width;
                            var pixelY = pixelIndex / source.Width;
                            if (pixelX < cropLeft || pixelX > cropRight || pixelY < cropTop || pixelY > cropBottom) continue;
                            isolated.SetPixel(pixelX - cropLeft, pixelY - cropTop, source.GetPixel(pixelX, pixelY));
                        }

                        var scale = Math.Min((double)safeSize / cropWidth, (double)safeSize / cropHeight);
                        var drawWidth = Math.Max(1, (int)Math.Round(cropWidth * scale));
                        var drawHeight = Math.Max(1, (int)Math.Round(cropHeight * scale));
                        var centroidX = (component.CenterX - cropLeft) * scale;
                        var centroidY = (component.CenterY - cropTop) * scale;
                        var cellCenterX = column * cellSize + cellSize / 2.0;
                        var cellCenterY = row * cellSize + cellSize / 2.0;
                        var safeLeft = column * cellSize + (cellSize - safeSize) / 2;
                        var safeTop = row * cellSize + (cellSize - safeSize) / 2;
                        var drawX = (int)Math.Round(cellCenterX - centroidX);
                        var drawY = (int)Math.Round(cellCenterY - centroidY);
                        drawX = Math.Max(safeLeft, Math.Min(drawX, safeLeft + safeSize - drawWidth));
                        drawY = Math.Max(safeTop, Math.Min(drawY, safeTop + safeSize - drawHeight));

                        graphics.DrawImage(
                            isolated,
                            new Rectangle(drawX, drawY, drawWidth, drawHeight),
                            new Rectangle(0, 0, cropWidth, cropHeight),
                            GraphicsUnit.Pixel
                        );
                    }
                }
            }

            target.Save(destinationPath, ImageFormat.Png);
        }
    }

    private static List<Component> FindComponents(Bitmap source, byte alphaThreshold)
    {
        var width = source.Width;
        var height = source.Height;
        var visited = new bool[width * height];
        var result = new List<Component>();
        var queue = new Queue<int>();
        var offsets = new[] { -1, 1, -width, width };

        for (var y = 0; y < height; y++)
        for (var x = 0; x < width; x++)
        {
            var start = y * width + x;
            if (visited[start] || source.GetPixel(x, y).A < alphaThreshold) continue;
            var component = new Component();
            visited[start] = true;
            queue.Enqueue(start);

            while (queue.Count > 0)
            {
                var current = queue.Dequeue();
                var currentX = current % width;
                var currentY = current / width;
                var color = source.GetPixel(currentX, currentY);
                component.Pixels.Add(current);
                component.MinX = Math.Min(component.MinX, currentX);
                component.MinY = Math.Min(component.MinY, currentY);
                component.MaxX = Math.Max(component.MaxX, currentX);
                component.MaxY = Math.Max(component.MaxY, currentY);
                component.AlphaMass += color.A;
                component.WeightedX += currentX * color.A;
                component.WeightedY += currentY * color.A;

                foreach (var offset in offsets)
                {
                    var next = current + offset;
                    if (next < 0 || next >= visited.Length || visited[next]) continue;
                    var nextX = next % width;
                    var nextY = next / width;
                    if (Math.Abs(nextX - currentX) + Math.Abs(nextY - currentY) != 1) continue;
                    if (source.GetPixel(nextX, nextY).A < alphaThreshold) continue;
                    visited[next] = true;
                    queue.Enqueue(next);
                }
            }

            if (component.Pixels.Count >= 100) result.Add(component);
        }

        return result;
    }
}
