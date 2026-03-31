# 🐳 DOCKER - Desenvolvimento Local

Guia para usar Docker Compose no desenvolvimento local.

---

## 🚀 Quick Start

```bash
# 1. Entrar na pasta do projeto
cd ~/Documents/GitHub/veritas

# 2. Copiar .env
cp .env.example .env

# 3. Iniciar
docker-compose up -d

# 4. Acessar
http://localhost:8080

# 5. Ver logs
docker-compose logs -f veritas-app
```

---

## 📊 O que roda?

```
┌─────────────────────────────────┐
│   docker-compose.yml            │
├─────────────────────────────────┤
│                                 │
│  postgres-local (5432)          │
│  ├─ Database: veritas_dev       │
│  ├─ User: dev_user              │
│  └─ Password: dev_password      │
│                                 │
│  veritas-app (8080)             │
│  ├─ Spring Boot                 │
│  ├─ Profile: dev                │
│  └─ Conecta em: postgres-local  │
│                                 │
└─────────────────────────────────┘
```

---

## 🔄 Comandos Principais

### Iniciar/Parar

```bash
docker-compose up -d          # Iniciar background
docker-compose up             # Iniciar com logs
docker-compose down           # Parar
docker-compose down -v        # Parar + remover volumes (CLEAN)
docker-compose restart        # Reiniciar
```

### Logs

```bash
docker-compose logs -f veritas-app       # Spring Boot
docker-compose logs -f postgres-local    # PostgreSQL
docker-compose logs -f                   # Tudo
```

### Status

```bash
docker-compose ps             # Ver containers
docker-compose exec veritas-app bash          # Terminal no app
docker-compose exec postgres-local psql -U dev_user -d veritas_dev  # Terminal no DB
```

### Rebuild

```bash
docker-compose build --no-cache           # Reconstruir imagem
docker-compose up -d --build               # Rebuild + start
```

---

## 🗄️ Acessar o PostgreSQL

### Via CLI

```bash
docker-compose exec postgres-local psql -U dev_user -d veritas_dev

# Dentro do psql:
\dt                          # Listar tabelas
\d table_name                # Descrever tabela
SELECT * FROM table_name;    # Query
\q                           # Sair
```

### Via Client (DBeaver, PgAdmin)

```
Host: localhost
Port: 5432
Database: veritas_dev
User: dev_user
Password: dev_password
```

---

## 🧹 Limpeza

```bash
# Remover tudo (containers + volumes), reset completo
docker-compose down -v

# Ver volumes
docker volume ls

# Deletar volume específico
docker volume rm veritas_postgres_dev_data
```

---

## 🆘 Troubleshooting

**"Port 5432 already in use"**
```bash
# Mudar porta no docker-compose.yml
# Ou: PGPORT=5433 docker-compose up -d
```

**"Cannot connect to database"**
```bash
# Verificar se PostgreSQL está healthy
docker-compose ps
docker-compose logs postgres-local
```

**"Spring Boot fails to start"**
```bash
# Ver logs
docker-compose logs veritas-app

# Variável SPRING_PROFILES_ACTIVE=dev? Verificar .env
cat .env | grep SPRING_PROFILES_ACTIVE
```

---

## 💡 Tips

### Aumentar Memória
```bash
# No .env:
JAVA_OPTS=-Xmx1g -Xms512m
```

### Usar Makefile (helper)
```bash
make up              # Inicia
make logs           # Logs
make db             # Acessa PostgreSQL
make clean-all      # Remove tudo
make help           # Ver todos
```
