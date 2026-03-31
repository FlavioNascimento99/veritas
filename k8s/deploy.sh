#!/bin/bash

# ============================================================
# Kubernetes Deployment Script - Veritas
# ============================================================
# 
# Facilita o deploy da aplicação para Kubernetes
# 
# Uso:
#   ./k8s/deploy.sh              # Deploy com kustomize
#   ./k8s/deploy.sh verify        # Apenas visualizar
#   ./k8s/deploy.sh rollback      # Reverter para versão anterior
#   ./k8s/deploy.sh logs          # Ver logs da aplicação
#   ./k8s/deploy.sh clean         # Remover tudo
#
# ============================================================

set -o pipefail

# ========================================
# Cores para output
# ========================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ========================================
# Funções
# ========================================

print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ $1${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

check_prerequisites() {
    print_header "Verificando Pré-requisitos"
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        print_error "kubectl não está instalado"
        exit 1
    fi
    print_success "kubectl encontrado"
    
    # Check conexão com cluster
    if ! kubectl cluster-info &> /dev/null; then
        print_error "Sem conexão com cluster Kubernetes"
        exit 1
    fi
    print_success "Conectado ao cluster"
    
    # Check namespace
    if ! kubectl get namespace veritas &> /dev/null; then
        print_warning "Namespace 'veritas' não existe, será criado"
    else
        print_success "Namespace 'veritas' existe"
    fi
}

verify_deploy() {
    print_header "Verificando Deploy (dry-run)"
    kubectl apply -k k8s/ --dry-run=client -o yaml
}

deploy() {
    print_header "Iniciando Deploy"
    
    # Apply manifests
    print_info "Aplicando manifests com Kustomize..."
    if kubectl apply -k k8s/; then
        print_success "Manifests aplicados com sucesso"
    else
        print_error "Falha ao aplicar manifests"
        exit 1
    fi
    
    # Wait for deployment
    print_info "Aguardando deployment ficar pronto..."
    if kubectl rollout status deployment/veritas-app -n veritas --timeout=5m; then
        print_success "Deployment está pronto!"
    else
        print_warning "Timeout aguardando deployment"
        print_info "Verificando status..."
        kubectl describe deployment veritas-app -n veritas
    fi
    
    # Show info
    print_header "Informações do Deploy"
    echo ""
    echo "Namespace:"
    kubectl get pods -n veritas
    echo ""
    echo "Service:"
    kubectl get service -n veritas
    echo ""
    echo "Ingress:"
    kubectl get ingress -n veritas
    echo ""
    echo "HPA:"
    kubectl get hpa -n veritas
}

verify_secrets() {
    print_header "Verificando Secrets"
    
    if kubectl get secret supabase-credentials -n veritas &> /dev/null; then
        print_success "Secret 'supabase-credentials' existe"
    else
        print_error "Secret 'supabase-credentials' NÃO encontrado"
        print_info "Criar com:"
        cat << 'EOF'
kubectl create secret generic supabase-credentials \
  --from-literal=SUPABASE_DB_HOST=db.supabase.co \
  --from-literal=SUPABASE_DB_PORT=5432 \
  --from-literal=SUPABASE_DB_NAME=postgres \
  --from-literal=SUPABASE_DB_USER=postgres \
  --from-literal=SUPABASE_DB_PASSWORD='SUA_SENHA' \
  -n veritas
EOF
        return 1
    fi
}

show_logs() {
    print_header "Logs da Aplicação"
    kubectl logs -f deployment/veritas-app -n veritas --tail=50
}

show_status() {
    print_header "Status do Deployment"
    
    print_info "Pods:"
    kubectl get pods -n veritas -o wide
    
    echo ""
    print_info "Deployment:"
    kubectl get deployment veritas-app -n veritas -o wide
    
    echo ""
    print_info "Service:"
    kubectl get service -n veritas
    
    echo ""
    print_info "Ingress:"
    kubectl get ingress -n veritas
    
    echo ""
    print_info "HPA:"
    kubectl get hpa -n veritas
}

rollback() {
    print_header "Revertendo Deploy"
    
    if kubectl rollout undo deployment/veritas-app -n veritas; then
        print_success "Deployment revertido"
        
        print_info "Aguardando novo rollout..."
        kubectl rollout status deployment/veritas-app -n veritas --timeout=5m
    else
        print_error "Falha ao reverter"
        exit 1
    fi
}

restart_deployment() {
    print_header "Reiniciando Deployment"
    
    if kubectl rollout restart deployment/veritas-app -n veritas; then
        print_success "Deployment reiniciado"
        
        print_info "Aguardando novo rollout..."
        kubectl rollout status deployment/veritas-app -n veritas --timeout=5m
    else
        print_error "Falha ao reiniciar"
        exit 1
    fi
}

clean() {
    print_header "Removendo Deployment"
    
    print_warning "Isto irá DELETAR:"
    echo "  - Namespace veritas"
    echo "  - Todos os pods"
    echo "  - Todos os volumes"
    echo ""
    read -p "Deseja continuar? (sim/não): " response
    
    if [[ "$response" == "sim" ]]; then
        if kubectl delete namespace veritas; then
            print_success "Namespace deletado"
        else
            print_error "Falha ao deletar"
            exit 1
        fi
    else
        print_info "Cancelado"
    fi
}

# ========================================
# Main
# ========================================

COMMAND=${1:-deploy}

case $COMMAND in
    deploy)
        check_prerequisites
        verify_secrets || exit 1
        deploy
        show_status
        ;;
    verify)
        check_prerequisites
        verify_deploy
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    rollback)
        rollback
        show_status
        ;;
    restart)
        restart_deployment
        show_status
        ;;
    clean)
        clean
        ;;
    help)
        cat << 'EOF'
Kubernetes Deployment Script - Veritas

Uso:
  ./k8s/deploy.sh [comando]

Comandos:
  deploy      Deploy da aplicação (padrão)
  verify      Visualizar manifests (dry-run)
  status      Ver status do deployment
  logs        Ver logs em tempo real
  restart     Reiniciar deployment
  rollback    Reverter para versão anterior
  clean       Remover tudo (namespace)
  help        Mostrar esta ajuda

Exemplos:
  ./k8s/deploy.sh                # Deploy
  ./k8s/deploy.sh verify         # Verificar
  ./k8s/deploy.sh logs           # Ver logs
  ./k8s/deploy.sh clean          # Limpar
EOF
        ;;
    *)
        print_error "Comando desconhecido: $COMMAND"
        echo "Use './k8s/deploy.sh help' para ajuda"
        exit 1
        ;;
esac
