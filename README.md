# Kairós Lite — Android 2.3 Gingerbread (Samsung Galaxy Y)

Versão minimalista, 100% offline e leve em **Java puro + XML clássico** do [**Kairós**](https://github.com/Lucas2Araujo/Kairos), feita para rodar em aparelhos antigos como o **Samsung Galaxy Y** (`armv6` / `armeabi`, Android 2.3 API 9/10, tela QVGA `240x320` px).

> Projeto experimental derivado do repositório principal: **[Lucas2Araujo/Kairos](https://github.com/Lucas2Araujo/Kairos)**.

## Funcionalidades do MVP

- **Bíblia Sagrada (`ARA.sqlite`)**: leitura offline de todos os 66 livros com seletores de Livro e Capítulo + botões `<` e `>` para navegar rapidamente entre capítulos.
- **Hinário Adventista (`hinario_lite.db`)**: 601 hinos com atalho direto por número (`Nº` + **Ir**), lista rolável com *fast scroll* e leitura da letra em tela cheia.
- **Meditação Pôr do Sol (`meditacoes_2026.json`)**: pré-seleção automática da meditação da semana atual com versículo-chave, referência, autor e texto completo.
- **Tema Claro / Escuro sem Barra de Título**: alternância entre `Theme.Black.NoTitleBar` e `Theme.Light.NoTitleBar` salva em `SharedPreferences`, maximizando a área útil de leitura em `240x320`.

## Como Preparar os Assets e Compilar o APK

### Pré-requisitos
- JDK 8+ (`javac`, `jar`, `keytool`)
- Android SDK com `build-tools` (`aapt2`, `d8`, `zipalign`, `apksigner`) e qualquer `platforms/android-*` (defina `ANDROID_HOME`)
- Python 3 (apenas stdlib, caso queira regenerar os assets a partir do repositório principal `Kairos`)

### 1. (Opcional) Regenerar assets a partir do repositório `Kairos`
```bash
python3 scripts/prepare_assets.py --source ../Kairos
```
Isso copia `ARA.sqlite` e `meditacoes_2026.json` e gera `assets/hinario_lite.db` (~0,8 MB, sem tabelas FTS5 incompatíveis com o SQLite 3.6.22 do Android 2.3).

### 2. Compilar o APK (API 9/10 + Assinatura v1 JAR)
```bash
./build.sh
```
O APK pronto será gerado em `build/kairos-lite-debug.apk`.

### 3. Instalar no Galaxy Y
```bash
adb install -r build/kairos-lite-debug.apk
```

## Notas Técnicas de Compatibilidade (Android 2.3)

- **Zero dependências externas**: sem `AppCompat`, `AndroidX`, `kotlin-stdlib` ou Gradle pesado.
- **Sem erro `UNCOMPRESS_DATA_MAX` (1 MB)**: os arquivos `.sqlite` e `.db` são empacotados sem compressão (`-0 sqlite -0 db` no `aapt2 link`).
- **Dalvik DEX `035` + Assinatura v1**: `d8 --min-api 9` e `apksigner --min-sdk-version 9 --v1-signing-enabled true` garantem instalação e execução nativas no Gingerbread.
