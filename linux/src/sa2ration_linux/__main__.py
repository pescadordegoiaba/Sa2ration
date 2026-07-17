from __future__ import annotations

import argparse
import json
import sys

from sa2ration_linux.controller import DisplayController
from sa2ration_linux.daemon import run_daemon


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Sa2ration Linux — controles LCD seguros")
    parser.add_argument("--diagnose", action="store_true", help="imprime diagnóstico JSON")
    parser.add_argument("--apply-profile", metavar="NOME", help="aplica um perfil integrado")
    parser.add_argument("--reset", action="store_true", help="restaura valores neutros")
    parser.add_argument("--restore", action="store_true", help="restaura a última configuração estável")
    parser.add_argument("--daemon", action="store_true", help="restaura e mantém o perfil da sessão")
    return parser


def main() -> int:
    args = _parser().parse_args()
    if args.daemon:
        return run_daemon()
    if args.diagnose or args.apply_profile or args.reset or args.restore:
        controller = DisplayController()
        if args.diagnose:
            print(json.dumps(controller.diagnostic(), ensure_ascii=False, indent=2))
            return 0
        if not controller.monitors:
            print("Nenhum monitor compatível detectado", file=sys.stderr)
            return 2
        monitor = next((item for item in controller.monitors if controller.current and item.id == controller.current.monitor_id), controller.monitors[0])
        if args.apply_profile:
            result = controller.apply_profile(monitor, args.apply_profile)
        elif args.restore:
            result = controller.restore_stable(monitor)
        else:
            result = controller.neutral(monitor)
        if not result.success:
            print("; ".join(result.errors), file=sys.stderr)
            return 3
        print("; ".join(result.messages))
        return 0
    from PySide6.QtWidgets import QApplication
    from sa2ration_linux.ui import MainWindow, STYLE

    app = QApplication(sys.argv)
    app.setApplicationName("Sa2ration Linux")
    app.setOrganizationName("Sa2ration")
    app.setStyleSheet(STYLE)
    window = MainWindow()
    window.show()
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
