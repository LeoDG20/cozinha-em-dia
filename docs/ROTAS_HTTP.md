# Rotas HTTP do protótipo

| Método | Rota | Objetivo |
|---|---|---|
| `GET` | `/` | Painel inicial |
| `GET` | `/login` | Tela de login |
| `POST` | `/login` | Autenticar usuário |
| `GET` | `/cadastro` | Tela de criação de conta |
| `POST` | `/cadastro` | Criar conta |
| `POST` | `/logout` | Encerrar sessão |
| `GET` | `/receitas` | Listar receitas do usuário, com busca (`q`) e filtro de disponibilidade (`situacao`) |
| `GET` | `/receitas/nova` | Exibir formulário de cadastro |
| `POST` | `/receitas` | Cadastrar receita, ingredientes, etapas, imagem e vídeos (`multipart/form-data`) |
| `GET` | `/receitas/{id}` | Ver detalhes de uma receita |
| `GET` | `/receitas/{id}/editar` | Exibir formulário de edição |
| `POST` | `/receitas/{id}/editar` | Salvar edição |
| `POST` | `/receitas/{id}/excluir` | Excluir receita e os arquivos de mídia dela |
| `POST` | `/receitas/{id}/compartilhar` | Compartilhar a receita com outro usuário, pelo e-mail |
| `POST` | `/receitas/{id}/salvar-copia` | Salvar, na conta do destinatário, uma cópia da receita compartilhada |
| `GET` | `/compartilhadas` | Listar receitas compartilhadas com o usuário |
| `POST` | `/compartilhamentos/{id}/remover` | Desfazer um compartilhamento (destinatário ou dono) |
| `GET` | `/midia/{arquivo}` | Entregar imagem ou vídeo enviado (exige login e aceita requisições parciais) |
| `GET` | `/api/ingredientes/corrigir?nome=` | Sugerir a grafia correta de um ingrediente (JSON) |
| `GET` | `/api/ingredientes/sugestoes?q=` | Sugerir ingredientes conhecidos para autocompletar (JSON) |
| `GET` | `/despensa` | Consultar ingredientes do usuário |
| `POST` | `/despensa` | Adicionar ou somar um ingrediente (converte unidades compatíveis) |
| `POST` | `/despensa/editar` | Alterar a quantidade e a unidade de um item |
| `POST` | `/despensa/remover` | Remover item da despensa |
| `GET` | `/o-que-posso-fazer` | Comparar estoque e ingredientes das receitas |

As rotas privadas exigem usuário autenticado pelo **Spring Security** (`SecurityConfig`); quem não está autenticado é redirecionado para `/login`. `POST /login` e `POST /logout` são tratados pelo Spring Security (campos `email` e `senha`), e todo `POST` exige o token CSRF, inserido automaticamente pelo Thymeleaf nos formulários.
