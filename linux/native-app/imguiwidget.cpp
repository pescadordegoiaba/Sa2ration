// SPDX-License-Identifier: GPL-2.0-or-later
#include "imguiwidget.h"

#include "imgui.h"
#include "imgui_impl_opengl3.h"

#include <QFocusEvent>
#include <QFileInfo>
#include <QKeyEvent>
#include <QMouseEvent>
#include <QOpenGLContext>
#include <QOpenGLFunctions>
#include <QWheelEvent>

#include <algorithm>
#include <array>
#include <cmath>

namespace
{
bool toggleSwitch(const char *label, bool *value)
{
    const ImGuiStyle &style = ImGui::GetStyle();
    const float height = ImGui::GetFrameHeight() * 1.08F;
    const float width = height * 1.85F;
    const ImVec2 position = ImGui::GetCursorScreenPos();
    ImGui::InvisibleButton(label, ImVec2(width, height));
    const bool changed = ImGui::IsItemClicked();
    if (changed) {
        *value = !*value;
    }

    const ImU32 background = ImGui::GetColorU32(*value ? ImGuiCol_ButtonActive : ImGuiCol_FrameBg);
    const ImU32 knob = ImGui::GetColorU32(ImGuiCol_Text);
    ImDrawList *draw = ImGui::GetWindowDrawList();
    draw->AddRectFilled(position, ImVec2(position.x + width, position.y + height), background,
                        height * 0.5F);
    const float radius = height * 0.37F;
    const float centerX = *value ? position.x + width - height * 0.5F : position.x + height * 0.5F;
    draw->AddCircleFilled(ImVec2(centerX, position.y + height * 0.5F), radius, knob);

    ImGui::SameLine(0.0F, style.ItemInnerSpacing.x * 2.0F);
    ImGui::AlignTextToFramePadding();
    ImGui::TextUnformatted("Ativar/Desativar");
    return changed;
}

bool valueControl(const char *id, const char *label, float *value, float sliderMaximum,
                  float inputMinimum, float inputMaximum, const char *format)
{
    bool changed = false;
    ImGui::PushID(id);
    ImGui::TextUnformatted(label);
    ImGui::SetNextItemWidth(-145.0F);
    changed |= ImGui::SliderFloat("##slider", value, 0.0F, sliderMaximum, format,
                                  ImGuiSliderFlags_AlwaysClamp);
    ImGui::SameLine();
    ImGui::SetNextItemWidth(135.0F);
    changed |= ImGui::InputFloat("##input", value, 0.05F, 0.5F, format);
    if (!std::isfinite(*value)) {
        *value = 1.0F;
        changed = true;
    }
    *value = std::clamp(*value, inputMinimum, inputMaximum);
    ImGui::PopID();
    return changed;
}

void applyStyle()
{
    ImGui::StyleColorsDark();
    ImGuiStyle &style = ImGui::GetStyle();
    style.WindowRounding = 13.0F;
    style.ChildRounding = 11.0F;
    style.FrameRounding = 7.0F;
    style.GrabRounding = 8.0F;
    style.ScrollbarRounding = 10.0F;
    style.WindowPadding = ImVec2(20.0F, 18.0F);
    style.FramePadding = ImVec2(10.0F, 7.0F);
    style.ItemSpacing = ImVec2(10.0F, 11.0F);
    style.Colors[ImGuiCol_WindowBg] = ImVec4(0.055F, 0.071F, 0.10F, 1.0F);
    style.Colors[ImGuiCol_ChildBg] = ImVec4(0.09F, 0.12F, 0.16F, 1.0F);
    style.Colors[ImGuiCol_FrameBg] = ImVec4(0.15F, 0.18F, 0.23F, 1.0F);
    style.Colors[ImGuiCol_FrameBgHovered] = ImVec4(0.20F, 0.29F, 0.40F, 1.0F);
    style.Colors[ImGuiCol_FrameBgActive] = ImVec4(0.22F, 0.37F, 0.55F, 1.0F);
    style.Colors[ImGuiCol_Button] = ImVec4(0.15F, 0.23F, 0.34F, 1.0F);
    style.Colors[ImGuiCol_ButtonHovered] = ImVec4(0.21F, 0.38F, 0.58F, 1.0F);
    style.Colors[ImGuiCol_ButtonActive] = ImVec4(0.18F, 0.49F, 0.82F, 1.0F);
    style.Colors[ImGuiCol_SliderGrab] = ImVec4(0.35F, 0.66F, 1.0F, 1.0F);
    style.Colors[ImGuiCol_SliderGrabActive] = ImVec4(0.55F, 0.78F, 1.0F, 1.0F);
    style.Colors[ImGuiCol_CheckMark] = ImVec4(0.70F, 0.86F, 1.0F, 1.0F);
}
}

