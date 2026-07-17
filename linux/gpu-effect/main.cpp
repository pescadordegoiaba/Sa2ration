/* SPDX-License-Identifier: GPL-2.0-or-later */

#include "sa2rationgpueffect.h"

namespace KWin
{

KWIN_EFFECT_FACTORY_SUPPORTED(Sa2rationGpuEffect,
    "metadata.json",
    return Sa2rationGpuEffect::supported();)

} // namespace KWin

#include "main.moc"
