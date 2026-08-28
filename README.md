# MirrorCounter — Watch4 SM-R860

Marcador de dois jogadores para uso na mesa. O placar superior fica girado em
180°. Fundo preto, dois números e uma divisória discreta quando ativo.

## Versão 2.1 — mudança autorizada em 28/08/2026

- Sem os símbolos +/−; as áreas de toque permanecem iguais.
- Número verde ao somar e vermelho ao reduzir, durante 200 ms. Efeitos
  independentes por jogador, sem animação contínua.
- Vibração de 70 ms (amplitude máxima quando suportada); 120 ms ao zerar.
- Bip de 45 ms; 70 ms ao zerar. Respeita perfil silencioso/vibração e volume
  de sistema do relógio. Não altera o volume global.
- Tela mantida ligada **somente com o app em primeiro plano**.
- Brilho fixo da janela: 35% ativo, 5% após 8 segundos sem interação.
  Esses valores são parâmetros do Android, não medidas físicas de luminância.
- No estado escurecido, só os números ficam visíveis. Um toque clareia a tela
  e já marca o ponto ao soltar. Os números se deslocam até 3 pixels, uma vez
  por minuto, para reduzir o risco de burn-in; não é garantia contra burn-in.
- Ao sair para Configurações ou outro app, libera tela/brilho, cancela callbacks
  e libera áudio. Não cria serviço em segundo plano nem wakelock próprio.

**O estado escurecido é uma tela interativa com brilho baixo, não o Ambient
Mode de baixo consumo do hardware.** Mantê-la ligada custa mais bateria.
A autonomia e a legibilidade precisam ser medidas no Watch4 real. A API de
Ambient permanece como fallback caso o sistema imponha esse estado.

## Controles preservados

Na orientação de cada jogador:

- Área esquerda (primeiro terço): −1.
- Número/área direita: +1.
- Segurar o número por 900 ms e soltar: zerar aquele jogador.
- Segurar a divisória central por 1 segundo e soltar: zerar os dois.
- Sete toques rápidos no centro: Configurações do sistema.
- Placares 0–99, salvos localmente e restaurados ao reabrir.

## Sensores e bateria

O MirrorCounter não solicita nem registra sensores de saúde, movimento,
luminosidade, GPS, microfone ou câmera. Não usa rede. Som e vibração são saídas.
A permissão WAKE_LOCK vem da integração Ambient; o app não adquire um lock próprio.

Isso **não desliga sensores que o Wear OS/Samsung ou outros apps utilizam**.
Veja [SENSORS_AND_BATTERY.md](SENSORS_AND_BATTERY.md) para a configuração e os
limites verificáveis. Nenhuma alteração foi aplicada remotamente no relógio.
Não executar debloat amplo como substituto de confirmar o estado dos sensores.

## Compilar e testar

GitHub Actions na branch `mirrorcounter-r860`, mantendo `main` intacta.
JDK 17, Gradle 8.13, AGP 8.13.2, Kotlin 2.3.21, compile/target SDK 36, min SDK 30.

```sh
./gradlew assembleDebug lintDebug testDebugUnitTest --no-daemon --stacktrace
```

Os testes Robolectric usam APIs 30 e 35; não substituem teste físico nem medição
energética. O workflow verifica assinatura e publica APK/checksum/metadados,
lint e relatório dos testes. Consulte BUILD_STATUS.md para a execução verificada.

## Atualização no relógio

O package continua `com.riftking.mirrorcounter`, versão 2.1 (versionCode 3).
O APK é debug-signed e um novo runner pode usar chave diferente. Se a instalação
recusar atualizar por assinatura incompatível, anote os dois placares antes de
qualquer desinstalação; desinstalar apaga os dados. Não é necessário refazer o
pareamento ADB se a conexão já funciona.

Os scripts antigos de setup/lockdown ainda são opcionais: alteram HOME/apps e
não foram executados nesta etapa. O modo de tela da versão 2.1 não precisa do
setup antigo nem de AOD ligado para evitar o timeout normal do app.
