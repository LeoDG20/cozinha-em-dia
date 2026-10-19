# Cozinha em Dia — protótipo acadêmico

Aplicação web independente para cadastro de receitas, controle de despensa e verificação dos pratos que podem ser preparados com os ingredientes disponíveis. O projeto foi estruturado em Java + Spring Boot seguindo MVC, com páginas Thymeleaf e persistência SQLite.

## O que já funciona

- **Conta e sessão:** cadastro, login, logout e páginas protegidas por sessão; senha armazenada com BCrypt.
- **Receitas:** listar, visualizar, cadastrar, editar e excluir; registrar tempo, porções, ingredientes, quantidades, unidades e modo de preparo.
- **Despensa:** adicionar ingredientes, somar quantidades existentes e remover itens.
- **Comparação:** classifica receitas como disponíveis, parciais ou indisponíveis; lista a quantidade que falta de cada ingrediente.
- **Dados de demonstração:** seis receitas, vinte ingredientes e uma despensa inicial para explorar a aplicação sem cadastrar tudo do zero.

O protótipo usa comparação de quantidade e unidade. Nesta versão, não há conversão automática entre unidades (por exemplo, `kg` para `g`); receita e despensa devem usar a mesma unidade para o ingrediente.

## Tecnologias

- Java 21
- Spring Boot 3.5.7, Spring Web e Spring Data JPA
- Hibernate ORM com dialeto SQLite
- SQLite + SQLite JDBC
- Thymeleaf, HTML5, CSS3
- BCrypt para hash de senha

## Como executar no Windows pelo VS Code

1. Instale o **JDK 21** e as extensões **Extension Pack for Java** e **Spring Boot Extension Pack** no VS Code.
2. Extraia o ZIP e abra a pasta `cozinha-em-dia` no VS Code (a pasta que contém `pom.xml`).
3. Aguarde a extensão Java importar o projeto e resolver as dependências. Na primeira abertura, será necessária conexão à internet para baixá-las.
4. Abra `src/main/java/br/com/cozinhaemdia/CozinhaEmDiaApplication.java` e clique em **Run** acima do método `main`. Não é necessário instalar o Maven globalmente para usar o botão Run da extensão Java.
5. Quando o terminal mostrar `Started CozinhaEmDiaApplication`, abra `http://localhost:8081` no navegador. Para parar o servidor, clique no botão de parar na área de execução ou pressione `Ctrl+C` no terminal.

A aplicação está configurada para a porta **8081**, pois a porta 8080 estava ocupada no ambiente de teste do usuário. Caso a porta 8081 também esteja ocupada, altere `server.port` em `src/main/resources/application.properties`.

### Conta demonstrativa

- **E-mail:** `demo@cozinha.local`
- **Senha:** `demo123`

Também é possível criar uma conta própria pela tela de cadastro. As receitas demonstrativas ficam vinculadas somente à conta de demonstração.

## Visualizar o banco de dados

O arquivo já criado e preenchido está em:

```text
data/receitas_despensa.db
```

Estamos usando o **DB Browser for SQLite** para visualizar o banco. Abra o programa e selecione `Open Database` para abrir esse arquivo. Na aba **Browse Data**, escolha as tabelas `usuarios`, `receitas`, `ingredientes`, `receita_ingredientes` ou `despensa`. A aba **Database Structure** mostra as tabelas e suas colunas.

Os scripts de referência ficam em `database/schema.sql` e `database/sample_data.sql`. Se for executar os `INSERT`s do arquivo de dados novamente, faça isso em um banco vazio, porque as chaves são fixas para o conjunto inicial.

O caminho do banco é relativo à pasta de execução do projeto (`./data/receitas_despensa.db`). Abra e execute o projeto a partir da raiz `cozinha-em-dia` para que o arquivo de banco fornecido seja encontrado.

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
  config/       configuração web, proteção de sessão e dados iniciais
  controller/   rotas e preparação dos dados para as telas
  model/        entidades JPA
  repository/   interfaces de persistência
  service/      regras de receita, despensa e disponibilidade
src/main/resources/
  templates/    páginas Thymeleaf
  static/css/   estilo da interface
  application.properties
```

## Limites desta primeira versão

- Não inclui geração de receitas por IA, integração com supermercados, informações nutricionais, controle de validade, avaliações ou serviços externos, alinhado ao escopo definido para o trabalho.
- A interface é um protótipo funcional, não uma versão final de produção.
- Para uma entrega de produção, seria necessário complementar validação, testes automatizados, proteção CSRF, mensagens de erro mais completas e conversão de unidades.

**Visualizador gratuito para Windows:** [DB Browser for SQLite — página oficial](https://sqlitebrowser.org/dl/#windows). Abra o arquivo `data/receitas_despensa.db` usando `Open Database`.
