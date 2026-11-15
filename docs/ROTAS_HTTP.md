# Rotas HTTP do protótipo

| Método | Rota | Objetivo |
|---|---|---|
| `GET` | `/` | Painel inicial |
| `GET` | `/login` | Tela de login |
| `POST` | `/login` | Autenticar usuário |
| `GET` | `/cadastro` | Tela de criação de conta |
| `POST` | `/cadastro` | Criar conta |
| `POST` | `/logout` | Encerrar sessão |
| `GET` | `/receitas` | Listar receitas do usuário |
| `GET` | `/receitas/nova` | Exibir formulário de cadastro |
| `POST` | `/receitas` | Cadastrar receita e ingredientes |
| `GET` | `/receitas/{id}` | Ver detalhes de uma receita |
| `GET` | `/receitas/{id}/editar` | Exibir formulário de edição |
| `POST` | `/receitas/{id}/editar` | Salvar edição |
| `POST` | `/receitas/{id}/excluir` | Excluir receita |
| `GET` | `/despensa` | Consultar ingredientes do usuário |
| `POST` | `/despensa` | Adicionar ou somar um ingrediente (converte unidades compatíveis) |
| `POST` | `/despensa/editar` | Alterar a quantidade e a unidade de um item |
| `POST` | `/despensa/remover` | Remover item da despensa |
| `GET` | `/o-que-posso-fazer` | Comparar estoque e ingredientes das receitas |

As rotas privadas exigem usuário autenticado pelo **Spring Security** (`SecurityConfig`); quem não está autenticado é redirecionado para `/login`. `POST /login` e `POST /logout` são tratados pelo Spring Security (campos `email` e `senha`), e todo `POST` exige o token CSRF, inserido automaticamente pelo Thymeleaf nos formulários.
