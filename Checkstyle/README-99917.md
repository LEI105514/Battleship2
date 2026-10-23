# Checkstyle — Quality Gate MethodName

**Aluno:** Afonso Carolo — LEI-99917  
**Projeto:** Battleship2  
**Branch:** `99917-checkstyle`  
**Pull Request:** [PR #22](https://github.com/LEI105514/Battleship2/pull/22)

## 1. Objetivo

O objetivo desta tarefa foi implementar um Quality Gate que verifica automaticamente se os nomes dos métodos Java respeitam as convenções de programação.

Para isso, foi utilizada a ferramenta Checkstyle, integrada no GitHub Actions, permitindo detetar nomes incorretos durante a revisão de Pull Requests.

## 2. Regra escolhida

Foi selecionada a regra **MethodName**, responsável por verificar os nomes dos métodos Java.

A expressão regular utilizada foi:

`^[a-z][a-zA-Z0-9]*$`

Esta expressão exige que o nome de um método comece por uma letra minúscula e que os restantes caracteres sejam letras ou números.

Exemplos:

| Nome do método | Resultado |
|---|---|
| `calcularPontos()` | Correto |
| `getPlayerName()` | Correto |
| `CalcularPontos()` | Incorreto |
| `calcular_pontos()` | Incorreto |

## 3. Configuração

Foram criados os seguintes ficheiros:

- `checkstyle-99917.xml` — configuração XML com a regra `MethodName`.
- `.github/workflows/checkstyle-99917.yaml` — workflow que executa automaticamente o Checkstyle no GitHub Actions.

A configuração utiliza a severidade `error` e a action `dbelyaev/action-checkstyle@v4.4.0`.

O workflow está configurado para analisar as alterações introduzidas nos Pull Requests, utilizando `filter_mode: added`, e para falhar quando forem detetadas violações com severidade de erro.

## 4. Testes realizados

Foram realizadas três execuções para validar o funcionamento do Quality Gate.

| Execução | Resultado | Descrição |
|---|---|---|
| #1 | Success | Execução inicial sem novas violações. |
| #2 | Failure | Foi introduzido deliberadamente o método `CalcularPontos()`. |
| #3 | Success | O nome foi corrigido para `calcularPontos()`. |

### 4.1. Execução vermelha — Violação detetada

**Execução #2:** [Consultar execução vermelha](https://github.com/LEI105514/Battleship2/actions/runs/40024738781/job/120657968680)

Foi criada a classe temporária `CheckstyleNameTest99917.java`, contendo o método:

```java
public void CalcularPontos() {
    System.out.println("Teste Checkstyle");
}
```

O Checkstyle identificou corretamente a violação da regra `MethodName`, indicando o ficheiro e a localização do problema.

A mensagem apresentada no GitHub Actions indicava:

`CheckstyleNameTest99917.java:6:17: error: Name 'CalcularPontos' must match pattern '^[a-z][a-zA-Z0-9]*$'.`

Como foi detetada uma violação com severidade `error`, o Quality Gate terminou com o estado **Failure**.

### 4.2. Execução verde — Violação corrigida

**Execução #3:** [Consultar execução verde](https://github.com/LEI105514/Battleship2/actions/runs/40025613192)

O método foi corrigido para:

```java
public void calcularPontos() {
    System.out.println("Teste Checkstyle");
}
```

Após esta alteração, o GitHub Actions executou novamente o Checkstyle e terminou com o estado **Success**.

Os resultados demonstraram que a ferramenta consegue detetar uma violação da convenção de nomes e verificar a sua correção.

## 5. Limitações do Checkstyle

O Checkstyle verifica se os identificadores cumprem padrões sintáticos, mas não consegue determinar se um nome descreve corretamente a finalidade de um método.

Por exemplo, um método chamado `abc()` pode respeitar a expressão regular definida, apesar de não ter um nome suficientemente significativo.

Além disso, a configuração utilizada verifica apenas os nomes dos métodos e considera as linhas adicionadas nos Pull Requests, não garantindo a identificação de todas as violações já existentes no projeto.

## 6. Proteção do branch main

A ficha propõe configurar uma regra de proteção da `main` que exija a aprovação do Quality Gate antes de permitir o merge.

No entanto, não foi possível configurar esta proteção porque a conta utilizada não possui permissões de administração do repositório. O GitHub apresenta a mensagem:

"You don't have access to repository options."

Deste modo, o workflow deteta e sinaliza as violações, mas não foi possível garantir tecnicamente o bloqueio de merges quando o Checkstyle falha.

## 7. Conclusão

A integração do Checkstyle com o GitHub Actions permitiu automatizar a verificação das convenções dos nomes dos métodos Java.

O teste demonstrou que a introdução de um nome incorreto provoca a falha do Quality Gate e que a correção permite recuperar o estado de sucesso.

Apesar das suas limitações, esta ferramenta contribui para melhorar a consistência e a manutenção do código.