"""Нарезка сезонных иконок Ежедневника из art/seasons/icon-<сезон>.png (1254×1254, тёмно-синий фон).

    python tools/season_icons.py

Для каждого сезона (winter, spring, summer, autumn):
- передний слой адаптивной иконки mipmap-<плотность>/ic_launcher_foreground_<сезон>.png (108 dp: 108–432 px),
  залитый фоном, с маскотом внутри безопасной зоны 66 dp — лаунчеры обрезают края кругом, «капсулой» и т. п.;
- плитка маскота для меню ☰ drawable-nodpi/mascot_<сезон>.webp — видимая часть иконки (72 dp из 108), 288 px;
и плитка обычного маскота mascot_default.webp из прежней иконки (для выключенной сезонной иконки).

Масштаб общий для всех сезонов — по самому большому маскоту с нарядом, чтобы иконка не меняла размер со сменой сезона.
Нужны Pillow и numpy.
"""

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art" / "seasons"
RES = ROOT / "app" / "src" / "main" / "res"
SEASONS = ["winter", "spring", "summer", "autumn"]
# Фон иконки — @color/ic_launcher_background
BACKGROUND = (0x09, 0x17, 0x58)
DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
# Радиус безопасной зоны — 33 dp из 108
SAFE_RADIUS = 33 / 108


def content_radius(image: Image.Image) -> float:
    """Самое дальнее от центра место, где картинка отличается от фона (маскот, наряд, снежинки)."""
    pixels = np.asarray(image.convert("RGB")).astype(int)
    height, width, _ = pixels.shape
    corner = pixels[5, 5]
    mask = np.abs(pixels - corner).sum(axis=2) > 60
    ys, xs = np.nonzero(mask)
    return float(np.sqrt((xs - width / 2) ** 2 + (ys - height / 2) ** 2).max()) / width


def feathered(image: Image.Image, border: float = 0.06) -> Image.Image:
    """Края картинки плавно уходят в прозрачность, чтобы на фоне не было шва."""
    size = image.size[0]
    edge = int(size * border)
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rectangle([edge, edge, size - edge, size - edge], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(edge / 2))
    result = image.convert("RGBA")
    result.putalpha(mask)
    return result


def foreground(image: Image.Image, scale: float, size: int) -> Image.Image:
    """Передний слой размером size×size: фон и картинка, уменьшенная в scale раз от size."""
    canvas = Image.new("RGBA", (size, size), BACKGROUND + (255,))
    side = round(size * scale)
    picture = feathered(image.resize((side, side), Image.LANCZOS))
    offset = (size - side) // 2
    canvas.alpha_composite(picture, (offset, offset))
    return canvas.convert("RGB")


def main():
    images = {season: Image.open(ART / f"icon-{season}.png").convert("RGB") for season in SEASONS}
    # Доля стороны исходника, которую занимает самый большой маскот (радиус)
    radius = max(content_radius(image) for image in images.values())
    scale = SAFE_RADIUS / radius
    print(f"масштаб картинки в слое: {scale:.3f}")
    for season, image in images.items():
        for density, size in DENSITIES.items():
            folder = RES / f"mipmap-{density}"
            folder.mkdir(parents=True, exist_ok=True)
            foreground(image, scale, size).save(folder / f"ic_launcher_foreground_{season}.png", optimize=True)
        # Плитка для меню: видимая часть иконки, 72 dp из 108
        big = foreground(image, scale, 432)
        crop = (432 - 288) // 2
        tile = big.crop((crop, crop, crop + 288, crop + 288))
        nodpi = RES / "drawable-nodpi"
        nodpi.mkdir(parents=True, exist_ok=True)
        tile.save(nodpi / f"mascot_{season}.webp", quality=88, method=6)
        print(season, "готово")
    # Обычный маскот без наряда — для шапки меню, когда сезонная иконка выключена: та же видимая часть иконки
    plain = Image.open(RES / "mipmap-xxxhdpi" / "ic_launcher_foreground.png").convert("RGB")
    crop = (432 - 288) // 2
    plain.crop((crop, crop, crop + 288, crop + 288)).save(RES / "drawable-nodpi" / "mascot_default.webp", quality=88, method=6)
    print("обычный маскот готов")


if __name__ == "__main__":
    main()
