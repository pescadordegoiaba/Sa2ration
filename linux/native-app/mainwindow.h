// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once

#include "gpueffectcontroller.h"

#include <QMainWindow>

class GpuEffectController;
class ImGuiControlWidget;
class QLabel;
class QTimer;

class MainWindow final : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);

private slots:
    void scheduleApply(const GpuEffectValues &values);
    void applyPending();
    void offerDangerousConfirmation();
    void showDiagnostic();

private:
    GpuEffectController *m_controller = nullptr;
    ImGuiControlWidget *m_controls = nullptr;
    QLabel *m_backendBadge = nullptr;
    QTimer *m_applyTimer = nullptr;
    GpuEffectValues m_pending;
    bool m_hasPending = false;
    bool m_confirmationOpen = false;
};
