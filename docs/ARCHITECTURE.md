# 🎯 ARCHITECTURE - Planejamento e Roadmap

Arquitetura completa do projeto Veritas (dev → prod).

---

## 📊 Visão Geral

```
┌────────────────────┐
│  Workspace Local   │
│  (VS Code)         │
└─────────┬──────────┘
          │
   ┌──────┴────────┐
   │               │
┌──▼────┐     ┌───▼──────┐
│  DEV   │     │   PROD   │
│ LOCAL  │     │  CLOUD   │
├────────┤     ├──────────┤
│ Docker │     │ Kubernetes
│ Compose│     │ + Supabase
└────────┘     └──────────┘
```

---

## 🧪 DESENVOLVIMENTO (Local Docker)

**Arquivo:** `docker-compose.yml`

**Stack:**
- Spring Boot (profile: dev)
- PostgreSQL 16 local
- Desenvolvimento rápido e iterativo

**Como usar:**
```bash
docker-compose up -d
# http://localhost:8080
```

**Dados:**
- PostgreSQL local em container
- Dados podem ser perdidos entre restarts (OK!)
- Perfeito para testes

---

## 🚀 PRODUÇÃO (Kubernetes)

**Pasta:** `k8s/`

**Stack:**
- Kubernetes cluster
- Spring Boot (profile: prod)
- Supabase PostgreSQL (gerenciado)
- Auto-scaling automático
- HTTPS automático

**Como usar:**
```bash
kubectl create secret generic supabase-credentials ...
kubectl apply -k k8s/
```

**Características:**
- 3 replicas (alta disponibilidade)
- Auto-scale até 10 pods
- Zero downtime deployments
- Health checks automáticas
- TLS/SSL automático

---

## 🔄 Fluxo Completo

```
┌─────────────────────────────────────────────┐
│ 1. DESENVOLVIMENTO (Local Docker)           │
│    ├─ PostgreSQL local                      │
│    ├─ Profile: dev                          │
│    └─ Dados podem ser perdidos              │
└─────────────────┬───────────────────────────┘
                  │ (git push + merge)
┌─────────────────▼───────────────────────────┐
│ 2. CI/CD GitHub Actions (automático)        │
│    ├─ Build Docker image                    │
│    ├─ Test (opcional)                       │
│    └─ Push para registry                    │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│ 3. PRODUÇÃO (Kubernetes)                    │
│    ├─ 3 replicas                            │
│    ├─ Supabase PostgreSQL                   │
│    ├─ Profile: prod                         │
│    ├─ Auto-scale 3-10 pods                  │
│    └─ HTTPS automático                      │
└─────────────────────────────────────────────┘
```

---

## 📁 Estrutura do Projeto

```
veritas/
├─ docs/                          ← Documentação (você está aqui)
│  ├─ README.md                   # Index
│  ├─ PROFILES.md                 # Dev vs Prod
│  ├─ DOCKER.md                   # Docker local
│  ├─ KUBERNETES.md               # Kubernetes
│  └─ ARCHITECTURE.md             # Este arquivo
│
├─ k8s/                           ← Kubernetes manifests
│  ├─ 01-namespace.yml
│  ├─ 02-secret.yml
│  ├─ 03-configmap.yml
│  ├─ 04-deployment.yml
│  ├─ 05-service.yml
│  ├─ 06-ingress.yml
│  ├─ 07-hpa.yml
│  ├─ 08-pdb.yml
│  ├─ 09-rbac.yml
│  ├─ kustomization.yml
│  └─ deploy.sh
│
├─ src/                           ← Código Spring Boot
│  ├─ main/
│  │  ├─ java/
│  │  └─ resources/
│  │     ├─ application.properties
│  │     ├─ application-dev.yml
│  │     └─ application-prod.yml
│  └─ test/
│
├─ docker-compose.yml             ← Dev local
├─ Dockerfile                      ← Build multi-stage
├─ Makefile                        ← Helper commands
├─ .env.example                    ← Template
├─ pom.xml                         ← Maven
└─ README.md                       ← Project root
```

