from __future__ import annotations

from types import SimpleNamespace

import pytest

from xray_fluent.application.node_runtime_service import (
    node_supported_by_core,
    node_uses_xray_sidecar,
    proxy_core_for_node,
)
from xray_fluent.engines.singbox import runtime_planner as planner
from xray_fluent.engines.xray.tun import XRAY_TUN_INBOUND_TAG, apply_xray_tun
from xray_fluent.link_parser import parse_single
from xray_fluent.models import AppSettings, RoutingSettings, normalize_core_preference

UUID = "6f1c2d3e-4b5a-4c7d-8e9f-0a1b2c3d4e5f"
PBK = "Gk3x9sQ2mVb7Lr1tYzW8eHn4PcJ6uKdA0fXoTiRqS5E"


def _both_cores_node():
    return parse_single(
        f"vless://{UUID}@nl.example.net:443?security=reality&sni=www.microsoft.com&fp=chrome&pbk={PBK}&sid=a1&type=tcp#nl"
    )


def _singbox_only_node():
    return parse_single(f"hysteria2://{UUID}@pl.example.net:8443?sni=pl.example.net#pl")


def _xray_only_node():
    node = _both_cores_node()
    node.outbound["streamSettings"]["finalmask"] = {"type": "x"}
    return node


def test_core_preference_is_normalized_and_persisted() -> None:
    assert normalize_core_preference("sing-box-extended") == "singbox"
    assert normalize_core_preference("XRAY") == "xray"
    assert normalize_core_preference("nonsense") == "auto"
    settings = AppSettings()
    assert settings.core_preference == "auto"
    settings.core_preference = "xray"
    assert AppSettings.from_dict(settings.to_dict()).core_preference == "xray"
    assert AppSettings.from_dict({}).core_preference == "auto"


def test_auto_keeps_the_hybrid_and_fixed_cores_are_strict() -> None:
    nodes = (_both_cores_node(), _singbox_only_node(), _xray_only_node())
    assert [proxy_core_for_node(n, "auto") for n in nodes] == ["singbox", "singbox", "xray"]
    assert [proxy_core_for_node(n, "xray") for n in nodes] == ["xray", "xray", "xray"]
    assert [proxy_core_for_node(n, "singbox") for n in nodes] == ["singbox", "singbox", "singbox"]
    assert proxy_core_for_node(None, "auto") == "xray"
    assert proxy_core_for_node(None, "singbox") == "singbox"
    assert [node_supported_by_core(n, "auto") for n in nodes] == [True, True, True]
    assert [node_supported_by_core(n, "xray") for n in nodes] == [True, False, True]
    assert [node_supported_by_core(n, "singbox") for n in nodes] == [True, True, False]


def test_only_auto_tun_uses_an_xray_sidecar() -> None:
    nodes = (_both_cores_node(), _singbox_only_node(), _xray_only_node())
    assert [node_uses_xray_sidecar(n, "auto") for n in nodes] == [False, False, True]
    assert [node_uses_xray_sidecar(n, "singbox") for n in nodes] == [False, False, False]
    assert planner.classify_node_for_singbox(nodes[2], "auto") == "hybrid_xray_sidecar"


def test_singbox_only_core_refuses_an_xray_server_instead_of_a_sidecar() -> None:
    document = SimpleNamespace(
        payload={"outbounds": [{"type": "direct", "tag": "proxy"}], "inbounds": []},
        source_path=None,
        text_hash="",
        has_proxy_outbound=True,
    )
    with pytest.raises(ValueError, match="требует ядро Xray"):
        planner.plan_singbox_runtime(document, _xray_only_node(), core_preference="singbox")


def test_xray_servers_never_join_the_singbox_selector() -> None:
    selected = _singbox_only_node()
    other_sb = parse_single(f"hysteria2://{UUID}@de.example.net:8443?sni=de.example.net#de")
    both, x_only = _both_cores_node(), _xray_only_node()
    for preference in ("auto", "singbox"):
        config = {"outbounds": [{"type": "direct", "tag": "proxy"}]}
        _selector, signatures = planner._ensure_singbox_hot_switch_selector(
            config,
            selected_node=selected,
            candidate_nodes=[other_sb, both, x_only],
            multiplex_enabled=False,
            multiplex_concurrency=8,
            core_preference=preference,
        )
        members = {node_id for node_id, _ in signatures}
        assert both.id in members and x_only.id not in members


