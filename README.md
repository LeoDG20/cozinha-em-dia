# Cozinha em Dia

Aplicação web desenvolvida como trabalho acadêmico para o cadastro de receitas, o controle da despensa e a verificação de quais pratos podem ser preparados com os ingredientes disponíveis. O sistema segue o padrão MVC, com Java, Spring Boot, Thymeleaf e PostgreSQL.

## Equipe

| Integrante | Conta no GitHub |
|---|---|
| Leonardo De Gobbi | [LeoDG20](https://github.com/LeoDG20) |
| Matheus Molina Baretta | [matheusmolina05](https://github.com/matheusmolina05) |
| Michael Jun Ohara | [butterJun](https://github.com/butterJun) |
| Rafael Sabino Figueiredo | [rafael-blaze](https://github.com/rafael-blaze) |

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
- Hibernate ORM e PostgreSQL (16 ou superior)
- Maven Wrapper, que dispensa a instalação do Maven

## Execução local no Windows

### Execução automática (recomendada)

1. Baixe o ZIP do projeto e extraia-o, ou clone o repositório com `git clone`.
2. Na pasta raiz, execute `CozinhaEmDia.exe` com um duplo clique (o arquivo `executar.bat` faz o mesmo).
3. O programa verifica o ambiente e prepara o que for necessário:
   - instala o JDK 21, caso não esteja instalado;
   - instala o PostgreSQL 16, caso não esteja instalado, e cria o usuário e o banco de dados da aplicação;
   - inicia a aplicação com o Maven Wrapper e abre o navegador em `http://localhost:8081`.
4. Entre com a conta de demonstração ou crie uma conta própria na tela de cadastro.

Observações sobre a primeira execução:

- É necessária conexão com a internet, pois são baixados o JDK, o PostgreSQL e as dependências do Maven.
- O Windows solicitará permissão de administrador durante a instalação do JDK e do PostgreSQL.
- Quando o PostgreSQL é instalado pelo programa, o usuário administrador `postgres` recebe a senha `postgres123`. O PostgreSQL passa a funcionar como serviço do Windows, na porta 5432.
- Se o computador já tiver um PostgreSQL instalado, ele é reaproveitado. Caso a senha do usuário `postgres` não seja `postgres123`, o programa solicita a senha para criar o banco da aplicação. Se preferir, crie o banco manualmente (veja "Criação manual do banco").
- Por ser um programa sem assinatura digital, o Windows SmartScreen ou o antivírus pode exibir um aviso. O executável apenas chama o script `scripts/preparar-e-executar.ps1`, cujo código está no repositório (`scripts/lancador/Lancador.cs` e `compilar.bat` permitem recompilá-lo).
- Para apenas verificar o que está instalado, sem instalar nada, execute `CozinhaEmDia.exe -SomenteVerificar`.

Para encerrar a aplicação, use `Ctrl+C` na janela do terminal. O PostgreSQL continua em execução como serviço do Windows.

A aplicação utiliza a porta 8081. Caso esteja ocupada, altere `server.port` em `src/main/resources/application.properties`.

### Execução manual

Requisitos: JDK 21 (`java -version` deve indicar a versão 21) e PostgreSQL 14 ou superior em execução na porta 5432, com o banco criado conforme a seção "Criação manual do banco". O Maven não precisa estar instalado. Em um terminal aberto na pasta raiz do projeto:

```bash
.\mvnw.cmd spring-boot:run
```

Depois, acesse `http://localhost:8081`.

### Conta de demonstração

- E-mail: `demo@cozinha.local`
- Senha: `demo123`

Após o cadastro de uma nova conta, o usuário já é autenticado. As receitas e a despensa de demonstração pertencem apenas à conta de demonstração.

## Execução local no Linux e no macOS

O `CozinhaEmDia.exe` funciona apenas no Windows. Nos demais sistemas, a preparação é feita com os comandos abaixo, executados em um terminal. São necessários o JDK 21 e o PostgreSQL 14 ou superior; o Maven não precisa ser instalado, pois o projeto inclui o Maven Wrapper (`mvnw`).

### 1. Instalação do JDK 21 e do PostgreSQL

**Ubuntu / Debian:**

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk postgresql
sudo systemctl enable --now postgresql
```

**macOS (com [Homebrew](https://brew.sh)):**

```bash
brew install openjdk@21 postgresql@16
brew services start postgresql@16
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
export PATH="$(brew --prefix postgresql@16)/bin:$PATH"
```

No macOS, as duas linhas `export` valem apenas para o terminal atual. Para torná-las permanentes, adicione-as ao arquivo `~/.zshrc`.

Confira a instalação com `java -version` (deve indicar a versão 21) e `psql --version`.

### 2. Criação do usuário e do banco da aplicação

**Ubuntu / Debian** (o superusuário do PostgreSQL é o usuário `postgres` do sistema):

```bash
sudo -u postgres psql -c "CREATE USER cozinha WITH PASSWORD 'cozinha123';"
sudo -u postgres psql -c "CREATE DATABASE cozinha_em_dia OWNER cozinha;"
```

**macOS** (o superusuário do PostgreSQL instalado pelo Homebrew é o próprio usuário logado):

```bash
psql postgres -c "CREATE USER cozinha WITH PASSWORD 'cozinha123';"
psql postgres -c "CREATE DATABASE cozinha_em_dia OWNER cozinha;"
```

Para conferir o acesso, execute `psql -h localhost -U cozinha -d cozinha_em_dia -c "SELECT 1;"` e informe a senha `cozinha123`.

### 3. Execução da aplicação

Na pasta raiz do projeto:

```bash
./mvnw spring-boot:run
```

Na primeira execução, o Maven Wrapper baixa as dependências, o que exige conexão com a internet. Quando o terminal exibir `Started CozinhaEmDiaApplication`, acesse `http://localhost:8081` e entre com a conta de demonstração. Para encerrar, use `Ctrl+C`.

Se aparecer o erro `Permission denied` ao executar o `mvnw`, conceda a permissão de execução com `chmod +x mvnw`.

## Banco de dados

O sistema utiliza PostgreSQL, com os parâmetros abaixo (valores padrão de `application.properties`):

| Parâmetro | Valor |
|---|---|
| Servidor | `localhost:5432` |
| Banco | `cozinha_em_dia` |
| Usuário | `cozinha` |
| Senha | `cozinha123` |

Esses valores podem ser substituídos pelas variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`.

As tabelas são criadas pelo Hibernate na inicialização (`spring.jpa.hibernate.ddl-auto=update`). Em seguida, a classe `DataInitializer` cadastra a conta de demonstração e os dados de exemplo, caso ainda não existam. Os dados ficam no próprio PostgreSQL e permanecem entre as execuções.

### Criação manual do banco

O `CozinhaEmDia.exe` cria o usuário e o banco automaticamente. Para fazê-lo à mão, execute como superusuário (no `psql` ou no pgAdmin):

```sql
CREATE USER cozinha WITH PASSWORD 'cozinha123';
CREATE DATABASE cozinha_em_dia OWNER cozinha;
```

Para apagar os dados e retornar ao estado inicial, remova e recrie o banco:

```sql
DROP DATABASE cozinha_em_dia;
CREATE DATABASE cozinha_em_dia OWNER cozinha;
```

Os arquivos `database/schema.sql` e `database/sample_data.sql` reproduzem o esquema e os dados de exemplo em SQL, apenas como referência. Eles não precisam ser executados, pois a aplicação já cria as tabelas e os dados. Se forem utilizados, devem ser executados em um banco vazio, porque os identificadores dos dados de exemplo são fixos.

### Consulta ao banco

É possível consultar as tabelas com o `psql`, o pgAdmin (instalado junto com o PostgreSQL) ou o DBeaver, usando os parâmetros da tabela anterior. Por exemplo:

```bash
psql -h localhost -U cozinha -d cozinha_em_dia -c "SELECT id, nome, email FROM usuarios;"
```

## Autenticação

A autenticação é feita com Spring Security e Thymeleaf.

- `config/SecurityConfig.java` define as rotas públicas (`/login`, `/cadastro` e arquivos estáticos), exige autenticação nas demais e configura o login por formulário, o logout (`POST /logout`) e a proteção CSRF.
- `security/CustomUserDetailsService.java` consulta o usuário pelo e-mail na tabela `usuarios`.
- `security/UsuarioAutenticado.java` representa o usuário autenticado e é recebido nos controllers por meio de `@AuthenticationPrincipal`.
- As senhas utilizam `BCryptPasswordEncoder`.
- Os formulários Thymeleaf que usam `th:action` recebem o token CSRF automaticamente. Requisições `POST` sem o token são rejeitadas com o código 403.

## Testes automatizados

No Windows:

```bash
.\mvnw.cmd test
```

No Linux e no macOS:

```bash
./mvnw test
```

Os testes verificam o redirecionamento de rotas protegidas, o login com BCrypt, a rejeição de senha incorreta, o cadastro, o logout, a exigência do token CSRF, a renderização das páginas autenticadas e a validade dos scripts SQL. Eles utilizam um PostgreSQL embutido e descartável, portanto não dependem de nenhuma instalação nem alteram o banco de desenvolvimento.

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
mvnw, mvnw.cmd, .mvn/   Maven Wrapper
CozinhaEmDia.exe, executar.bat   preparam o ambiente (JDK e PostgreSQL) e iniciam a aplicação
scripts/        script de preparação (PowerShell) e código do lançador
```

## Limitações

- O escopo não inclui geração de receitas por inteligência artificial, integração com supermercados, informações nutricionais, controle de validade, avaliações ou serviços externos.
- A interface é um protótipo funcional e não uma versão de produção.
- Uma versão de produção exigiria validações adicionais, ampliação dos testes, tratamento mais completo de erros, conversão de unidades e o uso de migrações de banco (por exemplo, Flyway) no lugar de `ddl-auto=update`.
