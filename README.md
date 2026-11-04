<div align="center">

# 🗓️ AgendaFlow API

### A engrenagem por trás de uma agenda SaaS mais simples, segura e escalável.

API REST para gestão de empresas, usuários, clientes e autenticação em uma plataforma de agendamentos multi-tenant.

<p>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-Database-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/JWT-Stateless-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT">
</p>

</div>

---

## ✨ O que é

O **AgendaFlow** é o backend de um SaaS de agendamento pensado para negócios que precisam organizar sua operação sem misturar os dados de uma empresa com os de outra.

Cada empresa funciona como um **tenant** independente, com seus próprios usuários, clientes e futuros agendamentos. A autenticação é stateless, baseada em JWT, e o acesso é protegido por perfis.

> **Uma API, vários negócios, dados no lugar certo.**

## 🧩 Funcionalidades

- 🔐 Login com autenticação JWT
- 🏢 Cadastro de novas empresas e do respectivo administrador
- 👥 Criação de usuários vinculados à empresa atual
- 🧑‍🤝‍🧑 Listagem de clientes com autorização por perfil
- 🛡️ Controle de acesso com Spring Security
- 🧱 Isolamento de dados por tenant usando filtros do Hibernate
- 🔑 Senhas protegidas com BCrypt
- ✅ Validação de payloads com Jakarta Validation
- 🗃️ Persistência com Spring Data JPA e PostgreSQL

## 🏗️ Arquitetura em camadas

```text
src/main/java/br/com/ruan_dev/saas
├── controllers/   # Endpoints HTTP
├── dto/           # Objetos de entrada e saída
├── exceptions/    # Tratamento global de erros
├── models/        # Entidades persistidas
├── repositories/  # Acesso a dados
├── security/      # JWT, perfis e contexto do tenant
└── services/      # Regras de negócio
```

## 🔌 API

A API oferece autenticação, gestão de empresas, usuários e clientes, com
autorização baseada em perfis e isolamento dos dados por tenant.

As rotas, URLs internas e exemplos de requisições não são publicados neste
README. Consulte a documentação privada do projeto ou o ambiente autorizado
para obter os detalhes de integração.

## 🚀 Como executar

### Pré-requisitos

- Java 21+
- PostgreSQL
- Maven (ou o Maven Wrapper incluído no projeto)

### 1. Configure o banco

Crie um banco PostgreSQL e configure as credenciais localmente. Não publique
valores reais no repositório:

```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
```

Use um arquivo local não versionado ou variáveis de ambiente. Para produção,
prefira um gerenciador de segredos.

### 2. Inicie a aplicação

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### 3. Autenticação

O acesso à API utiliza JWT e perfis de autorização. As credenciais iniciais,
quando necessárias para desenvolvimento, devem ser mantidas apenas no ambiente
local e nunca publicadas no README ou no controle de versão.

## 🔐 Perfis de acesso

| Perfil | Responsabilidade |
|---|---|
| `MASTER` | Provisiona novas empresas e seus administradores |
| `ADMIN` | Administra usuários e consulta clientes do próprio tenant |

O tenant atual é identificado durante a autenticação e aplicado às consultas por meio do `TenantContext` e do filtro do Hibernate.

## 🧪 Testes

Execute os testes com:

```bash
./mvnw test
```

## 🛣️ Próximos passos

- [ ] Implementar CRUD completo de agendamentos
- [ ] Adicionar criação e edição de clientes
- [ ] Documentar a API com OpenAPI/Swagger
- [ ] Adicionar paginação e filtros de agenda
- [ ] Cobrir fluxos de autenticação e autorização com testes
- [ ] Externalizar configurações sensíveis por ambiente

## 🤝 Contribuindo

1. Crie uma branch para sua alteração.
2. Faça mudanças pequenas e focadas.
3. Execute os testes.
4. Abra um pull request descrevendo o problema e a solução.

## 📄 Licença

Este projeto ainda não possui uma licença definida.

---

<div align="center">

Feito para transformar horários espalhados em uma operação organizada. 💙

</div>
