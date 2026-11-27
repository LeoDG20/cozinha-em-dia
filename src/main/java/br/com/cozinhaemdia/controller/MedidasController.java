package br.com.cozinhaemdia.controller;

import br.com.cozinhaemdia.service.ConversorUnidades;
import br.com.cozinhaemdia.service.CorretorIngredientes;
import br.com.cozinhaemdia.service.ReceitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Conversor de medidas: massa, volume e medidas caseiras (xícara e colheres), com densidade de ingredientes comuns. */
@Controller
public class MedidasController {
    private static final List<String> REFERENCIA = List.of("Farinha de trigo", "Açúcar", "Açúcar mascavo", "Arroz", "Aveia",
            "Amido de milho", "Chocolate em pó", "Queijo ralado", "Manteiga", "Óleo", "Leite", "Mel", "Sal");

    public record Equivalencia(String ingrediente, String xicara, String colher) {}
    public record Resultado(boolean ok, String texto, String aviso) {}

    private final CorretorIngredientes corretor;

    @Autowired
    public MedidasController(CorretorIngredientes corretor) { this.corretor = corretor; }

    @GetMapping("/medidas")
    public String medidas(@RequestParam(required = false) String valor, @RequestParam(defaultValue = "xícara") String de,
                          @RequestParam(defaultValue = "g") String para, @RequestParam(required = false) String ingrediente, Model model) {
        model.addAttribute("unidades", ConversorUnidades.UNIDADES_CONVERSIVEIS);
        model.addAttribute("sugestoes", corretor.nomesConhecidos());
        model.addAttribute("valor", valor == null ? "" : valor);
        model.addAttribute("de", de);
        model.addAttribute("para", para);
        model.addAttribute("ingrediente", ingrediente == null ? "" : ingrediente);
        model.addAttribute("resultado", valor == null || valor.isBlank() ? null : converter(valor, de, para, ingrediente));
        model.addAttribute("referencia", referencia());
        return "medidas";
    }

    static Resultado converter(String valorTexto, String de, String para, String ingrediente) {
        double valor;
        try { valor = Double.parseDouble(valorTexto.trim().replace(',', '.')); }
        catch (NumberFormatException ex) { return new Resultado(false, null, "Informe um número, por exemplo 2 ou 1,5."); }
        if (!(valor > 0) || Double.isInfinite(valor)) return new Resultado(false, null, "A quantidade deve ser maior que zero.");

        String nome = ingrediente == null ? "" : ingrediente.trim();
        Optional<Double> convertido = ConversorUnidades.converter(valor, de, para, nome);
        if (convertido.isEmpty()) {
            boolean precisaIngrediente = ConversorUnidades.converter(valor, de, para).isEmpty() && nome.isEmpty();
            return new Resultado(false, null, precisaIngrediente
                    ? "Para passar de peso para volume (ou o contrário), informe o ingrediente: farinha, açúcar, leite, óleo e outros."
                    : "Não há conversão conhecida entre " + de + " e " + para + (nome.isEmpty() ? "." : " para " + nome + "."));
        }
        String aviso = !nome.isEmpty() && ConversorUnidades.converter(valor, de, para).isEmpty()
                ? "Valor aproximado: depende da densidade do ingrediente e de como ele é medido (rasa ou cheia, peneirada ou não)." : null;
        String quantidadeOrigem = ReceitaService.formatar(valor) + " " + de + (nome.isEmpty() ? "" : " de " + nome);
        return new Resultado(true, quantidadeOrigem + " = " + formatarResultado(convertido.get()) + " " + para, aviso);
    }

    private static String formatarResultado(double v) {
        if (v >= 100) return ReceitaService.formatar((double) Math.round(v));
        return ReceitaService.formatar(Math.round(v * 100) / 100.0);
    }

    private static List<Equivalencia> referencia() {
        List<Equivalencia> lista = new ArrayList<>();
        for (String nome : REFERENCIA) {
            double d = ConversorUnidades.densidade(nome).orElse(1.0);
            lista.add(new Equivalencia(nome, formatarResultado(240 * d) + " g", formatarResultado(15 * d) + " g"));
        }
        return lista;
    }
}
