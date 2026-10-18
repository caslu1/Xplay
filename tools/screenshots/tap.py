"""Prints the center "x y" of the first UI node whose text or content-desc matches.

Usage: python3 tap.py ui.xml QUERY   (QUERY "re:<regex>" matches text by regex)
"""
import re
import sys
import xml.etree.ElementTree as ET

query = sys.argv[2]
for node in ET.parse(sys.argv[1]).iter("node"):
    text, desc = node.get("text", ""), node.get("content-desc", "")
    if query.startswith("re:"):
        hit = re.search(query[3:], text) is not None
    else:
        hit = query in (text, desc)
    if hit:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        sys.exit(0)
sys.exit(1)
