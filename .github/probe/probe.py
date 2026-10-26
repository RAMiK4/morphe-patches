import json, urllib.request, base64, sys, collections

API = "https://youtubei.googleapis.com/youtubei/v1/"
UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

def ctx(client):
    return {"client": {"clientName": client, "clientVersion": "2.20250101.00.00", "hl": "tr", "gl": "TR"}}

def post(endpoint, body):
    req = urllib.request.Request(API + endpoint + "?prettyPrint=false", data=json.dumps(body).encode(),
                                 headers={"Content-Type": "application/json", "User-Agent": UA})
    try:
        return json.load(urllib.request.urlopen(req, timeout=20))
    except Exception as e:
        print("  REQUEST FAILED", endpoint, e)
        return None

def hist(o, c=None):
    c = c if c is not None else collections.Counter()
    if isinstance(o, dict):
        for k, v in o.items():
            if k.endswith("Renderer") or k.endswith("ViewModel"):
                c[k] += 1
            hist(v, c)
    elif isinstance(o, list):
        for v in o:
            hist(v, c)
    return c

def posts(o, out):
    if isinstance(o, dict):
        for k, v in o.items():
            if k == "sharedPostRenderer":
                continue
            if k in ("backstagePostRenderer", "postRenderer") and isinstance(v, dict):
                out.append(v)
                continue
            posts(v, out)
    elif isinstance(o, list):
        for v in o:
            posts(v, out)
    return out

def tokens(o, path, out):
    if isinstance(o, dict):
        for k, v in o.items():
            if k == "token" and isinstance(v, str) and len(v) > 20:
                out.append((path, v[:30]))
            tokens(v, path + "/" + k, out)
    elif isinstance(o, list):
        for i, v in enumerate(o):
            tokens(v, path + "[]", out)
    return out

def text(t):
    if not t: return ""
    if "simpleText" in t: return t["simpleText"]
    return "".join(r.get("text", "") for r in t.get("runs", []))

def show_posts(ps):
    for p in ps:
        print("   -", p.get("postId"), "|", text(p.get("publishedTimeText")), "|", text(p.get("contentText"))[:70].replace("\n", " "))

def resolve(handle):
    r = post("navigation/resolve_url", {"context": ctx("WEB"), "url": "https://www.youtube.com/" + handle})
    try:
        return r["endpoint"]["browseEndpoint"]["browseId"]
    except Exception:
        return None

def varint(n):
    out = b""
    while n & ~0x7F:
        out += bytes([(n & 0x7F) | 0x80]); n >>= 7
    return out + bytes([n])

def search_params(q):
    qb = q.encode()
    b = b"\x12\x06search" + b"\x2a" + varint(len(qb)) + qb + b"\xb8\x01\x00\x92\x03\x00\xf2\x06\x04\x0a\x02\x5a\x00"
    return base64.urlsafe_b64encode(b).decode()

handle, query = sys.argv[1], sys.argv[2]
cid = resolve(handle)
print("CHANNEL", handle, cid)
if not cid: sys.exit(0)

print("\n=== MWEB search tab ===")
r = post("browse", {"context": ctx("MWEB"), "browseId": cid, "params": search_params(query), "query": query})
for page in range(1, 6):
    if not r: break
    ps = posts(r, [])
    print(f" page {page}: posts={len(ps)} hist={dict(hist(r).most_common(12))}")
    show_posts(ps[:5])
    toks = tokens(r, "", [])
    print("  tokens:", toks[-3:])
    if not toks: break
    r = post("browse", {"context": ctx("MWEB"), "continuation": [t for t in tokens(r, "", []) if "continuationCommand" in t[0]][-1][1] if False else None})
    break

print("\n=== WEB posts tab ===")
r = post("browse", {"context": ctx("WEB"), "browseId": cid, "params": "Egljb21tdW5pdHnyBgQKAkoA"})
if r:
    tabs = r.get("contents", {}).get("twoColumnBrowseResultsRenderer", {}).get("tabs", [])
    for t in tabs:
        tr = t.get("tabRenderer", {})
        url = tr.get("endpoint", {}).get("commandMetadata", {}).get("webCommandMetadata", {}).get("url")
        print("  tab", url, "selected" if tr.get("selected") else "", tr.get("endpoint", {}).get("browseEndpoint", {}).get("params"))
for page in range(1, 8):
    if not r: break
    ps = posts(r, [])
    print(f" page {page}: posts={len(ps)} topkeys={list(r.keys())} hist={dict(hist(r).most_common(10))}")
    show_posts(ps[:3])
    def find_cont(o):
        found = []
        def walk(o, path):
            if isinstance(o, dict):
                if "continuationItemRenderer" in o:
                    c = o["continuationItemRenderer"]
                    found.append((path, json.dumps(c)[:400]))
                for k, v in o.items():
                    walk(v, path + "/" + k)
            elif isinstance(o, list):
                for v in o: walk(v, path + "[]")
        walk(o, "")
        return found
    conts = find_cont(r)
    for p_, c in conts[-2:]:
        print("  continuationItemRenderer at", p_[-120:])
        print("   ", c)
    tok = None
    for p_, c in conts:
        try:
            tok = json.loads(c + "" if c.endswith("}") else "{}")["continuationEndpoint"]["continuationCommand"]["token"]
        except Exception:
            pass
    if not tok:
        # full parse
        def walk2(o):
            nonlocal_tok = []
            def w(o):
                if isinstance(o, dict):
                    cir = o.get("continuationItemRenderer")
                    if isinstance(cir, dict):
                        try: nonlocal_tok.append(cir["continuationEndpoint"]["continuationCommand"]["token"])
                        except Exception: pass
                    for v in o.values(): w(v)
                elif isinstance(o, list):
                    for v in o: w(v)
            w(o); return nonlocal_tok
        tl = walk2(r)
        print("  command tokens:", len(tl))
        tok = tl[-1] if tl else None
    if not tok:
        print("  NO CONTINUATION TOKEN"); break
    r = post("browse", {"context": ctx("WEB"), "continuation": tok})
