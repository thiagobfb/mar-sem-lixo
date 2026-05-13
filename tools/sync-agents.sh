#!/bin/bash

# sync-agents.sh
# Versão Linux do sync-agents.ps1

set -e  # Exit on error

# Parâmetros
MODE="thin"
CLEAN=false

# Parse argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        --mode)
            MODE="$2"
            shift 2
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        -h|--help)
            echo "Uso: $0 [--mode thin|full] [--clean]"
            echo ""
            echo "Opções:"
            echo "  --mode thin|full    Define o modo (padrão: thin)"
            echo "  --clean             Limpa diretórios antes de gerar"
            echo "  -h, --help          Mostra esta ajuda"
            exit 0
            ;;
        *)
            echo "Argumento desconhecido: $1"
            echo "Use $0 --help para ajuda"
            exit 1
            ;;
    esac
done

# Validar MODE
if [[ "$MODE" != "thin" && "$MODE" != "full" ]]; then
    echo "Erro: --mode deve ser 'thin' ou 'full'"
    exit 1
fi

# Diretórios
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$SCRIPT_DIR")"

SRC_AGENTS="$ROOT/.claude/agents"
CODEX_SKILLS="$ROOT/.codex/skills"
GEMINI_CMDS="$ROOT/.gemini/commands"
COPILOT_AGTS="$ROOT/.github/agents"
COPILOT_INSTR_DIR="$ROOT/.github"
COPILOT_INSTR_FILE="$ROOT/.github/copilot-instructions.md"

# Função para limpar/criar diretório
reset_dir() {
    local dir=$1
    if [ -d "$dir" ]; then
        rm -rf "$dir"
    fi
    mkdir -p "$dir"
}

# Criar ou limpar diretórios
if [ "$CLEAN" = true ]; then
    reset_dir "$CODEX_SKILLS"
    reset_dir "$GEMINI_CMDS"
    reset_dir "$COPILOT_AGTS"
    mkdir -p "$COPILOT_INSTR_DIR"
else
    mkdir -p "$CODEX_SKILLS" "$GEMINI_CMDS" "$COPILOT_AGTS" "$COPILOT_INSTR_DIR"
fi

# Copilot instructions
cat > "$COPILOT_INSTR_FILE" <<'EOF'
# Copilot Instructions
Use o arquivo AGENTS.md (raiz do repositório) como regras globais do projeto.
Siga comandos Maven, estrutura de módulos e padrões de PR descritos lá.
Priorize mudanças mínimas e verificáveis, com checklist de testes.
EOF

# Função para gerar thin profile
new_thin_profile() {
    local agent_name=$1
    local agent_lower=$(echo "$agent_name" | tr '[:upper:]' '[:lower:]')

    case "$agent_lower" in
        debugger)
            cat <<'EOF'
Use AGENTS.md como regras globais do repositório.

Papel: Debugger Java Sênior (JSF/PrimeFaces + Hibernate).
Objetivo: eliminar causa raiz (RCA) com correção mínima e verificável.

Formato de saída (curto):
1) Causa provável
2) Onde no código (arquivos/métodos)
3) Correção mínima (passos)
4) Checklist de testes
5) Riscos/efeitos colaterais
EOF
            ;;
        refactoring-expert)
            cat <<'EOF'
Use AGENTS.md como regras globais do repositório.

Papel: Especialista em refatoração Java.
Objetivo: melhorar legibilidade/manutenibilidade sem alterar comportamento.

Formato de saída:
- Problemas encontrados (curto)
- Refatoração proposta (passos)
- Impacto e risco
- Testes recomendados
EOF
            ;;
        performance-optimizer)
            cat <<'EOF'
Use AGENTS.md como regras globais do repositório.

Papel: Otimização de performance (Java/SQL/JSF).
Objetivo: reduzir tempo/uso de recursos com mudança mínima.

Formato de saída:
- Gargalo provável
- Onde medir
- Correção proposta
- Como validar (métricas/testes)
EOF
            ;;
        security-auditor)
            cat <<'EOF'
Use AGENTS.md como regras globais do repositório.

Papel: Auditoria de segurança.
Objetivo: identificar riscos e sugerir mitigação prática.

Formato de saída:
- Achados (alta/média/baixa)
- Impacto
- Correção recomendada
- Como verificar/testar
EOF
            ;;
        test-automator)
            cat <<'EOF'
Use AGENTS.md como regras globais do repositório.

Papel: Automação de testes (JUnit/Mockito).
Objetivo: criar testes focados e rápidos.

Formato de saída:
- O que testar
- Tipo (unit/integration)
- Estrutura sugerida
- Exemplos curtos
EOF
            ;;
        *)
            cat <<EOF
Use AGENTS.md como regras globais do repositório.

Papel: $agent_name.
Objetivo: executar tarefas do papel com respostas curtas e acionáveis.

Formato de saída:
- Resumo
- Passos objetivos
- Testes/validação
EOF
            ;;
    esac
}

# Verificar se diretório de agentes existe
if [ ! -d "$SRC_AGENTS" ]; then
    echo "Erro: Diretório $SRC_AGENTS não encontrado"
    exit 1
fi

# Processar cada arquivo .md em .claude/agents
for agent_file in "$SRC_AGENTS"/*.md; do
    # Verificar se existem arquivos
    if [ ! -e "$agent_file" ]; then
        echo "Aviso: Nenhum arquivo .md encontrado em $SRC_AGENTS"
        break
    fi

    # Nome do agente (sem extensão)
    agent_name=$(basename "$agent_file" .md)

    # Ler conteúdo completo
    full_content=$(cat "$agent_file")

    # Escolher conteúdo baseado no modo
    if [ "$MODE" = "full" ]; then
        content="$full_content"
    else
        content=$(new_thin_profile "$agent_name")
    fi

    # 1) Codex Skill
    skill_dir="$CODEX_SKILLS/$agent_name"
    mkdir -p "$skill_dir"

    cat > "$skill_dir/SKILL.md" <<EOF
---
name: $agent_name
description: $([ "$MODE" = "full" ] && echo "FULL" || echo "THIN") - importado de .claude/agents/$(basename "$agent_file")
---

$content
EOF

    # 2) Gemini command (TOML)
    cat > "$GEMINI_CMDS/$agent_name.toml" <<EOF
description="$agent_name ($([ "$MODE" = "full" ] && echo "FULL" || echo "THIN"))"
prompt="""
$content
"""
EOF

    # 3) Copilot agent profile
    cat > "$COPILOT_AGTS/$agent_name.agent.md" <<EOF
# $agent_name ($([ "$MODE" = "full" ] && echo "FULL" || echo "THIN"))
Fonte: .claude/agents/$(basename "$agent_file")

$content
EOF

done

echo "OK: sync concluído | Mode=$MODE | Clean=$CLEAN | Bash=$(bash --version | head -n1)"
echo "Gerados: .codex/skills | .gemini/commands | .github/agents | .github/copilot-instructions.md"