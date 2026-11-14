# Cozinha em Dia

Aplicação web desenvolvida como trabalho acadêmico para o cadastro de receitas, o controle da despensa e a verificação de quais pratos podem ser preparados com os ingredientes disponíveis. O sistema segue o padrão MVC, com Java, Spring Boot, Thymeleaf e PostgreSQL.

Equipe: Leonardo De Gobbi, Matheus Molina Baretta, Michael Jun Ohara e Rafael Sabino Figueiredo.

## Funcionalidades

- Conta de usuário: cadastro, login e logout, com páginas protegidas por Spring Security. As senhas são armazenadas com BCrypt e os formulários possuem proteção contra CSRF.
- Receitas: listagem, visualização, cadastro, edição e exclusão, com tempo de preparo, porções, ingredientes, quantidades, unidades e modo de preparo.
- Despensa: inclusão de ingredientes, soma de quantidades de itens já existentes e remoção.
- Comparação: classificação das receitas em disponíveis, parciais ou indisponíveis, com a quantidade que falta de cada ingrediente.
- Dados de demonstração: seis receitas, vinte ingredientes e uma despensa inicial, criados na primeira execução.

A comparação considera quantidade e unidade. Nesta versão não há conversão automática entre unidades (por exemplo, de `kg` para `g`), de modo que receita e despensa devem usar a mesma unidade para cada ingrediente.

## Tecnologias

- Java 21
- Spring Boot 3.5.7 (Spring Web, Spring Data JPA e Spring Security 6)
- Thymeleaf com a extensão de integração ao Spring Security
- Hibernate ORM e PostgreSQL 16
- Maven Wrapper, que dispensa a instalação do Maven
- Docker Compose, para executar o banco de dados

## Execução local no Windows

### Requisitos

1. JDK 21 (`java -version` deve indicar a versão 21).
2. Docker Desktop, em execução, para subir o PostgreSQL. Caso prefira instalar o PostgreSQL diretamente, veja a seção "Alternativa sem Docker".
3. Conexão com a internet na primeira execução, pois o Maven Wrapper e o Docker baixam as dependências e a imagem do banco.

O Maven não precisa estar instalado.

### Passos

1. Baixe o ZIP do projeto e extraia-o, ou clone o repositório com `git clone`.
2. Abra a pasta raiz do projeto, que contém os arquivos `pom.xml`, `mvnw.cmd` e `docker-compose.yml`.
3. Execute o arquivo `executar.bat` com um duplo clique. Ele inicia o banco de dados e, em seguida, a aplicação.
4. Aguarde a mensagem `Started CozinhaEmDiaApplication` no terminal e acesse `http://localhost:8081` no navegador.
5. Entre com a conta de demonstração ou crie uma conta própria na tela de cadastro.

Os mesmos passos podem ser feitos manualmente, em um terminal aberto na pasta raiz:

```bash
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

Para encerrar a aplicação, use `Ctrl+C` no terminal. Para parar o banco de dados, execute `docker compose stop`.

A aplicação utiliza a porta 8081. Caso esteja ocupada, altere `server.port` em `src/main/resources/application.properties`.

### Conta de demonstração

- E-mail: `demo@cozinha.local`
- Senha: `demo123`

Após o cadastro de uma nova conta, o usuário já é autenticado. As receitas e a despensa de demonstração pertencem apenas à conta de demonstração.

## Banco de dados

O sistema utiliza PostgreSQL. O arquivo `docker-compose.yml` cria um contêiner com as seguintes configurações, que coincidem com os valores padrão de `application.properties`:

| Parâmetro | Valor |
|---|---|
| Servidor | `localhost:5432` |
| Banco | `cozinha_em_dia` |
| Usuário | `cozinha` |
| Senha | `cozinha123` |

Esses valores podem ser substituídos pelas variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`.

As tabelas são criadas pelo Hibernate na inicialização (`spring.jpa.hibernate.ddl-auto=update`). Em seguida, a classe `DataInitializer` cadastra a conta de demonstração e os dados de exemplo, caso ainda não existam. Os dados ficam em um volume do Docker (`dados_postgres`) e permanecem entre as execuções.

