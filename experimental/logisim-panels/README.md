# Logisim Panels

Interface com painéis móveis para o Logisim Evolution 5.1.0-dev. Preserva o editor,
os circuitos `.circ`, a simulação e os comandos originais. Versão de avaliação 0.2.0.

## Imagens do programa

Imagens fornecidas pelo autor do projeto, com o exemplo e a interface em English, o idioma padrão. Capturadas no Panels 0.1.0 (Logisim 5.0.0); a versão 0.2.0 preserva essa interface e atualiza a base para 5.1.0-dev.

![Área de circuito com Biblioteca, Propriedades, Simulação e Tabela verdade abertas](docs/images/workspace.png)

A barra indica os painéis abertos. A Simulação controla as entradas reais do circuito e a Tabela verdade exibe a equação proposicional. Painéis e barra podem ser movidos e redimensionados.

![Configurações de idioma, tema, seleção, sinal 1, opacidade e animações](docs/images/settings.png)

## Usar sem instalar Java

- **Windows x64:** extraia `Logisim-Panels-0.2.0-windows-x64.zip` e abra `Logisim Panels.exe`.
- **Linux x64:** extraia `Logisim-Panels-0.2.0-linux-x64.tar.gz` e execute `./start.sh`.
  `./example.sh` abre o exemplo. `./install-menu.sh` adiciona um atalho ao menu de aplicativos.
  É necessário um ambiente gráfico Linux com suporte a X11/XWayland, fontes e bibliotecas de desktop.
  Em Ubuntu/Debian, as dependências usuais são `libx11-6 libxext6 libxi6 libxrender1 libxtst6 libfontconfig1 fonts-dejavu-core`.
- Os dois pacotes incluem o Java Temurin 21 e o Logisim; não precisam do Logisim original instalado.
- Os pacotes são portáteis: mantenha todos os arquivos na pasta extraída.

**Idioma:** English é o padrão. No símbolo de disposição/configurações da barra,
abra **Settings and appearance…** e escolha **Português (Brasil)** em **Language**.
A escolha é salva para as próximas aberturas, junto aos temas e à transparência.

As configurações ficam no perfil do usuário: `%LOCALAPPDATA%/logisim-panels` no Windows;
`$XDG_CONFIG_HOME/logisim-panels` ou `~/.config/logisim-panels` no Linux.
O programa pode ficar em uma pasta sem permissão de escrita. Não distribuímos suas preferências pessoais.

## Controles

Os símbolos abrem os painéis Biblioteca, Propriedades, Simulação e Tabela verdade.
A seleção indica cada painel aberto. Arraste o cabeçalho para mover, use qualquer
borda ou canto para redimensionar, `−` para minimizar e `×` para fechar.
A barra de símbolos também é móvel e pode formar linhas ou colunas.

`Ctrl+Alt+1…4` abre/fecha os painéis; `Ctrl+Alt+0` oculta todos; `Esc` fecha o painel com foco.
A Biblioteca inclui busca, acesso rápido recolhível e **Legacy** para o explorador original.
A tabela mostra a equação proposicional, calcula até 10 bits de entrada e exporta CSV.
Circuitos com memória/relógios devem ser acompanhados pela Simulação.

## Organização do projeto

| Pasta | Conteúdo |
|---|---|
| `src/main/java` | Interface e alteração visual pontual de `Probe` |
| `src/main/resources` | Temas, traduções e fonte com sua licença |
| `src/test/java` | Verificações de integração, simulação, layout, aparência e idioma |
| `assets` | Circuito de exemplo e ícones originais |
| `scripts` | Compilação e geração dos pacotes |
| `docs` | Guias e resultados de verificação |
| `vendor` | Dependências externas, excluídas do Git |
| `build`, `dist` | Saídas geradas, excluídas do Git |

## Compilar e verificar

A interface agora é compilada junto aos fontes atuais do fork, usando Gradle e JDK 21+. A revisão original está registrada em [UPSTREAM.json](UPSTREAM.json). O aplicativo usa um JAR único, sem carregar a release 5.0.0 ou sobrepor classes no classpath.

Clone o fork, selecione a branch `logisim-panels` e execute na raiz do repositório:

```sh
./gradlew --no-daemon --no-configuration-cache test verifyPanels shadowJar
```

No Windows use `gradlew.bat`; os testes originais de memória exigem `xxd`, incluído em `Git/usr/bin`. No Linux sem display, use `xvfb-run -a`. O JAR fica em `build/libs/logisim-evolution-5.1.0dev-all.jar`. `-Ppanels=false` seleciona o ponto de entrada do editor original.

O auxiliar Python aceita `--jdk`, `--upstream` e `--tests`; ele usa o mesmo build nativo. Os pacotes incluem o código-fonte correspondente completo.

## Testes e resultados

- **350 verificações funcionais** passaram localmente no Windows: integração do editor, simulação, redimensionamento, layout, temas, alinhamento dos pinos e idioma.
- **13 verificações adicionais** confirmam os novos TTL ao criar, salvar e reabrir circuitos.
- **734 testes originais** passaram localmente, sem falhas ou testes omitidos.
- Os pacotes atualizados são verificados separadamente; veja o relatório abaixo.

[Execução anterior da versão 0.1.0 no GitHub](https://github.com/toybile/logisim-evolution/actions/runs/37790235710) · [Relatório detalhado](docs/VERIFICACOES.md)

São testes direcionados; não cobrem todos os recursos do Logisim. Ainda falta a conferência em um desktop Linux real.

## Compartilhar e contribuir

Esta modificação é distribuída sob GPL v3, com atribuições e licenças preservadas.
Cada pacote inclui `sources.zip`, contendo esta interface e o código-fonte correspondente
da base atual do Logisim Evolution, com as alterações nativas deste fork. Ao redistribuir, mantenha as licenças e os fontes acessíveis.
Os fontes e instruções da distribuição Java estão em `docs/THIRD-PARTY.md`.

É possível apresentar esta proposta ao repositório original. A interface está integrada ao build nativo deste fork. A adoção pelo projeto original ainda exige revisão e acordo com os mantenedores. Não há aceitação garantida.

Original: https://github.com/logisim-evolution/logisim-evolution
Resultados e limitações atuais: `docs/VERIFICACOES.md`.