ImGuiControlWidget::ImGuiControlWidget(QWidget *parent)
    : QOpenGLWidget(parent)
{
    setFocusPolicy(Qt::StrongFocus);
    setMouseTracking(true);
    setMinimumSize(540, 540);
    m_repaintTimer.setInterval(16);
    connect(&m_repaintTimer, &QTimer::timeout, this, QOverload<>::of(&ImGuiControlWidget::update));
    m_repaintTimer.start();
}

ImGuiControlWidget::~ImGuiControlWidget()
{
    if (!m_context) {
        return;
    }
    makeCurrent();
    ImGui::SetCurrentContext(m_context);
    if (m_rendererReady) {
        ImGui_ImplOpenGL3_Shutdown();
    }
    ImGui::DestroyContext(m_context);
    doneCurrent();
}

void ImGuiControlWidget::initializeGL()
{
    m_context = ImGui::CreateContext();
    ImGui::SetCurrentContext(m_context);
    ImGuiIO &io = ImGui::GetIO();
    io.BackendPlatformName = "sa2ration_qt6";
    io.IniFilename = nullptr;
    io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;

    const std::array<const char *, 4> fontCandidates = {
        "/usr/share/fonts/noto/NotoSans-Regular.ttf",
        "/usr/share/fonts/TTF/DejaVuSans.ttf",
        "/usr/share/fonts/gnu-free/FreeSans.ttf",
        "/usr/share/fonts/liberation/LiberationSans-Regular.ttf",
    };
    for (const char *path : fontCandidates) {
        if (QFileInfo::exists(QString::fromUtf8(path))) {
            io.Fonts->AddFontFromFileTTF(path, 17.0F);
            break;
        }
    }

    applyStyle();
    m_rendererReady = ImGui_ImplOpenGL3_Init("#version 140");
    m_frameTimer.start();
}

void ImGuiControlWidget::paintGL()
{
    if (!m_context || !m_rendererReady) {
        return;
    }
    ImGui::SetCurrentContext(m_context);
    ImGuiIO &io = ImGui::GetIO();
    const qreal ratio = devicePixelRatioF();
    io.DisplaySize = ImVec2(static_cast<float>(width()), static_cast<float>(height()));
    io.DisplayFramebufferScale = ImVec2(static_cast<float>(ratio), static_cast<float>(ratio));
    const qint64 elapsed = m_frameTimer.restart();
    io.DeltaTime = elapsed > 0 ? static_cast<float>(elapsed) / 1000.0F : 1.0F / 60.0F;

    ImGui_ImplOpenGL3_NewFrame();
    ImGui::NewFrame();
    buildInterface();
    ImGui::Render();

    QOpenGLFunctions *functions = QOpenGLContext::currentContext()->functions();
    functions->glViewport(0, 0, static_cast<int>(std::lround(width() * ratio)),
                          static_cast<int>(std::lround(height() * ratio)));
    functions->glClearColor(0.055F, 0.071F, 0.10F, 1.0F);
    functions->glClear(GL_COLOR_BUFFER_BIT);
    ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());
}

void ImGuiControlWidget::resizeGL(int width, int height)
{
    Q_UNUSED(width)
    Q_UNUSED(height)
}

