# Verificação da interface ajustável

08/10/2026 — Windows, Logisim Evolution 5.0.0, Java 26; compilação Java compatível com versão 21.

138 verificações passaram:

- 35 de integração: salvar/reabrir o circuito, oito saídas de Y = (A ∧ B) ∨ ¬C, propagação nativa, controles e recuperação do editor original.
- 8 de análise/simulação: cálculo em segundo plano, pinos e saída reais e descarte de resultados desatualizados.
- 71 de interação: quatro símbolos selecionados independentemente, ferramentas e comandos, pesquisa, formatos da barra, posições e tamanhos, redimensionamento pelas oito direções, controle diagonal, limites, janela pequena, atributos e Desfazer.
- 24 de aparência/equação: expressão real com todos os símbolos legíveis, equivalência com as oito linhas da tabela, fundo com ou sem grade, sinal 1 e seleção independentes, indicador real da aba atualizado, preservação do espaço sem contorno extra, persistência, composição translúcida, quadros intermediários de fade, reabertura durante fechamento e desativação das transições.

Após a última correção de atualização dos indicadores e contornos, foram repetidas as 24 verificações de aparência/equação; a bateria anterior de 114 não foi repetida para essa correção. Todas as execuções finais terminaram sem erros.

Conferência visual realizada no programa: biblioteca e seus grupos, recolhimento e rolagem do Acesso rápido, Legacy, interruptores sem cortes, números centralizados nos pinos clássicos e equação no canto superior direito. A instalação original não foi sobrescrita.

Escopo: circuitos de teste próprios, tabela integrada combinacional com até 10 bits de entrada. Menus e ferramentas nativas conservados. FPGA, HDL, bibliotecas externas e todos os componentes não foram auditados individualmente.

Os fontes das verificações estão em src/local/logisim/panels na prévia, ou em ../src/local/logisim/panels na pasta de desenvolvimento. Os arquivos check-*.properties são configurações dos testes, não preferências para distribuição.

## Correção do alinhamento dos pinos — 08/10/2026

Círculo e dígito dos pinos clássicos de um bit usam o centro do contorno visível. Os caracteres são centralizados pela forma vetorial efetivamente desenhada, evitando deslocamento da fonte em zoom fracionário.

Verificação específica por renderização do componente nativo: entradas/saídas, 0/1, quatro orientações e zoom de 100%, 125%, 150% e 200%. Foram 64 desenhos, com 192 verificações de presença/alinhamento; maior diferença observada na rasterização: 0,5 pixel. A versão anterior falhou nesta verificação de centralização. A bateria geral de 138 verificações não foi repetida para esta mudança de apresentação.

Fontes dessa verificação: ../verificacao/alinhamento/PinAlignmentChecks.java. Conferência também no exemplo aberto no programa.
