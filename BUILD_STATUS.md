# Estado da retomada — 27/08/2026

## Resultado da preparação inicial (antes do envio ao GitHub)

Preparação de build concluída; compilação do app e APK ainda NÃO validados.
Nenhum workflow foi enviado ou executado no GitHub nesta retomada.
Nenhum APK foi gerado. Não há teste em relógio/emulador nem medição de bateria.

## Alterações realizadas

- Substituído o bootstrap caseiro por Gradle Wrapper oficial 8.13, incluindo
  `gradlew`, `gradlew.bat`, JAR e properties com SHA-256 da distribuição.
- Criado `.github/workflows/build.yml`: Ubuntu 24.04, JDK 17, SDK 36,
  Build Tools 35.0.0, `assembleDebug`, `lintDebug`, verificação por `apksigner`
  e artifact com APK, SHA-256 e identificação do commit/run.
- Actions fixadas em commits consultados diretamente no GitHub.
- Adicionados `.gitignore`, `.gitattributes` e instruções de build no README.
- Aplicativo, recursos, manifest, versões Gradle/AGP/Kotlin/SDK/dependências
  e scripts ADB preservados byte a byte em relação ao ZIP original.
  Nenhuma mudança de UX ou estratégia de bateria.

## Verificações concluídas

- Leitura integral do handoff e dos arquivos originais do projeto.
- `./gradlew --version`: Gradle 8.13 executado com Java 17.0.20.
- Sintaxe YAML do workflow e `bash -n` para cada passo shell: OK.
- `sh -n gradlew`: OK.
- Integridade do wrapper JAR e checksum oficial: OK.
- Comparação binária dos arquivos originais: só README e scripts wrapper
  foram alterados; os arquivos novos são infraestrutura/documentação.
- Consultada compatibilidade oficial AGP 8.13.2 / Gradle 8.13 / JDK 17 / SDK 36.
- Inspecionado AAR Activity 1.13.0: minCompileSdk 36, minAGP 8.9.1.
- Inspecionados fontes e manifest do AAR Wear 1.3.0: a assinatura de
  `AmbientLifecycleObserver(this, mainExecutor, ambientCallback)` é válida.
  A declaração `uses-library com.google.android.wearable` já vem do manifest
  da dependência; a suspeita inicial de declaração faltante foi descartada.
  A permissão WAKE_LOCK é exigida pela API, mas o app não adquire wakelock próprio.

O Java local inicialmente não conseguiu baixar a distribuição por falta de
acesso direto à rede. O download por HTTPS do ambiente foi verificado por
SHA-256 e usado para validar a inicialização do wrapper. A tentativa posterior
de `assembleDebug` não chegou a produzir resultado de compilação; o ambiente
também não tem Android SDK instalado. Não interpretar isso como build aprovado
ou como erro comprovado no código.

## Bloqueio e próximo passo

- Conta GitHub conectada confirmada: `leonardolealluz183-bit`.
- `MirrorCounter-R860` não apareceu na busca nem na lista de repositórios acessíveis.
- O conector disponível tem escrita de arquivos/commits, mas não criação de
  repositório. Não foi usado outro repositório sem autorização.
- Pedir ao usuário para criar `MirrorCounter-R860`, preferencialmente privado,
  com **Add a README file** ativado, e disponibilizá-lo à conexão GitHub.
- Confirmar o repositório e quaisquer instruções AGENTS.md. Enviar os arquivos
  da raiz deste projeto (não a pasta externa do handoff) em commit/branch dedicado.
- O evento push do workflow inicia o build. Acompanhar a execução real,
  ler logs de falhas, corrigir apenas o necessário e repetir até passar.
- Baixar o artifact do mesmo commit, verificar APK/assinatura/hash e entregar.
- Só depois orientar instalação normal no Watch4 pelo S23 Ultra e testar UX,
  persistência, Ambient Mode e saída de emergência. Não rodar scripts
  setup/lockdown antes disso; eles alteram HOME/configurações do relógio.
- Não alterar UX, gestos, vibração ou estratégia de bateria sem consultar.

## Fontes técnicas

- AGP: https://developer.android.com/build/releases/agp-8-13-0-release-notes
- Checksums Gradle: https://gradle.org/release-checksums/
- API ambient: https://developer.android.com/reference/kotlin/androidx/wear/ambient/package-summary
- Fontes Wear 1.3.0: https://dl.google.com/dl/android/maven2/androidx/wear/wear/1.3.0/wear-1.3.0-sources.jar
- AAR Wear 1.3.0: https://dl.google.com/dl/android/maven2/androidx/wear/wear/1.3.0/wear-1.3.0.aar
- AAR Activity 1.13.0: https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.13.0/activity-1.13.0.aar
- GitHub Actions: https://docs.github.com/actions/guides/building-and-testing-java-with-gradle

Wrapper JAR SHA-256:
`81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`

Gradle distribution SHA-256:
`20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78`

## Continuação autorizada

O usuário autorizou usar `leonardolealluz183-bit/automate` (público), em branch
separada `mirrorcounter-r860`. O bloqueio de escolha de repositório está resolvido.
Este commit prepara a primeira execução; ainda não declara build/APK aprovado.
A branch main não será alterada.
