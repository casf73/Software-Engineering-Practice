/* 在线购物系统第四轮产品原型 · 交互脚本
   只做原型演示所需的轻量交互：弹窗、页签、勾选合计、定制预览。
   不发起任何网络请求，也不依赖任何框架。 */
(function () {
  'use strict';

  /* ---------- 弹窗 ---------- */
  document.addEventListener('click', function (event) {
    var opener = event.target.closest('[data-dialog-open]');
    if (opener) {
      event.preventDefault();
      if (opener.classList.contains('is-disabled') || opener.hasAttribute('disabled')) return;
      var dialog = document.getElementById(opener.getAttribute('data-dialog-open'));
      if (dialog) {
        dialog.classList.add('is-open');
      }
      return;
    }
    var closer = event.target.closest('[data-dialog-close]');
    if (closer) {
      var overlay = closer.closest('.dialog-overlay');
      if (overlay) overlay.classList.remove('is-open');
      return;
    }
    if (event.target.classList.contains('dialog-overlay')) {
      event.target.classList.remove('is-open');
    }
  });

  document.addEventListener('keydown', function (event) {
    if (event.key !== 'Escape') return;
    document.querySelectorAll('.dialog-overlay.is-open').forEach(function (node) {
      node.classList.remove('is-open');
    });
  });

  /* ---------- 页签 ---------- */
  function activateTab(tabs, name) {
    tabs.querySelectorAll('[data-tab]').forEach(function (node) {
      node.classList.toggle('is-active', node.getAttribute('data-tab') === name);
    });
    var root = tabs.parentElement;
    root.querySelectorAll('[data-panel]').forEach(function (panel) {
      panel.classList.toggle('is-active', panel.getAttribute('data-panel') === name);
    });
  }

  document.querySelectorAll('[data-tabs]').forEach(function (group) {
    group.addEventListener('click', function (event) {
      var tab = event.target.closest('[data-tab]');
      if (!tab) return;
      activateTab(group, tab.getAttribute('data-tab'));
    });
  });

  document.querySelectorAll('[data-goto-tab]').forEach(function (link) {
    link.addEventListener('click', function (event) {
      var group = document.querySelector('[data-tabs]');
      if (!group) return;
      event.preventDefault();
      activateTab(group, link.getAttribute('data-goto-tab'));
    });
  });

  /* ---------- 商品卡片选中态 ---------- */
  document.querySelectorAll('.catalog-item').forEach(function (item) {
    item.addEventListener('click', function () {
      document.querySelectorAll('.catalog-item.is-selected').forEach(function (node) {
        node.classList.remove('is-selected');
      });
      item.classList.add('is-selected');
    });
  });

  /* ---------- 购物车勾选合计 ---------- */
  var cartChecks = document.querySelectorAll('.js-select');
  function refreshCart() {
    var total = 0;
    var count = 0;
    cartChecks.forEach(function (box) {
      if (!box.checked) return;
      count += 1;
      total += Number(box.getAttribute('data-price') || 0) * Number(box.getAttribute('data-quantity') || 1);
    });
    var text = document.querySelector('.js-cart-summary');
    if (text) {
      text.textContent = '已选 ' + count + ' 种 · ¥' + total.toFixed(2);
    }
    var checkout = document.querySelector('.js-checkout');
    if (checkout) {
      checkout.classList.toggle('is-disabled', count === 0);
    }
  }
  cartChecks.forEach(function (box) { box.addEventListener('change', refreshCart); });
  if (cartChecks.length) refreshCart();

  /* ---------- 定制工作台实时预览 ---------- */
  var studio = document.getElementById('designStudio');
  if (studio) {
    var templates = { classic: '经典留白', modern: '现代简约', craft: '手作纸纹' };
    var motifs = { none: '', heart: '♥', star: '★', leaf: '❧' };
    var motifNames = { none: '无', heart: '爱心', star: '星星', leaf: '枝叶' };
    var colors = { ink: '墨蓝', rose: '玫瑰红', forest: '森林绿' };
    var preview = document.getElementById('giftPreview');
    var previewText = document.getElementById('giftText');
    var previewMotif = document.getElementById('giftMotif');
    var summary = document.getElementById('designSummary');
    var confirmButton = document.getElementById('confirmDesign');

    function current() {
      return {
        template: studio.querySelector('[name=template]').value,
        text: studio.querySelector('[name=text]').value,
        motif: studio.querySelector('[name=motif]').value,
        color: studio.querySelector('[name=color]').value
      };
    }

    function render() {
      var design = current();
      preview.className = 'gift ' + design.template + ' ' + design.color;
      previewMotif.textContent = motifs[design.motif] || '';
      previewMotif.hidden = !motifs[design.motif];
      previewText.textContent = design.text || (design.motif === 'none' ? '在此预览定制文字' : '');
      previewText.style.opacity = design.text ? '1' : '.45';
      if (summary) {
        summary.innerHTML = '<span>模板 <b>' + templates[design.template] + '</b></span>' +
          '<span>文字 <b>' + (design.text || '未填写') + '</b></span>' +
          '<span>图案 <b>' + (design.motif === 'none' ? '无' : motifNames[design.motif] + ' ' + motifs[design.motif]) + '</b></span>' +
          '<span>配色 <b>' + colors[design.color] + '</b></span>';
      }
      var valid = design.text.trim() || design.motif !== 'none';
      if (confirmButton) confirmButton.classList.toggle('is-disabled', !valid);
    }

    studio.addEventListener('input', render);
    studio.addEventListener('change', render);
    var fill = document.querySelector('[data-fill-sample]');
    if (fill) {
      fill.addEventListener('click', function () {
        studio.querySelector('[name=template]').value = 'craft';
        studio.querySelector('[name=text]').value = '毕业快乐';
        studio.querySelector('[name=motif]').value = 'leaf';
        studio.querySelector('[name=color]').value = 'forest';
        render();
      });
    }
    render();
  }
})();
