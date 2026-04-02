# 🔧 Correções de Persistência - Entidade Meeting

## Problemas Identificados e Corrigidos

### 1. ❌ **CascadeType.ALL em Processos (CRÍTICO)**
**Problema:** 
```java
@OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<Process> processes;
```

Se um Process falhava ao ser salvo, a transação inteira era revertida, impedindo a persistência de Meeting.

**Solução:**
```java
@OneToMany(mappedBy = "meeting", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
private List<Process> processes;
```

Agora apenas PERSIST e MERGE são aplicadas - processos não são deletados ao deletar Meeting.

---

### 2. ❌ **FetchType.EAGER em Participants (PERFORMANCE)**
**Problema:**
```java
@ManyToMany(cascade = {...}, fetch = FetchType.EAGER)
private List<Professor> participants;
```

EAGER loading causava N+1 queries desnecessárias, carregando todos os participants mesmo quando não necessário.

**Solução:**
```java
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
private List<Professor> participants;
```

---

### 3. ❌ **Sem Validadores de Campo (DATA INTEGRITY)**
**Problema:** Nenhuma validação nos campos obrigatórios:
- `collegiate` (NULL permitido)
- `status` (NULL permitido)
- `description` (vazio permitido)

**Solução:** Adicionada classe com validadores JPA:
```java
@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "collegiate_id")
@NotNull(message = "Reunião deve estar associada a um colegiado")
private Collegiate collegiate;

@NotBlank(message = "Descrição da reunião não pode ser vazia")
@Column(name = "description")
private String description;

@NotNull(message = "Status da reunião não pode ser nulo")
@Enumerated(EnumType.STRING)
@Column(name = "status")
private MeetingStatus status;
```

---

### 4. ❌ **Método @PrePersist para Debug (DIAGNOSTICS)**
**Adicionado:** Validação e logging pré-persistência para identificar problemas:

```java
@PrePersist
public void prePersist() {
  log.debug("=== PRE-PERSIST MEETING ===");
  log.debug("ID: {}, Collegiate: {}, Status: {}, Description: {}", ...);
  
  if (this.collegiate == null) {
    throw new IllegalArgumentException("Collegiate não pode ser nulo ao persistir Meeting");
  }
  if (this.status == null) {
    throw new IllegalArgumentException("Status não pode ser nulo ao persistir Meeting");
  }
  if (this.description == null || this.description.trim().isEmpty()) {
    throw new IllegalArgumentException("Description não pode ser vazia ao persistir Meeting");
  }
}
```

---

### 5. ❌ **RestExceptionHandler Genérico (ERROR VISIBILITY)**
**Problema:** Todos os erros retornavam como 500 genérico, ocultando o problema real.

**Solução:** Adicionados handlers específicos para:
- `DataIntegrityViolationException` → 409 CONFLICT
- `ConstraintViolationException` → 400 BAD_REQUEST
- `PersistenceException` → 500 com mensagem detalhada
- `IllegalArgumentException` → 400 BAD_REQUEST
- `IllegalStateException` → 400 BAD_REQUEST

---

## 📋 Requisitos para Criar um Meeting

Agora, para **persistir um Meeting com sucesso**, você DEVE fornecer:

```json
{
  "description": "Reunião do Colegiado XYZ - 02/04/2026",  // ✅ OBRIGATÓRIO (não vazio)
  "collegiate": {
    "id": 1  // ✅ OBRIGATÓRIO (ID de colegiado existente)
  },
  "status": "DISPONIVEL",  // ✅ OBRIGATÓRIO (DISPONIVEL | EM_ANDAMENTO | FINALIZADA)
  "participants": [
    { "id": 1 },
    { "id": 2 },
    { "id": 3 }
    // ✅ OPCIONAL (IDs de professores existentes, preferenciosamente número ÍMPAR)
  ],
  "processes": [],  // ✅ OPCIONAL (pode estar vazio)
  "scheduledDate": "2026-04-05T14:00:00",  // ✅ OPCIONAL
  "openedAt": null,  // ✅ OPCIONAL
  "active": false  // ✅ OPCIONAL (padrão false)
}
```

---

## 🧪 Como Testar

### Via cURL (Local):
```bash
curl -X POST http://localhost:8080/api/meetings \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Reunião Teste - 02/04/2026",
    "collegiate": { "id": 1 },
    "status": "DISPONIVEL",
    "participants": [
      { "id": 1 },
      { "id": 2 },
      { "id": 3 }
    ],
    "processes": [],
    "active": false
  }'
```

### Via Postman:
1. POST → `{{HOST}}/api/meetings`
2. Body (raw JSON):
```json
{
  "description": "Nova Reunião - Teste",
  "collegiate": { "id": 1 },
  "status": "DISPONIVEL",
  "participants": [{ "id": 1 }, { "id": 2 }, { "id": 3 }],
  "processes": [],
  "active": false
}
```

---

## 🚀 Esperado vs Resultado Anterior

### ❌ ANTES (Erro)
```
Status: 500 Internal Server Error
{
  "error": "null"  // Erro silencioso, sem saber o que falhou
}
```

### ✅ DEPOIS (Sucesso + Erro Claro)
```
Status: 201 Created (sucesso)
{
  "id": 5,
  "description": "Nova Reunião - Teste",
  "collegiate": { "id": 1, "description": "Colegiado Admin" },
  "status": "DISPONIVEL",
  "participants": [...],
  "processes": [],
  "scheduledDate": null,
  "createdAt": "2026-04-02T13:25:00",
  "active": false
}

OU se houver erro:

Status: 400 Bad Request
{
  "error": "Reunião deve estar associada a um colegiado",  // ✅ Mensagem clara
  "timestamp": "2026-04-02T13:25:00",
  "status": 400
}
```

---

## 🔍 Se Ainda Persistir Erro

Se você ainda encontrar problemas de persistência, **ative os logs DEBUG** em `application.properties`:

```properties
# Já presente em application.properties, mas verifique:
logging.level.org.hibernate.SQL=DEBUG
logging.level.br.edu.ifpb.veritas=DEBUG
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Então rode a aplicação e procure por:
- `=== PRE-PERSIST MEETING ===` (nosso novo log)
- `INSERT INTO tb_meetings` (SQL real)
- Qualquer `IllegalArgumentException` com mensagem detalhada

---

## 📝 Arquivos Modificados

1. **Meeting.java** 
   - ✅ Cascade de Processes: `PERSIST, MERGE` (era `ALL`)
   - ✅ FetchType de Participants: `LAZY` (era `EAGER`)
   - ✅ Validadores: `@NotNull`, `@NotBlank` nos campos obrigatórios
   - ✅ Método `@PrePersist` com logging e validação
   - ✅ Import @Slf4j para logging

2. **RestExceptionHandler.java**
   - ✅ Handlers específicos para exceptions de persistência
   - ✅ Mensagens de erro claras e debug info

---

Agora teste e me reporte o resultado! 🎯
