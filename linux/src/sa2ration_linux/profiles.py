from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Profile:
    name: str
    brightness: float
    temperature: int
    description: str


BUILTIN_PROFILES: tuple[Profile, ...] = (
    Profile("Neutro", 100, 6500, "Sem redução de brilho ou aquecimento de cor."),
    Profile("Leitura", 75, 4800, "Brilho moderado e branco mais confortável."),
    Profile("Noturno", 45, 3200, "Menos luminância e luz azul para ambientes escuros."),
    Profile("Baixa luminosidade", 20, 2600, "Redução forte para uso em quarto escuro."),
    Profile("Externo", 100, 6500, "Máxima legibilidade permitida pelo backend."),
    Profile("Personalizado", 100, 6500, "Valores definidos manualmente."),
)


def profile_by_name(name: str) -> Profile | None:
    return next((profile for profile in BUILTIN_PROFILES if profile.name.casefold() == name.casefold()), None)
