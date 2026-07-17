# Detecção de root

`RootEnvironmentDetector` executa primeiro um teste funcional estruturado. Root funcional exige exit code zero, ausência de timeout e stdout de `id -u` exatamente igual a `0`.

Depois coleta, de forma somente leitura, `su -v`, `su -V`, binários/daemons conhecidos, propriedades, mounts e evidências expostas por Magisk, KernelSU, SukiSU Ultra, ReSukiSU e APatch. O resultado contém implementação, arquitetura (userspace/kernel/híbrida), versão, confiança, caminho do binário, evidências e avisos.

Backends específicos compartilham atualmente o executor `su`, mas declaram arquitetura, módulo, diretório/serviço de boot e limitações separadamente. A escolha manual define preferência futura; nunca marca root funcional sem `id -u = 0`.

Saídas ou argumentos marcados como sensíveis podem ser redigidos no log. Comandos têm timeout, cancelamento por interrupção e execução serial para evitar múltiplos shells concorrentes.