void ImGuiControlWidget::buildInterface()
{
    ImGui::SetNextWindowPos(ImVec2(0.0F, 0.0F));
    ImGui::SetNextWindowSize(ImGui::GetIO().DisplaySize);
    constexpr ImGuiWindowFlags flags = ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove
        | ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoBringToFrontOnFocus;
    ImGui::Begin("Sa2rationControls", nullptr, flags);

    ImGui::TextColored(ImVec4(0.43F, 0.71F, 1.0F, 1.0F), "PIPELINE DE COR DA GPU");
    ImGui::Spacing();

    bool changed = toggleSwitch("##sa2ration-enabled", &m_values.enabled);
    ImGui::SameLine();
    ImGui::TextDisabled(m_values.enabled ? "efeito aplicado" : "efeito neutro");

    ImGui::Spacing();
    ImGui::PushStyleColor(ImGuiCol_ChildBg, m_statusOk ? ImVec4(0.08F, 0.20F, 0.16F, 1.0F)
                                                       : ImVec4(0.24F, 0.12F, 0.12F, 1.0F));
    ImGui::BeginChild("status", ImVec2(0.0F, 58.0F), ImGuiChildFlags_Borders);
    ImGui::TextWrapped("%s", m_status.toUtf8().constData());
    ImGui::EndChild();
    ImGui::PopStyleColor();

    ImGui::BeginDisabled(!m_values.enabled);
    ImGui::BeginChild("image-controls", ImVec2(0.0F, 292.0F), ImGuiChildFlags_Borders);
    ImGui::TextColored(ImVec4(0.72F, 0.84F, 1.0F, 1.0F), "Imagem");
    ImGui::Separator();
    changed |= valueControl("brightness", "Brilho digital", &m_values.brightness, 3.0F, 0.0F, 10.0F, "%.2fx");
    changed |= valueControl("contrast", "Contraste", &m_values.contrast, 5.0F, 0.0F, 10.0F, "%.2fx");
    changed |= valueControl("saturation", "Saturação", &m_values.saturation, 5.0F, 0.0F, 10.0F, "%.2fx");

    ImGui::PushID("offset");
    ImGui::TextUnformatted("Offset de luminância");
    ImGui::SetNextItemWidth(-145.0F);
    changed |= ImGui::SliderFloat("##slider", &m_values.offset, -1.0F, 1.0F, "%+.2f",
                                  ImGuiSliderFlags_AlwaysClamp);
    ImGui::SameLine();
    ImGui::SetNextItemWidth(135.0F);
    changed |= ImGui::InputFloat("##input", &m_values.offset, 0.05F, 0.25F, "%+.2f");
    m_values.offset = std::clamp(std::isfinite(m_values.offset) ? m_values.offset : 0.0F, -2.0F, 2.0F);
    ImGui::PopID();
    ImGui::EndChild();
    ImGui::EndDisabled();

    ImGui::TextDisabled("Os sliders usam uma faixa prática. Os campos numéricos aceitam até 10x.");
    ImGui::TextDisabled("Valores extremos são revertidos automaticamente após 15 segundos.");

    if (ImGui::Button("Restaurar neutro", ImVec2(155.0F, 0.0F))) {
        emit restoreNeutralRequested();
    }
    ImGui::SameLine();
    if (ImGui::Button("Última estável", ImVec2(145.0F, 0.0F))) {
        emit restoreStableRequested();
    }
    ImGui::SameLine();
    if (ImGui::Button("Recarregar efeito", ImVec2(155.0F, 0.0F))) {
        emit reloadEffectRequested();
    }

    if (changed) {
        emit valuesEdited(m_values.normalized());
    }
    if (ImGui::IsMouseReleased(ImGuiMouseButton_Left)) {
        emit editingFinished();
    }
    ImGui::End();
}

void ImGuiControlWidget::setValues(const GpuEffectValues &values)
{
    if (!m_ignoreExternalValues) {
        m_values = values;
    }
}

void ImGuiControlWidget::setBackendStatus(const QString &status, bool ok)
{
    m_status = status;
    m_statusOk = ok;
}

int ImGuiControlWidget::mapMouseButton(Qt::MouseButton button)
{
    switch (button) {
    case Qt::LeftButton:
        return 0;
    case Qt::RightButton:
        return 1;
    case Qt::MiddleButton:
        return 2;
    case Qt::BackButton:
        return 3;
    case Qt::ForwardButton:
        return 4;
    default:
        return -1;
    }
}

void ImGuiControlWidget::mouseMoveEvent(QMouseEvent *event)
{
    ImGui::SetCurrentContext(m_context);
    ImGui::GetIO().AddMousePosEvent(static_cast<float>(event->position().x()),
                                    static_cast<float>(event->position().y()));
    event->accept();
}

void ImGuiControlWidget::mousePressEvent(QMouseEvent *event)
{
    setFocus(Qt::MouseFocusReason);
    ImGui::SetCurrentContext(m_context);
    const int button = mapMouseButton(event->button());
    if (button >= 0) {
        ImGui::GetIO().AddMouseButtonEvent(button, true);
    }
    event->accept();
}

