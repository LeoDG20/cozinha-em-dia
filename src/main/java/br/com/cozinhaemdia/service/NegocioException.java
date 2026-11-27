package br.com.cozinhaemdia.service;

/**
 * Violação de uma regra de negócio. Além do texto padrão em português (usado em logs e nos testes de
 * serviço), carrega o código da mensagem traduzível e os argumentos que a preenchem.
 */
public class NegocioException extends IllegalArgumentException {
    private final String codigo;
    private final transient Object[] argumentos;

    public NegocioException(String codigo, String mensagemPadrao, Object... argumentos) {
        super(mensagemPadrao);
        this.codigo = codigo;
        this.argumentos = argumentos;
    }

    public String getCodigo() { return codigo; }
    public Object[] getArgumentos() { return argumentos; }
}
