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
| `POST` | `/receitas/{id}/compartilhar` | Compartilhar a receita com amigos (parâmetro `amigo`, um ou vários identificadores, e `mensagem` opcional). Recusa quem ainda não é amigo |
| `POST` | `/receitas/{id}/salvar-copia` | Salvar, na conta do destinatário, uma cópia da receita compartilhada |
| `POST` | `/receitas/{id}/adaptar` | Criar uma versão adaptada (cópia completa com o nome informado) e abrir a edição dela. Vale para receitas próprias e compartilhadas |
| `POST` | `/receitas/{id}/comentarios` | Comentar uma receita própria ou compartilhada com o usuário (parâmetro `texto`, até 500 caracteres) |
| `POST` | `/receitas/{id}/comentarios/{comentarioId}/excluir` | Excluir um comentário (o autor ou o dono da receita) |
| `GET` | `/compartilhadas` | Listar receitas compartilhadas com o usuário |
| `POST` | `/compartilhamentos/{id}/remover` | Desfazer um compartilhamento (destinatário ou dono). O parâmetro opcional `receita` faz o dono voltar à página da receita |
| `GET` | `/midia/{arquivo}` | Entregar imagem ou vídeo enviado (exige login e aceita requisições parciais) |
| `GET` | `/api/ingredientes/corrigir?nome=` | Sugerir a grafia correta de um ingrediente (JSON) |
| `GET` | `/api/ingredientes/sugestoes?q=` | Sugerir ingredientes conhecidos para autocompletar (JSON) |
| `GET` | `/api/ingredientes/verificar?nome=` | Informar se o nome é conhecido e, se não for, listar até quatro alternativas por escrita e som (JSON: `conhecido`, `corrigido`, `alterado`, `sugestoes`) |
| `GET` | `/despensa` | Consultar ingredientes do usuário |
| `POST` | `/despensa` | Adicionar ou somar um ingrediente (converte unidades compatíveis) |
| `POST` | `/despensa/editar` | Alterar a quantidade e a unidade de um item |
| `POST` | `/despensa/remover` | Remover item da despensa |
| `GET` | `/o-que-posso-fazer` | Comparar estoque e ingredientes das receitas |
| `GET` | `/mercado` | Lista de mercado: escolher receitas (`receitas`, `f`) e ver o que falta, somado entre elas |
| `POST` | `/mercado/comprar` | Somar à despensa os itens marcados como comprados (`item` no formato `quantidade\|unidade\|nome`) |
| `GET` | `/medidas` | Conversor de medidas (`valor`, `de`, `para` e `ingrediente` opcional) com tabelas de referência |
| `GET` | `/amigos` | Listar amigos, pedidos recebidos e pedidos enviados |
| `POST` | `/amigos` | Enviar pedido de amizade (`email`) |
| `POST` | `/amigos/{id}/aceitar` | Aceitar um pedido recebido |
| `POST` | `/amigos/{id}/recusar` | Recusar um pedido recebido ou cancelar um pedido enviado |
| `POST` | `/amigos/{id}/desfazer` | Desfazer uma amizade |

Qualquer rota aceita o parâmetro `lang` (`pt-BR` ou `en`) para trocar o idioma da interface; a escolha fica no cookie `idioma`, e valores desconhecidos são ignorados.

As rotas privadas exigem usuário autenticado pelo **Spring Security** (`SecurityConfig`); quem não está autenticado é redirecionado para `/login`. `POST /login` e `POST /logout` são tratados pelo Spring Security (campos `email` e `senha`), e todo `POST` exige o token CSRF, inserido automaticamente pelo Thymeleaf nos formulários.
