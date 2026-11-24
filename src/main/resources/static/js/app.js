(function () {
  'use strict';

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

  // Prévia de arquivos escolhidos (imagem de capa e fotos das etapas).
  function previa(input) {
    var alvo = document.getElementById(input.getAttribute('data-previa'));
    if (!alvo) return;
    var f = input.files && input.files[0];
    if (!f || f.type.indexOf('image/') !== 0) { return; }
    alvo.src = URL.createObjectURL(f);
    alvo.hidden = false;
  }
  document.addEventListener('change', function (e) {
    if (e.target.matches && e.target.matches('input[type=file][data-previa]')) previa(e.target);
    if (e.target.matches && e.target.matches('input[type=file][data-limite]')) {
      var f = e.target.files && e.target.files[0];
      var limite = parseInt(e.target.getAttribute('data-limite'), 10);
      if (f && f.size > limite) {
        window.alert('O arquivo "' + f.name + '" tem mais de ' + Math.round(limite / 1048576) + ' MB e não poderá ser enviado.');
        e.target.value = '';
      }
    }
  });

  // Linhas dinâmicas do formulário de receita.
  var contador = Date.now();
  function adicionar(botao) {
    var lista = document.getElementById(botao.getAttribute('data-lista'));
    var modelo = document.getElementById(botao.getAttribute('data-modelo'));
    if (!lista || !modelo) return;
    var html = modelo.innerHTML.replace(/__CHAVE__/g, String(++contador));
    var caixa = document.createElement('div');
    caixa.innerHTML = html.trim();
    var linha = caixa.firstElementChild;
    lista.appendChild(linha);
    numerar(lista);
    var primeiro = linha.querySelector('input[type=text],textarea');
    if (primeiro) primeiro.focus();
    ligarCorrecao(linha);
  }
  function numerar(lista) {
    lista.querySelectorAll('.num').forEach(function (n, i) { n.textContent = i + 1; });
  }
  document.addEventListener('click', function (e) {
    var add = e.target.closest && e.target.closest('[data-adicionar]');
    if (add) { adicionar(add); return; }
    var rem = e.target.closest && e.target.closest('[data-remover-linha]');
    if (rem) {
      var linha = rem.closest('[data-linha]');
      var lista = linha.parentElement;
      if (lista.querySelectorAll('[data-linha]').length > 1) { lista.removeChild(linha); numerar(lista); }
      else { linha.querySelectorAll('input[type=text],input[type=number],textarea').forEach(function (c) { c.value = ''; }); }
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

  // Correção automática do nome do ingrediente ao sair do campo.
  function ligarCorrecao(raiz) {
    raiz.querySelectorAll('input.js-ingrediente').forEach(function (campo) {
      if (campo.getAttribute('data-ligado')) return;
      campo.setAttribute('data-ligado', '1');
      campo.addEventListener('input', function () {
        var dica = campo.parentElement.querySelector('.dica-correcao');
        if (dica) dica.remove();
      });
      campo.addEventListener('blur', function () {
        var valor = campo.value.trim();
        if (valor.length < 3) return;
        fetch('/api/ingredientes/corrigir?nome=' + encodeURIComponent(valor), { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
          .then(function (r) { return r.ok ? r.json() : null; })
          .then(function (c) {
            if (!c || !c.alterado || campo.value.trim() !== valor) return;
            campo.value = c.corrigido;
            var antiga = campo.parentElement.querySelector('.dica-correcao');
            if (antiga) antiga.remove();
            var dica = document.createElement('span');
            dica.className = 'dica-correcao';
            dica.textContent = 'Corrigido de "' + c.original + '" para "' + c.corrigido + '".';
            campo.parentElement.appendChild(dica);
          })
          .catch(function () { /* a correção também é feita no servidor ao salvar */ });
      });
    });
  }
  ligarCorrecao(document);
})();
