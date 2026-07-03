## Documentação Base do Projeto Collegialis - Veritas

### 1. Introdução e Objetivo

O **Veritas** (nome de código original do projeto: **Collegialis**) é um sistema para **controlar o fluxo de julgamento de processos para um colegiado de professores**. Um colegiado é um grupo de professores que se reúne para julgar processos criados por alunos sobre assuntos acadêmicos.

O sistema deve gerenciar diversos perfis de usuários, cada um com acesso a funcionalidades específicas.

### 2. Fluxo Principal do Processo de Julgamento

O julgamento de um processo acadêmico segue o seguinte fluxo no sistema:

1.  **Criação:** Um aluno abre um processo fazendo uma solicitação acadêmica (por exemplo, reabertura de matrícula ou dilatação de prazo).
2.  **Distribuição:** O coordenador do curso recebe o processo e o encaminha para um professor, que será designado como **professor relator**, para julgar se o processo é procedente ou não.
3.  **Planejamento:** O coordenador marca reuniões do colegiado e define a **pauta** (processos que serão julgados).
4.  **Votação e Resultado:**
    *   Durante a sessão, o relator apresenta seu voto (de acordo ou não com o pleito do aluno).
    *   Os demais membros do colegiado votam se concordam ou não com o relator.
    *   A **maioria dos votos define o resultado**.
    *   **Regra de Julgamento:** Se o relator votou pelo deferimento/indeferimento e a maioria votou com ele, o processo é considerado deferido/indeferido. **Se a maioria votou divergente do relator, o resultado final do processo será o contrário ao do relator**.

> **Nota de implementação:** na prática, o voto de um membro do colegiado usa o mesmo domínio do relator (deferimento/indeferimento do processo), e "concordar com o relator" significa votar no mesmo sentido que ele. A apuração (`VoteService.calculateResult`) compara cada voto de membro com a decisão do relator para determinar maioria/divergência, coberta por testes em `VoteServiceTest`.

### 3. Requisitos Funcionais (REQFUNC)

Os requisitos funcionais são divididos por perfil de usuário, definindo as ações que o sistema deve permitir:

#### Aluno
*   **REQFUNC 1:** Cadastrar novo processo, informando o assunto e um texto com seu requerimento.
*   **REQFUNC 2:** Consultar o estado de todos os seus processos, podendo filtrá-los pelo status ou assunto e ordená-los pela data de criação. O aluno também pode realizar **upload de documentos** para fundamentar seu pedido.
*   Ter acesso aos andamentos dos processos (se já foi distribuído para um relator, se está na pauta, etc.).

#### Professor (Relator ou Membro)
*   **REQFUNC 3:** Consultar todos os processos que lhe foram designados pelo coordenador.
*   **REQFUNC 4:** Consultar todas as reuniões do colegiado, podendo filtrar pelo status (finalizada ou agendada).
*   **REQFUNC 5:** Votar pelo deferimento ou indeferimento de um processo, podendo **redigir um texto com sua justificativa de voto**.
*   **REQFUNC 6:** Consultar reuniões agendadas para o colegiado nas quais foi escalado para participar.

#### Coordenador
*   Distribuição de processos e acompanhamento do estado de todos os processos.
*   **REQFUNC 7:** Consultar todos os processos do colegiado, podendo filtrá-los por status, pelo aluno interessado ou pelo professor relator (caso já tenha sido distribuído).
*   **REQFUNC 8:** Distribuir um processo para um professor membro do colegiado, designando-o como relator do processo.
*   **REQFUNC 9:** Criar uma sessão do colegiado e definir sua data, sua pauta (processos que serão julgados) e os membros que participarão dela.
*   **REQFUNC 10:** Iniciar uma sessão de julgamento. **Apenas uma sessão pode estar iniciada por vez**.
*   **REQFUNC 11:** Apregoar (designar qual processo está em julgamento) cada processo da pauta, indicando o voto de cada membro. O sistema deve calcular automaticamente o resultado (deferido ou indeferido).
*   **REQFUNC 12:** Finalizar uma sessão, impedindo que qualquer informação acerca dos julgamentos seja alterada.
*   Presidir a reunião (consultar a pauta e ir julgando cada processo).

