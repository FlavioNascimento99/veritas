# 📚 Documentação - Veritas

Guias completos para desenvolvimento, deployment e arquitetura da aplicação.

---

## 📖 Documentos

### 🧪 [PROFILES.md](./PROFILES.md)
Configuração de profiles Spring Boot (dev vs prod)
- Como funcionam os profiles dev/prod
- Variáveis de ambiente
- Quando usar cada um

### 🐳 [DOCKER.md](./DOCKER.md)
Desenvolvimento local com Docker Compose
- Setup rápido
- Comandos principais
- Troubleshooting

### ☸️ [KUBERNETES.md](./KUBERNETES.md)
Deploy em Kubernetes (produção)
- Manifests explicados
- Como fazer deploy
- Scaling e monitoring

### 🎯 [ARCHITECTURE.md](./ARCHITECTURE.md)
Planejamento e roadmap do projeto
- Arquitetura completa
- Fluxo dev → prod
- Próximas etapas

---

## 🚀 Quick Start

### Desenvolvimento Local
```bash
cd ~/Documents/GitHub/veritas
cp .env.example .env
docker-compose up -d
# Acessa: http://localhost:8080
```

### Produção (Kubernetes)
```bash
kubectl create secret generic supabase-credentials \
  --from-literal=SUPABASE_DB_HOST=db.supabase.co \
  --from-literal=SUPABASE_DB_USER=postgres \
  --from-literal=SUPABASE_DB_PASSWORD='SUA_SENHA'

kubectl apply -k k8s/
```

---

## 📂 Estrutura do Projeto

```
veritas/
├─ docs/                     ← Você está aqui
│  ├─ README.md
│  ├─ PROFILES.md
│  ├─ DOCKER.md
│  ├─ KUBERNETES.md
│  └─ ARCHITECTURE.md
├─ k8s/                      ← Manifests Kubernetes
├─ src/                      ← Código Spring Boot
├─ docker-compose.yml        ← Dev local
├─ Makefile                  ← Helper commands
└─ .env.example              ← Template env vars
```

---

## 🎓 Roadmap

| Fase | Status | Docs |
|------|--------|------|
| Profiles (dev/prod) | ✅ Completo | [PROFILES.md](./PROFILES.md) |
| Docker Compose | ✅ Completo | [DOCKER.md](./DOCKER.md) |
| Kubernetes | ✅ Completo | [KUBERNETES.md](./KUBERNETES.md) |
| Arquitetura | ✅ Completo | [ARCHITECTURE.md](./ARCHITECTURE.md) |

---

## 💡 Onde Procurar?

**"Como fazer desenvolvimento local?"**
→ Ver [DOCKER.md](./DOCKER.md)

**"Como funciona dev vs prod?"**
→ Ver [PROFILES.md](./PROFILES.md)

**"Como fazer deploy em Kubernetes?"**
→ Ver [KUBERNETES.md](./KUBERNETES.md)

**"Qual é a arquitetura completa?"**
→ Ver [ARCHITECTURE.md](./ARCHITECTURE.md)
