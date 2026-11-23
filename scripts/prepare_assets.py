#!/usr/bin/env python3
"""Prepara os assets e icones do Kairos Lite para Android 2.3 (SQLite 3.6.22).

Uso:
    python3 scripts/prepare_assets.py [--source /caminho/para/Kairos]
"""

import argparse
import json
import os
import shutil
import sqlite3
import sys


def prepare_hinario_lite(src_db: str, dst_db: str, label: str) -> None:
    if not os.path.isfile(src_db):
        raise FileNotFoundError(f"Banco de origem nao encontrado: {src_db}")

    if os.path.exists(dst_db):
        os.remove(dst_db)

    src = sqlite3.connect(src_db)
    dst = sqlite3.connect(dst_db)
    try:
        # Formato de banco compativel com SQLite 3.6.22 (Android 2.3 Gingerbread)
        dst.execute("PRAGMA legacy_file_format = ON")
        dst.execute("PRAGMA journal_mode = DELETE")
        dst.execute(
            """
            CREATE TABLE hino (
                id INTEGER PRIMARY KEY,
                numero TEXT NOT NULL,
                titulo TEXT NOT NULL,
                letra TEXT NOT NULL,
                autor_letra TEXT,
                autor_musica TEXT,
                texto_base TEXT,
                categoria TEXT,
                subcategoria TEXT,
                textos_relacionados TEXT
            )
            """
        )

        # Mapa de textos biblicos relacionados por hino_id
        rel_map = {}
        try:
            for hino_id, ref in src.execute(
                """
                SELECT ht.hino_id, tb.referencia
                FROM hino_texto ht
                JOIN texto_biblico tb ON tb.id = ht.texto_id
                ORDER BY ht.hino_id, tb.id
                """
            ):
                if ref and ref.strip():
                    rel_map.setdefault(hino_id, []).append(ref.strip())
        except sqlite3.Error:
            pass

        rows = src.execute(
            """
            SELECT id, numero, titulo, letra, autor_letra, autor_musica,
                   texto_base, categoria, subcategoria
            FROM hino
            ORDER BY CAST(numero AS INTEGER), numero
            """
        ).fetchall()

        if not rows:
            raise RuntimeError(f"Nenhum hino encontrado em {src_db}.")

        enriched = []
        for r in rows:
            hino_id = r[0]
            rel_refs = " | ".join(rel_map.get(hino_id, []))
            enriched.append(r + (rel_refs,))

        dst.executemany(
            """
            INSERT INTO hino (
                id, numero, titulo, letra, autor_letra, autor_musica,
                texto_base, categoria, subcategoria, textos_relacionados
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            enriched,
        )
        dst.execute("CREATE INDEX idx_hino_numero ON hino(numero)")
        dst.commit()
        dst.execute("VACUUM")

        integrity = dst.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            raise RuntimeError(f"Falha no integrity_check de {label}: {integrity}")

        empty_lyrics = dst.execute(
            "SELECT COUNT(*) FROM hino WHERE letra IS NULL OR TRIM(letra) = ''"
        ).fetchone()[0]
        if empty_lyrics > 0:
            raise RuntimeError(f"Encontrados {empty_lyrics} hinos sem letra em {label}.")

        count = dst.execute("SELECT COUNT(*) FROM hino").fetchone()[0]
        size_kb = os.path.getsize(dst_db) / 1024.0
        print(f"[OK] {label} gerado: {count} hinos ({size_kb:.1f} KB)")
    finally:
        src.close()
        dst.close()


def prepare_bible(src_db: str, dst_db: str) -> None:
    if not os.path.isfile(src_db):
        raise FileNotFoundError(f"ARA.sqlite nao encontrado: {src_db}")

    shutil.copy2(src_db, dst_db)
    conn = sqlite3.connect(dst_db)
    try:
        conn.execute("PRAGMA journal_mode = DELETE")
        integrity = conn.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            raise RuntimeError(f"Falha no integrity_check de ARA.sqlite: {integrity}")
        books = conn.execute("SELECT COUNT(*) FROM book").fetchone()[0]
        verses = conn.execute("SELECT COUNT(*) FROM verse").fetchone()[0]
        size_mb = os.path.getsize(dst_db) / (1024.0 * 1024.0)
        print(f"[OK] ARA.sqlite copiado: {books} livros, {verses} versiculos ({size_mb:.2f} MB)")
    finally:
        conn.close()


def prepare_pericopes(src_db: str, dst_db: str) -> None:
    if not os.path.isfile(src_db):
        raise FileNotFoundError(f"pericopes.sqlite nao encontrado: {src_db}")

    if os.path.exists(dst_db):
        os.remove(dst_db)

    src = sqlite3.connect(src_db)
    dst = sqlite3.connect(dst_db)
    try:
        dst.execute("PRAGMA legacy_file_format = ON")
        dst.execute("PRAGMA journal_mode = DELETE")
        dst.execute(
            """
            CREATE TABLE pericope (
                book_id INTEGER NOT NULL,
                chapter INTEGER NOT NULL,
                verse INTEGER NOT NULL,
                title TEXT NOT NULL
            )
            """
        )
        rows = src.execute(
            "SELECT book_id, chapter, verse, title FROM pericope ORDER BY book_id, chapter, verse"
        ).fetchall()
        dst.executemany(
            "INSERT INTO pericope (book_id, chapter, verse, title) VALUES (?, ?, ?, ?)",
            rows,
        )
        dst.execute("CREATE INDEX idx_pericope_lookup ON pericope(book_id, chapter, verse)")
        dst.commit()
        dst.execute("VACUUM")

        integrity = dst.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            raise RuntimeError(f"Falha no integrity_check de pericopes.sqlite: {integrity}")
        size_kb = os.path.getsize(dst_db) / 1024.0
        print(f"[OK] pericopes.sqlite gerado: {len(rows)} pericopes ({size_kb:.1f} KB)")
    finally:
        src.close()
        dst.close()


def prepare_sunset(src_json: str, dst_json: str) -> None:
    if not os.path.isfile(src_json):
        raise FileNotFoundError(f"meditacoes_2026.json nao encontrado: {src_json}")

    with open(src_json, "r", encoding="utf-8") as f:
        data = json.load(f)

    if not isinstance(data, list) or not data:
        raise RuntimeError("meditacoes_2026.json deve ser um array JSON nao vazio.")

    shutil.copy2(src_json, dst_json)
    size_kb = os.path.getsize(dst_json) / 1024.0
    print(f"[OK] meditacoes_2026.json copiado: {len(data)} meditacoes ({size_kb:.1f} KB)")


def prepare_launcher_icons(src_icon: str, res_dir: str) -> None:
    if not os.path.isfile(src_icon):
        print(f"[WARN] Icone de origem nao encontrado: {src_icon}")
        return

    try:
        from PIL import Image, ImageDraw
    except ImportError:
        print("[WARN] Pillow nao instalado; pulando geracao de icones.")
        return

    base = Image.open(src_icon).convert("RGBA")
    # Desenha uma faixa escura com borda dourada e texto pixel-art "LITE" na base em 144x144
    work = base.resize((144, 144), Image.LANCZOS)
    draw = ImageDraw.Draw(work)

    # Faixa na base
    band_top = 104
    draw.rectangle([4, band_top, 139, 140], fill=(17, 14, 10, 245), outline=(217, 167, 115, 255), width=3)

    # Bitmap 5x7 nitido para "LITE" (garante legibilidade mesmo em 36x36 ldpi)
    glyphs = {
        "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
        "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
        "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
        "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    }
    scale = 3
    word = "LITE"
    char_w = 5 * scale
    gap = 2 * scale
    total_w = len(word) * char_w + (len(word) - 1) * gap
    start_x = (144 - total_w) // 2
    start_y = band_top + (36 - 7 * scale) // 2

    gold = (217, 167, 115, 255)
    for idx, ch in enumerate(word):
        gx = start_x + idx * (char_w + gap)
        for row_i, row in enumerate(glyphs[ch]):
            for col_i, bit in enumerate(row):
                if bit == "1":
                    x0 = gx + col_i * scale
                    y0 = start_y + row_i * scale
                    draw.rectangle([x0, y0, x0 + scale - 1, y0 + scale - 1], fill=gold)

    densities = [("drawable-ldpi", 36), ("drawable-mdpi", 48), ("drawable-hdpi", 72)]
    for folder, size in densities:
        out_dir = os.path.join(res_dir, folder)
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, "ic_launcher.png")
        resized = work.resize((size, size), Image.LANCZOS)
        resized.save(out_path, "PNG", optimize=True)
        print(f"[OK] Icone gerado: {folder}/ic_launcher.png ({size}x{size})")


def main() -> int:
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    default_source = os.path.abspath(os.path.join(repo_root, "..", "Kairos"))

    parser = argparse.ArgumentParser(description="Prepara assets enxutos para o Kairos Galaxy Y.")
    parser.add_argument(
        "--source",
        default=default_source,
        help="Diretorio raiz do repositorio Kairos principal (padrao: ../Kairos)",
    )
    args = parser.parse_args()

    assets_dir = os.path.join(repo_root, "assets")
    res_dir = os.path.join(repo_root, "res")
    os.makedirs(assets_dir, exist_ok=True)

    prepare_bible(
        os.path.join(args.source, "assets", "ARA.sqlite"),
        os.path.join(assets_dir, "ARA.sqlite"),
    )
    prepare_pericopes(
        os.path.join(args.source, "assets", "pericopes.sqlite"),
        os.path.join(assets_dir, "pericopes.sqlite"),
    )
    prepare_hinario_lite(
        os.path.join(args.source, "assets", "hinario.db"),
        os.path.join(assets_dir, "hinario_lite.db"),
        "hinario_lite.db",
    )
    prepare_hinario_lite(
        os.path.join(args.source, "assets", "hinario_antigo.db"),
        os.path.join(assets_dir, "hinario_antigo_lite.db"),
        "hinario_antigo_lite.db",
    )
    prepare_sunset(
        os.path.join(args.source, "meditacoes_2026.json"),
        os.path.join(assets_dir, "meditacoes_2026.json"),
    )
    prepare_launcher_icons(
        os.path.join(args.source, "android", "app", "src", "main", "res", "mipmap-xxxhdpi", "ic_launcher.png"),
        res_dir,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
