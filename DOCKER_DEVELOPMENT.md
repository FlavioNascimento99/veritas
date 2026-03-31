# 🐳 Docker Development - Veritas

> Guia completo para trabalhar com Docker Compose em desenvolvimento local

---

## 📋 Pré-requisitos

- ✅ Docker instalado ([download](https://www.docker.com))
- ✅ Docker Compose instalado (vem com Docker Desktop)
- ✅ Git

**Verificar instalação:**
```bash
docker --version
docker-compose --version
```

---

## 🚀 Começar Rápido

### 1️⃣ Primeira Execução

```bash
# Clone ou navegue ao projeto
cd ~/Documents/GitHub/veritas

# Crie o arquivo .env com as variáveis de desenvolvimento
cp .env.example .env

# Inicie os containers
docker-compose up -d

# Verifique o status
docker-compose ps
```

### 2️⃣ Acessar a Aplicação

- **API Spring Boot:** http://localhost:8080
- **PostgreSQL:** `localhost:5432`
- **Usuário DB:** `dev_user`
- **Senha DB:** `dev_password`
- **Database:** `veritas_dev`

### 3️⃣ Ver Logs em Tempo Real

```bash
# Logs da aplicação Spring Boot
docker-compose logs -f veritas-app

# Logs do PostgreSQL
docker-compose logs -f postgres-local

# Todos os logs
docker-compose logs -f
```

---

## 📊 Arquitetura do docker-compose

```
┌─────────────────────────────────────────┐
│       docker-compose.yml                │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
   ┌───▼──────┐    ┌───▼─────────┐
   │ postgres │    │  veritas-app│
   │  -local  │    │   (Spring)  │
   │ (DB)     │    │             │
   └──────────┘    └─────────────┘
       │ 5432              │ 8080
       │ (container)       │ (container)
       └────────┬──────────┘
                │ Network
         veritas-network
```

---

## 🔄 Comandos Principais

### Iniciar/Parar Containers

```bash
# Iniciar em background
docker-compose up -d

# Iniciar com logs visíveis (foreground)
docker-compose up

# Parar containers (mantém dados)
docker-compose stop

# Parar e remover containers
docker-compose down

# Remover TUDO (containers + volumes + dados)
docker-compose down -v

# Reiniciar um serviço específico
docker-compose restart veritas-app
```

### Gerenciar Containers

```bash
# Ver status dos containers
docker-compose ps

# Ver logs em tempo real
docker-compose logs -f veritas-app

# Ver últimas 100 linhas de log
docker-compose logs --tail 100 veritas-app

# Executar comando dentro do container
docker-compose exec veritas-app bash
docker-compose exec postgres-local psql -U dev_user -d veritas_dev
```

### Reconstruir a Imagem

```bash
# Força reconstruir a imagem do Spring Boot (útil após mudanças no código)
docker-compose up -d --build

# Apenas reconstruir sem iniciar
docker-compose build --no-cache
```

---

## 🗄️ Acessar o Banco de Dados

### Via PostgreSQL CLI

```bash
# Terminal interativo do PostgreSQL
docker-compose exec postgres-local psql -U dev_user -d veritas_dev

# Comandos úteis dentro do psql:
# \dt              → listar tabelas
# \d table_name    → descrever tabela
# SELECT * FROM table_name;  → query
# \q               → sair
```

### Via Client Externo (DBeaver, PgAdmin, etc)

```
Host: localhost
Port: 5432
Database: veritas_dev
User: dev_user
Password: dev_password
```

---

## 🔧 Customizar Variáveis de Ambiente

### Usar arquivo `.env`

```bash
# 1. Crie .env baseado em .env.example
cp .env.example .env

# 2. Edite .env conforme necessário
# Exemplo:
# PGUSER=seu_usuario
# PGPASSWORD=sua_senha
# SPRING_PROFILES_ACTIVE=dev

# 3. Reinicie docker-compose
docker-compose down
docker-compose up -d
```

### Sobrescrever via Linha de Comando

```bash
# Apenas para esta execução
PGUSER=outro_user docker-compose up -d
```

---

## 🐛 Troubleshooting

### ❌ Erro: "Cannot connect to Docker daemon"

**Solução:**
```bash
# Verifique se Docker está rodando
docker ps

# Se não estiver, inicie Docker Desktop ou o daemon
```

### ❌ Erro: "Port 5432 already in use"

**Solução:**
```bash
# Mude a porta no docker-compose.yml
# Ou mude a variável de ambiente:
PGPORT=5433 docker-compose up -d

# Acesse em: localhost:5433
```

### ❌ Erro: "Falha ao conectar no banco"

**Verificar:**
```bash
# Verifique se o PostgreSQL está saudável
docker-compose ps

# Veja os logs do PostgreSQL
docker-compose logs postgres-local
```

### ❌ "Spring Boot não found database"

**Verificar:**
```bash
# Confirme que PGHOST=postgres-local (não localhost!)
cat .env | grep PGHOST

# Reinicie tudo
docker-compose down -v
docker-compose up -d
```

---

## 📈 Performance & Otimizações

### Aumentar Memória do Spring Boot

```bash
# No .env:
JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC

# Reinicie:
docker-compose restart veritas-app
```

### Limpar Dados Antigos

```bash
# Remove tudo (containers, volumes, dados)
docker-compose down -v

# Reinicia limpo
docker-compose up -d
```

---

## 🔐 Segurança (Boas Práticas)

### ⚠️ NÃO faça em produção:

```
❌ NEVER commit .env
❌ NEVER use dev_password em produção
❌ NEVER exponha portas PostgreSQL publicamente
✅ Sempre use SPRING_PROFILES_ACTIVE=dev em dev
```

### ✅ Boas práticas:

```bash
# Use .env.example como template
cp .env.example .env
# Edite .env apenas localmente
git add .env.example    # commit
git ignore .env          # ignore
```

---

## 📚 Próximos Passos

- ✅ [x] Docker Compose (este arquivo)
- ⬜ [ ] Kubernetes Manifests (próximo passo)
- ⬜ [ ] CI/CD Pipeline (GitHub Actions)

---

## 📖 Referência Rápida

| Comando | Descrição |
|---------|-----------|
| `docker-compose up -d` | Inicia em background |
| `docker-compose down` | Para e remove containers |
| `docker-compose logs -f` | Logs em tempo real |
| `docker-compose exec veritas-app bash` | Terminal no container |
| `docker-compose ps` | Ver status |

---

## 🆘 Suporte

Se encontrar problemas, consulte:
- Docker Docs: https://docs.docker.com
- Docker Compose: https://docs.docker.com/compose
- Spring Boot Docker: https://spring.io/guides/gs/spring-boot-docker/
