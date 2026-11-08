# Checkstyle — Convenção de nomes de CONSTANTES (LEI-62015, Temóteo da Costa)

## Identificador escolhido
**Constantes** (`static final`). Regra Checkstyle **`ConstantName`** com o padrão `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$` — ou seja, nomes em MAIÚSCULAS com palavras separadas por `_` (ex.: `WIN_RESULT`, `MAX_SHIPS`). Diferente dos colegas (99917 = `MethodName`, 105514 = `TypeName`).

## Configuração
- `checkstyle-62015.xml`: `Checker` → `TreeWalker` → `ConstantName` com o `format` acima, `severity=error`.
- Workflow: `.github/workflows/checkstyle-62015.yaml` — action `dbelyaev/action-checkstyle@v4.4.0`, corre em `pull_request`, `filter_mode: added` e `fail_level: error` (falha quando há violações nas linhas adicionadas).

## Execuções (passo 4)
Demonstração no PR #43 (ramo `62015-checkstyle-demo`), introduzindo em `Scoreboard.java` a constante mal nomeada `private static final int maxShips = 5;`:
- Gate a **FALHAR**: https://github.com/LEI105514/Battleship2/actions/runs/42529376676
- Gate a **PASSAR** (após renomear para `MAX_SHIPS`): https://github.com/LEI105514/Battleship2/actions/runs/42529628991

## O que o Checkstyle NÃO consegue verificar
O Checkstyle valida apenas a **forma** do identificador (a expressão regular), não o seu **significado**: não sabe se o nome é claro ou adequado ao conceito (p. ex., `MAX_X` passa a regra mas pode ser um nome pouco informativo). Também não deteta constantes que **deviam existir** mas foram escritas como números/strings mágicos no código, nem avalia coerência semântica entre nomes relacionados.
