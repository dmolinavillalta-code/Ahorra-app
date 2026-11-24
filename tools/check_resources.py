#!/usr/bin/env python3
"""Comprobaciones estáticas de recursos Android (no sustituye a compilar con el SDK)."""
import re, sys, pathlib
import xml.etree.ElementTree as ET

root = pathlib.Path(__file__).resolve().parent.parent
res = root / "app/src/main/res"
errors = []

def strings(path):
    out = {}
    for el in ET.parse(path).getroot().iter("string"):
        out[el.get("name")] = "".join(el.itertext())
    return out

es = strings(res / "values/strings.xml")
en = strings(res / "values-en/strings.xml")

for name in sorted(set(es) ^ set(en)):
    errors.append(f"string '{name}' solo existe en un idioma")

def args(text):
    return sorted(re.findall(r"%(\d+)\$[ds]", text))

for name in es:
    if name in en and args(es[name]) != args(en[name]):
        errors.append(f"argumentos distintos en '{name}': {args(es[name])} vs {args(en[name])}")
    for lang, txt in (("es", es[name]), ("en", en.get(name, ""))):
        # Un % suelto (no %N$s/%N$d ni %%) rompe String.format en tiempo de ejecución.
        if re.search(r"%(?!%|\d+\$[ds])", txt.replace("%%", "")):
            errors.append(f"'%' sin escapar en {lang}:{name}")

used = set()
for f in (root / "app/src/main").rglob("*.kt"):
    used |= set(re.findall(r"R\.string\.(\w+)", f.read_text(encoding="utf-8")))
for name in sorted(used):
    if name not in es: errors.append(f"R.string.{name} usado en código pero falta en es")
    if name not in en: errors.append(f"R.string.{name} usado en código pero falta en en")
unused = sorted(set(es) - used)

manifest = (root / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
for ref in re.findall(r'@(drawable|mipmap|xml|style|string)/([\w.]+)', manifest):
    kind, name = ref
    if kind == "string":
        if name not in es: errors.append(f"manifest referencia @string/{name} inexistente")
    elif kind == "style":
        if not any(f'name="{name}"' in p.read_text(encoding="utf-8") for p in res.glob("values*/themes.xml")):
            errors.append(f"manifest referencia @style/{name} inexistente")
    else:
        if not any(res.glob(f"{kind}*/{name}.xml")): errors.append(f"manifest referencia @{kind}/{name} inexistente")
declared = [e.get("{http://schemas.android.com/apk/res/android}name") for e in ET.parse(root / "app/src/main/AndroidManifest.xml").getroot().iter("uses-permission")]
if "android.permission.INTERNET" in declared: errors.append("el manifiesto declara INTERNET (la v1 no debe tenerlo)")

for f in list(res.rglob("*.xml")) + [root / "app/src/main/AndroidManifest.xml"]:
    try: ET.parse(f)
    except ET.ParseError as e: errors.append(f"XML mal formado {f.relative_to(root)}: {e}")

for f in (root / "app/src/main").rglob("*.kt"):
    for ref in re.findall(r"R\.drawable\.(\w+)", f.read_text(encoding="utf-8")):
        if not any(res.glob(f"drawable*/{ref}.xml")): errors.append(f"R.drawable.{ref} inexistente ({f.name})")

print("permisos declarados:", declared)
print(f"strings es={len(es)} en={len(en)} usados={len(used)} sin_usar={unused}")
if errors:
    print("ERRORES:"); [print(" -", e) for e in errors]; sys.exit(1)
print("OK: recursos coherentes")