> **Nota de implementação:** o Coordenador não é uma entidade própria no modelo de dados. Ele é representado por um `Professor` com a flag `coordinator = true`, vinculado a um `Course` via `coordinatorAt`. Essa escolha evita duplicar cadastro/autenticação entre "professor" e "coordenador", já que na prática um coordenador é sempre também um professor do colegiado.

#### Administrador
*   **REQFUNC 13:** Realizar operações CRUD (Criação, Leitura, Atualização, Exclusão) para **colegiados** (conjunto de professores que o compõem).
*   **REQFUNC 14:** Realizar CRUDs para **alunos, professores e coordenadores**.
*   **REQFUNC 15:** Realizar CRUDs para **assuntos de processos**.

### 4. Requisitos Não Funcionais (REQNAOFUNC)

Estes requisitos definem as especificações técnicas obrigatórias para a implementação do sistema:

| ID | Requisito Não Funcional | Citação da Fonte |
| :--- | :--- | :--- |
| **REQNAOFUNC 1** | Utilizar o **Spring Boot 3.1.2** na implementação. | |
| **REQNAOFUNC 2** | Utilizar **framework de CSS** (ex: Bootstrap). | |
| **REQNAOFUNC 3** | Utilizar **qualquer banco de dados relacional**. *A escolha é o PostgreSQL (conforme conversação anterior).* | |
| **REQNAOFUNC 4** | Utilizar o **Hibernate** como *provider* de persistência. | |
| **REQNAOFUNC 5** | Utilizar **validação e campos de mensagens para mostrar erros** em todos os formulários. | |
| **REQNAOFUNC 6** | Utilizar o padrão **Post\_Redirect\_Get** (PRG). | |
| **REQNAOFUNC 7** | Utilizar **layouts e fragmentos** para os *templates* **Thymeleaf**. | |
| **REQNAOFUNC 8** | Utilizar **mecanismos de autenticação e autorização do Spring Security**. | |
| **REQNAOFUNC 9** | Utilizar **paginação em tabelas** com reflexo no banco de dados, limitando as consultas aos registros da página atual. | ⚠️ Parcial: a listagem de processos (`DashboardController`) usa `Pageable`/`Page<T>`. Os CRUDs de colegiados, alunos e professores (`AdminController`) ainda carregam a lista completa via `findAll()`. |
| **REQNAOFUNC 10** | Utilizar uma **anotação específica com regra própria para validar a matrícula**. | |

> **Nota de implementação:** o requisito original (REQNAOFUNC 1) pedia Spring Boot 3.1.2, versão vigente à época do enunciado. O projeto foi atualizado para **Spring Boot 3.4.3** (ver `pom.xml`), mantendo a mesma stack (Spring Data JPA, Spring Security, Thymeleaf, Hibernate).

### 5. Recomendações e Ferramentas

*   **Nomenclatura:** É recomendado utilizar um nome próprio para o sistema, como **"Saturno"** ou **"eColegiado"**, e evitar nomes genéricos como "projeto" ou "projetospring".
*   **Ferramentas:** O uso de **Maven** e **Git** é recomendado.
*   **Entrega:** O código deve mostrar funcionalidades plenas (com telas e registro no banco).
*   **Colaboração:** O repositório Git deve conter *commits* de **TODOS os membros da equipe** como registro da participação na implementação.
*   **Arquitetura:** Os diagramas de Casos de Usos, de Classe e de Estados são sugeridos como ponto de partida e devem ser melhorados e refinados. Telas prototipadas (planejamento de sessão e condução de julgamento) foram fornecidas como referência.

### 6. Estrutura do Projeto
```
veritas/
├── src/
│   ├── main/
│   │   ├── java/br/edu/ifpb/veritas/
│   │   │   ├── configs/
│   │   │   ├── controllers/
│   │   │   │   ├── api/
│   │   │   │   └── web/
│   │   │   ├── dtos/
│   │   │   ├── enums/
│   │   │   ├── exceptions/
│   │   │   ├── models/
│   │   │   ├── repositories/
│   │   │   ├── services/
│   │   │   └── VeritasApplication.java
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── fragments/
│   │       │   ├── layout/
│   │       │   ├── pages/
│   │       │   ├── home.html
│   │       │   └── register.html
│   │       └── application.properties
│   └── test/
│       └── java/br/edu/ifpb/veritas/services/
└── pom.xml
```