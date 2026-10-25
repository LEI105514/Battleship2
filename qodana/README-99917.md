# Qodana — Quality Gate Long Method

**Aluno:** Afonso Carolo — LEI-99917  
**Projeto:** Battleship2  
**Pull Request:** [PR #21](https://github.com/LEI105514/Battleship2/pull/21)  
**Branch:** `99917-qodana-long-method`

## 1. Code Smell escolhido

O code smell escolhido foi **Long Method**, que ocorre quando um método contém um número excessivo de instruções, dificultando a sua leitura, compreensão, manutenção e teste.

Para identificar este problema, foi utilizado o **Qodana for JVM Community 2026.2**, integrado no GitHub Actions.

## 2. Mapeamento entre Code Smell e inspeção

| Elemento | Configuração |
|---|---|
| Code Smell | Long Method |
| Inspeção da IDE/Qodana | Overly long method |
| ID da inspeção | `NonCommentSourceStatements` |
| Métrica utilizada | Número de instruções de código-fonte sem comentários |
| Opção de configuração | `m_limit` |
| Limiar escolhido | 30 |
| Condição de falha | Pelo menos 1 nova violação |

O limiar de **30 instruções** significa que os métodos que ultrapassam este valor são sinalizados pela inspeção.

Esta métrica aproxima a identificação de métodos excessivamente longos, embora não seja equivalente à contagem tradicional de linhas de código (LOC).

## 3. Justificação do limiar

A escolha do limiar pretende identificar métodos com um número elevado de instruções e incentivar a sua divisão em métodos menores, com responsabilidades bem definidas.

A literatura sobre métricas orientadas a objetos apresenta critérios para avaliar o tamanho e a complexidade dos métodos. Contudo, os limiares dependem da métrica utilizada e do contexto do projeto.

Neste trabalho foi adotado um limiar operacional de **30 instruções sem comentários**, adequado à inspeção disponibilizada pelo Qodana. Este valor não representa uma conversão direta dos limiares de LOC apresentados na literatura.

## 4. Configuração do Quality Gate

Foram utilizados os seguintes ficheiros:

- `.github/workflows/qodana-smell-99917.yaml` — workflow do GitHub Actions.
- `qodana-99917.yaml` — configuração específica do aluno, incluindo o perfil de inspeção efetivamente utilizado.
- `.qodana/profiles/profile-99917.yaml` — perfil separado de referência da inspeção Long Method.
- `.qodana/baseline-99917.sarif.json` — registo dos problemas existentes antes do teste.

O workflow utiliza `JetBrains/qodana-action@v2026.2`, com análise ao projeto e limiar de falha de zero novas violações.

A configuração personalizada ativa a inspeção `NonCommentSourceStatements`, com `m_limit: 30`, em vez de utilizar o conjunto completo de inspeções do perfil padrão.

## 5. Baseline

Na primeira análise corretamente configurada, o Qodana identificou **7 métodos demasiado longos** no projeto.

Foi criada uma baseline a partir desse relatório para registar os problemas preexistentes.

Assim, os métodos já identificados não impedem continuamente a execução do workflow, mas a introdução de um novo método que viole a inspeção provoca a falha do Quality Gate.

A baseline não corrige os problemas antigos: apenas permite distingui-los de novas ocorrências.

## 6. Testes realizados

| Execução | Resultado | Descrição |
|---|---|---|
| #8 | Failure | Foram detetados 7 métodos longos preexistentes. |
| #9 | Success | Com a baseline configurada, não foram encontrados problemas novos. |
| #10 | Failure | Foi introduzido deliberadamente um método longo, originando 1 nova violação. |
| #11 | Success | Após remover o método de teste, o Quality Gate voltou a passar. |

### Evidência — falha após introdução do cheiro

**Execução #10:** [Consultar execução vermelha](https://github.com/LEI105514/Battleship2/actions/runs/40007062747)

Foi criada temporariamente a classe `LongMethodTest99917.java`, contendo um método com mais de 30 instruções. O Qodana identificou uma nova ocorrência de **Overly long method** e o workflow falhou.

### Evidência — sucesso após correção

**Execução #11:** [Consultar execução verde](https://github.com/LEI105514/Battleship2/actions/runs/40009249380)

A classe temporária foi eliminada e o Qodana voltou a executar a análise sem detetar novos problemas.

Esta sequência confirma que o Quality Gate deteta a introdução de um novo Long Method e volta a passar depois da sua remoção.

## 7. Limitações da abordagem

A solução apresenta algumas limitações:

1. A contagem de instruções não mede diretamente a complexidade cognitiva ou a dificuldade de compreensão de um método.
2. Um método com menos de 30 instruções pode continuar a ser difícil de manter.
3. Um método com mais de 30 instruções não é necessariamente incorreto; poderá justificar-se em determinados contextos.
4. A baseline contém sete problemas antigos, que continuam a existir no projeto e deverão ser analisados futuramente.
5. A inspeção Long Method não combina automaticamente diferentes métricas, como LOC e complexidade ciclomática, através de uma condição lógica AND.
6. A obrigatoriedade de aprovação do workflow antes de efetuar merge depende da configuração das regras de proteção do branch no GitHub.

## 8. Reflexão final

A integração do Qodana com o GitHub Actions permitiu automatizar a deteção do code smell Long Method.

Os testes demonstraram o funcionamento do Quality Gate: uma nova violação provoca a falha da análise e a sua correção permite recuperar o estado de sucesso.

A utilização de uma baseline tornou possível aplicar esta verificação de forma incremental, sem exigir a refatoração imediata de todos os métodos antigos.

O Quality Gate constitui, assim, um mecanismo de apoio à manutenção da qualidade do código, embora não substitua a revisão humana nem a análise de outras métricas.


Não foi possível configurar a proteção do branch main, porque a minha conta não possui permissões de administração do repositório. O GitHub apresenta a mensagem "You don't have access to repository options".