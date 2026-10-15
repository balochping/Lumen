from __future__ import annotations

import subprocess

from xray_fluent import subprocess_utils


def test_run_text_pumped_runs_directly_without_gui_thread(monkeypatch) -> None:
    expected = subprocess.CompletedProcess(["tool"], 0, b"ok", b"")
    calls: list[tuple[list[str], float, bool, int | None]] = []

    def fake_run_text(command, *, timeout, check, creationflags):
        calls.append((command, timeout, check, creationflags))
        return expected

    monkeypatch.setattr(subprocess_utils, "_qt_app_on_current_thread", lambda: None)
    monkeypatch.setattr(subprocess_utils, "run_text", fake_run_text)

    result = subprocess_utils.run_text_pumped(
        ["tool"],
        timeout=2.5,
        check=True,
        creationflags=123,
    )

    assert result is expected
    assert calls == [(["tool"], 2.5, True, 123)]


def test_gui_thread_waits_keep_the_event_loop_running() -> None:
    import sys
    import time

    from PyQt6.QtCore import QCoreApplication, QProcess, QTimer

    app = QCoreApplication.instance() or QCoreApplication(sys.argv)
    ticks: list[float] = []
    timer = QTimer()
    timer.setInterval(5)
    timer.timeout.connect(lambda: ticks.append(time.monotonic()))
    timer.start()
    try:
        subprocess_utils.sleep_with_events(0.2)
        # The old 50 ms blocking slices allowed at most ~4 ticks here; keep
        # the bound loose so a loaded CI machine does not make it flaky.
        assert len(ticks) >= 10

        process = QProcess()
        process.setProgram(sys.executable)
        process.setArguments(["-c", "import time; time.sleep(30)"])
        process.start()
        assert subprocess_utils.wait_for_qprocess_started(process, 5000)
        started = time.monotonic()
        process.kill()
        assert subprocess_utils.wait_for_qprocess_finished(process, 5000)
        assert time.monotonic() - started < 2.0
    finally:
        timer.stop()
    assert app is QCoreApplication.instance()
