from __future__ import annotations

from typing import Any

from .models import Node


_UDP_PROTOCOLS = {"awg", "hysteria", "hysteria2", "masque", "tuic", "warp", "wireguard"}
_TCP_PROTOCOLS = {"http", "shadowsocks", "socks", "trojan", "vless", "vmess"}


def node_transport(node: Node) -> str:
    """Return a compact transport label for server lists and sorting."""
    outbound = node.outbound if isinstance(node.outbound, dict) else {}
    stream = outbound.get("streamSettings")
    if isinstance(stream, dict):
        network = _normalize_transport(stream.get("network"))
        if network:
            return network

    native = outbound.get("singbox")
    if isinstance(native, dict):
        native_type = str(native.get("type") or "").strip().lower()
        if native_type == "naive":
            return "QUIC" if native.get("quic") is True else "H2"
        if native_type in {"openvpn", "openvpn-client"}:
            return _normalize_transport(native.get("proto") or native.get("network")) or "UDP"
        transport = native.get("transport")
        if isinstance(transport, str) and native_type == "mieru":
            return f"MIERU/{transport.upper()}"
        if native_type in {"mieru", "masque"}:
            return native_type.upper()
        if isinstance(transport, dict):
            transport_type = _normalize_transport(transport.get("type"))
            if transport_type:
                return transport_type

    protocol = str(outbound.get("protocol") or node.scheme or "").strip().lower()
    if protocol in _UDP_PROTOCOLS:
        return "UDP"
    if protocol == "xray_config":
        return "AUTO"
    if protocol in {"mieru", "naive", "singbox_config"}:
        return protocol.upper()
    if protocol in _TCP_PROTOCOLS:
        return "TCP"
    return "—"


def _normalize_transport(value: Any) -> str:
    network = str(value or "").strip().lower()
    aliases = {"raw": "TCP", "http": "H2"}
    if network in aliases:
        return aliases[network]
    return network.upper() if network else ""


_PROTOCOL_NAMES = {
    "vless": "VLESS", "vmess": "VMess", "trojan": "Trojan", "ss": "Shadowsocks",
    "shadowsocks": "Shadowsocks", "socks": "SOCKS", "socks5": "SOCKS", "http": "HTTP",
    "https": "HTTPS", "hysteria": "Hysteria", "hy": "Hysteria", "hysteria2": "Hysteria2",
    "hy2": "Hysteria2", "tuic": "TUIC", "wireguard": "WireGuard", "wg": "WireGuard",
    "awg": "AmneziaWG", "warp": "WARP", "naive": "Naive", "masque": "MASQUE",
    "mieru": "Mieru", "anytls": "AnyTLS", "openvpn": "OpenVPN", "snell": "Snell",
    "xray_config": "Xray JSON", "singbox_config": "sing-box JSON", "auto": "AUTO",
}


def node_protocol_label(node: Node) -> str:
    """Human protocol label for compact server captions, e.g. ``VLESS Reality``."""
    outbound = node.outbound if isinstance(node.outbound, dict) else {}
    native = outbound.get("singbox") if isinstance(outbound.get("singbox"), dict) else {}
    raw = str(node.scheme or outbound.get("protocol") or native.get("type") or "").strip().lower()
    name = _PROTOCOL_NAMES.get(raw, raw.upper())
    security = ""
    stream = outbound.get("streamSettings")
    if isinstance(stream, dict):
        security = str(stream.get("security") or "").strip().lower()
    tls = native.get("tls") if isinstance(native.get("tls"), dict) else {}
    if not security and tls:
        reality = tls.get("reality")
        security = "reality" if isinstance(reality, dict) and (reality.get("enabled") or reality) else ("tls" if tls.get("enabled") else "")
    if security == "reality":
        return f"{name} Reality".strip()
    if security == "tls" and raw in {"vless", "vmess", "trojan"}:
        return f"{name} TLS".strip()
    return name or "—"
