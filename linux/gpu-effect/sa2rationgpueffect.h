/* SPDX-License-Identifier: GPL-2.0-or-later */

#pragma once

#include "effect/offscreeneffect.h"

#include <QSet>
#include <QTimer>

namespace KWin
{

class GLShader;

class Sa2rationGpuEffect final : public OffscreenEffect
{
    Q_OBJECT

public:
    Sa2rationGpuEffect();
    ~Sa2rationGpuEffect() override;

    static bool supported();
    bool isActive() const override;
    bool provides(Feature feature) override;
    int requestedEffectChainPosition() const override;
    void reconfigure(ReconfigureFlags flags) override;

private Q_SLOTS:
    void addWindow(KWin::EffectWindow *window);
    void removeWindow(KWin::EffectWindow *window);
    void restoreStableConfiguration();

private:
    struct Values
    {
        bool enabled = false;
        float brightness = 1.0f;
        float contrast = 1.0f;
        float saturation = 1.0f;
        float offset = 0.0f;
    };

    bool ensureShader();
    Values readValues(const KConfigGroup &group, const QString &prefix = QString()) const;
    void applyValues(const Values &values);
    void updateRedirectedWindows();
    void armRecoveryTimer(qint64 temporaryUntilMs);
    void writeCurrentValues(KConfigGroup &group, const Values &values, qint64 temporaryUntilMs);

    std::unique_ptr<GLShader> m_shader;
    QSet<EffectWindow *> m_windows;
    QTimer m_recoveryTimer;
    Values m_values;
    bool m_shaderAttempted = false;
    bool m_valid = true;
};

} // namespace KWin
