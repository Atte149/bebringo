#!/usr/bin/env python3
"""
Генератор студийного офлайн-аудио для треков Bebringo через Edge-TTS (libopus 24kbps).
Извлекает все реплики из YAML-файла трека и упаковывает их в app/src/main/assets/audio/<track_id>/
"""
import os
import re
import sys
import json
import hashlib
import asyncio
import argparse
import subprocess
from pathlib import Path

try:
    import yaml
except ImportError:
    print("Установите PyYAML: pip install pyyaml", file=sys.stderr)
    sys.exit(1)

try:
    import edge_tts
except ImportError:
    print("Установите edge-tts: pip install edge-tts", file=sys.stderr)
    sys.exit(1)

# Голоса по умолчанию для румынского языка
VOICE_MALE = "ro-RO-EmilNeural"
VOICE_FEMALE = "ro-RO-AlinaNeural"

def normalize_text(text: str) -> str:
    """Нормализует строку для стабильного поиска в хэш-таблице."""
    t = text.strip()
    t = re.sub(r"\s+", " ", t)
    return t

def text_slug(text: str) -> str:
    """Генерирует уникальное имя файла по md5-хэшу нормализованного текста."""
    clean = normalize_text(text).lower()
    h = hashlib.md5(clean.encode("utf-8")).hexdigest()[:12]
    # Добавляем префикс из букв для наглядности (первые 15 символов a-z0-9)
    prefix = re.sub(r"[^a-zA-Z0-9]+", "_", clean)[:16].strip("_")
    if not prefix:
        prefix = "phrase"
    return f"{prefix}_{h}"

def extract_phrases_from_track(yaml_path: str):
    """Извлекает все уникальные тексты из YAML-трека."""
    with open(yaml_path, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f)

    track_id = data.get("id", "ro_a1")
    phrases = {} # text -> voice

    # 1. Леммы и примеры
    for lemma in data.get("lemmas", []):
        if lemma.get("lemma"):
            phrases[normalize_text(lemma["lemma"])] = VOICE_MALE
        if lemma.get("example"):
            phrases[normalize_text(lemma["example"])] = VOICE_MALE

    # 2. Шаги уроков
    for unit in data.get("units", []):
        for ch in unit.get("chapters", []):
            for sc in ch.get("scenes", []):
                for b in sc.get("blocks", []):
                    for step in b.get("steps", []):
                        # Аудирование и диктанты
                        if step.get("audio"):
                            phrases[normalize_text(step["audio"])] = VOICE_MALE
                        if step.get("word"):
                            phrases[normalize_text(step["word"])] = VOICE_MALE
                        # Примеры из теории
                        for ex in step.get("examples", []):
                            if ex.get("primary"):
                                phrases[normalize_text(ex["primary"])] = VOICE_MALE
                        # Диалоги (чередуем голоса по спикерам)
                        for line in step.get("lines", []):
                            txt = line.get("text")
                            if txt:
                                spk = (line.get("speaker") or "").lower()
                                voice = VOICE_FEMALE if ("ana" in spk or "femeie" in spk or "maria" in spk) else VOICE_MALE
                                phrases[normalize_text(txt)] = voice

    return track_id, phrases

async def generate_single_audio(text: str, voice: str, out_ogg_path: str, tmp_dir: str):
    """Синтезирует речь через edge-tts и сжимает в ogg opus."""
    tmp_mp3 = os.path.join(tmp_dir, f"{hashlib.md5(text.encode()).hexdigest()}.mp3")
    communicate = edge_tts.Communicate(text, voice)
    await communicate.save(tmp_mp3)

    # Конвертация в ogg opus 24 kbps 24000 Hz моно
    cmd = [
        "ffmpeg", "-y", "-loglevel", "error",
        "-i", tmp_mp3,
        "-c:a", "libopus",
        "-b:a", "24k",
        "-ar", "24000",
        "-ac", "1",
        out_ogg_path
    ]
    subprocess.run(cmd, check=True)
    if os.path.exists(tmp_mp3):
        os.remove(tmp_mp3)

async def main_async(args):
    track_id, phrases = extract_phrases_from_track(args.track_yaml)
    out_dir = Path(args.output_dir) / track_id
    out_dir.mkdir(parents=True, exist_ok=True)
    tmp_dir = Path("/tmp/bebringo_tts")
    tmp_dir.mkdir(parents=True, exist_ok=True)

    print(f"=== Bebringo Audio Generator ===")
    print(f"Трек: {track_id}")
    print(f"Всего уникальных фраз: {len(phrases)}")
    print(f"Каталог вывода: {out_dir}")

    manifest = {}
    tasks = []
    sem = asyncio.Semaphore(5) # не более 5 параллельных запросов

    async def worker(idx, text, voice):
        slug = text_slug(text)
        filename = f"{slug}.ogg"
        ogg_path = out_dir / filename
        manifest[normalize_text(text).lower()] = filename

        if ogg_path.exists() and not args.force:
            return

        async with sem:
            try:
                await generate_single_audio(text, voice, str(ogg_path), str(tmp_dir))
                print(f"[{idx+1}/{len(phrases)}] OK: {text[:40]}")
            except Exception as e:
                print(f"[{idx+1}/{len(phrases)}] ОШИБКА: {text} -> {e}", file=sys.stderr)

    for i, (text, voice) in enumerate(phrases.items()):
        tasks.append(worker(i, text, voice))

    await asyncio.gather(*tasks)

    # Сохраняем manifest.json
    manifest_path = out_dir / "manifest.json"
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    total_bytes = sum(f.stat().st_size for f in out_dir.glob("*.ogg"))
    print(f"\nГотово! Сгенерировано {len(manifest)} файлов.")
    print(f"Общий размер аудио: {total_bytes / 1024:.1f} КБ (~{total_bytes / (1024*1024):.2f} МБ)")
    print(f"Манифест: {manifest_path}")

def main():
    parser = argparse.ArgumentParser(description="Generate offline audio for Bebringo tracks")
    parser.add_argument("--track-yaml", default="app/src/main/assets/course/a1.yml", help="Path to track YAML file")
    parser.add_argument("--output-dir", default="app/src/main/assets/audio", help="Output directory for audio assets")
    parser.add_argument("--force", action="store_true", help="Force regenerate existing files")
    args = parser.parse_args()
    asyncio.run(main_async(args))

if __name__ == "__main__":
    main()
