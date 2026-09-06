"""生成不含个人信息的PDF导入测试样本。"""
from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.utils import ImageReader
from PIL import Image, ImageDraw, ImageFont

out = Path(__file__).resolve().parents[1] / 'app/src/androidTest/assets'
out.mkdir(parents=True, exist_ok=True)
font_path = 'C:/Windows/Fonts/simsun.ttc'
pdfmetrics.registerFont(TTFont('ChineseFixture', font_path, subfontIndex=0))
items = [(130, 70, '星期一'), (290, 70, '星期二'), (450, 70, '星期三'),
         (25, 175, '1-2节'), (25, 325, '3-4节'),
         (130, 130, '高等数学'), (130, 153, '张老师'), (130, 176, '1-16周'), (130, 199, 'A101'),
         (290, 280, '大学英语'), (290, 303, '李老师'), (290, 326, '1-8周'), (290, 349, 'B202')]
c = canvas.Canvas(str(out / 'text-table.pdf'), pagesize=(595, 842))
c.setFont('ChineseFixture', 14)
for x, y, text in items:
    c.drawString(x, 842-y, text)
c.save()
image = Image.new('RGB', (1190, 1684), 'white')
draw = ImageDraw.Draw(image)
font = ImageFont.truetype(font_path, 28)
for x, y, text in items:
    draw.text((x*2, y*2-25), text, fill='black', font=font)
c = canvas.Canvas(str(out / 'scan-table.pdf'), pagesize=(595, 842))
c.drawImage(ImageReader(image), 0, 0, width=595, height=842)
c.save()
print('Generated text-table.pdf and scan-table.pdf')
