"""Xray-owned TUN (main core: Xray).

Xray's own ``tun`` inbound creates the Wintun adapter, routes the default
routes into it (``autoSystemRoutingTable``) and binds every outbound to the
physical interface (``autoOutboundsInterface``), so the proxy server's own
traffic never loops back into the tunnel and no sing-box is involved.
"""
from __future__ import annotations

from ipaddress import ip_address, ip_network
from pathlib import Path
from typing import TYPE_CHECKING, Any

from ...constants import XRAY_PATH_DEFAULT
from ...path_utils import resolve_configured_path
from ...xray_inbounds import build_xray_sniffing

if TYPE_CHECKING:
    from ...models import AppSettings, RoutingSettings

XRAY_TUN_INBOUND_TAG = "tun-in"
XRAY_TUN_INTERFACE = "lumen_xray"
XRAY_DNS_OUTBOUND_TAG = "dns-out"
_TUN_GATEWAY = ["172.19.0.1/30", "fdfe:dcba:9877::1/126"]
# The adapter's DNS server lives inside the tunnel, so every system lookup
# reaches Xray and is answered by its DNS module (hijacked below).
_TUN_DNS = "172.19.0.2"


def xray_wintun_dll(xray_path: str) -> Path | None:
    """wintun.dll next to the configured xray.exe, or None when it is missing."""
    exe = resolve_configured_path(
        xray_path,
        default_path=XRAY_PATH_DEFAULT,
        use_default_if_empty=True,
        migrate_default_location=True,
    )
    if exe is None:
        return None
    dll = Path(exe).parent / "wintun.dll"
    return dll if dll.is_file() else None


def _ensure_outbound(outbounds: list[Any], tag: str, protocol: str) -> None:
    if any(isinstance(item, dict) and str(item.get("tag") or "") == tag for item in outbounds):
        return
    outbounds.append({"tag": tag, "protocol": protocol})


def _doh_server(address: str, kind: str) -> str:
    address = str(address or "").strip()
    if not address:
        return ""
    if "://" in address:
        return address
    try:
        ip_address(address)
    except ValueError:
        # A DoH host name would need DNS to reach DNS; keep plain lookups on IPs.
        return ""
    if kind == "https":
        return f"https://{address}/dns-query"
    if kind == "tcp":
        return f"tcp://{address}"
    return address


def _build_dns(routing: RoutingSettings) -> dict[str, Any]:
    kind = str(getattr(routing, "dns_bootstrap_type", "https") or "https").lower()
    servers = [
        server
        for server in (_doh_server(item, kind) for item in getattr(routing, "dns_bootstrap_servers", []) or [])
        if server
    ] or ["https://1.1.1.1/dns-query", "https://8.8.8.8/dns-query"]
    strategy = str(getattr(routing, "dns_bootstrap_strategy", "ipv4_only") or "").lower()
    dns: dict[str, Any] = {
        "servers": servers,
        "queryStrategy": "UseIPv4" if strategy == "ipv4_only" else "UseIP",
        "tag": "dns-internal",
    }
    hosts = getattr(routing, "dns_hosts", None) or {}
    if isinstance(hosts, dict) and hosts:
        dns["hosts"] = {str(name): list(values) for name, values in hosts.items() if values}
    return dns


def _route_exclude_cidrs(routing: RoutingSettings) -> list[str]:
    cidrs: list[str] = []
    for item in getattr(routing, "tun_route_exclude_address", []) or []:
        try:
            cidrs.append(str(ip_network(str(item).strip(), strict=False)))
        except ValueError:
            continue
    return cidrs


def apply_xray_tun(payload: dict[str, Any], settings: AppSettings, routing: RoutingSettings) -> None:
    """Add the TUN inbound and its DNS/route contract to a runtime Xray config."""
    route_only = bool(getattr(settings, "sniff_route_only", False))
    tun_inbound = {
        "tag": XRAY_TUN_INBOUND_TAG,
        "protocol": "tun",
        "settings": {
            "name": XRAY_TUN_INTERFACE,
            "mtu": max(1280, min(int(getattr(settings, "tun_mtu", 1500) or 1500), 9000)),
            "gateway": list(_TUN_GATEWAY),
            "autoSystemRoutingTable": ["0.0.0.0/0", "::/0"],
            "autoOutboundsInterface": "auto",
        },
        "sniffing": {**build_xray_sniffing(route_only=route_only), "destOverride": ["http", "tls", "quic"]},
    }
    hijack_dns = bool(getattr(routing, "dns_hijack_enabled", True)) and str(
        getattr(routing, "dns_mode", "builtin") or "builtin"
    ) != "system"
    if hijack_dns:
        tun_inbound["settings"]["dns"] = [_TUN_DNS]

    inbounds = payload.setdefault("inbounds", [])
    inbounds[:] = [
        item for item in inbounds
        if not (isinstance(item, dict) and str(item.get("tag") or "") == XRAY_TUN_INBOUND_TAG)
    ]
    inbounds.append(tun_inbound)

    outbounds = payload.setdefault("outbounds", [])
    _ensure_outbound(outbounds, "direct", "freedom")
    _ensure_outbound(outbounds, "block", "blackhole")

    route = payload.setdefault("routing", {})
    rules = route.setdefault("rules", [])
    head: list[dict[str, Any]] = []
    if hijack_dns:
        _ensure_outbound(outbounds, XRAY_DNS_OUTBOUND_TAG, "dns")
        if not isinstance(payload.get("dns"), dict):
            payload["dns"] = _build_dns(routing)
        head.append({
            "type": "field",
            "inboundTag": [XRAY_TUN_INBOUND_TAG],
            "port": "53",
            "outboundTag": XRAY_DNS_OUTBOUND_TAG,
        })
    excluded = _route_exclude_cidrs(routing)
    if excluded:
        head.append({"type": "field", "ip": excluded, "outboundTag": "direct"})
    if bool(getattr(settings, "tun_block_quic", True)):
        # Browsers fall back to HTTP/2 through the proxy (same as sing-box TUN).
        head.append({
            "type": "field",
            "inboundTag": [XRAY_TUN_INBOUND_TAG],
            "network": "udp",
            "port": "443",
            "outboundTag": "block",
        })
    rules[0:0] = head
