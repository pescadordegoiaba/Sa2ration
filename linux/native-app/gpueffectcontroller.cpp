// SPDX-License-Identifier: GPL-2.0-or-later
#include "gpueffectcontroller.h"

#include <QDateTime>
#include <QDBusConnection>
#include <QDBusInterface>
#include <QDBusMessage>
#include <QDir>
#include <QFileInfo>
#include <QSettings>
#include <QStandardPaths>
#include <QVariant>

#include <algorithm>
#include <cmath>

namespace
{
constexpr auto serviceName = "org.kde.KWin";
constexpr auto objectPath = "/Effects";
constexpr auto interfaceName = "org.kde.kwin.Effects";
constexpr qint64 recoveryWindowMs = 15'000;

float finiteClamp(float value, float minimum, float maximum, float fallback)
{
    if (!std::isfinite(value)) {
        return fallback;
    }
    return std::clamp(value, minimum, maximum);
}

QString configurationPath()
{
    const QString base = QStandardPaths::writableLocation(QStandardPaths::GenericConfigLocation);
    return QDir(base).filePath(QStringLiteral("sa2ration-linux/gpu.ini"));
}

GpuEffectValues readValues(QSettings &settings, const QString &prefix)
{
    GpuEffectValues values;
    values.enabled = settings.value(prefix + QStringLiteral("Enabled"), false).toBool();
    values.brightness = settings.value(prefix + QStringLiteral("Brightness"), 1.0).toFloat();
    values.contrast = settings.value(prefix + QStringLiteral("Contrast"), 1.0).toFloat();
    values.saturation = settings.value(prefix + QStringLiteral("Saturation"), 1.0).toFloat();
    values.offset = settings.value(prefix + QStringLiteral("Offset"), 0.0).toFloat();
    return values.normalized();
}

void writeValues(QSettings &settings, const GpuEffectValues &values, const QString &prefix)
{
    settings.setValue(prefix + QStringLiteral("Enabled"), values.enabled);
    settings.setValue(prefix + QStringLiteral("Brightness"), values.brightness);
    settings.setValue(prefix + QStringLiteral("Contrast"), values.contrast);
    settings.setValue(prefix + QStringLiteral("Saturation"), values.saturation);
    settings.setValue(prefix + QStringLiteral("Offset"), values.offset);
}
}

bool GpuEffectValues::isDangerous() const
{
    return enabled && (brightness < 0.2F || brightness > 2.0F || contrast < 0.15F
                       || contrast > 2.5F || saturation > 3.0F || std::abs(offset) > 0.4F);
}

GpuEffectValues GpuEffectValues::normalized() const
{
    GpuEffectValues result = *this;
    result.brightness = finiteClamp(brightness, 0.0F, 10.0F, 1.0F);
    result.contrast = finiteClamp(contrast, 0.0F, 10.0F, 1.0F);
    result.saturation = finiteClamp(saturation, 0.0F, 10.0F, 1.0F);
    result.offset = finiteClamp(offset, -2.0F, 2.0F, 0.0F);
    return result;
}

GpuEffectController::GpuEffectController(QObject *parent)
    : QObject(parent)
{
    qRegisterMetaType<GpuEffectValues>();
    readConfiguration();
}

GpuEffectValues GpuEffectController::current() const
{
    return m_current;
}

GpuEffectValues GpuEffectController::stable() const
{
    return m_stable;
}

QString GpuEffectController::statusText() const
{
    return m_status;
}

bool GpuEffectController::effectLoaded() const
{
    return m_effectLoaded;
}

void GpuEffectController::readConfiguration()
{
    QSettings settings(configurationPath(), QSettings::IniFormat);
    settings.beginGroup(QStringLiteral("Effect"));
    m_current = readValues(settings, QString());
    m_stable = readValues(settings, QStringLiteral("Stable"));
    const qint64 temporaryUntil = settings.value(QStringLiteral("TemporaryUntilMs"), 0).toLongLong();
    settings.endGroup();

    if (temporaryUntil > 0 && temporaryUntil <= QDateTime::currentMSecsSinceEpoch()) {
        m_current = m_stable;
        writeConfiguration(m_stable, m_stable, 0);
    }
}

bool GpuEffectController::writeConfiguration(const GpuEffectValues &current,
                                             const GpuEffectValues &stable,
                                             qint64 temporaryUntilMs)
{
    const QString path = configurationPath();
    if (!QDir().mkpath(QFileInfo(path).absolutePath())) {
        setStatus(QStringLiteral("Não foi possível criar a pasta de configuração."), false);
        return false;
    }

    QSettings settings(path, QSettings::IniFormat);
    settings.beginGroup(QStringLiteral("Effect"));
    writeValues(settings, current.normalized(), QString());
    writeValues(settings, stable.normalized(), QStringLiteral("Stable"));
    settings.setValue(QStringLiteral("TemporaryUntilMs"), temporaryUntilMs);
    settings.endGroup();
    settings.sync();
    if (settings.status() != QSettings::NoError) {
        setStatus(QStringLiteral("Falha ao salvar a configuração do efeito GPU."), false);
        return false;
    }
    return true;
}

