# /// script
# requires-python = ">=3.11"
# dependencies = ["beautifulsoup4>=4.13,<5"]
# ///
# Render cached source pages into line-oriented authoring inputs.
from __future__ import annotations

import json
from pathlib import Path
import re
import sys
from typing import Final, TypedDict

from bs4 import BeautifulSoup


class Source(TypedDict):
    url: str
    revision: int


class ArgumentPage(TypedDict):
    parser: str
    source: Source
    text: list[str]


BLOCKS: Final = ("h2", "h3", "h4", "h5", "p", "dt", "dd", "li", "tr", "pre")


def prepare(directory: Path) -> None:
    for file in sorted((directory / "wiki").rglob("*.json")):
        page = json.loads(file.read_text(encoding="utf-8"))
        soup = BeautifulSoup(page["html"], "html.parser")
        for element in soup.select("style, script, .mw-editsection, sup.reference, .history-json, #toc"):
            element.decompose()
        lines = [page["title"], "Source: " + page["url"], "Revision: " + str(page["revision"]), ""]
        types: dict[str, ArgumentPage] = {}
        current: str | None = None
        for block in soup.find_all(BLOCKS):
            heading = block.name in ("h2", "h3", "h4", "h5")
            if not heading and block.find(BLOCKS) is not None:
                continue
            text = block.get_text(" | " if block.name == "tr" else " ", strip=True)
            if not text:
                continue
            if heading:
                lines.extend(("", "#" * (int(block.name[1:]) - 1) + " " + text))
                if re.fullmatch(r"(?:brigadier|minecraft):[a-z_]+", text):
                    current = text
                    types[current] = {"parser": text, "source": {"url": page["url"], "revision": page["revision"]}, "text": []}
                elif block.name in ("h2", "h3"):
                    current = None
            else:
                lines.append(text)
            if current is not None:
                types[current]["text"].append(text)
        relative = file.relative_to(directory / "wiki").with_suffix(".txt")
        target = directory / "wiki-text" / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text("\n".join(lines) + "\n", encoding="utf-8")
        if page["title"] == "Argument types":
            output = directory / "argument-types"
            output.mkdir(parents=True, exist_ok=True)
            for name, definition in types.items():
                destination = output / (name.replace(":", "_") + ".json")
                destination.write_text(json.dumps(definition, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    prepare(Path(sys.argv[1]))
