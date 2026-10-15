from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, TimeoutError as FutureTimeoutError
import locale
import os
import subprocess
import time
from pathlib import Path
from typing import Any, Callable


CREATE_NO_WINDOW = 0x08000000 if os.name == "nt" else 0
_SUBPROCESS_EXECUTOR = ThreadPoolExecutor(max_workers=4, thread_name_prefix="xray_fluent_subprocess")


def is_windows_shutting_down() -> bool:
    """Return True once Windows has started ending the interactive session."""
    if os.name != "nt":
        return False
    try:
        import ctypes

        return bool(ctypes.windll.user32.GetSystemMetrics(0x2000))
    except Exception:
        return False


def decode_output(data: bytes | None) -> str:
    if not data:
        return ""
    encodings: list[str] = ["utf-8"]
    if os.name == "nt":
        encodings.extend(["cp866", locale.getpreferredencoding(False), "cp1251"])
    for encoding in dict.fromkeys(encodings):
        try:
            return data.decode(encoding)
        except (LookupError, UnicodeDecodeError):
            continue
    return data.decode("utf-8", errors="replace")


def result_output_text(result: subprocess.CompletedProcess[bytes]) -> str:
    return decode_output(result.stdout or result.stderr or b"")


def run_text(
    command: list[str],
    *,
    timeout: float,
    check: bool = False,
    creationflags: int | None = None,
) -> subprocess.CompletedProcess[bytes]:
    kwargs: dict[str, int | bool | float] = {
        "capture_output": True,
        "text": False,
        "timeout": timeout,
        "check": check,
    }
    if creationflags is not None:
        kwargs["creationflags"] = creationflags
    return subprocess.run(command, **kwargs)


def _qt_app_on_current_thread() -> Any | None:
    try:
        from PyQt6.QtCore import QThread
        from PyQt6.QtWidgets import QApplication
    except Exception:
        return None

    app = QApplication.instance()
    if app is None or QThread.currentThread() != app.thread():
        return None
    return app


def pump_qt_events() -> None:
    app = _qt_app_on_current_thread()
    if app is None:
        return
    from PyQt6.QtCore import QEventLoop

    app.processEvents(QEventLoop.ProcessEventsFlag.ExcludeUserInputEvents)


def _wait_in_event_loop(done: Callable[[], bool], timeout_sec: float, *, poll_ms: int = 4) -> bool:
    """Wait on the GUI thread without ever blocking it.

    A nested event loop keeps painting and animations at full frame rate while
    the condition is pending (the old fixed 50 ms blocking slices capped the
    UI at ~20 fps and stuttered). User input stays queued until the wait ends,
    as before, so a second click cannot re-enter the operation being waited on.
    """
    if done():
        return True
    from PyQt6.QtCore import QEventLoop, Qt, QTimer

    loop = QEventLoop()
    deadline = QTimer()
    deadline.setSingleShot(True)
    deadline.setTimerType(Qt.TimerType.PreciseTimer)
    deadline.timeout.connect(loop.quit)
    poll = QTimer()
    poll.setTimerType(Qt.TimerType.PreciseTimer)
    poll.setInterval(max(1, int(poll_ms)))

    def check() -> None:
        if done():
            loop.quit()

    poll.timeout.connect(check)
    deadline.start(max(0, int(timeout_sec * 1000)))
    poll.start()
    try:
        loop.exec(QEventLoop.ProcessEventsFlag.ExcludeUserInputEvents)
    finally:
        poll.stop()
        deadline.stop()
    return done()


def sleep_with_events(duration_sec: float, *, step_sec: float = 0.05) -> None:
    deadline = time.monotonic() + max(0.0, duration_sec)
    if _qt_app_on_current_thread() is not None:
        _wait_in_event_loop(lambda: time.monotonic() >= deadline, max(0.0, duration_sec), poll_ms=8)
        return
    while True:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            return
        time.sleep(min(step_sec, remaining))


def _wait_for_qprocess_call(
    process: Any,
    method_name: str,
    timeout_ms: int,
    *,
    slice_ms: int = 50,
) -> bool:
    waiter = getattr(process, method_name)
    state = getattr(process, "state", None)
    if _qt_app_on_current_thread() is not None and callable(state) and method_name in (
        "waitForStarted",
        "waitForFinished",
    ):
        from PyQt6.QtCore import QProcess

        not_running = QProcess.ProcessState.NotRunning
        running = QProcess.ProcessState.Running
        if method_name == "waitForStarted":
            # Settled once the launch either succeeded or failed.
            _wait_in_event_loop(lambda: state() != QProcess.ProcessState.Starting, timeout_ms / 1000.0)
            return state() == running
        _wait_in_event_loop(lambda: state() == not_running, timeout_ms / 1000.0)
        return state() == not_running

    remaining = max(0, int(timeout_ms))
    while remaining > 0:
        step = min(slice_ms, remaining)
        if waiter(step):
            return True
        pump_qt_events()
        remaining -= step

    pump_qt_events()
    return False


def wait_for_qprocess_started(process: Any, timeout_ms: int) -> bool:
    return _wait_for_qprocess_call(process, "waitForStarted", timeout_ms)


def wait_for_qprocess_finished(process: Any, timeout_ms: int) -> bool:
    return _wait_for_qprocess_call(process, "waitForFinished", timeout_ms)


def wait_for_qprocess_ready_read(process: Any, timeout_ms: int) -> bool:
    return _wait_for_qprocess_call(process, "waitForReadyRead", timeout_ms)


def wait_for_future_with_events(future: Any, timeout_sec: float, *, slice_sec: float = 0.05) -> Any:
    if _qt_app_on_current_thread() is not None:
        if not _wait_in_event_loop(future.done, max(0.0, float(timeout_sec))):
            raise TimeoutError("Future did not complete before timeout")
        return future.result()
    deadline = time.monotonic() + max(0.0, float(timeout_sec))
    while True:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            raise TimeoutError("Future did not complete before timeout")
        try:
            return future.result(timeout=min(slice_sec, remaining))
        except FutureTimeoutError:
            pass


def run_text_pumped(
    command: list[str],
    *,
    timeout: float,
    check: bool = False,
    creationflags: int | None = None,
) -> subprocess.CompletedProcess[bytes]:
    if _qt_app_on_current_thread() is None:
        return run_text(
            command,
            timeout=timeout,
            check=check,
            creationflags=creationflags,
        )
    future = _SUBPROCESS_EXECUTOR.submit(
        run_text,
        command,
        timeout=timeout,
        check=check,
        creationflags=creationflags,
    )
    return wait_for_future_with_events(future, timeout + 0.5)


def is_same_path(left: str | Path | None, right: str | Path | None) -> bool:
    if not left or not right:
        return False
    try:
        return Path(left).resolve() == Path(right).resolve()
    except Exception:
        return False


def kill_processes_by_path(process_name: str, executable_path: str | Path, *, timeout: float = 5.0) -> bool:
    # Compatibility shim: a path/name is not process ownership. Runtime managers
    # already terminate/wait on their retained Popen handles. Do not kill a
    # different instance or a concurrent probe as an "orphan" fallback.
    return False
