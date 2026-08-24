import ipaddress
import re
import socket
from urllib.parse import urlparse
import httpx
from bs4 import BeautifulSoup

MAX_BYTES = 2_000_000

class UnsafeUrl(ValueError):
    pass

def _validate_public_url(url: str) -> None:
    p = urlparse(url)
    if p.scheme not in ("http", "https") or not p.hostname:
        raise UnsafeUrl("Only public http/https URLs are allowed")
    host = p.hostname.lower()
    if host in {"localhost", "localhost.localdomain"} or host.endswith(".local"):
        raise UnsafeUrl("Local addresses are blocked")
    try:
        for info in socket.getaddrinfo(host, p.port or (443 if p.scheme == "https" else 80), type=socket.SOCK_STREAM):
            ip = ipaddress.ip_address(info[4][0])
            if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_multicast or ip.is_reserved:
                raise UnsafeUrl("Private/reserved addresses are blocked")
    except socket.gaierror as e:
        raise UnsafeUrl("Host resolution failed") from e

async def fetch_clean_text(url: str) -> str:
    _validate_public_url(url)
    headers = {"User-Agent": "MG-AI-Research/0.4 (+research-bot)"}
    async with httpx.AsyncClient(timeout=15.0, follow_redirects=True, headers=headers) as client:
        r = await client.get(url)
        r.raise_for_status()
        content_type = r.headers.get("content-type", "")
        if "text/html" not in content_type and "text/plain" not in content_type:
            return ""
        raw = r.content[:MAX_BYTES]
    text = raw.decode(r.encoding or "utf-8", errors="ignore")
    if "text/html" in content_type:
        soup = BeautifulSoup(text, "html.parser")
        for tag in soup(["script", "style", "noscript", "svg", "nav", "footer"]):
            tag.decompose()
        text = soup.get_text(" ")
    text = re.sub(r"\s+", " ", text).strip()
    return text[:50_000]
