## Roadmap de Evolução (5 tarefas priorizadas)

### Resumo

Análise baseada no código atual (Spring Boot 4.0.3, Thymeleaf, JPA) e baseline de testes (./mvnw test concluído com sucesso, incluindo integrações MySQL/Postgres).
Direção escolhida: equilibrado.
Objetivo: entregar valor funcional sem perder robustez operacional.

### Tarefas Priorizadas (impacto x esforço)

1. Camada REST v1 para Owners/Pets/Visits

  - Problema atual: app é majoritariamente server-side (Thymeleaf); integrações externas ficam limitadas.
  - Entrega: endpoints JSON versionados (/api/v1/owners, /api/v1/owners/{id}, /api/v1/owners/{id}/pets, /api/v1/owners/{id}/pets/{petId}/visits) com paginação e validação.
  - Mudanças de interface pública: novo contrato REST com DTOs e erros padronizados (application/problem+json).
  - Esforço: médio.
  - Impacto: alto (habilita mobile, integrações e evolução futura).

2. Hardening de segurança e exposição operacional

  - Problema atual: management.endpoints.web.exposure.include=* e endpoint de falha (/oups) acessível.
  - Entrega: Spring Security básico (auth para escrita e actuator sensível), exposure de actuator por perfil, desativação/guard de /oups fora de dev.
  - Mudanças de interface pública: endpoints protegidos por autenticação/roles; actuator restrito.
  - Esforço: médio.
  - Impacto: alto (redução de risco em produção).

3. Busca avançada de Owners e melhor UX de navegação

  - Problema atual: busca apenas por lastName e paginação fixa.
  - Entrega: filtros combináveis (lastName, city, telephone, opcional petType), persistência dos filtros na paginação e feedback melhor de vazio.
  - Mudanças de interface pública: novos query params em /owners (web e, se feito junto, REST).
  - Esforço: médio.
  - Impacto: médio-alto (valor direto para operação diária).

4. Refatoração de performance JPA (redução de EAGER e consultas)

  - Problema atual: FetchType.EAGER em relações de Owner/Pet pode escalar mal com mais dados.
  - Entrega: migrar para LAZY com @EntityGraph/queries específicas para telas críticas; revisar pontos com potencial N+1.
  - Mudanças de interface pública: nenhuma funcional; melhora de latência/uso de banco.
  - Esforço: médio.
  - Impacto: médio-alto (escalabilidade e custo).

5. Governança de qualidade: cobertura mínima e pipeline mais útil

  - Problema atual: existe relatório JaCoCo, mas sem gate mínimo; pipeline não falha por cobertura baixa.
  - Entrega: regras JaCoCo (ex.: line coverage mínima por módulo), separação clara de testes unitários vs integração, e job de CI com matriz enxuta e previsível.
  - Mudanças de interface pública: nenhuma; mudança no processo de entrega.
  - Esforço: baixo-médio.
  - Impacto: médio-alto (evita regressões silenciosas).
