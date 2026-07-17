// SPDX-License-Identifier: GPL-2.0-or-later
#pragma once

#include <QObject>
#include <QString>

struct GpuEffectValues
{
    bool enabled = false;
    float brightness = 1.0F;
    float contrast = 1.0F;
    float saturation = 1.0F;
    float offset = 0.0F;

    [[nodiscard]] bool isDangerous() const;
    [[nodiscard]] GpuEffectValues normalized() const;
};

class GpuEffectController final : public QObject
{
    Q_OBJECT

public:
    explicit GpuEffectController(QObject *parent = nullptr);

    [[nodiscard]] GpuEffectValues current() const;
    [[nodiscard]] GpuEffectValues stable() const;
    [[nodiscard]] QString statusText() const;
    [[nodiscard]] bool effectLoaded() const;

public slots:
    void initialize();
    void applyEditedValues(const GpuEffectValues &values);
    void confirmCurrent();
    void revertToStable();
    void restoreNeutral();
    void reloadEffect();

signals:
    void valuesChanged(const GpuEffectValues &values);
    void statusChanged(const QString &status, bool ok);
    void dangerousValuesApplied();

private:
    static constexpr auto pluginId = "sa2ration_gpu";

    void readConfiguration();
    bool writeConfiguration(const GpuEffectValues &current, const GpuEffectValues &stable,
                            qint64 temporaryUntilMs);
    bool ensureEffectLoaded();
    bool reconfigureEffect();
    bool callEffectMethod(const QString &method, bool requireTrueResult = false);
    void setStatus(const QString &text, bool ok);

    GpuEffectValues m_current;
    GpuEffectValues m_stable;
    QString m_status = QStringLiteral("Preparando o efeito GPU...");
    bool m_effectLoaded = false;
};

Q_DECLARE_METATYPE(GpuEffectValues)
