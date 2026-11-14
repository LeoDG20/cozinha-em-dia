# Cozinha em Dia — protótipo acadêmico

Aplicação web independente para cadastro de receitas, controle de despensa e verificação dos pratos que podem ser preparados com os ingredientes disponíveis. O projeto foi estruturado em Java + Spring Boot seguindo MVC, com páginas Thymeleaf e persistência SQLite.

## O que já funciona

- **Conta e sessão:** cadastro, login, logout e páginas protegidas com **Spring Security**; senha armazenada com BCrypt e formulários protegidos contra CSRF.
- **Receitas:** listar, visualizar, cadastrar, editar e excluir; registrar tempo, porções, ingredientes, quantidades, unidades e modo de preparo.
- **Despensa:** adicionar ingredientes, somar quantidades existentes e remover itens.
- **Comparação:** classifica receitas como disponíveis, parciais ou indisponíveis; lista a quantidade que falta de cada ingrediente.
- **Dados de demonstração:** seis receitas, vinte ingredientes e uma despensa inicial para explorar a aplicação sem cadastrar tudo do zero.

O protótipo usa comparação de quantidade e unidade. Nesta versão, não há conversão automática entre unidades (por exemplo, `kg` para `g`); receita e despensa devem usar a mesma unidade para o ingrediente.

## Tecnologias

- Java 21
- Spring Boot 3.5.7, Spring Web e Spring Data JPA
- Spring Security 6 (autenticação, autorização e CSRF)
- Hibernate ORM com dialeto SQLite
- SQLite + SQLite JDBC
- Thymeleaf (com integração ao Spring Security), HTML5, CSS3
- Maven Wrapper (não é preciso instalar o Maven)
- BCrypt para hash de senha

## Como executar (Windows)

**Pré-requisito único:** JDK 21 instalado (`java -version` deve mostrar a versão 21). Não é necessário instalar o Maven: o projeto traz o **Maven Wrapper** (`mvnw.cmd`). Na primeira execução é preciso ter conexão com a internet, pois o wrapper baixa o Maven e as dependências do projeto.

1. Baixe o ZIP e extraia-o, ou clone o repositório com `git clone`.
2. Abra a **pasta raiz** do projeto (a que contém o `pom.xml` e o `mvnw.cmd`).
3. Execute de uma destas formas:
   - **Duplo clique** em `executar.bat`; ou
   - em um terminal aberto nessa pasta: `.\mvnw.cmd spring-boot:run`
4. Quando aparecer `Started CozinhaEmDiaApplication` no terminal, abra **http://localhost:8081** no navegador.
5. Entre com a conta demonstrativa (abaixo) ou crie uma conta própria em **Criar uma conta**.

Para parar o servidor, pressione `Ctrl+C` no terminal (ou feche a janela do `executar.bat`).

Também é possível abrir a pasta no VS Code (com o *Extension Pack for Java*) e clicar em **Run** acima do método `main` de `CozinhaEmDiaApplication.java`.

Para rodar os testes automatizados de autenticação: `.\mvnw.cmd test`.

A aplicação usa a porta **8081**. Caso esteja ocupada, altere `server.port` em `src/main/resources/application.properties`.

### Conta demonstrativa

- **E-mail:** `demo@cozinha.local`
- **Senha:** `demo123`

Também é possível criar uma conta própria pela tela de cadastro (após o cadastro, o usuário já entra autenticado). As receitas demonstrativas ficam vinculadas somente à conta de demonstração.

## Visualizar o banco de dados

O arquivo já criado e preenchido está em:

```text
data/receitas_despensa.db
```

Instale o **DB Browser for SQLite** e selecione `Open Database` para abrir esse arquivo. Na aba **Browse Data**, escolha as tabelas `usuarios`, `receitas`, `ingredientes`, `receita_ingredientes` ou `despensa`. A aba **Database Structure** mostra as tabelas e suas colunas.

Os scripts de referência ficam em `database/schema.sql` e `database/sample_data.sql`. Se for executar os `INSERT`s do arquivo de dados novamente, faça isso em um banco vazio, porque as chaves são fixas para o conjunto inicial.

### Como o banco SQLite é criado e armazenado

O banco é **um único arquivo**, `data/receitas_despensa.db`, configurado em `application.properties` com o caminho relativo `jdbc:sqlite:./data/receitas_despensa.db`. Não há servidor de banco para instalar.

- Ao iniciar, o Hibernate (`ddl-auto=update`) cria as tabelas que ainda não existirem; por isso o sistema também funciona se o arquivo `.db` for apagado (um novo é criado, desde que a pasta `data/` exista).
- Na inicialização, o `DataInitializer` cria a conta demonstrativa e os dados de exemplo caso ainda não existam.
- Todas as contas, receitas e despensas ficam nesse arquivo; para restaurar o estado inicial, substitua-o pelo original do repositório.
- Como o caminho é relativo, execute o projeto a partir da **pasta raiz** (o `executar.bat` já faz isso).

## Autenticação (Spring Security)

- `config/SecurityConfig.java`: define as rotas públicas (`/login`, `/cadastro`, arquivos estáticos), exige autenticação nas demais, configura login por formulário (campos `email` e `senha`), logout (`POST /logout`) e proteção CSRF.
- `security/CustomUserDetailsService.java`: busca o usuário pelo e-mail na tabela `usuarios` do SQLite.
- `security/UsuarioAutenticado.java`: usuário autenticado (id e nome) injetado nos controllers com `@AuthenticationPrincipal`.
- Senhas com **BCrypt** (`BCryptPasswordEncoder`); os hashes já gravados no banco continuam válidos.
- Os formulários Thymeleaf (`th:action`) recebem o token CSRF automaticamente; requisições `POST` sem o token retornam 403.

## Modelagem

A documentação da modelagem está em [`docs/MODELAGEM.md`](docs/MODELAGEM.md), com entidades, cardinalidades, regras principais e diagrama ER. A especificação das rotas está em [`docs/ROTAS_HTTP.md`](docs/ROTAS_HTTP.md).

### Arquitetura aplicada

```text
Navegador / Thymeleaf
        ↓ HTTP
     Controller
        ↓
      Service       ← regras de negócio e comparação de ingredientes
        ↓
     Repository
        ↓ JPA / Hibernate
       SQLite
```

## Estrutura do código

```text
src/main/java/br/com/cozinhaemdia/
  config/       Spring Security (SecurityConfig) e dados iniciais
  security/     UserDetailsService e usuário autenticado
  controller/   rotas e preparação dos dados para as telas
  model/        entidades JPA
  repository/   interfaces de persistência
  service/      regras de receita, despensa e disponibilidade
src/main/resources/
  templates/    páginas Thymeleaf
  static/css/   estilo da interface
  application.properties
src/test/java/   testes de autenticação (login, logout, cadastro, CSRF, rotas protegidas)
mvnw, mvnw.cmd, .mvn/   Maven Wrapper
executar.bat    atalho para iniciar o sistema no Windows
```

## Limites desta primeira versão

- Não inclui geração de receitas por IA, integração com supermercados, informações nutricionais, controle de validade, avaliações ou serviços externos, alinhado ao escopo definido para o trabalho.
- A interface é um protótipo funcional, não uma versão final de produção.
- Para uma entrega de produção, seria necessário complementar validação, ampliar os testes automatizados, mensagens de erro mais completas e conversão de unidades.

**Visualizador gratuito para Windows:** [DB Browser for SQLite — página oficial](https://sqlitebrowser.org/dl/#windows). Abra o arquivo `data/receitas_despensa.db` usando `Open Database`.
