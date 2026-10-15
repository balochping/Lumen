"""Report GUI-thread stalls with the Python stack that caused them.

A 50 ms heartbeat timer runs on the GUI thread; a daemon thread watches it.
When the heartbeat is late by more than the threshold, the GUI thread's stack
is sampled, and once the thread is free again one log line names the stall's
duration and the code it was stuck in. The full stack goes to the debug log.

``LUMEN_UI_STALL_MS`` overrides the threshold (``0`` turns the monitor off).
"""
from __future__ import annotations

import logging
import os
import sys
import threading
import time
import traceback

from PyQt6.QtCore import QObject, QTimer

log = logging.getLogger("xray_fluent.app")

_HEARTBEAT_MS = 50
_POLL_SEC = 0.025
_DEFAULT_THRESHOLD_MS = 250
_PACKAGE_MARKER = "xray_fluent"


def _threshold_ms() -> int:
    raw = os.environ.get("LUMEN_UI_STALL_MS", "").strip()
    if not raw:
        return _DEFAULT_THRESHOLD_MS
    try:
        return max(0, int(raw))
    except ValueError:
        return _DEFAULT_THRESHOLD_MS


def _own_frames(stack: traceback.StackSummary) -> list[traceback.FrameSummary]:
    """Frames from Lumen's own code, innermost last."""
    return [frame for frame in stack if _PACKAGE_MARKER in frame.filename.replace("\\", "/")]


def _short(frame: traceback.FrameSummary) -> str:
    path = frame.filename.replace("\\", "/")
    index = path.rfind(_PACKAGE_MARKER + "/")
    name = path[index + len(_PACKAGE_MARKER) + 1:] if index >= 0 else os.path.basename(path)
    return f"{name}:{frame.lineno} {frame.name}"


class UiStallMonitor(QObject):
    def __init__(self, parent: QObject | None = None, *, threshold_ms: int | None = None) -> None:
        super().__init__(parent)
        self._threshold = (_threshold_ms() if threshold_ms is None else max(0, threshold_ms)) / 1000.0
        self._gui_ident = threading.get_ident()
        self._beat = time.monotonic()
        self._stop = threading.Event()
        self._thread: threading.Thread | None = None
        self._timer = QTimer(self)
        self._timer.setInterval(_HEARTBEAT_MS)
        self._timer.timeout.connect(self._on_beat)

    @property
    def enabled(self) -> bool:
        return self._threshold > 0

    def start(self) -> None:
        if not self.enabled or self._thread is not None:
            return
        self._beat = time.monotonic()
        self._timer.start()
        self._thread = threading.Thread(target=self._watch, name="lumen-ui-stall", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._timer.stop()
        self._stop.set()

    def _on_beat(self) -> None:
        self._beat = time.monotonic()

    def _sample(self) -> traceback.StackSummary | None:
        frame = sys._current_frames().get(self._gui_ident)
        if frame is None:
            return None
        return traceback.extract_stack(frame)

    def _watch(self) -> None:
        stall_started = 0.0
        samples: list[traceback.StackSummary] = []
        last_poll = time.monotonic()
        while not self._stop.wait(_POLL_SEC):
            now = time.monotonic()
            # The whole process was suspended (sleep, debugger): not a stall.
            suspended = now - last_poll > max(1.0, self._threshold * 2)
            last_poll = now
            beat = self._beat
            late = now - beat - _HEARTBEAT_MS / 1000.0
            if suspended:
                stall_started = 0.0
                samples.clear()
                continue
            if late > self._threshold:
                if not stall_started:
                    stall_started = beat + _HEARTBEAT_MS / 1000.0
                if len(samples) < 8:
                    stack = self._sample()
                    if stack is not None:
                        samples.append(stack)
                continue
            if stall_started:
                self._report(beat - stall_started, samples)
                stall_started = 0.0
                samples = []

    @staticmethod
    def _report(duration: float, samples: list[traceback.StackSummary]) -> None:
        if not samples:
            return
        # The innermost Lumen frame seen most often is where the time went.
        counts: dict[str, int] = {}
        for stack in samples:
            own = _own_frames(stack)
            if own:
                key = _short(own[-1])
                counts[key] = counts.get(key, 0) + 1
        where = max(counts, key=counts.get) if counts else "Qt/QML"
        log.warning("[ui] GUI thread blocked for %d ms in %s", int(duration * 1000), where)
        own = _own_frames(samples[0]) or list(samples[0])
        log.debug("[ui] stall stack:\n%s", "".join(traceback.format_list(own[-12:])))
