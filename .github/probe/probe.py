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

import time
def list_token(r):
    """Continuation of the post list only: tab content and appended items, not the header."""
    found = []
    def w(o):
        if isinstance(o, dict):
            cir = o.get("continuationItemRenderer")
            if isinstance(cir, dict):
                try: found.append(cir["continuationEndpoint"]["continuationCommand"]["token"])
                except Exception: pass
            for k, v in o.items():
                if k in ("backstagePostThreadRenderer", "header", "engagementPanel"): continue
                w(v)
        elif isinstance(o, list):
            for v in o: w(v)
    for key in ("contents", "onResponseReceivedEndpoints", "onResponseReceivedActions"):
        w(r.get(key))
    return found[-1] if found else None

print("\n=== WEB posts tab (fixed token) ===")
t0 = time.time()
r = post("browse", {"context": ctx("WEB"), "browseId": cid, "params": "Egljb21tdW5pdHnyBgQKAkoA"})
total = 0; matches = 0; page = 0
q = query.lower()
while r and page < 60:
    page += 1
    ps = posts(r, [])
    total += len(ps)
    for p_ in ps:
        if q in text(p_.get("contentText")).lower(): matches += 1
    if page in (1, 2, 3, 10, 30, 60) or not ps:
        print(f" page {page}: posts={len(ps)} total={total} matches={matches} oldest={text(ps[-1].get('publishedTimeText')) if ps else '-'} t={time.time()-t0:.1f}s")
    tok = list_token(r)
    if not tok or not ps:
        print("  end: token" , bool(tok)); break
    r = post("browse", {"context": ctx("WEB"), "continuation": tok})
print(f" DONE pages={page} posts={total} matches={matches} t={time.time()-t0:.1f}s")