---

## 🎓 Profiles Spring Boot

### Dev (`application-dev.yml`)
```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://postgres-local:5432/veritas_dev
  jpa:
    hibernate:
      ddl-auto: create-drop    # Recria a cada startup
  sql:
    init:
      mode: always             # Roda scripts
logging:
  level:
    root: INFO
    br.edu.ifpb.veritas: DEBUG  # Debug ativado
```

### Prod (`application-prod.yml`)
```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:postgresql://${SUPABASE_DB_HOST}:...
  jpa:
    hibernate:
      ddl-auto: validate       # NÃO altera schema!
  sql:
    init:
      mode: never              # Sem scripts automáticos
logging:
  level:
    root: WARN                 # Menos verbose
```

---

## 🚀 Deployment Strategy

### Rolling Update (Zero Downtime)

```
Pod 1: OLD → NEW (1 pod extra durante transição)
Pod 2: OLD (continua)
Pod 3: OLD (continua)
         ↓
Pod 1: NEW ✓
Pod 2: OLD → NEW
Pod 3: OLD (continua)
         ↓
Pod 1: NEW ✓
Pod 2: NEW ✓
Pod 3: OLD → NEW
         ↓
Pod 1: NEW ✓
Pod 2: NEW ✓
Pod 3: NEW ✓ (deployment completo)
```

---

## 📈 Auto-scaling (HPA)

```
CPU < 70%? → Scale down (min 3 replicas)
CPU > 70%? → Scale up (max 10 replicas)
Memory < 80%? → Scale down
Memory > 80%? → Scale up
```

---

## 🔒 Segurança

- ✅ Credentials em Secrets (não em ConfigMap)
- ✅ Network Policy (tráfego controlado)
- ✅ RBAC (Service Account com permissões mínimas)
- ✅ TLS/SSL automático (Let's Encrypt)
- ✅ Non-root user nos containers
- ✅ Health checks (liveness/readiness)
- ✅ Resource limits (CPU/Memória)

---

## 📊 Comparação Dev vs Prod

| Aspecto | Dev | Prod |
|---------|-----|------|
| **Banco** | PostgreSQL local | Supabase (cloud) |
| **Replicas** | 1 | 3 (+ auto-scale) |
| **Profile** | dev | prod |
| **Logs** | DEBUG | INFO |
| **DDL** | create-drop | validate |
| **Inicialização** | Automática | Manual |
| **Segurança** | Baixa | Alta |
| **Disponibilidade** | N/A | 99.9% (SLA) |

---

## ⏱️ Timeline

### Fase 1: Setup ✅
- Profiles Spring Boot
- Docker Compose
- Documentação

### Fase 2: Kubernetes ✅
- Manifests completos
- Auto-scaling
- High availability

### Fase 3: CI/CD ⬜
- GitHub Actions
- Build automático
- Deploy automático

### Fase 4: Monitoring ⬜
- Prometheus
- Grafana
- Alertas

### Fase 5: Backup ⬜
- Snapshots automáticos
- Disaster recovery
- Multi-region

---

## 🎯 Quick Reference

```
LOCAL DEV:
  docker-compose up -d
  http://localhost:8080
  DB: localhost:5432 (dev_user/dev_password)

PRODUCTION:
  kubectl create secret generic supabase-credentials ...
  kubectl apply -k k8s/
  http://veritas.example.com (HTTPS)
  DB: Supabase (gerenciado)
```

---

## 📚 Documentos Relacionados

- [PROFILES.md](./PROFILES.md) - Dev vs Prod
- [DOCKER.md](./DOCKER.md) - Docker local
- [KUBERNETES.md](./KUBERNETES.md) - Kubernetes

---

## 🎓 Conclusion

Você tem:
- ✅ Desenvolvimento local rápido com Docker
- ✅ Produção confiável com Kubernetes
- ✅ Documentação completa
- ✅ Scripts de automação

**Pronto para começar!** 🚀