bool GpuEffectController::callEffectMethod(const QString &method, bool requireTrueResult)
{
    QDBusInterface effects(QString::fromLatin1(serviceName), QString::fromLatin1(objectPath),
                           QString::fromLatin1(interfaceName), QDBusConnection::sessionBus());
    if (!effects.isValid()) {
        setStatus(QStringLiteral("KWin não está disponível nesta sessão. Use Plasma Wayland ou X11 com composição OpenGL."), false);
        return false;
    }

    const QDBusMessage reply = effects.call(method, QString::fromLatin1(pluginId));
    if (reply.type() == QDBusMessage::ErrorMessage) {
        setStatus(QStringLiteral("KWin recusou %1: %2").arg(method, reply.errorMessage()), false);
        return false;
    }
    if (requireTrueResult && !reply.arguments().isEmpty() && !reply.arguments().first().toBool()) {
        setStatus(QStringLiteral("O KWin não conseguiu carregar o efeito Sa2ration GPU."), false);
        return false;
    }
    return true;
}

bool GpuEffectController::ensureEffectLoaded()
{
    QDBusInterface effects(QString::fromLatin1(serviceName), QString::fromLatin1(objectPath),
                           QString::fromLatin1(interfaceName), QDBusConnection::sessionBus());
    if (!effects.isValid()) {
        setStatus(QStringLiteral("KWin não está disponível nesta sessão."), false);
        m_effectLoaded = false;
        return false;
    }

    const QDBusMessage query = effects.call(QStringLiteral("isEffectLoaded"), QString::fromLatin1(pluginId));
    if (query.type() != QDBusMessage::ErrorMessage && !query.arguments().isEmpty()
        && query.arguments().first().toBool()) {
        m_effectLoaded = true;
        return true;
    }

    if (!callEffectMethod(QStringLiteral("loadEffect"), true)) {
        m_effectLoaded = false;
        return false;
    }
    m_effectLoaded = true;
    return true;
}

bool GpuEffectController::reconfigureEffect()
{
    if (!ensureEffectLoaded()) {
        return false;
    }
    return callEffectMethod(QStringLiteral("reconfigureEffect"));
}

void GpuEffectController::setStatus(const QString &text, bool ok)
{
    m_status = text;
    emit statusChanged(text, ok);
}

void GpuEffectController::initialize()
{
    emit valuesChanged(m_current);
    if (!ensureEffectLoaded()) {
        return;
    }
    if (!reconfigureEffect()) {
        return;
    }
    setStatus(m_current.enabled
                  ? QStringLiteral("Ativo: brilho, contraste e saturação estão sendo processados pela GPU no KWin.")
                  : QStringLiteral("Pronto: efeito GPU carregado no KWin. Use Ativar/Desativar para ligar."),
              true);
}

void GpuEffectController::applyEditedValues(const GpuEffectValues &values)
{
    const GpuEffectValues next = values.normalized();
    const bool dangerous = next.isDangerous();
    const qint64 expiry = dangerous ? QDateTime::currentMSecsSinceEpoch() + recoveryWindowMs : 0;
    const GpuEffectValues nextStable = dangerous ? m_stable : next;

    if (!writeConfiguration(next, nextStable, expiry)) {
        return;
    }
    m_current = next;
    m_stable = nextStable;

    if (!reconfigureEffect()) {
        return;
    }
    emit valuesChanged(m_current);
    if (dangerous) {
        setStatus(QStringLiteral("Alteração extrema aplicada temporariamente por 15 segundos."), true);
        emit dangerousValuesApplied();
    } else {
        setStatus(m_current.enabled
                      ? QStringLiteral("Ativo: configuração aplicada pela GPU.")
                      : QStringLiteral("Desativado: o efeito continua carregado e neutro."),
                  true);
    }
}

void GpuEffectController::confirmCurrent()
{
    m_stable = m_current;
    if (writeConfiguration(m_current, m_current, 0) && reconfigureEffect()) {
        setStatus(QStringLiteral("Configuração confirmada como estável."), true);
    }
}

void GpuEffectController::revertToStable()
{
    m_current = m_stable;
    if (writeConfiguration(m_stable, m_stable, 0) && reconfigureEffect()) {
        emit valuesChanged(m_current);
        setStatus(QStringLiteral("Última configuração estável restaurada."), true);
    }
}

void GpuEffectController::restoreNeutral()
{
    GpuEffectValues neutral;
    m_current = neutral;
    m_stable = neutral;
    if (writeConfiguration(neutral, neutral, 0) && reconfigureEffect()) {
        emit valuesChanged(m_current);
        setStatus(QStringLiteral("Display restaurado ao estado neutro. O efeito permanece pronto no KWin."), true);
    }
}

void GpuEffectController::reloadEffect()
{
    m_effectLoaded = false;
    initialize();
}
