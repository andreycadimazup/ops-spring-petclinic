
## Card 1 - REST v1 para Owners/Pets/Visits

Contexto
A aplicação é focada em Thymeleaf; integrações externas são limitadas.

Objetivo
Disponibilizar API REST versionada para leitura e escrita controlada de Owners, Pets e Visits.

Escopo (In)
- Endpoints em /api/v1/owners, /api/v1/owners/{id}, /api/v1/owners/{id}/pets, /api/v1/owners/{id}/pets/{petId}/visits.
- DTOs de request/response.
- Paginação e validação de payload.
- Respostas de erro padronizadas com application/problem+json.

Escopo (Out)
- Versionamento v2.
- OAuth2/JWT completo.

Critérios de Aceite
- [ ] Endpoints REST funcionam para os fluxos principais com HTTP status corretos.
- [ ] Erros de validação retornam formato consistente (problem+json).
- [ ] Paginação funciona e é documentada nos responses.
- [ ] Testes de contrato e integração cobrindo casos de sucesso e falha.

Checklist Técnico
- [ ] Criar controllers REST separados dos controllers MVC.
- [ ] Criar DTOs e mapeamento (evitar expor entidades JPA diretamente).
- [ ] Implementar tratamento global de exceções.
- [ ] Criar testes com MockMvc/WebTestClient e integração.

Dependências
Sem bloqueios fortes; ideal iniciar antes do hardening para validar segurança depois.

Riscos / Atenções
Risco de quebrar comportamento web se reusar lógica sem separação clara.

Labels sugeridas
type:feature, area:api, priority:high, status:backlog

Status inicial no board
Backlog

———

## Card 2 - Hardening de segurança e exposição operacional

Contexto
management.endpoints.web.exposure.include=* e endpoint /oups acessível elevam risco em produção.

Objetivo
Reduzir superfície de ataque e definir padrão mínimo de segurança por ambiente.

Escopo (In)
- Configuração de segurança para endpoints de escrita e actuator sensível.
- Exposição de actuator por perfil (dev/test/prod).
- Restringir/desativar /oups fora de desenvolvimento.
- Ajustar documentação de operação.

Escopo (Out)
- SSO corporativo.
- RBAC complexo multi-tenant.

Critérios de Aceite
- [ ] Endpoints sensíveis exigem autenticação.
- [ ] Em perfil de produção, actuator expõe apenas endpoints necessários.
- [ ] /oups indisponível ou protegido fora de dev.
- [ ] Testes de segurança cobrindo acesso anônimo vs autenticado.

Checklist Técnico
- [ ] Adicionar configuração Spring Security.
- [ ] Revisar application*.properties por perfil.
- [ ] Implementar testes de autorização.
- [ ] Validar logs/observabilidade após mudança.

Dependências
Ideal após Card 1 para proteger também os endpoints novos da API.

Riscos / Atenções
Possível bloqueio acidental de rotas web existentes se regras forem muito amplas.

Labels sugeridas
type:hardening, area:security, priority:high, status:backlog

Status inicial no board
Backlog

———

## Card 3 - Busca avançada de Owners + UX de navegação

Contexto
Busca atual de owners é limitada a lastName com navegação simples.

Objetivo
Melhorar eficiência de busca e usabilidade para operação diária.

Escopo (In)
- Filtros combináveis: lastName, city, telephone, opcional petType.
- Persistência dos filtros entre páginas.
- Melhor feedback para resultado vazio.
- Ajustes de UX nos templates de busca/listagem.

Escopo (Out)
- Busca full-text.
- Exportação de relatórios.

Critérios de Aceite
- [ ] Usuário consegue combinar filtros sem perder estado ao paginar.
- [ ] Mensagens de “nenhum resultado” claras e consistentes.
- [ ] Testes cobrindo combinações principais de filtros.
- [ ] Regressão visual/funcional mínima nas telas existentes.

Checklist Técnico
- [ ] Ajustar repositório/query para filtros combináveis.
- [ ] Atualizar controller e binding de query params.
- [ ] Atualizar templates Thymeleaf.
- [ ] Criar testes MVC para filtros e paginação.

Dependências
Pode rodar em paralelo com Card 2.

Riscos / Atenções
Complexidade de query pode impactar performance sem índices adequados.

Labels sugeridas
type:feature, area:web, priority:medium, status:backlog

Status inicial no board
Backlog

———

## Card 4 - Refatoração de performance JPA (EAGER -> LAZY + consultas dirigidas)

Contexto
Relacionamentos com FetchType.EAGER podem causar sobrecarga e piorar com crescimento de dados.

Objetivo
Melhorar latência e eficiência de acesso a banco sem regressão funcional.

Escopo (In)
- Revisão de fetch strategy para LAZY onde aplicável.
- Uso de @EntityGraph ou queries dedicadas para telas críticas.
- Mitigar N+1 em fluxos principais (ownerDetails, listagens).

Escopo (Out)
- Migração para arquitetura reativa.
- Mudanças estruturais profundas de domínio.

Critérios de Aceite
- [ ] Redução observável de queries em cenários críticos.
- [ ] Sem exceções de lazy loading nas views/serialização.
- [ ] Testes de integração mantidos verdes.
- [ ] Comportamento funcional preservado.

Checklist Técnico
- [ ] Mapear consultas críticas atuais.
- [ ] Alterar fetch strategy com segurança.
- [ ] Introduzir consultas/graphs onde necessário.
- [ ] Validar com testes e logs SQL.

Dependências
Preferível após Card 3 para validar impacto nas novas buscas.

Riscos/Atenções
Risco de quebrar renderização se acesso a coleções LAZY ocorrer fora de contexto transacional.

Labels sugeridas
type:refactor, area:performance, priority:medium, status:backlog

Status inicial no board
Backlog

———

## Card 5 - Governança de qualidade (gate de cobertura + pipeline)

Contexto
Há relatório JaCoCo, mas sem gate mínimo obrigatório no CI.

Objetivo
Evitar regressões silenciosas e tornar pipeline mais confiável para evolução contínua.

Escopo (In)
- Definir threshold mínimo de cobertura no build.
- Separar claramente testes unitários e integração no CI.
- Ajustar jobs/workflows para feedback mais previsível.

Escopo (Out)
- Cobertura 100%.
- Mudança de provedor de CI.

Critérios de Aceite
- [ ] Build falha quando cobertura mínima não for atingida.
- [ ] Pipeline diferencia execução de unit vs integração.
- [ ] Tempo e estabilidade do CI melhoram ou permanecem estáveis.
- [ ] Documentação de execução local atualizada.

Checklist Técnico
- [ ] Configurar regras JaCoCo no Maven/Gradle.
- [ ] Revisar workflows GitHub Actions.
- [ ] Ajustar estratégia de execução de integração (Docker/Testcontainers).
- [ ] Validar em PR real com cenário de falha proposital de cobertura.

Dependências
Ideal no final do ciclo para consolidar padrão de qualidade dos cards anteriores.

Riscos / Atenções
Threshold muito agressivo pode gerar ruído e travar entregas iniciais.

Labels sugeridas
type:chore, area:ci, area:test, priority:medium, status:backlog

Status inicial no board
Backlog

