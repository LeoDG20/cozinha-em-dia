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
- Receitas: listagem com busca e filtros, visualização, cadastro, edição e exclusão, com tempo de preparo, porções, ingredientes, quantidades, unidades e modo de preparo em etapas.
- Imagem de capa em cada receita (arquivo ou endereço da internet). Receitas sem foto recebem uma capa ilustrada.
- Material de aula: link de videoaula do YouTube, vídeo próprio (MP4 ou WebM, até 60 MB) e foto opcional em cada etapa do preparo.
- Correção automática de ingredientes digitados com erro (por exemplo, `farna` vira `Farinha`), no cadastro de receitas, na despensa e na busca, com sugestões de autocompletar.
- Sugestões para nomes digitados com erro (por exemplo, `xocolate` gera a pergunta "Você quis dizer: Chocolate?"). O algoritmo é local e não usa serviço externo: combina distância de edição de Damerau-Levenshtein, uma chave fonética para o português (`ch` e `x`, `ss`, `ç` e `z`, `h` mudo, `qu` e `k`, vogal final) e a comparação de trechos de três letras. Está em `service/CorretorIngredientes.java`.
- Compartilhamento de receitas entre usuários pelo e-mail, com mensagem opcional. O destinatário vê o que falta na própria despensa e pode salvar uma cópia editável. O dono vê, no cartão e na página da receita, com quem ela foi compartilhada, e pode revogar o acesso.
- Adaptação de receitas: o botão "Adaptar" cria uma nova versão, com nome próprio, que já traz copiados ingredientes, etapas, fotos e vídeos. A receita original não é alterada, e a versão indica de qual receita veio.
- Despensa: inclusão de ingredientes, soma de quantidades de itens já existentes (com conversão de unidades), edição da quantidade e da unidade de cada item, remoção, contagem de receitas que usam cada ingrediente e lista de compras com o que falta, somado entre as receitas.
- Comparação: classificação das receitas em disponíveis, parciais ou indisponíveis, com a quantidade que falta de cada ingrediente.
- Receita: ajuste de porções com recálculo das quantidades, marcação de ingredientes e impressão.
- Dados de demonstração: dezesseis receitas com fotografia e uma despensa inicial, criadas na primeira execução. Receitas já existentes não são duplicadas.

A comparação considera quantidade e unidade, com conversão automática entre unidades da mesma grandeza: massa (`mg`, `g` e `kg`) e volume (`cl`, `ml` e `l`). Assim, 1 `kg` na despensa atende uma receita que pede 500 `g`, e vice-versa. Unidades sem equivalência fixa (`unidade`, `dente`, `colher` e `xícara`) só são comparadas com elas mesmas. A regra está em `service/ConversorUnidades.java`.

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

As imagens e os vídeos enviados ficam na pasta `uploads/`, ao lado da aplicação (configurável pela variável `UPLOADS_DIR`), e não são versionados. O banco guarda apenas o nome de cada arquivo.

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

Os testes verificam o envio de imagem, vídeo e fotos de etapas, o compartilhamento entre dois usuários, a adaptação de receitas, as sugestões para ingredientes digitados com erro, a lista de compras, a busca com correção de digitação, o redirecionamento de rotas protegidas, o login com BCrypt, a rejeição de senha incorreta, o cadastro, o logout, a exigência do token CSRF, a renderização das páginas autenticadas e a validade dos scripts SQL. Eles utilizam um PostgreSQL embutido e descartável, portanto não dependem de nenhuma instalação nem alteram o banco de desenvolvimento.

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
  service/      regras de receita, despensa, disponibilidade, mídia, correção de ingredientes e compartilhamento
src/main/resources/
  templates/    páginas Thymeleaf
  static/css/   estilo da interface
  static/js/    linhas dinâmicas do formulário, prévia de arquivos e correção ao digitar
  application.properties
src/test/java/  testes automatizados
database/       scripts SQL de referência
mvnw, mvnw.cmd, .mvn/   Maven Wrapper
CozinhaEmDia.exe, executar.bat   preparam o ambiente (JDK e PostgreSQL) e iniciam a aplicação
scripts/        script de preparação (PowerShell) e código do lançador
```

## Créditos das fotografias

As fotografias das receitas de demonstração são arquivos de licença livre do Wikimedia Commons. Elas não ficam no repositório: a aplicação as carrega diretamente de `commons.wikimedia.org` (endereço `Special:FilePath`) e, por isso, precisa de acesso à internet para exibi-las. Sem conexão, a receita usa a capa ilustrada. A tela de entrada e a de cadastro usam as fotografias de espaguete e de pão de queijo, que constam na lista.

| Arquivo | Autoria | Licença |
|---|---|---|
| Omelet With Fixings.jpg | Stripey the crab | CC BY-SA 3.0 |
| Spaghetti aglio e olio KB.jpg | Bairuilong | CC BY-SA 4.0 |
| Liat Portal for Foodie Disorder - Grilled chicken with rice and vegetables.jpg | HaJunkiyada | CC BY-SA 4.0 |
| Foodiesfeed.com pouring-honey-on-pancakes-with-walnuts.jpg | Mae Mu | CC0 |
| Banana bread cut into slices.jpg | Shisma | CC BY 4.0 |
| Lentil salad (29308018298).jpg | Joey Doll | CC BY 2.0 |
| Pao de queijo brasil.jpg | Acfariac | CC BY-SA 4.0 |
| Brigadeiros.jpg | Rodrigogomesonetwo | CC BY-SA 4.0 |
| Bolo de cenoura com chocolate e granulado.jpg | Sintegrity | CC BY-SA 4.0 |
| Prato com arroz e feijão, frango grelhado, batata frita, salada e ovo frito como adicionais.jpg | Tet | CC0 |
| Lasagna 2022.jpg | Derk29 | CC BY-SA 4.0 |
| Traditional Vegetable Soup from Argentina.jpg | Horacio Cambeiro | CC BY-SA 3.0 |
| MashedPotatoes.jpg | Renee Comet (Instituto Nacional do Câncer dos EUA) | Domínio público |
| Farofa de ovo.jpg | Melsj | CC BY-SA 4.0 |
| Pudim de leite (3544235183).jpg | Marcelo Träsel | CC BY-SA 2.0 |
| Beef Stroganoff-03.jpg | V 2 | CC BY 2.0 |

As páginas de cada arquivo no Wikimedia Commons trazem o texto completo da licença e a autoria original.

## Limitações

- O escopo não inclui geração de receitas por inteligência artificial, integração com supermercados, informações nutricionais, controle de validade, avaliações ou serviços externos.
- A interface é um protótipo funcional e não uma versão de produção.
- Uma versão de produção exigiria validações adicionais, ampliação dos testes, tratamento mais completo de erros, conversão entre medidas caseiras (colher e xícara) e o uso de migrações de banco (por exemplo, Flyway) no lugar de `ddl-auto=update`.
