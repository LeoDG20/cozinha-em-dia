(function () {
  'use strict';

  function normalizar(texto) {
    return (texto || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/\s+/g, ' ').trim();
  }

  // Destaca no menu a página atual.
  var caminho = location.pathname.replace(/\/$/, '') || '/';
  document.querySelectorAll('.main-nav a').forEach(function (a) {
    var alvo = (a.getAttribute('href') || '').replace(/\/$/, '') || '/';
    if (alvo === caminho || (alvo !== '/' && caminho.indexOf(alvo + '/') === 0)) a.classList.add('active');
  });

  // Confirmação antes de ações destrutivas.
  document.addEventListener('submit', function (e) {
    var msg = e.target.getAttribute('data-confirmar');
    if (msg && !window.confirm(msg)) e.preventDefault();
  });

  // Ampliação das fotos das etapas.
  var caixa = document.createElement('div');
  caixa.className = 'lightbox';
  caixa.innerHTML = '<img alt="">';
  document.body.appendChild(caixa);
  caixa.addEventListener('click', function () { caixa.classList.remove('aberto'); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') caixa.classList.remove('aberto'); });
  document.querySelectorAll('img[data-ampliar]').forEach(function (img) {
    img.addEventListener('click', function () {
      caixa.querySelector('img').src = img.src;
      caixa.classList.add('aberto');
    });
  });

  // Prévia de arquivos escolhidos e verificação do tamanho.
  document.addEventListener('change', function (e) {
    var campo = e.target;
    if (!campo.matches || !campo.matches('input[type=file]')) return;
    var f = campo.files && campo.files[0];
    if (campo.hasAttribute('data-limite') && f) {
      var limite = parseInt(campo.getAttribute('data-limite'), 10);
      if (f.size > limite) {
        window.alert('O arquivo "' + f.name + '" tem mais de ' + Math.round(limite / 1048576) + ' MB e não poderá ser enviado.');
        campo.value = '';
        return;
      }
    }
    var alvo = document.getElementById(campo.getAttribute('data-previa') || '');
    if (alvo && f && f.type.indexOf('image/') === 0) { alvo.src = URL.createObjectURL(f); alvo.hidden = false; }
  });

  // Linhas dinâmicas do formulário de receita.
  var contador = Date.now();
  function numerar(lista) {
    lista.querySelectorAll('.num').forEach(function (n, i) { n.textContent = i + 1; });
  }
  document.addEventListener('click', function (e) {
    var add = e.target.closest && e.target.closest('[data-adicionar]');
    if (add) {
      var lista = document.getElementById(add.getAttribute('data-lista'));
      var modelo = document.getElementById(add.getAttribute('data-modelo'));
      if (!lista || !modelo) return;
      var envoltorio = document.createElement('div');
      envoltorio.innerHTML = modelo.innerHTML.replace(/__CHAVE__/g, String(++contador)).trim();
      var linha = envoltorio.firstElementChild;
      lista.appendChild(linha);
      numerar(lista);
      var primeiro = linha.querySelector('input[type=text],textarea');
      if (primeiro) primeiro.focus();
      ligarVerificacao(linha);
      return;
    }
    var rem = e.target.closest && e.target.closest('[data-remover-linha]');
    if (rem) {
      var alvo = rem.closest('[data-linha]');
      var pai = alvo.parentElement;
      if (pai.querySelectorAll('[data-linha]').length > 1) { pai.removeChild(alvo); numerar(pai); }
      else { alvo.querySelectorAll('input[type=text],input[type=number],textarea').forEach(function (c) { c.value = ''; }); }
      return;
    }
    var tirar = e.target.closest && e.target.closest('[data-tirar-foto]');
    if (tirar) {
      var bloco = tirar.closest('[data-linha]');
      var atual = bloco.querySelector('input[type=hidden][name^=etapaFotoAtual_]');
      if (atual) atual.value = '';
      var miniatura = bloco.querySelector('img');
      if (miniatura) miniatura.hidden = true;
      tirar.hidden = true;
    }
  });

  // Verificação do nome do ingrediente: corrige erros evidentes e oferece alternativas quando há dúvida.
  function limparSugestoes(campo) {
    var pai = campo.parentElement;
    pai.querySelectorAll('.dica-correcao, .sugestoes-digitacao').forEach(function (n) { n.remove(); });
  }
  function mostrarAlternativas(campo, rotulo, nomes, original) {
    var area = document.createElement('div');
    area.className = 'sugestoes-digitacao';
    var texto = document.createElement('span');
    texto.textContent = rotulo;
    area.appendChild(texto);
    nomes.forEach(function (nome) {
      var b = document.createElement('button');
      b.type = 'button';
      b.textContent = nome;
      b.addEventListener('click', function () {
        campo.value = nome;
        limparSugestoes(campo);
        var dica = document.createElement('span');
        dica.className = 'dica-correcao';
        dica.textContent = 'Usando "' + nome + '".';
        campo.parentElement.appendChild(dica);
        campo.dispatchEvent(new Event('change', { bubbles: true }));
      });
      area.appendChild(b);
    });
    if (original) {
      var manter = document.createElement('button');
      manter.type = 'button';
      manter.className = 'manter';
      manter.textContent = 'Manter "' + original + '"';
      manter.addEventListener('click', function () { limparSugestoes(campo); });
      area.appendChild(manter);
    }
    campo.parentElement.appendChild(area);
  }
  function ligarVerificacao(raiz) {
    raiz.querySelectorAll('input.js-ingrediente').forEach(function (campo) {
      if (campo.getAttribute('data-ligado')) return;
      campo.setAttribute('data-ligado', '1');
      campo.addEventListener('input', function () { limparSugestoes(campo); });
      campo.addEventListener('blur', function () {
        var valor = campo.value.trim();
        if (valor.length < 2) return;
        fetch('/api/ingredientes/verificar?nome=' + encodeURIComponent(valor), { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
          .then(function (r) { return r.ok ? r.json() : null; })
          .then(function (v) {
            if (!v || campo.value.trim() !== valor) return;
            limparSugestoes(campo);
            if (v.conhecido) { if (v.corrigido && v.corrigido !== valor) campo.value = v.corrigido; return; }
            if (v.alterado) {
              campo.value = v.corrigido;
              var dica = document.createElement('span');
              dica.className = 'dica-correcao';
              dica.textContent = 'Corrigido de "' + v.original + '" para "' + v.corrigido + '".';
              campo.parentElement.appendChild(dica);
              var outras = (v.sugestoes || []).filter(function (n) { return n !== v.corrigido; });
              if (outras.length) mostrarAlternativas(campo, 'Ou então:', outras, null);
            } else if (v.sugestoes && v.sugestoes.length) {
              mostrarAlternativas(campo, 'Você quis dizer:', v.sugestoes, v.original);
            }
          })
          .catch(function () { /* a correção também é feita no servidor ao salvar */ });
      });
    });
  }
  ligarVerificacao(document);

  // Filtro de linhas de uma tabela pelo texto digitado (sem acentos).
  document.querySelectorAll('input[data-filtrar]').forEach(function (campo) {
    var linhas = document.querySelectorAll(campo.getAttribute('data-filtrar'));
    var aviso = document.getElementById(campo.getAttribute('data-sem-resultado') || '');
    campo.addEventListener('input', function () {
      var termo = normalizar(campo.value), visiveis = 0;
      linhas.forEach(function (l) {
        var mostrar = !termo || normalizar(l.getAttribute('data-texto') || l.textContent).indexOf(termo) >= 0;
        l.hidden = !mostrar;
        if (mostrar) visiveis++;
      });
      if (aviso) aviso.hidden = visiveis > 0;
    });
  });

  // Ajuste de porções na página da receita: recalcula as quantidades proporcionalmente.
  document.querySelectorAll('[data-escala]').forEach(function (caixaEscala) {
    var base = parseFloat(caixaEscala.getAttribute('data-base')) || 1;
    var atual = base;
    var saida = caixaEscala.querySelector('output');
    var formato = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });
    function desenhar() {
      saida.textContent = atual;
      document.querySelectorAll('[data-qtd]').forEach(function (el) {
        el.textContent = formato.format(parseFloat(el.getAttribute('data-qtd')) * atual / base);
      });
      var original = caixaEscala.querySelector('[data-original]');
      if (original) original.hidden = atual === base;
    }
    caixaEscala.querySelectorAll('[data-passo]').forEach(function (b) {
      b.addEventListener('click', function () {
        atual = Math.min(99, Math.max(1, atual + parseInt(b.getAttribute('data-passo'), 10)));
        desenhar();
      });
    });
    var voltar = caixaEscala.querySelector('[data-original]');
    if (voltar) voltar.addEventListener('click', function () { atual = base; desenhar(); });
  });

  // Copiar a lista de compras e imprimir.
  document.addEventListener('click', function (e) {
    var copiar = e.target.closest && e.target.closest('[data-copiar]');
    if (copiar) {
      var itens = document.querySelectorAll(copiar.getAttribute('data-copiar') + ' li');
      var texto = Array.prototype.map.call(itens, function (li) { return '- ' + li.getAttribute('data-linha-texto'); }).join('\n');
      var original = copiar.textContent;
      var pronto = function () { copiar.textContent = 'Lista copiada'; setTimeout(function () { copiar.textContent = original; }, 1800); };
      if (navigator.clipboard) navigator.clipboard.writeText(texto).then(pronto);
      return;
    }
    if (e.target.closest && e.target.closest('[data-imprimir]')) window.print();
  });

  // Ao abrir "Adaptar receita", o cursor já fica no campo do nome.
  document.querySelectorAll('details.adaptar').forEach(function (d) {
    d.addEventListener('toggle', function () {
      var campo = d.querySelector('input[name=nome]');
      if (d.open && campo) { campo.focus(); campo.select(); }
    });
  });
})();