void ImGuiControlWidget::mouseReleaseEvent(QMouseEvent *event)
{
    ImGui::SetCurrentContext(m_context);
    const int button = mapMouseButton(event->button());
    if (button >= 0) {
        ImGui::GetIO().AddMouseButtonEvent(button, false);
    }
    event->accept();
}

void ImGuiControlWidget::wheelEvent(QWheelEvent *event)
{
    ImGui::SetCurrentContext(m_context);
    const QPoint angle = event->angleDelta();
    ImGui::GetIO().AddMouseWheelEvent(static_cast<float>(angle.x()) / 120.0F,
                                     static_cast<float>(angle.y()) / 120.0F);
    event->accept();
}

int ImGuiControlWidget::mapKey(int qtKey)
{
    switch (qtKey) {
    case Qt::Key_Tab: return ImGuiKey_Tab;
    case Qt::Key_Left: return ImGuiKey_LeftArrow;
    case Qt::Key_Right: return ImGuiKey_RightArrow;
    case Qt::Key_Up: return ImGuiKey_UpArrow;
    case Qt::Key_Down: return ImGuiKey_DownArrow;
    case Qt::Key_PageUp: return ImGuiKey_PageUp;
    case Qt::Key_PageDown: return ImGuiKey_PageDown;
    case Qt::Key_Home: return ImGuiKey_Home;
    case Qt::Key_End: return ImGuiKey_End;
    case Qt::Key_Insert: return ImGuiKey_Insert;
    case Qt::Key_Delete: return ImGuiKey_Delete;
    case Qt::Key_Backspace: return ImGuiKey_Backspace;
    case Qt::Key_Space: return ImGuiKey_Space;
    case Qt::Key_Return: return ImGuiKey_Enter;
    case Qt::Key_Enter: return ImGuiKey_KeypadEnter;
    case Qt::Key_Escape: return ImGuiKey_Escape;
    default:
        if (qtKey >= Qt::Key_A && qtKey <= Qt::Key_Z) {
            return ImGuiKey_A + (qtKey - Qt::Key_A);
        }
        if (qtKey >= Qt::Key_0 && qtKey <= Qt::Key_9) {
            return ImGuiKey_0 + (qtKey - Qt::Key_0);
        }
        return ImGuiKey_None;
    }
}

void ImGuiControlWidget::submitKeyEvent(QKeyEvent *event, bool down)
{
    ImGui::SetCurrentContext(m_context);
    ImGuiIO &io = ImGui::GetIO();
    const Qt::KeyboardModifiers modifiers = event->modifiers();
    io.AddKeyEvent(ImGuiMod_Ctrl, modifiers.testFlag(Qt::ControlModifier));
    io.AddKeyEvent(ImGuiMod_Shift, modifiers.testFlag(Qt::ShiftModifier));
    io.AddKeyEvent(ImGuiMod_Alt, modifiers.testFlag(Qt::AltModifier));
    io.AddKeyEvent(ImGuiMod_Super, modifiers.testFlag(Qt::MetaModifier));
    const int key = mapKey(event->key());
    if (key != ImGuiKey_None) {
        io.AddKeyEvent(static_cast<ImGuiKey>(key), down);
    }
    if (down && !event->text().isEmpty() && !modifiers.testFlag(Qt::ControlModifier)) {
        const QByteArray utf8 = event->text().toUtf8();
        io.AddInputCharactersUTF8(utf8.constData());
    }
    event->accept();
}

void ImGuiControlWidget::keyPressEvent(QKeyEvent *event)
{
    submitKeyEvent(event, true);
}

void ImGuiControlWidget::keyReleaseEvent(QKeyEvent *event)
{
    submitKeyEvent(event, false);
}

void ImGuiControlWidget::focusInEvent(QFocusEvent *event)
{
    if (m_context) {
        ImGui::SetCurrentContext(m_context);
        ImGui::GetIO().AddFocusEvent(true);
    }
    QOpenGLWidget::focusInEvent(event);
}

void ImGuiControlWidget::focusOutEvent(QFocusEvent *event)
{
    if (m_context) {
        ImGui::SetCurrentContext(m_context);
        ImGui::GetIO().AddFocusEvent(false);
    }
    QOpenGLWidget::focusOutEvent(event);
}
