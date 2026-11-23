#!/usr/bin/env python3
"""Prepara os assets do Kairos Lite para Android 2.3 (SQLite 3.6.22).

Uso:
    python3 scripts/prepare_assets.py [--source /caminho/para/Kairos]
"""

import argparse
import json
import os
import shutil
import sqlite3
import sys


def prepare_hinario_lite(src_db: str, dst_db: str) -> None:
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
                texto_base TEXT
            )
            """
        )

        rows = src.execute(
            """
            SELECT id, numero, titulo, letra, autor_letra, texto_base
            FROM hino
            ORDER BY CAST(numero AS INTEGER), numero
            """
        ).fetchall()

        if not rows:
            raise RuntimeError("Nenhum hino encontrado na tabela hino de origem.")

        dst.executemany(
            "INSERT INTO hino (id, numero, titulo, letra, autor_letra, texto_base) VALUES (?, ?, ?, ?, ?, ?)",
            rows,
        )
        dst.execute("CREATE INDEX idx_hino_numero ON hino(numero)")
        dst.commit()
        dst.execute("VACUUM")

        # Validacoes de integridade
        integrity = dst.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            raise RuntimeError(f"Falha no integrity_check de hinario_lite.db: {integrity}")

        empty_lyrics = dst.execute(
            "SELECT COUNT(*) FROM hino WHERE letra IS NULL OR TRIM(letra) = ''"
        ).fetchone()[0]
        if empty_lyrics > 0:
            raise RuntimeError(f"Encontrados {empty_lyrics} hinos sem letra.")

        count = dst.execute("SELECT COUNT(*) FROM hino").fetchone()[0]
        size_kb = os.path.getsize(dst_db) / 1024.0
        print(f"[OK] hinario_lite.db gerado: {count} hinos ({size_kb:.1f} KB)")
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
    os.makedirs(assets_dir, exist_ok=True)

    prepare_bible(
        os.path.join(args.source, "assets", "ARA.sqlite"),
        os.path.join(assets_dir, "ARA.sqlite"),
    )
    prepare_hinario_lite(
        os.path.join(args.source, "assets", "hinario.db"),
        os.path.join(assets_dir, "hinario_lite.db"),
    )
    prepare_sunset(
        os.path.join(args.source, "meditacoes_2026.json"),
        os.path.join(assets_dir, "meditacoes_2026.json"),
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
