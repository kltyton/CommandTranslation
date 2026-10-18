# /// script
# requires-python = ">=3.11"
# dependencies = ["beautifulsoup4>=4.13,<5"]
# ///
# Read a source packet from stdin and prepare version-scoped component references.
from __future__ import annotations

from copy import deepcopy
import json
from pathlib import Path
import sys
from typing import Final, TypedDict

from bs4 import BeautifulSoup, Tag


class WikiPage(TypedDict):
    url: str
    revision: int
    html: str


class SourcePacket(TypedDict):
    native: dict[str, list[str]]
    pages: list[WikiPage]


class SourceText(TypedDict):
    url: str
    revision: int
    text: str


class ComponentReference(TypedDict):
    identifier: str
    versions: list[str]
    sources: list[SourceText]


BLOCKS: Final = ("h4", "h5", "p", "li", "dt", "dd", "tr", "pre")


def section_text(heading: Tag) -> str:
    wrapper = heading.find_parent("div", class_="mw-heading") or heading
    lines: list[str] = []
    for sibling in wrapper.find_next_siblings():
        if sibling.name in ("h2", "h3") or sibling.find(("h2", "h3")) is not None:
            break
        blocks = [sibling] if sibling.name in BLOCKS else sibling.find_all(BLOCKS)
        for block in blocks:
            own = deepcopy(block)
            for nested in own.find_all(("ul", "ol", "dl", "table")):
                nested.decompose()
            text = own.get_text(" ", strip=True)
            if text:
                depth = len(block.find_parents(("ul", "ol", "dl")))
                lines.append("  " * depth + text)
    return "\n".join(lines)


def prepare(packet: SourcePacket, output: Path) -> None:
    references: dict[str, ComponentReference] = {
        identifier: {"identifier": identifier, "versions": versions, "sources": []}
        for identifier, versions in packet["native"].items()
    }
    for page in packet["pages"]:
        soup = BeautifulSoup(page["html"], "html.parser")
        for element in soup.select("style,script,.mw-editsection,sup.reference,.navbox,#toc"):
            element.decompose()
        for heading in soup.find_all("h3"):
            name = heading.get_text(" ", strip=True)
            identifier = name if name.startswith("minecraft:") else "minecraft:" + name
            if identifier in references:
                references[identifier]["sources"].append({
                    "url": page["url"], "revision": page["revision"], "text": section_text(heading),
                })
    output.mkdir(parents=True, exist_ok=True)
    missing: list[str] = []
    for identifier, reference in sorted(references.items()):
        if not reference["sources"]:
            missing.append(identifier)
        file = output / (identifier.removeprefix("minecraft:") + ".json")
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(json.dumps(reference, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (output / "manifest.json").write_text(json.dumps({
        "components": sorted(references), "missing": missing,
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"components": len(references), "missing": missing}))


if __name__ == "__main__":
    source: SourcePacket = json.load(sys.stdin)
    prepare(source, Path(sys.argv[1]))
