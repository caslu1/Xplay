"""Prints the center "x y" of the first UI node whose text, content-desc or hint matches.

Usage: python3 tap.py ui.xml QUERY [N]   (QUERY "re:<regex>" matches by regex; N picks the N-th match)
"""
import re
import sys
import xml.etree.ElementTree as ET

query = sys.argv[2]
want = int(sys.argv[3]) if len(sys.argv) > 3 else 1
seen = 0
for node in ET.parse(sys.argv[1]).iter("node"):
    fields = [node.get("text", ""), node.get("content-desc", ""), node.get("hint", "")]
    if query.startswith("re:"):
        hit = any(re.search(query[3:], f) for f in fields if f)
    else:
        hit = query in fields
    if hit:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        if x2 - x1 < 2 or y2 - y1 < 2:
            continue
        seen += 1
        if seen == want:
            print((x1 + x2) // 2, (y1 + y2) // 2)
            sys.exit(0)
sys.exit(1)
