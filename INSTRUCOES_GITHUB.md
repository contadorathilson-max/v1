# Instruções para atualizar o app no GitHub

## Atualizar usando upload pelo navegador

1. Extraia o arquivo ZIP no seu computador.
2. Abra a pasta extraída.
3. No GitHub, entre no repositório do app.
4. Clique em **Add file** > **Upload files**.
5. Arraste todos os arquivos e pastas da pasta extraída para a página do GitHub.
6. Quando o GitHub perguntar se deve substituir arquivos existentes, confirme o envio.
7. Clique em **Commit changes**.
8. Depois vá em **Actions** e rode o workflow **Gerar APK Android**.

## Arquivos importantes

- `app/src/main/java/com/athilson/sorteadortimes/MainActivity.java`: código principal do app.
- `.github/workflows/build-apk.yml`: workflow que gera o APK.
- `app/build.gradle`: configuração do app Android.

## Melhorias desta versão

- Corrigido o problema das estrelas ficarem sempre em 3.
- Remoção de jogador ficou mais visível e com confirmação.
- Adicionados cabeçalhos nos campos de quantidade de times e jogadores por time.
- Adicionado ranking de gols e assistências.
- Adicionado botão para copiar o resultado do sorteio.
- Adicionado controle de reservas quando sobram jogadores.
