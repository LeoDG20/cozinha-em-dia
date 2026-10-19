PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS usuarios (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  senha TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS receitas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  usuario_id INTEGER NOT NULL,
  nome TEXT NOT NULL,
  descricao TEXT,
  modo_preparo TEXT NOT NULL,
  tempo_preparo_min INTEGER,
  porcoes INTEGER,
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
CREATE TABLE IF NOT EXISTS ingredientes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  nome TEXT NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS receita_ingredientes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  receita_id INTEGER NOT NULL,
  ingrediente_id INTEGER NOT NULL,
  quantidade REAL NOT NULL,
  unidade TEXT NOT NULL,
  UNIQUE (receita_id, ingrediente_id),
  FOREIGN KEY (receita_id) REFERENCES receitas(id),
  FOREIGN KEY (ingrediente_id) REFERENCES ingredientes(id)
);
CREATE TABLE IF NOT EXISTS despensa (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  usuario_id INTEGER NOT NULL,
  ingrediente_id INTEGER NOT NULL,
  quantidade REAL NOT NULL,
  unidade TEXT NOT NULL,
  UNIQUE (usuario_id, ingrediente_id),
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  FOREIGN KEY (ingrediente_id) REFERENCES ingredientes(id)
);
