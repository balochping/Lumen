"""Xray-owned TUN (main core: Xray).

Xray's own ``tun`` inbound creates the Wintun adapter and routes the default
routes into it (``autoSystemRoutingTable``).  Every outbound is bound to the
physical adapter by name: ``autoOutboundsInterface: "auto"`` did not bind on
real Windows machines, so traffic to the proxy server and "direct" traffic
looped back into the tunnel and nothing loaded.

DNS: the adapter's resolver (172.19.0.2, set by Xray) is answered by Xray's
DNS module, which asks the physical adapter's DNS servers directly.  Xray also
resolves names for its own dials through that module, never through the
system resolver (which would point back into the tunnel).
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
    from ...network_route_context import WindowsDefaultRouteContext

XRAY_TUN_INBOUND_TAG = "tun-in"
XRAY_TUN_INTERFACE = "lumen_xray"
XRAY_DNS_OUTBOUND_TAG = "dns-out"
XRAY_DNS_TAG = "dns-internal"
_TUN_GATEWAY = ["172.19.0.1/30", "fdfe:dcba:9877::1/126"]
_TUN_DNS = "172.19.0.2"
_FALLBACK_DNS = ["https://1.1.1.1/dns-query", "https://8.8.8.8/dns-query"]


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


def _route_exclude_cidrs(routing: RoutingSettings) -> list[str]:
    cidrs: list[str] = []
    for item in getattr(routing, "tun_route_exclude_address", []) or []:
        try:
            cidrs.append(str(ip_network(str(item).strip(), strict=False)))
        except ValueError:
            continue
    return cidrs


def _physical_dns_servers(route_context: WindowsDefaultRouteContext | None) -> list[str]:
    servers: list[str] = []
    for value in getattr(route_context, "dns_servers", ()) or ():
        try:
            address = ip_address(str(value).strip())
        except ValueError:
            continue
        if address.version == 4 and not address.is_loopback and str(address) != _TUN_DNS:
            servers.append(str(address))
    return servers


def _outbound_interface(route_context: WindowsDefaultRouteContext | None) -> str:
    alias = str(getattr(route_context, "interface_alias", "") or "").strip()
    if alias and not getattr(route_context, "tun_active", False) and alias != XRAY_TUN_INTERFACE:
        return alias
    return "auto"


def apply_xray_tun(
    payload: dict[str, Any],
    settings: AppSettings,
    routing: RoutingSettings,
    route_context: WindowsDefaultRouteContext | None = None,
) -> None:
    """Add the TUN inbound and its DNS/route contract to a runtime Xray config.

    ``route_context`` is the physical default route read before the tunnel
    comes up (its adapter name and DNS servers).
    """
    interface = _outbound_interface(route_context)
    tun_inbound = {
        "tag": XRAY_TUN_INBOUND_TAG,
        "protocol": "tun",
        "settings": {
            "name": XRAY_TUN_INTERFACE,
            "mtu": max(1280, min(int(getattr(settings, "tun_mtu", 1500) or 1500), 9000)),
            "gateway": list(_TUN_GATEWAY),
            "dns": [_TUN_DNS],
            "autoSystemRoutingTable": ["0.0.0.0/0", "::/0"],
            "autoOutboundsInterface": interface,
        },
        # The sniffed name decides the route and replaces the destination, so
        # a blocked site is resolved by the proxy server even when the local
        # DNS answered with a stub address.
        "sniffing": {**build_xray_sniffing(route_only=False), "destOverride": ["http", "tls", "quic"]},
    }

    inbounds = payload.setdefault("inbounds", [])
    inbounds[:] = [
        item for item in inbounds
        if not (isinstance(item, dict) and str(item.get("tag") or "") == XRAY_TUN_INBOUND_TAG)
    ]
    inbounds.append(tun_inbound)

    outbounds = payload.setdefault("outbounds", [])
    _ensure_outbound(outbounds, "direct", "freedom")
    _ensure_outbound(outbounds, "block", "blackhole")
    _ensure_outbound(outbounds, XRAY_DNS_OUTBOUND_TAG, "dns")
    for outbound in outbounds:
        if not isinstance(outbound, dict):
            continue
        protocol = str(outbound.get("protocol") or "")
        if protocol == "freedom":
            # Resolve through Xray's DNS module, not the system resolver.
            settings_block = outbound.setdefault("settings", {})
            if isinstance(settings_block, dict):
                settings_block.setdefault("domainStrategy", "UseIPv4")
        if protocol not in {"blackhole", "dns"} and interface != "auto":
            stream = outbound.setdefault("streamSettings", {})
            if isinstance(stream, dict):
                sockopt = stream.setdefault("sockopt", {})
                if isinstance(sockopt, dict):
                    sockopt.setdefault("interface", interface)

    dns: dict[str, Any] = {
        "servers": [*_physical_dns_servers(route_context), *_FALLBACK_DNS],
        "queryStrategy": "UseIPv4",
        "tag": XRAY_DNS_TAG,
    }
    hosts = getattr(routing, "dns_hosts", None) or {}
    if isinstance(hosts, dict) and hosts:
        dns["hosts"] = {str(name): list(values) for name, values in hosts.items() if values}
    payload["dns"] = dns

    route = payload.setdefault("routing", {})
    rules = route.setdefault("rules", [])
    head: list[dict[str, Any]] = [
        {"type": "field", "inboundTag": [XRAY_TUN_INBOUND_TAG], "port": "53", "outboundTag": XRAY_DNS_OUTBOUND_TAG},
        # Xray's own lookups leave directly through the physical adapter.
        {"type": "field", "inboundTag": [XRAY_DNS_TAG], "outboundTag": "direct"},
    ]
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
