# Qodana — Long Parameter List (LEI-62015, Temóteo da Costa)

## Cheiro escolhido
**Long Parameter List** (lista de parâmetros longa): método ou construtor com parâmetros a mais, o que dificulta a leitura, o uso correto e a manutenção. Consta do catálogo de Martin Fowler; no livro *Object-Oriented Metrics in Practice* é detetável quantitativamente pela contagem de parâmetros.

## Tabela de mapeamento (passo 4)

| Inspeção no IntelliJ/Qodana | ID | Métrica / valor-limite | O que NÃO é possível expressar |
|---|---|---|---|
| Method with too many parameters (Java → Method metrics) | `ParametersPerMethod` | nº de parâmetros do método; opção `m_limit` (default 5) | Não distingue o tipo nem a relação entre parâmetros; não deteta *Data Clumps* (parâmetros que andam sempre juntos e deviam ser um objeto); usa um limiar fixo, ao contrário do livro, que define limiares de forma relativa (média + desvio-padrão) e combina várias métricas por condições lógicas. |

## Configuração
- `qodana-62015.yaml` (raiz): linter `jetbrains/qodana-jvm-community:2026.2`, `projectJDK: 21`, inspeção `ParametersPerMethod` com `m_limit: 5`, `failureConditions.severityThresholds.any: 0`.
- Perfil de inspeção: `.qodana/profiles/profile-62015.yaml`.
- Workflow: `.github/workflows/qodana-smell-62015.yaml` (corre em `pull_request` e em `push` para `main`).

## Comportamento no código atual
Sobre o `main` limpo, o gate **passa** (verde) sem necessidade de *baseline*: a única classe com mais de 5 parâmetros é o construtor canónico do *record* `MoveRecord` (10 parâmetros), que a inspeção `ParametersPerMethod` **não reporta** (ignora construtores canónicos de *records*). O gate fica assim disponível para falhar apenas perante violações novas.

## Execuções (passo 9)
Demonstração no PR #42 (ramo `62015-qodana-demo`), introduzindo o método `demoLongParameterList(int, int, int, int, int, int)` (6 parâmetros) em `Scoreboard.java`:
- Gate a **FALHAR**: https://github.com/LEI105514/Battleship2/actions/runs/42527327167
- Gate a **PASSAR** (após remover o método): https://github.com/LEI105514/Battleship2/actions/runs/42527887673

## Reflexão
A inspeção detetou corretamente os métodos/construtores com mais parâmetros do que o limiar `m_limit=5`, mas revelou um **falso negativo** relevante: o construtor canónico do *record* `MoveRecord`, com 10 parâmetros, não é reportado, apesar de ser um caso claro de *Long Parameter List*. O resultado é **sensível ao limiar**: com `m_limit=5` apanha 6+ parâmetros, e subir o valor reduz as deteções sem qualquer base relativa ao sistema. A principal limitação face ao livro é usar um **limiar absoluto** (e não relativo à média/desvio-padrão de um conjunto de sistemas) e não conseguir exprimir o cheiro associado de *Data Clumps* (parâmetros que andam sempre juntos e deveriam formar um objeto).
