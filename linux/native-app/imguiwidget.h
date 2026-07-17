// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once

#include "gpueffectcontroller.h"

#include <QElapsedTimer>
#include <QOpenGLWidget>
#include <QTimer>

struct ImGuiContext;

class QFocusEvent;
class QKeyEvent;
class QMouseEvent;
class QWheelEvent;

class ImGuiControlWidget final : public QOpenGLWidget
{
    Q_OBJECT

public:
    explicit ImGuiControlWidget(QWidget *parent = nullptr);
    ~ImGuiControlWidget() override;

    void setValues(const GpuEffectValues &values);
    void setBackendStatus(const QString &status, bool ok);

signals:
    void valuesEdited(const GpuEffectValues &values);
    void editingFinished();
    void restoreNeutralRequested();
    void restoreStableRequested();
    void reloadEffectRequested();

protected:
    void initializeGL() override;
    void paintGL() override;
    void resizeGL(int width, int height) override;
    void mouseMoveEvent(QMouseEvent *event) override;
    void mousePressEvent(QMouseEvent *event) override;
    void mouseReleaseEvent(QMouseEvent *event) override;
    void wheelEvent(QWheelEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;
    void keyReleaseEvent(QKeyEvent *event) override;
    void focusInEvent(QFocusEvent *event) override;
    void focusOutEvent(QFocusEvent *event) override;

private:
    void buildInterface();
    void submitKeyEvent(QKeyEvent *event, bool down);
    static int mapMouseButton(Qt::MouseButton button);
    static int mapKey(int qtKey);

    ImGuiContext *m_context = nullptr;
    GpuEffectValues m_values;
    QString m_status = QStringLiteral("Preparando o efeito GPU...");
    bool m_statusOk = false;
    bool m_rendererReady = false;
    bool m_ignoreExternalValues = false;
    QElapsedTimer m_frameTimer;
    QTimer m_repaintTimer;
};
