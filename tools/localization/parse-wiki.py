# /// script
# requires-python = ">=3.11"
# dependencies = ["beautifulsoup4>=4.13,<5"]
# ///
# Read rendered Wiki pages as JSON on stdin; write extracted argument descriptions on stdout.
from __future__ import annotations

import json
import re
import sys
from dataclasses import asdict, dataclass

from bs4 import BeautifulSoup


@dataclass(frozen=True, slots=True)
class Page:
    locale: str
    commands: tuple[str, ...]
    html: str


@dataclass(frozen=True, slots=True)
class Definition:
    name: str
    sections: tuple[str, ...]
    text: str


@dataclass(frozen=True, slots=True)
class Descriptions:
    locale: str
    commands: tuple[str, ...]
    arguments: dict[str, str]
    definitions: tuple[Definition, ...]


@dataclass(frozen=True, slots=True)
class ReferenceText:
    locale: str
    commands: tuple[str, ...]
    text: str


def reference_text(page: Page) -> ReferenceText:
    soup = BeautifulSoup(page.html, "html.parser")
    for navigation in soup.select("sup.reference, .mw-editsection, .navbox, .toc, script, style"):
        navigation.decompose()
    return ReferenceText(page.locale, page.commands, soup.get_text("\n", strip=True))


def extract(page: Page) -> Descriptions:
    soup = BeautifulSoup(page.html, "html.parser")
    for reference in soup.select("sup.reference, .mw-editsection"):
        reference.decompose()
    definitions: list[Definition] = []
    sections: list[tuple[int, str]] = []
    for term in soup.select("h2, h3, h4, h5, p, dt"):
        if term.name in ("h2", "h3", "h4", "h5"):
            level = int(term.name[1:])
            while sections and sections[-1][0] >= level:
                sections.pop()
            sections.append((level, term.get_text(" ", strip=True)))
            continue
        label = term.get_text(" ", strip=True)
        header = re.match(r"^(?:JE\s*[:：]\s*)?((?:<[A-Za-z_][A-Za-z_0-9.-]*>[\s/|,]*)+)\s*[:：]", label)
        if not header:
            continue
        definition = term.find_next_sibling()
        if definition is None or definition.name not in ("dl", "dd"):
            continue
        description = definition.get_text(" ", strip=True)
        if description:
            for name in re.findall(r"<([^<>]+)>", header.group(1)):
                definitions.append(Definition(name, tuple(text for _, text in sections), description))
    for heading in soup.select("h2, h3, h4, h5"):
        label = heading.find("code")
        name = label.get_text(strip=True) if label is not None else heading.get_text(strip=True)
        condition = re.fullmatch(r"\(if\|unless\)\s+([a-z_]+)", name)
        if condition:
            name = condition.group(1)
        if name == "Run subcommand":
            name = "run"
        if re.fullmatch(r"[a-z][a-z_0-9-]{0,30}", name) is None:
            continue
        parent = heading.parent
        anchor = parent if parent is not None and parent.name == "div" else heading
        paragraph = anchor.find_next_sibling()
        if paragraph is not None and paragraph.name == "dl":
            paragraph = paragraph.find("dd", recursive=False)
        elif paragraph is not None and paragraph.name != "p":
            paragraph = None
        if paragraph is not None:
            description = paragraph.get_text(" ", strip=True)
            if description:
                definitions.append(Definition(name, (heading.get_text(" ", strip=True),), description))
    alternatives: dict[str, list[str]] = {}
    for definition in definitions:
        texts = alternatives.setdefault(definition.name, [])
        if definition.text not in texts:
            texts.append(definition.text)
    entries = {name: texts[0] for name, texts in alternatives.items() if len(texts) == 1}
    return Descriptions(locale=page.locale, commands=page.commands, arguments=entries,
                        definitions=tuple(definitions))


def main() -> None:
    rows = json.load(sys.stdin)
    pages = [Page(locale=row["locale"], commands=tuple(row["commands"]), html=row["html"]) for row in rows]
    results = [asdict(reference_text(page) if "--text" in sys.argv else extract(page)) for page in pages]
    sys.stdout.write(json.dumps(results, ensure_ascii=False))


if __name__ == "__main__":
    main()
