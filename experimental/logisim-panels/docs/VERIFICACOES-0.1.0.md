# Verificações — versão 0.1.0

- 35 verificações de integração do editor original, arquivos `.circ` e simulação.
- 8 verificações de análise assíncrona e controles que alteram os pinos reais.
- 71 verificações dos símbolos, oito direções de redimensionamento, canto, barra e edição com desfazer.
- 24 verificações de temas, opacidade, animações e correspondência entre equação e tabela.
- 192 verificações do alinhamento de 0/1, em quatro orientações e quatro ampliações. Diferença máxima de rasterização: 0,5 pixel.
- 20 verificações de idioma: padrão English, ida e volta para pt-BR, persistência, menus nativos, campos e preservação do layout.

Total: **350 verificações funcionais**. Não se trata de cobertura completa de todos os componentes do Logisim.
Os testes passaram também com o Java 21 incluído no pacote Windows. A escolha de idioma
foi verificada pelo formulário real de configurações. Outras 28 verificações validam
os arquivos compartilháveis, recursos, fontes, permissões de execução e SHA-256.

## Otimizações

O mesmo ensaio de 1.000 atualizações dos sinais inalterados, no circuito de exemplo,
registrou 7.000 pedidos de redesenho na versão anterior e zero nesta versão.
Isso mede os redesenhos evitados neste caso; não implica redução de 100% de CPU ou memória do programa.

Também foram reduzidas as varreduras periódicas da Biblioteca, evitadas substituições
de texto sem mudança e suspensas as atualizações periódicas quando a janela está minimizada.
Os painéis fechados liberam seus buffers de imagem; opacidade de 100% usa desenho direto.
A fonte dos números é reutilizada, com alternativa incluída para sistemas sem as fontes do Windows.
O build usa diretórios novos e separa testes dos arquivos distribuídos.

## Limitações

Os testes foram executados no Windows. O pacote Linux x64 preserva permissões de execução
e ligações do Java oficial, e inclui a mesma interface compilada para Java 21.
Ainda falta executar a interface gráfica numa máquina Linux real.
No fork do GitHub, a rotina `.github/workflows/logisim-panels.yml` passou em Windows
e Ubuntu com display virtual (Xvfb), usando Java 21 e o JAR oficial da release 5.0.0.
Resultado: https://github.com/toybile/logisim-evolution/actions/runs/37790235710
Esta execução automatizada não substitui a conferência num desktop Linux real.
Os pacotes não são assinados digitalmente. A versão de avaliação está publicada em [downloads](https://github.com/toybile/logisim-evolution/releases/tag/panels-v0.1.0).
