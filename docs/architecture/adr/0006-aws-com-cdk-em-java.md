# ADR 0006: Cloud AWS com infraestrutura como código em CDK Java

## Status

Aceito (2026-05)

## Contexto

A aplicação precisa de hospedagem para backend Spring Boot containerizado,
banco PostgreSQL com PostGIS, distribuição estática do frontend PWA e
storage de fotos. Contexto que influencia a decisão:

- **Conta AWS pré-existente** do desenvolvedor, sem necessidade de criar
  conta nova
- **Objetivo de portfólio**: demonstrar competência em AWS é
  significativamente mais valioso em entrevistas que demonstrar
  competência em PaaS simplificadas (Fly.io, Render). AWS aparece em
  ~80% das vagas de senior backend
- **Restrição de custo**: uma ONG não sustenta operação cara
  indefinidamente. Necessário ficar em torno de US$ 0–30/mês
- **Demonstrar IaC**: infraestrutura como código versionada é diferencial
  forte de portfólio frente a setups manuais via console
- **Coerência de stack**: o projeto já usa Java intensamente; manter
  IaC em Java reforça a narrativa em vez de fragmentar para Terraform
  (Hashicorp)

## Decisão

Usar **AWS** como provedor de cloud, com **AWS CDK em Java** como
ferramenta de Infrastructure as Code, e a seguinte topologia:

- **Backend**: ECS Fargate (container serverless gerenciado)
- **Banco**: RDS PostgreSQL t4g.micro com extensão PostGIS, Multi-AZ
  desabilitado (custo)
- **Frontend**: S3 + CloudFront (distribuição estática com CDN)
- **Storage de fotos**: S3 com lifecycle policies (Standard → Infrequent
  Access após 90 dias)
- **DNS**: Route 53 (zona hospedada para custom domain)
- **Secrets**: AWS Secrets Manager para credenciais do banco
- **Logs**: CloudWatch Logs com retenção de 14 dias
- **Network**: VPC própria com subnets públicas e privadas, sem NAT
  Gateway (custo), Security Groups bem configurados
- **CI/CD**: GitHub Actions deployando para AWS via OIDC (sem
  credenciais long-lived)

Estrutura de stacks no CDK:

- `NetworkStack`: VPC, subnets, security groups
- `DatabaseStack`: RDS, secrets do banco
- `BackendStack`: ECR, ECS Cluster, Task Definition, Service, ALB
- `FrontendStack`: S3 bucket, CloudFront distribution, ACM certificate

## Alternativas consideradas

- **Fly.io / Render / Railway.** PaaS simplificadas, deploy de container
  com poucos cliques, free tier generoso. Rejeitadas porque não agregam
  o mesmo peso de portfólio que AWS, e o objetivo do projeto inclui
  demonstrar competência em cloud enterprise.

- **Terraform.** Padrão de mercado para IaC, multi-cloud. Rejeitada em
  favor de CDK em Java porque mantém a stack do projeto coesa em uma
  linguagem só, e CDK em Java é menos comum (logo mais diferenciador)
  que Terraform.

- **AWS Console via cliques.** Rejeitada por não ser portfolio-worthy,
  ser propenso a erro humano, dificultar reprodução em outra conta e
  por inviabilizar revisão de mudanças via PR.

- **AWS Lambda para backend.** Rejeitada por incompatibilidade com
  Spring Boot tradicional (cold start na JVM), e por não corresponder
  ao padrão de carreira Java do desenvolvedor.

- **AWS App Runner.** PaaS-like da AWS, mais simples que ECS. Rejeitada
  porque ECS Fargate é mais comum em vagas e demonstra mais skills
  (Task Definitions, Services, ALB, auto-scaling) que App Runner
  abstrai.

- **AWS Elastic Beanstalk.** Rejeitada por ter percepção de "AWS
  legacy" em 2026. ECS Fargate é o padrão moderno.

- **EC2 puro com Docker.** Rejeitada por não usar capacidades modernas
  da AWS e por adicionar trabalho manual desnecessário (patching,
  scaling).

## Consequências

**Positivas:**
- Habilidades AWS demonstradas concretamente: ECS, Fargate, RDS, S3,
  CloudFront, IAM, Secrets Manager, CDK, CloudWatch
- IaC versionado em Git permite revisão, reprodução e rollback
- CDK em Java mantém coesão de stack e amplia a base Java do portfólio
- ADRs específicos de AWS (ECS Fargate, CDK Java, etc.) viram artefatos
  de portfólio
- Migração para outras contas AWS é trivial (mesma linguagem)

**Negativas:**
- Custo recorrente após free tier: US$ 15-30/mês (RDS é o maior componente)
- Curva de aprendizado de AWS é real (especialmente VPC, IAM, ECS)
- Risco de custo descontrolado se algo for mal configurado (mitigado
  por billing alerts obrigatórios desde dia 1)
- CDK em Java tem comunidade menor que CDK em TypeScript, mais comum

**Neutras:**
- Necessário configurar billing alerts (US$ 5, US$ 15, US$ 30) e
  AWS Budgets antes de qualquer recurso ser provisionado
- Necessário tagar todos os recursos consistentemente
  (`Project=mar-sem-lixo`, `ManagedBy=cdk`, etc.) para FinOps
