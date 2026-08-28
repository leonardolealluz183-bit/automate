# MirrorCounter R860 — ChatGPT Work Handoff

## Objetivo
Transformar um Samsung Galaxy Watch4 SM-R860 em um dispositivo praticamente dedicado a marcador de pontos para TCG/Riftbound, com foco máximo em autonomia de bateria.

## Interface desejada
- Dois placares na tela do relógio.
- Placar inferior normal para o usuário.
- Placar superior rotacionado 180 graus, legível pelo oponente do outro lado da mesa.
- Controles simples de + e - para cada lado.
- Fundo AMOLED preto.
- Sem animações e sem elementos desnecessários.
- Modo ambiente/baixo consumo mantendo somente os números visíveis.

## Hardware
- Samsung Galaxy Watch4 40 mm Bluetooth
- Modelo: SM-R860
- Telefone pareado: Samsung Galaxy S23 Ultra

## Estratégia escolhida
Evitar ROM custom inicialmente. Manter kernel/drivers/gerenciamento de energia Samsung e transformar o Wear OS em uma espécie de appliance/kiosk:
- app de marcador como HOME/launcher;
- abrir diretamente no boot ou ao voltar para HOME;
- desabilitar serviços/apps não usados via ADB de forma reversível;
- Wi-Fi/Bluetooth/NFC/localização desligados após instalação e testes;
- sem wakelock e sem KEEP_SCREEN_ON;
- usar Ambient Mode para economizar bateria;
- saída de emergência para Configurações por gesto secreto.

## Projeto atual
Arquivo incluído neste pacote:
- `MirrorCounter_R860_Appliance.zip`

Esse projeto foi criado como protótipo para Wear OS. Antes de publicar/instalar, revisar e compilar no GitHub Actions.

## Problema encontrado até agora
Tentativa de compilar no Android pelo app `com.m4coding.ide` falhou porque ele usava JVM antiga.
Tentativa com AndroidIDE oficial arm64-v8a também falhou no S23 Ultra porque o app fechava instantaneamente.

Decisão: usar GitHub Actions para compilar o APK.

## Próximos passos no Work
1. Inspecionar o conteúdo de `MirrorCounter_R860_Appliance.zip`.
2. Corrigir qualquer erro de Gradle/AGP/SDK e simplificar dependências.
3. Preferir Java/Kotlin e Android APIs compatíveis com Wear OS do Galaxy Watch4.
4. Criar workflow `.github/workflows/build.yml` que:
   - use Ubuntu;
   - configure JDK 17;
   - rode `./gradlew assembleDebug`;
   - publique o APK como artifact.
5. Se houver acesso ao GitHub conectado, criar ou usar o repositório `MirrorCounter-R860` e enviar o projeto.
6. Rodar CI, diagnosticar falhas e corrigir até gerar APK válido.
7. Depois orientar instalação pelo S23 Ultra no Watch4 via ADB wireless/GeminiMan WearOS Manager.
8. Só depois de confirmar que o app abre e o Ambient Mode funciona, aplicar o lockdown/debloat via ADB.

## Requisitos de bateria
Prioridade máxima:
- interface majoritariamente preta;
- CPU ociosa entre toques;
- sem timers contínuos;
- sem sensores;
- sem rede;
- sem animações;
- Ambient Mode sempre que possível;
- evitar burn-in com pequenos deslocamentos periódicos em modo ambiente.

## UX desejada
- tocar no número ou área direita: +1;
- botão/área esquerda: -1;
- long press no número: zerar aquele jogador;
- long press central: zerar os dois;
- salvar placar localmente;
- restaurar placar após reinício;
- 7 toques rápidos na linha central: abrir Configurações como saída de emergência.

## Critério de sucesso
O relógio deve se comportar, para uso prático, como um aparelho dedicado:
`ligar -> MirrorCounter -> marcar pontos -> Ambient Mode`
sem necessidade de usar o restante do Wear OS durante as partidas.

## Retomada no Work — 27/08/2026

Leia também `BUILD_STATUS.md` antes de continuar. O workflow e o wrapper oficial
foram preparados e verificados estaticamente. O aplicativo permaneceu intacto.
**APK gerado e verificado em 27/08/2026.**
O usuário autorizou usar o repositório público `leonardolealluz183-bit/automate`,
na branch separada `mirrorcounter-r860`, para executar o GitHub Actions.
A branch `main` deve permanecer intacta.

Build aprovado: https://github.com/leonardolealluz183-bit/automate/actions/runs/33125514387

Commit do APK: `8cedb148e4c307a62d3a797c50ca2b70a49149df`.
O erro de `kotlinOptions.jvmTarget` foi corrigido com `compilerOptions` tipado,
mantendo Java 17. Compilação, lint (0 erros/13 avisos), assinatura e checksum
foram verificados. O app ainda não foi testado no relógio; o próximo passo é
instalar normalmente e testar antes de mudar HOME ou executar lockdown.
Consulte `BUILD_STATUS.md` para a evidência completa e o hash do APK.

## Atualização autorizada — 28/08/2026 (prevalece sobre a UX/energia anterior)

O usuário instalou o APK 2.0 no Watch4 com GeminiMan e confirmou que funciona.
Relatou tela apagando após cerca de 10 s, símbolos poluindo a interface e feedback
fraco. Pediu remover os símbolos, aumentar vibração, incluir bip e verde/vermelho
por milissegundos ao somar/reduzir. Aprovou explicitamente manter a tela ligada
com redução de brilho, ciente do custo em relação ao Ambient de hardware.
Também pediu para desligar todos os outros sensores.

Implementação 2.1: 200 ms de cor só no placar alterado, vibração 70/120 ms,
bip 45/70 ms, brilho de janela 35% e 5% após 8 s, deslocamento mínimo de pixels
a cada minuto quando escurecido. Preserva limites, persistência, rotação e áreas
de toque. Toque no estado escurecido já pontua. Libera tela/áudio/callbacks ao sair.

Essa autorização substitui a restrição anterior contra KEEP_SCREEN_ON para esta
sessão em primeiro plano. Não adicionar wakelock próprio, sensores ou rede.
Não confundir o estado escurecido com Ambient real. Hardware/autonomia ainda
precisam de teste da versão nova; conferir BUILD_STATUS.md para status do CI.

Sensores do sistema **ainda não foram desativados**. O app não os utiliza.
SENSORS_AND_BATTERY.md registra a inspeção e a configuração pendente no relógio.
Não executar a lista antiga de debloat como se garantisse desligar sensores.
Não tocar em proteções de bateria/temperatura nem nos drivers essenciais.

Assinatura: o CI ainda utiliza debug key do runner. Verificar compatibilidade
na atualização; registrar placares antes de eventual desinstalação. Main intacta.
