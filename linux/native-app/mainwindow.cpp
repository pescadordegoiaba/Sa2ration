// SPDX-License-Identifier: GPL-2.0-or-later
#include "mainwindow.h"

#include "gpueffectcontroller.h"
#include "imguiwidget.h"

#include <QDialog>
#include <QDialogButtonBox>
#include <QFont>
#include <QHBoxLayout>
#include <QJsonDocument>
#include <QJsonObject>
#include <QLabel>
#include <QMessageBox>
#include <QPushButton>
#include <QStatusBar>
#include <QTimer>
#include <QVBoxLayout>
#include <QWidget>

namespace
{
class RecoveryDialog final : public QDialog
{
public:
    explicit RecoveryDialog(QWidget *parent)
        : QDialog(parent)
    {
        setWindowTitle(QStringLiteral("Confirmar configuração extrema"));
        setModal(true);
        auto *layout = new QVBoxLayout(this);
        auto *message = new QLabel(QStringLiteral(
            "A alteração pode tornar a tela difícil de usar. Confirme para mantê-la."));
        message->setWordWrap(true);
        layout->addWidget(message);
        m_countdown = new QLabel(this);
        QFont font = m_countdown->font();
        font.setPointSize(font.pointSize() + 8);
        font.setBold(true);
        m_countdown->setFont(font);
        m_countdown->setAlignment(Qt::AlignCenter);
        layout->addWidget(m_countdown);
        auto *buttons = new QDialogButtonBox(this);
        auto *revert = buttons->addButton(QStringLiteral("Reverter"), QDialogButtonBox::RejectRole);
        auto *keep = buttons->addButton(QStringLiteral("Manter"), QDialogButtonBox::AcceptRole);
        connect(revert, &QPushButton::clicked, this, &QDialog::reject);
        connect(keep, &QPushButton::clicked, this, &QDialog::accept);
        layout->addWidget(buttons);
        connect(&m_timer, &QTimer::timeout, this, [this] {
            --m_seconds;
            updateText();
            if (m_seconds <= 0) {
                reject();
            }
        });
        m_timer.start(1000);
        updateText();
    }

private:
    void updateText()
    {
        m_countdown->setText(QStringLiteral("Reversão automática em %1 s").arg(m_seconds));
    }

    QLabel *m_countdown = nullptr;
    QTimer m_timer;
    int m_seconds = 15;
};
}

MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent)
    , m_controller(new GpuEffectController(this))
    , m_controls(new ImGuiControlWidget(this))
    , m_applyTimer(new QTimer(this))
{
    setWindowTitle(QStringLiteral("Sa2ration Linux — GPU LCD Control"));
    setMinimumSize(620, 690);
    resize(760, 820);

    auto *central = new QWidget(this);
    auto *layout = new QVBoxLayout(central);
    layout->setContentsMargins(16, 14, 16, 14);
    layout->setSpacing(10);

    auto *header = new QHBoxLayout();
    auto *titles = new QVBoxLayout();
    auto *title = new QLabel(QStringLiteral("Sa2ration Linux"), central);
    QFont titleFont = title->font();
    titleFont.setPointSize(titleFont.pointSize() + 9);
    titleFont.setBold(true);
    title->setFont(titleFont);
    auto *subtitle = new QLabel(QStringLiteral("Qt + Dear ImGui · processamento global pela GPU do KWin"), central);
    subtitle->setStyleSheet(QStringLiteral("color:#8e9bad"));
    titles->addWidget(title);
    titles->addWidget(subtitle);
    header->addLayout(titles, 1);
    m_backendBadge = new QLabel(QStringLiteral("KWin GPU"), central);
    m_backendBadge->setStyleSheet(QStringLiteral(
        "QLabel{background:#19324a;color:#9ed0ff;border:1px solid #28577e;border-radius:10px;padding:7px 11px;font-weight:600;}"));
    header->addWidget(m_backendBadge);
    auto *diagnostic = new QPushButton(QStringLiteral("Diagnóstico"), central);
    connect(diagnostic, &QPushButton::clicked, this, &MainWindow::showDiagnostic);
    header->addWidget(diagnostic);
    layout->addLayout(header);
    layout->addWidget(m_controls, 1);
    setCentralWidget(central);

    setStyleSheet(QStringLiteral(
        "QMainWindow,QWidget{background:#0e1219;color:#e8edf5;}"
        "QPushButton{background:#1d2a3a;border:1px solid #334963;border-radius:8px;padding:8px 12px;}"
        "QPushButton:hover{background:#263b52;}"));

    m_applyTimer->setSingleShot(true);
    m_applyTimer->setInterval(70);
    connect(m_applyTimer, &QTimer::timeout, this, &MainWindow::applyPending);
    connect(m_controls, &ImGuiControlWidget::valuesEdited, this, &MainWindow::scheduleApply);
    connect(m_controls, &ImGuiControlWidget::editingFinished, this, &MainWindow::offerDangerousConfirmation);
    connect(m_controls, &ImGuiControlWidget::restoreNeutralRequested, m_controller,
            &GpuEffectController::restoreNeutral);
    connect(m_controls, &ImGuiControlWidget::restoreStableRequested, m_controller,
            &GpuEffectController::revertToStable);
    connect(m_controls, &ImGuiControlWidget::reloadEffectRequested, m_controller,
            &GpuEffectController::reloadEffect);
    connect(m_controller, &GpuEffectController::valuesChanged, m_controls,
            &ImGuiControlWidget::setValues);
    connect(m_controller, &GpuEffectController::statusChanged, this,
            [this](const QString &text, bool ok) {
                m_controls->setBackendStatus(text, ok);
                statusBar()->showMessage(text);
                m_backendBadge->setText(ok ? QStringLiteral("KWin GPU · pronto")
                                           : QStringLiteral("KWin GPU · indisponível"));
            });
    connect(m_controller, &GpuEffectController::dangerousValuesApplied, this,
            &MainWindow::offerDangerousConfirmation);

    m_controls->setValues(m_controller->current());
    m_controls->setBackendStatus(m_controller->statusText(), false);
    QTimer::singleShot(0, m_controller, &GpuEffectController::initialize);
}

void MainWindow::scheduleApply(const GpuEffectValues &values)
{
    m_pending = values;
    m_hasPending = true;
    m_applyTimer->start();
}

void MainWindow::applyPending()
{
    if (!m_hasPending) {
        return;
    }
    m_hasPending = false;
    m_controller->applyEditedValues(m_pending);
}

void MainWindow::offerDangerousConfirmation()
{
    if (m_confirmationOpen || !m_controller->current().isDangerous()) {
        return;
    }
    if (m_applyTimer->isActive()) {
        applyPending();
    }
    if (!m_controller->current().isDangerous()) {
        return;
    }

    m_confirmationOpen = true;
    RecoveryDialog dialog(this);
    const int result = dialog.exec();
    m_confirmationOpen = false;
    if (result == QDialog::Accepted) {
        m_controller->confirmCurrent();
    } else {
        m_controller->revertToStable();
    }
}

void MainWindow::showDiagnostic()
{
    const GpuEffectValues values = m_controller->current();
    const QJsonObject report {
        {QStringLiteral("aplicativo"), QStringLiteral("Sa2ration Linux 2.0.0")},
        {QStringLiteral("interface"), QStringLiteral("Qt 6 + Dear ImGui 1.92.7")},
        {QStringLiteral("backend"), QStringLiteral("KWin OpenGL effect")},
        {QStringLiteral("efeitoCarregado"), m_controller->effectLoaded()},
        {QStringLiteral("ativado"), values.enabled},
        {QStringLiteral("brilho"), values.brightness},
        {QStringLiteral("contraste"), values.contrast},
        {QStringLiteral("saturacao"), values.saturation},
        {QStringLiteral("offset"), values.offset},
        {QStringLiteral("status"), m_controller->statusText()},
    };
    QMessageBox box(this);
    box.setWindowTitle(QStringLiteral("Diagnóstico do backend"));
    box.setIcon(m_controller->effectLoaded() ? QMessageBox::Information : QMessageBox::Warning);
    box.setText(QStringLiteral("Estado atual do pipeline GPU"));
    box.setDetailedText(QString::fromUtf8(QJsonDocument(report).toJson(QJsonDocument::Indented)));
    box.exec();
}
