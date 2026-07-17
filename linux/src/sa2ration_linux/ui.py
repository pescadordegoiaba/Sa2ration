from __future__ import annotations

import json

from PySide6.QtCore import QSignalBlocker, Qt, QTimer
from PySide6.QtGui import QFont
from PySide6.QtWidgets import (
    QApplication,
    QCheckBox,
    QComboBox,
    QDialog,
    QDoubleSpinBox,
    QFileDialog,
    QFormLayout,
    QGridLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QScrollArea,
    QSlider,
    QSpinBox,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

from sa2ration_linux.controller import DisplayController
from sa2ration_linux.models import DisplaySettings, MonitorInfo
from sa2ration_linux.profiles import BUILTIN_PROFILES


STYLE = """
QMainWindow { background: #11151d; }
QWidget { color: #e8ecf3; font-size: 14px; }
QWidget#content { background: #11151d; }
QScrollArea { border: 0; background: transparent; }
QGroupBox {
  background: #1a202b; border: 1px solid #30394a; border-radius: 12px;
  margin-top: 14px; padding: 18px 14px 14px 14px; font-weight: 600;
}
QGroupBox::title { subcontrol-origin: margin; left: 14px; padding: 0 6px; color: #9cc7ff; }
QComboBox, QSpinBox, QDoubleSpinBox, QTextEdit {
  background: #252d3a; border: 1px solid #3b465a; border-radius: 7px; padding: 7px;
}
QComboBox:focus, QSpinBox:focus, QDoubleSpinBox:focus { border-color: #66a6ff; }
QPushButton { background: #293345; border: 1px solid #43516a; border-radius: 8px; padding: 9px 14px; }
QPushButton:hover { background: #34425a; }
QPushButton#primary { background: #397dd1; border-color: #5b9cec; font-weight: 600; }
QPushButton#danger { color: #ffb6b6; }
QSlider::groove:horizontal { height: 6px; background: #30394a; border-radius: 3px; }
QSlider::handle:horizontal { background: #69a9ff; width: 18px; margin: -6px 0; border-radius: 9px; }
QLabel#muted { color: #9aa6b8; font-size: 12px; }
QLabel#status { background: #202a38; border-radius: 8px; padding: 9px; }
"""


class RollbackDialog(QDialog):
    def __init__(self, parent: QWidget, seconds: int = 15) -> None:
        super().__init__(parent)
        self.remaining = seconds
        self.kept = False
        self.setWindowTitle("Confirmar alteração")
        self.setModal(True)
        layout = QVBoxLayout(self)
        layout.addWidget(QLabel("A alteração pode dificultar o uso da tela."))
        self.countdown = QLabel()
        self.countdown.setAlignment(Qt.AlignmentFlag.AlignCenter)
        font = QFont(self.countdown.font())
        font.setPointSize(font.pointSize() + 8)
        font.setBold(True)
        self.countdown.setFont(font)
        layout.addWidget(self.countdown)
        buttons = QHBoxLayout()
        keep = QPushButton("Manter")
        revert = QPushButton("Reverter agora")
        keep.setObjectName("primary")
        keep.clicked.connect(self._keep)
        revert.clicked.connect(self.reject)
        buttons.addWidget(revert)
        buttons.addWidget(keep)
        layout.addLayout(buttons)
        self.timer = QTimer(self)
        self.timer.timeout.connect(self._tick)
        self.timer.start(1000)
        self._refresh()

    def _refresh(self) -> None:
        self.countdown.setText(f"Reversão automática em {self.remaining} s")

    def _tick(self) -> None:
        self.remaining -= 1
        self._refresh()
        if self.remaining <= 0:
            self.reject()

    def _keep(self) -> None:
        self.kept = True
        self.accept()


class DiagnosticDialog(QDialog):
    def __init__(self, parent: QWidget, report: dict) -> None:
        super().__init__(parent)
        self.report = report
        self.setWindowTitle("Compatibilidade e diagnóstico")
        self.resize(720, 560)
        layout = QVBoxLayout(self)
        text = QTextEdit()
        text.setReadOnly(True)
        text.setPlainText(json.dumps(report, ensure_ascii=False, indent=2))
        layout.addWidget(text)
        buttons = QHBoxLayout()
        export = QPushButton("Exportar JSON")
        close = QPushButton("Fechar")
        export.clicked.connect(self._export)
        close.clicked.connect(self.accept)
        buttons.addWidget(export)
        buttons.addStretch()
        buttons.addWidget(close)
        layout.addLayout(buttons)

    def _export(self) -> None:
        path, _ = QFileDialog.getSaveFileName(self, "Exportar diagnóstico", "sa2ration-diagnostico.json", "JSON (*.json)")
        if path:
            try:
                with open(path, "w", encoding="utf-8") as stream:
                    json.dump(self.report, stream, ensure_ascii=False, indent=2)
            except OSError as error:
                QMessageBox.critical(self, "Falha ao exportar", str(error))


class MainWindow(QMainWindow):
    def __init__(self, controller: DisplayController | None = None) -> None:
        super().__init__()
        self.controller = controller or DisplayController()
        self.monitor: MonitorInfo | None = None
        self._loading = False
        self.setWindowTitle("Sa2ration Linux — Controle LCD")
        self.setMinimumSize(580, 650)
        self.resize(760, 830)
        self._build()
        self._load_monitors()
        self.keepalive = QTimer(self)
        self.keepalive.timeout.connect(self._keep_temperature)
        self.keepalive.start(10_000)

    def _build(self) -> None:
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        content = QWidget()
        content.setObjectName("content")
        root = QVBoxLayout(content)
        root.setContentsMargins(22, 18, 22, 24)
        root.setSpacing(12)

        title = QLabel("Sa2ration Linux")
        title_font = QFont(title.font())
        title_font.setPointSize(title_font.pointSize() + 10)
        title_font.setBold(True)
        title.setFont(title_font)
        root.addWidget(title)
        subtitle = QLabel("Controles reais e seguros para monitores LCD no Arch Linux e Manjaro")
        subtitle.setObjectName("muted")
        subtitle.setWordWrap(True)
        root.addWidget(subtitle)

        monitor_box = QGroupBox("Monitor")
        monitor_layout = QVBoxLayout(monitor_box)
        self.monitor_combo = QComboBox()
        self.monitor_combo.currentIndexChanged.connect(self._monitor_changed)
        self.monitor_info = QLabel()
        self.monitor_info.setObjectName("muted")
        self.monitor_info.setWordWrap(True)
        monitor_layout.addWidget(self.monitor_combo)
        monitor_layout.addWidget(self.monitor_info)
        root.addWidget(monitor_box)

        self.profile_box = QGroupBox("Perfil rápido")
        profile_layout = QVBoxLayout(self.profile_box)
        profile_row = QHBoxLayout()
        self.profile_combo = QComboBox()
        for profile in BUILTIN_PROFILES:
            self.profile_combo.addItem(profile.name, profile)
        profile_apply = QPushButton("Usar perfil")
        profile_apply.clicked.connect(self._apply_profile)
        profile_row.addWidget(self.profile_combo, 1)
        profile_row.addWidget(profile_apply)
        profile_layout.addLayout(profile_row)
        self.profile_hint = QLabel(BUILTIN_PROFILES[0].description)
        self.profile_hint.setObjectName("muted")
        self.profile_hint.setWordWrap(True)
        self.profile_combo.currentIndexChanged.connect(lambda index: self.profile_hint.setText(self.profile_combo.itemData(index).description))
        profile_layout.addWidget(self.profile_hint)
        root.addWidget(self.profile_box)

        self.image_box = QGroupBox("Imagem")
        image_layout = QGridLayout(self.image_box)
        self.brightness_slider, self.brightness_input = self._numeric_control(5, 100, 100, "%")
        self.temperature_slider, self.temperature_input = self._numeric_control(1000, 10000, 6500, " K", step=100)
        image_layout.addWidget(QLabel("Brilho da sessão"), 0, 0, 1, 2)
        image_layout.addWidget(self.brightness_slider, 1, 0)
        image_layout.addWidget(self.brightness_input, 1, 1)
        brightness_help = QLabel("No KDE/Wayland usa o dimming do KScreen; não é o brilho físico do backlight.")
        brightness_help.setObjectName("muted")
        brightness_help.setWordWrap(True)
        image_layout.addWidget(brightness_help, 2, 0, 1, 2)
        image_layout.addWidget(QLabel("Temperatura de cor"), 3, 0, 1, 2)
        image_layout.addWidget(self.temperature_slider, 4, 0)
        image_layout.addWidget(self.temperature_input, 4, 1)
        temp_help = QLabel("KWin Night Light no KDE/Wayland; XRandR gama no X11. 6500 K é neutro.")
        temp_help.setObjectName("muted")
        temp_help.setWordWrap(True)
        image_layout.addWidget(temp_help, 5, 0, 1, 2)
        root.addWidget(self.image_box)

        self.gpu_box = QGroupBox("Processamento GPU")
        gpu_layout = QVBoxLayout(self.gpu_box)
        self.gpu_status = QLabel()
        self.gpu_status.setObjectName("muted")
        self.gpu_status.setWordWrap(True)
        gpu_layout.addWidget(self.gpu_status)
        self.gpu_switch = QCheckBox("Ativar brilho, contraste e saturação na GPU")
        gpu_layout.addWidget(self.gpu_switch)
        self.gpu_controls = QWidget()
        gpu_grid = QGridLayout(self.gpu_controls)
        gpu_grid.setContentsMargins(0, 8, 0, 0)
        self.gpu_brightness_slider, self.gpu_brightness_input = self._float_control(0.0, 3.0, 10.0, 1.0)
        self.gpu_contrast_slider, self.gpu_contrast_input = self._float_control(0.0, 5.0, 10.0, 1.0)
        self.gpu_saturation_slider, self.gpu_saturation_input = self._float_control(0.0, 5.0, 10.0, 1.0)
        for row, (name, slider, field) in enumerate((
            ("Brilho GPU", self.gpu_brightness_slider, self.gpu_brightness_input),
            ("Contraste GPU", self.gpu_contrast_slider, self.gpu_contrast_input),
            ("Saturação GPU", self.gpu_saturation_slider, self.gpu_saturation_input),
        )):
            gpu_grid.addWidget(QLabel(name), row * 2, 0, 1, 2)
            gpu_grid.addWidget(slider, row * 2 + 1, 0)
            gpu_grid.addWidget(field, row * 2 + 1, 1)
        gpu_help = QLabel("O slider usa uma faixa prática; o campo aceita até 10×. Valores extremos usam rollback de 15 segundos.")
        gpu_help.setObjectName("muted")
        gpu_help.setWordWrap(True)
        gpu_grid.addWidget(gpu_help, 6, 0, 1, 2)
        gpu_layout.addWidget(self.gpu_controls)
        self.gpu_switch.toggled.connect(self.gpu_controls.setVisible)
        self.gpu_controls.setVisible(False)
        root.addWidget(self.gpu_box)

        self.mode_box = QGroupBox("Resolução e taxa de atualização")
        mode_layout = QVBoxLayout(self.mode_box)
        self.mode_combo = QComboBox()
        mode_layout.addWidget(self.mode_combo)
        mode_help = QLabel("A troca é temporária por 15 segundos até ser confirmada.")
        mode_help.setObjectName("muted")
        mode_layout.addWidget(mode_help)
        root.addWidget(self.mode_box)

        self.ddc_box = QGroupBox("Controles físicos DDC/CI")
        self.ddc_box.setVisible(False)
        ddc_layout = QFormLayout(self.ddc_box)
        self.physical_brightness = QSpinBox()
        self.physical_brightness.setRange(0, 100)
        self.physical_contrast = QSpinBox()
        self.physical_contrast.setRange(0, 100)
        self.physical_brightness_label = QLabel("Brilho físico")
        self.physical_contrast_label = QLabel("Contraste físico")
        ddc_layout.addRow(self.physical_brightness_label, self.physical_brightness)
        ddc_layout.addRow(self.physical_contrast_label, self.physical_contrast)
        root.addWidget(self.ddc_box)

        actions = QHBoxLayout()
        self.apply_button = QPushButton("Aplicar")
        self.apply_button.setObjectName("primary")
        self.apply_button.clicked.connect(self._apply)
        restore = QPushButton("Última estável")
        restore.clicked.connect(self._restore)
        neutral = QPushButton("Neutro")
        neutral.setObjectName("danger")
        neutral.clicked.connect(self._neutral)
        actions.addWidget(self.apply_button)
        actions.addWidget(restore)
        actions.addWidget(neutral)
        root.addLayout(actions)

        bottom = QHBoxLayout()
        self.status = QLabel("Detectando recursos…")
        self.status.setObjectName("status")
        self.status.setWordWrap(True)
        diagnostic = QPushButton("Diagnóstico")
        diagnostic.clicked.connect(self._diagnostic)
        bottom.addWidget(self.status, 1)
        bottom.addWidget(diagnostic)
        root.addLayout(bottom)
        scroll.setWidget(content)
        self.setCentralWidget(scroll)

    @staticmethod
    def _numeric_control(minimum: int, maximum: int, value: int, suffix: str, step: int = 1) -> tuple[QSlider, QSpinBox]:
        slider = QSlider(Qt.Orientation.Horizontal)
        slider.setRange(minimum, maximum)
        slider.setSingleStep(step)
        slider.setPageStep(step * 5)
        slider.setValue(value)
        spin = QSpinBox()
        spin.setRange(minimum, maximum)
        spin.setSingleStep(step)
        spin.setSuffix(suffix)
        spin.setValue(value)
        slider.valueChanged.connect(spin.setValue)
        spin.valueChanged.connect(slider.setValue)
        return slider, spin

    @staticmethod
    def _float_control(
        minimum: float,
        slider_maximum: float,
        input_maximum: float,
        value: float,
    ) -> tuple[QSlider, QDoubleSpinBox]:
        slider = QSlider(Qt.Orientation.Horizontal)
        slider.setRange(round(minimum * 100), round(slider_maximum * 100))
        slider.setSingleStep(5)
        slider.setPageStep(25)
        spin = QDoubleSpinBox()
        spin.setRange(minimum, input_maximum)
        spin.setDecimals(2)
        spin.setSingleStep(0.05)
        spin.setSuffix("×")

        def slider_changed(raw: int) -> None:
            spin.setValue(raw / 100.0)

        def input_changed(current: float) -> None:
            blocker = QSignalBlocker(slider)
            slider.setValue(round(max(minimum, min(slider_maximum, current)) * 100))
            del blocker

        slider.valueChanged.connect(slider_changed)
        spin.valueChanged.connect(input_changed)
        spin.setValue(value)
        input_changed(value)
        return slider, spin

    def _load_monitors(self) -> None:
        self.monitor_combo.clear()
        for monitor in self.controller.monitors:
            self.monitor_combo.addItem(f"{monitor.name} — {monitor.connector}", monitor)
        enabled = bool(self.controller.monitors)
        self.apply_button.setEnabled(enabled)
        self.profile_box.setEnabled(enabled)
        self.image_box.setEnabled(enabled)
        self.gpu_box.setEnabled(enabled)
        self.mode_box.setEnabled(enabled)
        self.ddc_box.setVisible(False)
        if not enabled:
            self.status.setText("Nenhum monitor foi detectado por um backend compatível.")
            self.monitor_info.setText("Use KDE Plasma/Wayland com KScreen ou uma sessão X11 com XRandR.")

    def _monitor_changed(self, index: int) -> None:
        monitor = self.monitor_combo.itemData(index)
        if not isinstance(monitor, MonitorInfo):
            return
        self.monitor = monitor
        self._loading = True
        settings = self.controller.settings_for(monitor)
        self.brightness_input.setValue(round(settings.brightness))
        self.temperature_input.setValue(settings.temperature)
        self.mode_combo.clear()
        selected = 0
        for position, mode in enumerate(monitor.modes):
            self.mode_combo.addItem(mode.label, mode.id)
            if mode.id == settings.mode_id:
                selected = position
        self.mode_combo.setCurrentIndex(selected)
        capabilities = self.controller.manager.capabilities(monitor)
        temperature_max = 6500 if self.controller.manager.display_backend is self.controller.manager.kscreen else 10000
        self.temperature_slider.setMaximum(temperature_max)
        self.temperature_input.setMaximum(temperature_max)
        self.ddc_box.setVisible(capabilities.physical_brightness.available or capabilities.physical_contrast.available)
        self.brightness_slider.setEnabled(capabilities.brightness.available)
        self.brightness_input.setEnabled(capabilities.brightness.available)
        self.temperature_slider.setEnabled(capabilities.temperature.available)
        self.temperature_input.setEnabled(capabilities.temperature.available)
        self.mode_combo.setEnabled(capabilities.resolution.available or capabilities.refresh_rate.available)
        gpu_available = capabilities.gpu_brightness.available
        self.gpu_switch.setEnabled(gpu_available)
        self.gpu_switch.setChecked(settings.gpu_enabled and gpu_available)
        self.gpu_controls.setEnabled(gpu_available)
        self.gpu_brightness_input.setValue(settings.gpu_brightness)
        self.gpu_contrast_input.setValue(settings.gpu_contrast)
        self.gpu_saturation_input.setValue(settings.gpu_saturation)
        self.gpu_status.setText(
            ("Disponível: " if gpu_available else "Indisponível: ")
            + capabilities.gpu_brightness.reason
        )
        self.physical_brightness.setVisible(capabilities.physical_brightness.available)
        self.physical_brightness_label.setVisible(capabilities.physical_brightness.available)
        self.physical_contrast.setVisible(capabilities.physical_contrast.available)
        self.physical_contrast_label.setVisible(capabilities.physical_contrast.available)
        ddc_display = self.controller.manager.ddc.detect().get(monitor.connector)
        if ddc_display and ddc_display.brightness is not None:
            self.physical_brightness.setValue(ddc_display.brightness)
        if ddc_display and ddc_display.contrast is not None:
            self.physical_contrast.setValue(ddc_display.contrast)
        self.monitor_info.setText(
            f"{monitor.manufacturer} · {monitor.panel_technology} ({monitor.technology_confidence}% de confiança) · "
            f"backend: {self.controller.manager.backend_name}"
        )
        missing = []
        if not capabilities.physical_brightness.available:
            missing.append("brilho físico")
        if not capabilities.physical_contrast.available:
            missing.append("contraste físico")
        if not capabilities.temperature.available:
            missing.append("temperatura")
        self.status.setText("Disponível. " + ("Sem suporte confirmado para: " + ", ".join(missing) + "." if missing else "Todos os controles detectados estão disponíveis."))
        self._loading = False

    def _settings(self) -> DisplaySettings | None:
        if self.monitor is None:
            return None
        capabilities = self.controller.manager.capabilities(self.monitor)
        return DisplaySettings(
            monitor_id=self.monitor.id,
            brightness=self.brightness_input.value(),
            temperature=self.temperature_input.value(),
            mode_id=str(self.mode_combo.currentData() or self.monitor.current_mode_id),
            physical_brightness=self.physical_brightness.value() if capabilities.physical_brightness.available else None,
            physical_contrast=self.physical_contrast.value() if capabilities.physical_contrast.available else None,
            gpu_enabled=self.gpu_switch.isChecked() and capabilities.gpu_brightness.available,
            gpu_brightness=self.gpu_brightness_input.value(),
            gpu_contrast=self.gpu_contrast_input.value(),
            gpu_saturation=self.gpu_saturation_input.value(),
        )

    def _apply(self) -> None:
        settings = self._settings()
        if self.monitor is None or settings is None:
            return
        self.status.setText("Aplicando…")
        QApplication.processEvents()
        result = self.controller.apply(self.monitor, settings)
        if not result.success:
            self.status.setText("Falha: " + "; ".join(result.errors))
            QMessageBox.critical(self, "Não foi possível aplicar", "\n".join(result.errors))
            return
        self.status.setText(" · ".join(result.messages))
        if result.dangerous:
            dialog = RollbackDialog(self)
            dialog.exec()
            if dialog.kept:
                confirmed, message = self.controller.confirm_current()
                if confirmed:
                    self.status.setText("Configuração confirmada como estável.")
                else:
                    self.status.setText("Falha ao confirmar: " + message)
                    self._restore()
            else:
                self._restore()

    def _apply_profile(self) -> None:
        profile = self.profile_combo.currentData()
        if self.monitor is None or profile.name == "Personalizado":
            return
        self.brightness_input.setValue(round(profile.brightness))
        self.temperature_input.setValue(profile.temperature)
        self._apply()

    def _restore(self) -> None:
        if self.monitor is None:
            return
        result = self.controller.restore_stable(self.monitor)
        self.status.setText("Última configuração estável restaurada." if result.success else "Falha: " + "; ".join(result.errors))
        if result.success:
            self._monitor_changed(self.monitor_combo.currentIndex())

    def _neutral(self) -> None:
        if self.monitor is None:
            return
        result = self.controller.neutral(self.monitor)
        self.status.setText("Display restaurado para valores neutros." if result.success else "Falha: " + "; ".join(result.errors))
        if result.success:
            self._monitor_changed(self.monitor_combo.currentIndex())

    def _diagnostic(self) -> None:
        DiagnosticDialog(self, self.controller.diagnostic()).exec()

    def _keep_temperature(self) -> None:
        settings = self.controller.current
        if settings and settings.temperature < 6500 and self.controller.manager.display_backend is self.controller.manager.kscreen:
            self.controller.manager.kwin.apply_temperature(settings.temperature)
