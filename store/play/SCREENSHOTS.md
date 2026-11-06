# Скриншоты для Google Play / Play Store Screenshots

Руководство по подготовке и публикации скриншотов приложения «Мафия» (com.serg.mafia) в Google Play Console.

---

## 1. Требования Google Play к скриншотам

- **Формат:** JPEG или 24-битный PNG (без прозрачности / alpha-канала).
- **Количество:** от 2 до 8 скриншотов для каждого типа устройств (телефон, 7-дюймовый планшет, 10-дюймовый планшет).
- **Разрешение:** минимум 320 px, максимум 3840 px по любой стороне. Соотношение сторон 16:9 или 9:16 (для портретного режима телефонов рекомендуется 1080×1920 или 1080×2400).
- **Исходники:** подготовленные эталонные снимки экрана находятся в каталоге `docs/screens/*.webp`.

---

## 2. Рекомендуемый набор и порядок скриншотов (8 экранов)

| № | Исходный файл в `docs/screens/` | Тема экрана | Подпись (UK) | Подпись (EN-US) | Подпись (RU-RU) |
|---|---|---|---|---|---|
| **1** | `01-setup.webp` | Настройка стола | **Стіл та баланс**<br>Від 4 до 20 гравців, авторозрахунок чорних | **Table Setup**<br>4 to 20 players, automatic team balancing | **Настройка стола**<br>От 4 до 20 игроков, авторасчёт мафии |
| **2** | `02-setup-roles.webp` | Выбор ролей | **Усі ключові ролі**<br>Дон, Комісар, Лікар, Метелик, Маніяк | **Special Roles**<br>Don, Sheriff, Doctor, Butterfly, Maniac | **Все ключевые роли**<br>Дон, Шериф, Доктор, Бабочка, Маньяк |
| **3** | `04-deal-role.webp` | Тайная раздача | **Таємна роздача**<br>Утримуй палець, щоб побачити свою роль | **Secret Deal**<br>Hold to reveal your role, release to hide | **Тайная раздача**<br>Удерживай палец, чтобы увидеть роль |
| **4** | `05-intro-night.webp` | Знакомство | **Нічне знайомство**<br>Ведучий бачить, хто має відкрити очі | **Intro Night**<br>Clear guide on who opens eyes first | **Ночное знакомство**<br>Ведущий видит, кто открывает глаза |
| **5** | `08-night-sheriff.webp` | Ход шерифа | **Покрокова ніч**<br>Миттєві підказки кольорів для ведучого | **Guided Night**<br>Instant faction hints for the moderator | **Пошаговая ночь**<br>Мгновенные подсказки цветов для ведущего |
| **6** | `09-morning.webp` | Итог ночи | **Автоматичний ранок**<br>Точний розрахунок пострілів та порятунку | **Morning Results**<br>Automatic resolution of kills and saves | **Итоги ночи**<br>Точный расчёт выстрелов и лечения |
| **7** | `10-day-speech.webp` | День и таймер | **День та таймери**<br>Черга виступів, виставлення та фоли | **Day Speeches**<br>Interactive timers, nominations & fouls | **День и таймеры**<br>Очередь речей, выставление и фолы |
| **8** | `12-cheatsheet.webp` | Шпаргалка стола | **Шпаргалка ведучого**<br>Хто є хто за столом у будь-який момент | **Moderator Cheatsheet**<br>Full table overview available anytime | **Шпаргалка ведущего**<br>Кто есть кто за столом в любой момент |

---

## 3. Скрипт подготовки скриншотов из исходников

Для автоматической конвертации исходников из `docs/screens/*.webp` в PNG без прозрачности под требования Play Console:

```bash
python3 -c "
import os
from PIL import Image

SRC = 'docs/screens'
OUT = 'store/play/screenshots'
os.makedirs(OUT, exist_ok=True)

SELECTED = [
    ('01-setup.webp', '01_setup.png'),
    ('02-setup-roles.webp', '02_roles.png'),
    ('04-deal-role.webp', '03_deal.png'),
    ('05-intro-night.webp', '04_intro.png'),
    ('08-night-sheriff.webp', '05_night.png'),
    ('09-morning.webp', '06_morning.png'),
    ('10-day-speech.webp', '07_speech.png'),
    ('12-cheatsheet.webp', '08_cheatsheet.png'),
]

for src_name, out_name in SELECTED:
    src_path = os.path.join(SRC, src_name)
    out_path = os.path.join(OUT, out_name)
    if os.path.exists(src_path):
        img = Image.open(src_path).convert('RGB')
        img.save(out_path, 'PNG', optimize=True)
        print(f'Converted {src_name} -> {out_name} ({img.size})')
"
```
