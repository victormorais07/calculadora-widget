# Calculadora Widget

Calculadora pra Android que funciona **direto no widget da tela inicial**, sem precisar abrir app nenhum. Fiz porque queria fazer conta rápida sem ficar abrindo e fechando a calculadora.

## Funcionalidades

- Calculadora completa no widget: soma, subtração, multiplicação, divisão e porcentagem
- Prévia do resultado enquanto digita, e a conta anterior aparece em cima depois do `=`
- Contas com `BigDecimal`, então não acontece aquele `0,1 + 0,2 = 0,30000000000000004`
- Número negativo depois de × e ÷ (ex: `5 × −3`)
- Cada widget guarda o próprio estado, dá pra ter mais de um na tela
- Widget redimensionável
- Botão no app pra fixar o widget na tela inicial direto (Android 8+)

## Tecnologias

- Kotlin
- Android SDK (`AppWidgetProvider`, `RemoteViews`, `SharedPreferences`)
- Gradle (Kotlin DSL)
- GitHub Actions pra gerar o APK

## Como funciona

O widget não tem uma tela "viva" como um app normal. Cada botão manda um `PendingIntent` pro `CalcWidgetProvider`, que carrega o estado salvo daquele widget, aplica a tecla, salva de novo e redesenha o widget.

A conta é feita por um parser simples em [`Calculator.kt`](app/src/main/java/com/victor/calcwidget/Calculator.kt), que respeita a ordem das operações (× e ÷ antes de + e −).

```
app/src/main/
├── java/com/victor/calcwidget/
│   ├── CalcWidgetProvider.kt   # recebe os toques e desenha o widget
│   ├── Calculator.kt           # avalia a expressão e formata o número
│   └── MainActivity.kt         # tela que explica como adicionar o widget
└── res/
    ├── layout/widget_calc.xml  # layout do widget
    ├── drawable/               # fundo e botões
    └── xml/calc_widget_info.xml
```

## Como gerar o APK

**Pelo GitHub:** todo push roda o workflow [Gerar APK](.github/workflows/gerar-apk.yml). O APK fica em *Actions → última execução → Artifacts*.

**Na máquina:** abra o projeto no Android Studio e rode, ou pela linha de comando (precisa do JDK 17, do Android SDK e do Gradle 8.7):

```bash
gradle assembleDebug
```

O APK sai em `app/build/outputs/apk/debug/app-debug.apk`.

## Como usar

1. Instale o APK
2. Toque e segure na tela inicial → **Widgets** → arraste a **Calculadora Widget**
3. Ou abra o app e toque em **Adicionar à tela inicial**
