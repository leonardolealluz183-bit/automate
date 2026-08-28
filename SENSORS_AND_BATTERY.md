# Sensores e bateria — estado real da configuração

## Pedido do usuário

Em 28/08/2026, o usuário autorizou a tela ligada com brilho reduzido e pediu
para desativar todos os outros sensores. Preservar tela sensível ao toque,
botões, alto-falante e motor de vibração. Não alterar proteções térmicas,
carregamento, bateria, drivers ou componentes essenciais.

## O que a versão 2.1 faz dentro do aplicativo

- Nenhuma assinatura de sensores, monitorização de saúde, GPS, microfone,
  câmera, leitura do sensor de luz ou acesso de rede.
- Brilho fixo por janela, independente de uma leitura de luz pelo app.
- Nenhum serviço de fundo; som e vibração somente ao tocar nos controles.
- Atualização visual por eventos, encerramento da cor após 200 ms, redução do
  brilho após 8 s e um deslocamento de pixels a cada 60 s enquanto escurecido.
- Cancela os callbacks ao sair do app. Não faz polling da bateria nem da rede.

Essas medidas não garantem que os sensores estejam eletricamente desligados:
o sistema Samsung e outros aplicativos podem mantê-los ativos.

## O que ainda precisa ser aplicado e conferido no Watch4

**Não aplicado. Não temos uma sessão ADB remota com o relógio.**

1. Confirmar se a versão Samsung oferece o controle `Sensores desativados`
   (`Sensors off`) nas opções do desenvolvedor. O caminho do Android genérico
   passa por blocos de configurações rápidas para desenvolvedores; não assumir
   que esse menu exista no Watch4. Pedir uma foto da tela antes de instruir.
2. Se existir, ativar e confirmar se som, vibração, tela e toque continuam
   funcionando. Esse controle bloqueia a entrega de dados por serviços padrão;
   sensores específicos da Samsung podem exigir tratamento separado.
3. Desligar medições automáticas de saúde, detecção de exercícios, detecção
   de uso no pulso/gestos e brilho adaptável que estiverem disponíveis na
   versão do relógio. Conferir os menus reais; não inventar nomes de chaves ADB.
4. Desligar localização e NFC. Depois da instalação e dos testes, desligar
   depuração sem fio, Wi-Fi e Bluetooth. Manter Wi-Fi/ADB até finalizar para
   não perder a conexão durante a configuração. Radios não são sensores.
5. Caso algum cliente continue solicitando sensores, identificar esse cliente
   no relatório antes de desabilitar um pacote. Não remover drivers nem
   desabilitar System UI, Configurações, gerenciamento térmico ou de energia.

Para inspeção por ADB (somente leitura), com o alvo correto selecionado:

```sh
adb -s IP:PORTA shell dumpsys sensorservice
adb -s IP:PORTA shell dumpsys sensor_privacy
adb -s IP:PORTA shell cmd sensor_privacy help
adb -s IP:PORTA shell getprop ro.build.version.release
adb -s IP:PORTA shell getprop ro.build.version.sdk
```

No terminal de shell já conectado do GeminiMan, não incluir `adb -s IP:PORTA shell`.
A existência de sensores na lista de hardware não significa que estejam ativos:
inspecionar clientes/conexões ativos. Ausência de clientes também não certifica
sensores ligados diretamente ao firmware Samsung ou circuitos de proteção.
`cmd sensor_privacy` varia por firmware; não chamar operações numéricas de Binder
nem assumir que bloquear microfone/câmera equivale a bloquear todos os sensores.

## Validação de autonomia

Após confirmar o APK e desligar a depuração/rádios, usar uma sessão real fora do
carregador. Registrar carga inicial/final e duração; repetir em condições
semelhantes. Não extrapolar uma medição curta para prometer horas de autonomia.
O modo de tela contínua custa mais energia que o Ambient real, mesmo com brilho
baixo; esse compromisso foi aprovado pelo usuário.

## Referências primárias

- Android Sensors off, funcionamento e customização por fabricante:
  https://source.android.com/docs/core/interaction/sensors/sensors-off
- Tela ligada e liberação em segundo plano:
  https://developer.android.com/develop/background-work/background-tasks/awake/screen-on
- Estados interactive/ambient e impacto na bateria:
  https://developer.android.com/training/wearables/always-on