def test_xray_tun_binds_the_physical_adapter_and_answers_dns_itself() -> None:
    from xray_fluent.network_route_context import WindowsDefaultRouteContext

    payload = {
        "inbounds": [{"tag": "socks-in", "protocol": "mixed", "port": 10808}],
        "outbounds": [{"tag": "proxy", "protocol": "vless"}, {"tag": "direct", "protocol": "freedom"}],
        "routing": {"rules": [{"type": "field", "network": "tcp,udp", "outboundTag": "direct"}]},
    }
    settings = AppSettings()
    settings.tun_mode = True
    settings.core_preference = "xray"
    settings.sniff_route_only = True
    routing = RoutingSettings()
    routing.tun_route_exclude_address = ["192.168.50.0/24", "bad"]
    context = WindowsDefaultRouteContext(interface_alias="Ethernet", dns_servers=("159.194.200.33", "fe80::1"))
    apply_xray_tun(payload, settings, routing, context)

    tun = next(item for item in payload["inbounds"] if item["tag"] == XRAY_TUN_INBOUND_TAG)
    assert tun["protocol"] == "tun"
    assert tun["settings"]["autoSystemRoutingTable"] == ["0.0.0.0/0", "::/0"]
    # "auto" did not bind on real machines: everything looped into the tunnel.
    assert tun["settings"]["autoOutboundsInterface"] == "Ethernet"
    assert tun["sniffing"]["routeOnly"] is False
    by_tag = {item["tag"]: item for item in payload["outbounds"]}
    assert by_tag["proxy"]["streamSettings"]["sockopt"]["interface"] == "Ethernet"
    assert by_tag["direct"]["streamSettings"]["sockopt"]["interface"] == "Ethernet"
    assert by_tag["direct"]["settings"]["domainStrategy"] == "UseIPv4"
    assert by_tag["dns-out"]["protocol"] == "dns"
    # Xray answers the tunnel resolver from the physical adapter's DNS.
    assert payload["dns"]["servers"][0] == "159.194.200.33"
    rules = payload["routing"]["rules"]
    assert rules[0] == {"type": "field", "inboundTag": ["tun-in"], "port": "53", "outboundTag": "dns-out"}
    assert rules[1] == {"type": "field", "inboundTag": ["dns-internal"], "outboundTag": "direct"}
    assert {"type": "field", "ip": ["192.168.50.0/24"], "outboundTag": "direct"} in rules
    assert any(rule.get("outboundTag") == "block" and rule.get("port") == "443" for rule in rules)
    assert rules[-1] == {"type": "field", "network": "tcp,udp", "outboundTag": "direct"}


def test_xray_routes_local_proxy_traffic_to_the_proxy_like_singbox() -> None:
    from xray_fluent.routing_runtime import apply_xray_gui_routing

    payload = {
        "inbounds": [
            {"tag": "socks-in", "protocol": "mixed"},
            {"tag": "http-in", "protocol": "http"},
            {"tag": "discord-socks-in", "protocol": "socks"},
            {"tag": "__app_health_x", "protocol": "http"},
        ],
        "routing": {"rules": []},
    }
    settings = AppSettings()
    routing = RoutingSettings()
    routing.preset_id = "except_ru"
    routing.tun_default_outbound = "direct"
    apply_xray_gui_routing(payload, routing, settings)
    rules = payload["routing"]["rules"]
    local = {"type": "field", "inboundTag": ["socks-in", "http-in"], "outboundTag": "proxy"}
    # After the explicit rules (RU direct, LAN, ...) and before the catch-all.
    assert rules[-2] == local
    assert rules.index(local) > next(i for i, r in enumerate(rules) if "geosite:category-ru" in (r.get("domain") or []))
