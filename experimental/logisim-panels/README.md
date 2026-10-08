# Logisim Panels

Interface com painéis móveis para o Logisim Evolution 5.0.0. Preserva o editor,
os circuitos `.circ`, a simulação e os comandos originais. Versão de avaliação 0.1.0.

## Imagens do programa

Capturas reais da versão Windows, com o circuito de exemplo incluído. As imagens usam Português (Brasil); English continua sendo o idioma padrão.

![Área de circuito com Biblioteca, Propriedades, Simulação e Tabela verdade abertas](docs/images/workspace.png)

A barra indica os painéis abertos. A Simulação controla as entradas reais do circuito e a Tabela verdade exibe a equação proposicional. Painéis e barra podem ser movidos e redimensionados.

![Configurações de idioma, tema, seleção, sinal 1, opacidade e animações](docs/images/settings.png)

## Usar sem instalar Java

- **Windows x64:** extraia `Logisim-Panels-0.1.0-windows-x64.zip` e abra `Logisim Panels.exe`.
- **Linux x64:** extraia `Logisim-Panels-0.1.0-linux-x64.tar.gz` e execute `./start.sh`.
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

Requer Python 3.11+ e JDK 21+. Não requer bibliotecas Python externas.
Obtenha `logisim-evolution-5.0.0-all.jar` na versão oficial e coloque em `vendor/`.
O pacote de fontes inclui também `vendor/logisim-evolution-5.0.0-source.zip`.
Para construir o original a partir desse código-fonte, extraia o arquivo e siga seu README/Gradle.

```sh
python scripts/build.py --jdk /caminho/do/jdk --tests
```

No Windows, a mesma instrução aceita um caminho como `"C:/Program Files/Java/jdk-26"`.
Os testes que criam janelas precisam de uma sessão gráfica. No Linux, podem rodar com `xvfb-run`.
`scripts/package.py` cria os pacotes completos usando os arquivos oficiais descritos em `vendor/README.md`.
Não há publicação automática nem envio ao GitHub.

## Testes e resultados

- **350 verificações funcionais** passaram localmente no Windows: integração do editor, simulação, redimensionamento, layout, temas, alinhamento dos pinos e idioma.
- As suítes automatizadas também passaram no GitHub em **Windows e Ubuntu com Xvfb**.
- **28 verificações dos pacotes** passaram localmente.

[Execução bem-sucedida no GitHub](https://github.com/toybile/logisim-evolution/actions/runs/37790235710) · [Relatório detalhado](docs/VERIFICACOES.md)

São testes direcionados; não cobrem todos os recursos do Logisim. Ainda falta a conferência em um desktop Linux real.

## Compartilhar e contribuir

Esta modificação é distribuída sob GPL v3, com atribuições e licenças preservadas.
Cada pacote inclui `sources.zip`, contendo esta interface e o código-fonte correspondente
do Logisim Evolution 5.0.0. Ao redistribuir, mantenha as licenças e os fontes acessíveis.
Os fontes e instruções da distribuição Java estão em `docs/THIRD-PARTY.md`.

É possível apresentar esta proposta ao repositório original. Ela ainda é uma extensão
com uma classe visual sobreposta; a integração definitiva ao projeto original exigiria
adequação à arquitetura e aos critérios dos mantenedores. Não há aceitação garantida.

Original: https://github.com/logisim-evolution/logisim-evolution
Resultados e limitações atuais: `docs/VERIFICACOES.md`.
