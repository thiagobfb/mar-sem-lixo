param(
    [ValidateSet("thin","full")]
    [string]$Mode = "thin",

    [switch]$Clean
)

# Detecta versão do PowerShell e configura encoding apropriado
$isPwsh7 = $PSVersionTable.PSVersion.Major -ge 6

if ($isPwsh7) {
    # PowerShell 7+
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    [Console]::InputEncoding = [System.Text.Encoding]::UTF8
    $PSDefaultParameterValues['*:Encoding'] = 'utf8NoBOM'
    $encoding = 'utf8NoBOM'
} else {
    # PowerShell 5.1
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding = [System.Text.Encoding]::UTF8
    $encoding = 'UTF8'
}

$root = Split-Path -Parent $PSScriptRoot

$srcAgents = Join-Path $root ".claude\agents"

$codexSkills = Join-Path $root ".codex\skills"
$geminiCmds  = Join-Path $root ".gemini\commands"
$copilotAgts = Join-Path $root ".github\agents"
$copilotInstrDir  = Join-Path $root ".github"
$copilotInstrFile = Join-Path $root ".github\copilot-instructions.md"

function Reset-Dir([string]$path) {
    if (Test-Path $path) {
        Remove-Item -Recurse -Force $path
    }
    New-Item -ItemType Directory -Force -Path $path | Out-Null
}

if ($Clean) {
    Reset-Dir $codexSkills
    Reset-Dir $geminiCmds
    Reset-Dir $copilotAgts
    New-Item -ItemType Directory -Force -Path $copilotInstrDir | Out-Null
} else {
    New-Item -ItemType Directory -Force -Path $codexSkills, $geminiCmds, $copilotAgts, $copilotInstrDir | Out-Null
}

# Copilot instructions
$copilotInstr = @"
# Copilot Instructions
Use o arquivo AGENTS.md (raiz do repositório) como regras globais do projeto.
Siga comandos Maven, estrutura de módulos e padrões de PR descritos lá.
Priorize mudanças mínimas e verificáveis, com checklist de testes.
"@
Set-Content -Path $copilotInstrFile -Value $copilotInstr -Encoding $encoding

function New-ThinProfile([string]$agentName) {
    switch ($agentName.ToLower()) {
        "debugger" {
            return @"
Use AGENTS.md como regras globais do repositório.

Papel: Debugger Java Sênior (JSF/PrimeFaces + Hibernate).
Objetivo: eliminar causa raiz (RCA) com correção mínima e verificável.

Formato de saída (curto):
1) Causa provável
2) Onde no código (arquivos/métodos)
3) Correção mínima (passos)
4) Checklist de testes
5) Riscos/efeitos colaterais
"@
        }
        "refactoring-expert" {
            return @"
Use AGENTS.md como regras globais do repositório.

Papel: Especialista em refatoração Java.
Objetivo: melhorar legibilidade/manutenibilidade sem alterar comportamento.

Formato de saída:
- Problemas encontrados (curto)
- Refatoração proposta (passos)
- Impacto e risco
- Testes recomendados
"@
        }
        "performance-optimizer" {
            return @"
Use AGENTS.md como regras globais do repositório.

Papel: Otimização de performance (Java/SQL/JSF).
Objetivo: reduzir tempo/uso de recursos com mudança mínima.

Formato de saída:
- Gargalo provável
- Onde medir
- Correção proposta
- Como validar (métricas/testes)
"@
        }
        "security-auditor" {
            return @"
Use AGENTS.md como regras globais do repositório.

Papel: Auditoria de segurança.
Objetivo: identificar riscos e sugerir mitigação prática.

Formato de saída:
- Achados (alta/média/baixa)
- Impacto
- Correção recomendada
- Como verificar/testar
"@
        }
        "test-automator" {
            return @"
Use AGENTS.md como regras globais do repositório.

Papel: Automação de testes (JUnit/Mockito).
Objetivo: criar testes focados e rápidos.

Formato de saída:
- O que testar
- Tipo (unit/integration)
- Estrutura sugerida
- Exemplos curtos
"@
        }
        default {
            return @"
Use AGENTS.md como regras globais do repositório.

Papel: $agentName.
Objetivo: executar tarefas do papel com respostas curtas e acionáveis.

Formato de saída:
- Resumo
- Passos objetivos
- Testes/validação
"@
        }
    }
}

# Sync agents
Get-ChildItem $srcAgents -Filter *.md | ForEach-Object {
    $name = $_.BaseName

    # Leitura com encoding apropriado
    $fullContent = Get-Content -Path $_.FullName -Raw -Encoding $encoding

    $content = if ($Mode -eq "full") { $fullContent } else { New-ThinProfile -agentName $name }

    # 1) Codex Skill
    $skillDir = Join-Path $codexSkills $name
    New-Item -ItemType Directory -Force -Path $skillDir | Out-Null

    $skillHeader = @"
---
name: $name
description: $(if ($Mode -eq "full") { "FULL" } else { "THIN" }) - importado de .claude/agents/$($_.Name)
---
"@

    Set-Content -Path (Join-Path $skillDir "SKILL.md") -Value ($skillHeader + "`n" + $content) -Encoding $encoding

    # 2) Gemini command (TOML)
    $toml = @"
description="$name ($(if ($Mode -eq "full") { "FULL" } else { "THIN" }))"
prompt="""
    $content
"""
"@
    Set-Content -Path (Join-Path $geminiCmds "$name.toml") -Value $toml -Encoding $encoding

    # 3) Copilot agent profile
    $agentMd = @"
# $name ($(if ($Mode -eq "full") { "FULL" } else { "THIN" }))
Fonte: .claude/agents/$($_.Name)

    $content
"@
    Set-Content -Path (Join-Path $copilotAgts "$name.agent.md") -Value $agentMd -Encoding $encoding
}

Write-Host "OK: sync concluído | Mode=$Mode | Clean=$($Clean.IsPresent) | PowerShell=$($PSVersionTable.PSVersion)"
Write-Host "Gerados: .codex/skills | .gemini/commands | .github/agents | .github/copilot-instructions.md"