-- Dados demonstrativos do protótipo. A senha do usuário demo é demo123.
INSERT INTO usuarios (id, nome, email, senha) VALUES
(1, 'Conta demonstração', 'demo@cozinha.local', '$2a$10$3JBRwgwdrXcvztq4RQeCaeBLsyPf7dMHDrm9hcXSk/VbTgGi9f2Ma');

INSERT INTO ingredientes (id, nome) VALUES
(1, 'Ovo'), (2, 'Queijo'), (3, 'Azeite'), (4, 'Sal'), (5, 'Macarrão'), (6, 'Alho'),
(7, 'Arroz'), (8, 'Frango'), (9, 'Cebola'), (10, 'Leite'), (11, 'Batata'), (12, 'Tomate'),
(13, 'Banana'), (14, 'Farinha de trigo'), (15, 'Aveia'), (16, 'Fermento químico'),
(17, 'Cenoura'), (18, 'Manteiga'), (19, 'Lentilha'), (20, 'Coentro');

INSERT INTO receitas (id, usuario_id, nome, descricao, modo_preparo, tempo_preparo_min, porcoes) VALUES
(1, 1, 'Omelete de queijo', 'Uma opção rápida para café da manhã ou jantar.', '1. Bata os ovos com uma pitada de sal.' || char(10) || '2. Aqueça o azeite em uma frigideira.' || char(10) || '3. Despeje os ovos, acrescente o queijo e cozinhe até firmar.', 10, 1),
(2, 1, 'Macarrão alho e óleo', 'Clássico simples para o dia a dia.', '1. Cozinhe o macarrão em água com sal.' || char(10) || '2. Doure o alho no azeite sem queimar.' || char(10) || '3. Misture o macarrão escorrido e sirva.', 20, 2),
(3, 1, 'Frango com arroz', 'Uma refeição completa com poucos ingredientes.', '1. Tempere o frango com alho e sal.' || char(10) || '2. Doure a cebola e o frango em um fio de azeite.' || char(10) || '3. Prepare o arroz e sirva junto.', 35, 2),
(4, 1, 'Panqueca simples', 'Massa versátil para recheios doces ou salgados.', '1. Misture a farinha, o leite e os ovos até ficar homogêneo.' || char(10) || '2. Aqueça uma frigideira untada.' || char(10) || '3. Coloque uma concha de massa, doure dos dois lados e recheie.', 25, 2),
(5, 1, 'Bolo de banana e aveia', 'Uma receita para aproveitar bananas maduras.', '1. Amasse as bananas e misture os ovos e o leite.' || char(10) || '2. Incorpore a aveia e o fermento.' || char(10) || '3. Leve ao forno preaquecido a 180 °C até dourar.', 35, 4),
(6, 1, 'Salada de lentilha', 'Uma opção fresca e simples para variar o cardápio.', '1. Cozinhe a lentilha até ficar macia.' || char(10) || '2. Misture com a cenoura ralada e o coentro picado.' || char(10) || '3. Tempere a gosto e sirva fria.', 30, 2);

INSERT INTO receita_ingredientes (id, receita_id, ingrediente_id, quantidade, unidade) VALUES
(1,1,1,3,'unidade'), (2,1,2,40,'g'), (3,1,3,5,'ml'), (4,1,4,2,'g'),
(5,2,5,200,'g'), (6,2,6,2,'dente'), (7,2,3,20,'ml'), (8,2,4,4,'g'),
(9,3,8,350,'g'), (10,3,7,180,'g'), (11,3,6,1,'dente'), (12,3,9,1,'unidade'), (13,3,3,10,'ml'), (14,3,4,3,'g'),
(15,4,14,150,'g'), (16,4,10,250,'ml'), (17,4,1,2,'unidade'), (18,4,4,2,'g'),
(19,5,13,3,'unidade'), (20,5,15,120,'g'), (21,5,1,2,'unidade'), (22,5,10,100,'ml'), (23,5,16,1,'colher'),
(24,6,19,200,'g'), (25,6,17,1,'unidade'), (26,6,20,10,'g');

INSERT INTO despensa (id, usuario_id, ingrediente_id, quantidade, unidade) VALUES
(1,1,1,6,'unidade'), (2,1,2,200,'g'), (3,1,3,100,'ml'), (4,1,4,200,'g'),
(5,1,5,500,'g'), (6,1,6,6,'dente'), (7,1,7,1000,'g'), (8,1,8,250,'g'),
(9,1,9,2,'unidade'), (10,1,10,500,'ml'), (11,1,11,4,'unidade'), (12,1,12,3,'unidade'), (13,1,13,1,'unidade');
