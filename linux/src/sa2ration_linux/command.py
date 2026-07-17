from __future__ import annotations

import subprocess
import time
from dataclasses import dataclass
from typing import Sequence


@dataclass(frozen=True)
class CommandResult:
    args: tuple[str, ...]
    exit_code: int
    stdout: str
    stderr: str
    timed_out: bool
    duration_ms: int

    @property
    def ok(self) -> bool:
        return self.exit_code == 0 and not self.timed_out


class CommandRunner:
    """Runs commands without a shell so UI values can never become shell code."""

    def run(self, args: Sequence[str], timeout: float = 8.0) -> CommandResult:
        safe_args = tuple(str(part) for part in args)
        if not safe_args or any("\x00" in part for part in safe_args):
            raise ValueError("Comando inválido")
        started = time.monotonic()
        try:
            process = subprocess.run(
                safe_args,
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False,
            )
            return CommandResult(
                safe_args,
                process.returncode,
                process.stdout.strip(),
                process.stderr.strip(),
                False,
                int((time.monotonic() - started) * 1000),
            )
        except subprocess.TimeoutExpired as error:
            return CommandResult(
                safe_args,
                -1,
                (error.stdout or "").strip() if isinstance(error.stdout, str) else "",
                (error.stderr or "").strip() if isinstance(error.stderr, str) else "Tempo esgotado",
                True,
                int((time.monotonic() - started) * 1000),
            )
        except OSError as error:
            return CommandResult(safe_args, 127, "", str(error), False, int((time.monotonic() - started) * 1000))
