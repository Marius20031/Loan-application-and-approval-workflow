"""Cyclomatic complexity per Java method, counted like Checkstyle does:
1 + if, for, while, do, case, catch, ternary (?), && and ||.

Usage: python3 complexity.py <file-or-folder> ...
Prints one line per method, then totals per file. Comments and strings are ignored.
"""
import re
import sys
from pathlib import Path

DECISIONS = re.compile(r"\b(if|for|while|do|case|catch)\b|\?|&&|\|\|")
METHOD = re.compile(
    r"^\s*(?:public|private|protected|static|final|synchronized|abstract|default|\s)*"
    r"[\w<>\[\],.? ]+\s+(\w+)\s*\([^;{)]*(?:\([^)]*\)[^;{)]*)*\)\s*(?:throws [\w., ]+)?\s*\{",
    re.M)
SKIP = {"if", "for", "while", "switch", "catch", "return", "new", "else", "synchronized"}


def strip(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    return re.sub(r'"(?:\\.|[^"\\])*"', '""', src)


def methods(src):
    clean = strip(src)
    for m in METHOD.finditer(clean):
        name = m.group(1)
        if name in SKIP:
            continue
        depth, i = 1, m.end()
        while i < len(clean) and depth:
            depth += {"{": 1, "}": -1}.get(clean[i], 0)
            i += 1
        body = clean[m.end():i]
        # a "switch ->" arm counts like a case
        arms = len(re.findall(r"->", body)) - len(re.findall(r"\)\s*->|\w\s*->\s*\{?\s*[\w.(]", ""))
        count = 1 + len(DECISIONS.findall(body)) + len(re.findall(r"\bdefault\s*->", body))
        yield name, count, body.count("\n") + 1


def main(paths):
    files = []
    for p in map(Path, paths):
        files += sorted(p.rglob("*.java")) if p.is_dir() else [p]
    for f in files:
        rows = list(methods(f.read_text(encoding="utf-8")))
        if not rows:
            continue
        print(f"{f}")
        for name, cc, lines in rows:
            print(f"  {cc:>3}  {lines:>4} lines  {name}")
        print(f"  total {sum(r[1] for r in rows)}, max {max(r[1] for r in rows)}, "
              f"avg {sum(r[1] for r in rows) / len(rows):.1f}, methods {len(rows)}")


main(sys.argv[1:] or ["."])
