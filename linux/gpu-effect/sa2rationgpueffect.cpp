/* SPDX-License-Identifier: GPL-2.0-or-later */

#include "sa2rationgpueffect.h"

#include "effect/effecthandler.h"
#include "opengl/glshader.h"
#include "opengl/glshadermanager.h"

#include <KConfigGroup>
#include <KSharedConfig>

#include <QDateTime>
#include <QLoggingCategory>

#include <algorithm>
#include <cmath>
#include <limits>
#include <utility>

Q_LOGGING_CATEGORY(KWIN_SA2RATION_GPU, "kwin_effect_sa2ration_gpu", QtWarningMsg)

static void ensureResources()
{
    Q_INIT_RESOURCE(sa2rationgpueffect);
}

namespace KWin
{

namespace
{
constexpr auto configFile = "sa2ration-linux/gpu.ini";
constexpr auto configGroup = "Effect";

float finiteClamp(double value, float minimum, float maximum, float fallback)
{
    if (!std::isfinite(value)) {
        return fallback;
    }
    return std::clamp(static_cast<float>(value), minimum, maximum);
}
}

Sa2rationGpuEffect::Sa2rationGpuEffect()
{
    m_recoveryTimer.setSingleShot(true);
    connect(&m_recoveryTimer, &QTimer::timeout, this, &Sa2rationGpuEffect::restoreStableConfiguration);
    connect(effects, &EffectsHandler::windowAdded, this, &Sa2rationGpuEffect::addWindow);
    connect(effects, &EffectsHandler::windowDeleted, this, &Sa2rationGpuEffect::removeWindow);
    reconfigure(ReconfigureAll);
}

Sa2rationGpuEffect::~Sa2rationGpuEffect()
{
    for (EffectWindow *window : std::as_const(m_windows)) {
        unredirect(window);
    }
}

bool Sa2rationGpuEffect::supported()
{
    return effects->isOpenGLCompositing() && OffscreenEffect::supported();
}

bool Sa2rationGpuEffect::isActive() const
{
    return m_valid && m_values.enabled && !m_windows.isEmpty();
}

bool Sa2rationGpuEffect::provides(Feature feature)
{
    return feature == Contrast;
}

int Sa2rationGpuEffect::requestedEffectChainPosition() const
{
    return 97;
}

bool Sa2rationGpuEffect::ensureShader()
{
    if (m_shader) {
        return true;
    }
    if (m_shaderAttempted) {
        return false;
    }
    m_shaderAttempted = true;
    ensureResources();
    m_shader = ShaderManager::instance()->generateShaderFromFile(
        ShaderTrait::MapTexture,
        QString(),
        QStringLiteral(":/effects/sa2ration_gpu/shaders/sa2ration.frag"));
    if (!m_shader) {
        qCCritical(KWIN_SA2RATION_GPU) << "Failed to load the Sa2ration GPU shader";
        m_valid = false;
        return false;
    }
    return true;
}

Sa2rationGpuEffect::Values Sa2rationGpuEffect::readValues(const KConfigGroup &group, const QString &prefix) const
{
    Values values;
    values.enabled = group.readEntry(prefix + QStringLiteral("Enabled"), false);
    values.brightness = finiteClamp(group.readEntry(prefix + QStringLiteral("Brightness"), 1.0), 0.0f, 10.0f, 1.0f);
    values.contrast = finiteClamp(group.readEntry(prefix + QStringLiteral("Contrast"), 1.0), 0.0f, 10.0f, 1.0f);
    values.saturation = finiteClamp(group.readEntry(prefix + QStringLiteral("Saturation"), 1.0), 0.0f, 10.0f, 1.0f);
    values.offset = finiteClamp(group.readEntry(prefix + QStringLiteral("Offset"), 0.0), -2.0f, 2.0f, 0.0f);
    return values;
}

void Sa2rationGpuEffect::applyValues(const Values &values)
{
    m_values = values;
    if (m_values.enabled && !ensureShader()) {
        m_values.enabled = false;
    }
    if (m_shader) {
        ShaderBinder binder { m_shader.get() };
        m_shader->setUniform("sa2Brightness", m_values.brightness);
        m_shader->setUniform("sa2Contrast", m_values.contrast);
        m_shader->setUniform("sa2Saturation", m_values.saturation);
        m_shader->setUniform("sa2Offset", m_values.offset);
    }
    updateRedirectedWindows();
    effects->addRepaintFull();
}

void Sa2rationGpuEffect::updateRedirectedWindows()
{
    if (!m_values.enabled || !m_valid || !m_shader) {
        for (EffectWindow *window : std::as_const(m_windows)) {
            unredirect(window);
        }
        m_windows.clear();
        return;
    }
    for (EffectWindow *window : effects->stackingOrder()) {
        addWindow(window);
    }
}

void Sa2rationGpuEffect::addWindow(EffectWindow *window)
{
    if (!m_values.enabled || !m_shader || m_windows.contains(window)) {
        return;
    }
    redirect(window);
    setShader(window, m_shader.get());
    m_windows.insert(window);
    window->addRepaintFull();
}

void Sa2rationGpuEffect::removeWindow(EffectWindow *window)
{
    m_windows.remove(window);
}

void Sa2rationGpuEffect::armRecoveryTimer(qint64 temporaryUntilMs)
{
    m_recoveryTimer.stop();
    if (temporaryUntilMs <= 0) {
        return;
    }
    const qint64 remaining = temporaryUntilMs - QDateTime::currentMSecsSinceEpoch();
    if (remaining <= 0) {
        restoreStableConfiguration();
        return;
    }
    m_recoveryTimer.start(static_cast<int>(std::min<qint64>(remaining, std::numeric_limits<int>::max())));
}

void Sa2rationGpuEffect::reconfigure(ReconfigureFlags flags)
{
    if (flags != ReconfigureAll) {
        return;
    }
    const KSharedConfig::Ptr config = KSharedConfig::openConfig(QString::fromLatin1(configFile));
    const KConfigGroup group(config, QString::fromLatin1(configGroup));
    const qint64 temporaryUntilMs = group.readEntry("TemporaryUntilMs", 0LL);
    applyValues(readValues(group));
    armRecoveryTimer(temporaryUntilMs);
}

void Sa2rationGpuEffect::writeCurrentValues(KConfigGroup &group, const Values &values, qint64 temporaryUntilMs)
{
    group.writeEntry("Enabled", values.enabled);
    group.writeEntry("Brightness", values.brightness);
    group.writeEntry("Contrast", values.contrast);
    group.writeEntry("Saturation", values.saturation);
    group.writeEntry("Offset", values.offset);
    group.writeEntry("TemporaryUntilMs", temporaryUntilMs);
}

void Sa2rationGpuEffect::restoreStableConfiguration()
{
    const KSharedConfig::Ptr config = KSharedConfig::openConfig(QString::fromLatin1(configFile));
    KConfigGroup group(config, QString::fromLatin1(configGroup));
    const Values stable = readValues(group, QStringLiteral("Stable"));
    writeCurrentValues(group, stable, 0);
    group.sync();
    applyValues(stable);
}

} // namespace KWin

#include "moc_sa2rationgpueffect.cpp"