Para apagar todos os dados e retornar ao estado inicial:

```bash
docker compose down -v
```

Os arquivos `database/schema.sql` e `database/sample_data.sql` reproduzem o esquema e os dados de exemplo em SQL, apenas como referência. Eles não precisam ser executados, pois a aplicação já cria as tabelas e os dados. Se forem utilizados, devem ser executados em um banco vazio, porque os identificadores dos dados de exemplo são fixos.

### Consulta ao banco

Com o contêiner em execução, é possível consultar as tabelas pelo terminal:

```bash
docker exec -it cozinha-em-dia-postgres psql -U cozinha -d cozinha_em_dia -c "SELECT id, nome, email FROM usuarios;"
```

Também é possível conectar uma ferramenta gráfica, como o DBeaver ou o pgAdmin, usando os parâmetros da tabela anterior.

### Alternativa sem Docker

1. Instale o PostgreSQL (versão 14 ou superior) e mantenha o serviço em execução na porta 5432.
2. Crie o usuário e o banco de dados, por exemplo no `psql` ou no pgAdmin:

```sql
CREATE USER cozinha WITH PASSWORD 'cozinha123';
CREATE DATABASE cozinha_em_dia OWNER cozinha;
```

3. Execute a aplicação com `.\mvnw.cmd spring-boot:run`. O `executar.bat` também pode ser usado: sem o Docker instalado, ele apenas inicia a aplicação.

## Autenticação

A autenticação é feita com Spring Security e Thymeleaf.

- `config/SecurityConfig.java` define as rotas públicas (`/login`, `/cadastro` e arquivos estáticos), exige autenticação nas demais e configura o login por formulário, o logout (`POST /logout`) e a proteção CSRF.
- `security/CustomUserDetailsService.java` consulta o usuário pelo e-mail na tabela `usuarios`.
- `security/UsuarioAutenticado.java` representa o usuário autenticado e é recebido nos controllers por meio de `@AuthenticationPrincipal`.
- As senhas utilizam `BCryptPasswordEncoder`.
- Os formulários Thymeleaf que usam `th:action` recebem o token CSRF automaticamente. Requisições `POST` sem o token são rejeitadas com o código 403.

## Testes automatizados

```bash
.\mvnw.cmd test
```

Os testes verificam o redirecionamento de rotas protegidas, o login com BCrypt, a rejeição de senha incorreta, o cadastro, o logout, a exigência do token CSRF, a renderização das páginas autenticadas e a validade dos scripts SQL. Eles utilizam um PostgreSQL embutido e descartável, portanto não dependem do Docker nem alteram o banco de desenvolvimento.

## Documentação

A modelagem de dados está em [`docs/MODELAGEM.md`](docs/MODELAGEM.md), com entidades, cardinalidades, regras principais e o diagrama entidade-relacionamento. As rotas estão descritas em [`docs/ROTAS_HTTP.md`](docs/ROTAS_HTTP.md).

### Arquitetura

```text
Navegador / Thymeleaf
        ↓ HTTP
   Spring Security
        ↓
     Controller
        ↓
      Service       regras de negócio e comparação de ingredientes
        ↓
     Repository
        ↓ JPA / Hibernate
     PostgreSQL
```

### Estrutura do código

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
src/test/java/  testes automatizados
database/       scripts SQL de referência
docker-compose.yml   banco PostgreSQL
mvnw, mvnw.cmd, .mvn/   Maven Wrapper
executar.bat    atalho para iniciar banco e aplicação no Windows
```

## Limitações

- O escopo não inclui geração de receitas por inteligência artificial, integração com supermercados, informações nutricionais, controle de validade, avaliações ou serviços externos.
- A interface é um protótipo funcional e não uma versão de produção.
- Uma versão de produção exigiria validações adicionais, ampliação dos testes, tratamento mais completo de erros, conversão de unidades e o uso de migrações de banco (por exemplo, Flyway) no lugar de `ddl-auto=update`.
