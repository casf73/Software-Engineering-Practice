const { createApp, ref, computed } = Vue;
const { ElMessage, ElMessageBox } = ElementPlus;
const sequence = ['PLACED', 'CONFIRMED', 'PREPARED', 'SHIPPING', 'COMPLETED'];
const labels = {
  PLACED: '客户下单', CONFIRMED: '商家确认', PREPARED: '备货完成',
  SHIPPING: '开始发货', COMPLETED: '交易完成', CANCELLED: '已取消',
};
const templates = [
  { id: 'classic', name: '经典留白' }, { id: 'modern', name: '现代简约' },
  { id: 'craft', name: '手作纸纹' },
];
const motifs = [
  { id: 'none', name: '无' }, { id: 'heart', name: '爱心', symbol: '♥' },
  { id: 'star', name: '星星', symbol: '★' }, { id: 'leaf', name: '枝叶', symbol: '❧' },
];
const colors = [
  { id: 'ink', name: '墨蓝' }, { id: 'rose', name: '玫瑰红' }, { id: 'forest', name: '森林绿' },
];

const GiftPreview = {
  props: { design: { type: Object, required: true } },
  setup: () => ({ symbol: id => motifs.find(motif => motif.id === id)?.symbol }),
  template: `<div class="gift" :class="[design.template, design.color]" aria-label="定制效果预览">
    <span v-if="symbol(design.motif)" class="motif">{{ symbol(design.motif) }}</span>
    <span>{{ design.text || (design.motif === 'none' ? '在此预览定制文字' : '') }}</span>
  </div>`,
};
const sampleProducts = () => [
  { id: 1, name: '手工木牌', price: 2000, stock: 3, listed: true, description: '可定制模板、祝福文字和图案。' },
  { id: 2, name: '手作相框', price: 1200, stock: 2, listed: true, description: '浅色木纹相框，适合摆放照片。' },
  { id: 3, name: '编织钥匙扣', price: 800, stock: 1, listed: true, description: '手工编织钥匙扣，演示结算库存边界。' },
  { id: 4, name: '陶瓷书签', price: 1600, stock: 0, listed: false, description: '当前已下架，仍可在收藏中辨认原条目。' },
];
const emptyDesign = () => ({ template: 'classic', text: '', motif: 'none', color: 'ink' });
const nameOf = (options, id) => options.find(option => option.id === id)?.name;
const money = cents => `¥${(cents / 100).toFixed(2)}`;
const availability = product => !product.stock ? '无货 · 已下架' : !product.listed ? '已下架' : `在售 · 库存 ${product.stock}`;
const timeNow = () => new Date().toLocaleString('zh-CN', { hour12: false });
const label = status => labels[status];
const next = order => {
  const index = sequence.indexOf(order.status);
  return index < 0 ? undefined : sequence[index + 1];
};
const designDescription = design =>
  [nameOf(templates, design.template), design.text, nameOf(motifs, design.motif), nameOf(colors, design.color)].filter(Boolean).join(' / ');

createApp({
  components: { GiftPreview },
  setup() {
    const page = ref('shopping');
    const shelf = ref('cart');
    const role = ref('buyer');
    const products = ref(sampleProducts());
    const entries = ref({ 1: 1, 2: 1, 3: 1 });
    const favorites = ref([4]);
    const selected = ref([]);
    const orders = ref([]);
    const productDetail = ref(null);
    const design = ref(emptyDesign());
    const confirmed = ref(null);
    const designOpen = ref(false);
    const artisanOpen = ref(false);
    const customProduct = computed(() => products.value[0]);
    const cart = computed(() => products.value
      .filter(product => Object.hasOwn(entries.value, product.id))
      .map(product => ({ ...product, quantity: entries.value[product.id] })));
    const favoriteRows = computed(() => favorites.value.map(id => products.value.find(product => product.id === id)));
    const selectedRows = computed(() => cart.value.filter(row => selected.value.includes(row.id)));
    const selectedTotal = computed(() => selectedRows.value.reduce((sum, row) => sum + row.price * row.quantity, 0));
    const validDesign = computed(() => Boolean(design.value.text.trim()) || design.value.motif !== 'none');
    let nextNumber = 401;

    function resetDemo() {
      page.value = 'shopping';
      shelf.value = 'cart';
      role.value = 'buyer';
      products.value = sampleProducts();
      entries.value = { 1: 1, 2: 1, 3: 1 };
      favorites.value = [4];
      selected.value = [];
      orders.value = [];
      design.value = emptyDesign();
      confirmed.value = null;
      productDetail.value = null;
      designOpen.value = false;
      artisanOpen.value = false;
      nextNumber = 401;
      ElMessage.success('已恢复演示数据');
    }
    function updateQuantity(id, quantity) {
      if (Number.isInteger(quantity) && quantity > 0) entries.value[id] = quantity;
    }
    function addCart(row) {
      if (!row.listed || !row.stock) return;
      entries.value[row.id] = (entries.value[row.id] || 0) + 1;
      shelf.value = 'cart';
      productDetail.value = null;
      ElMessage.success('已加入购物车');
    }
    function removeCart(id) {
      delete entries.value[id];
      selected.value = selected.value.filter(value => value !== id);
    }
    function favorite(id) {
      if (!favorites.value.includes(id)) favorites.value.push(id);
    }
    function moveToFavorites() {
      const ids = selectedRows.value.map(row => row.id);
      for (const id of ids) {
        favorite(id);
        removeCart(id);
      }
      shelf.value = 'favorites';
      ElMessage.success('所选商品已从购物车移入收藏');
    }
    async function confirm(text, title, confirmButtonText) {
      try {
        await ElMessageBox.confirm(text, title, { confirmButtonText, cancelButtonText: '返回' });
        return true;
      } catch {
        return false;
      }
    }
    function createOrder(lines) {
      const unavailable = lines.find(line => {
        const product = products.value.find(item => item.id === line.id);
        return !product.listed || product.stock < line.quantity;
      });
      if (unavailable) {
        ElMessage.warning(`${unavailable.name} 库存不足，本次未生成订单`);
        return false;
      }
      for (const line of lines) {
        const product = products.value.find(item => item.id === line.id);
        product.stock -= line.quantity;
        if (!product.stock) product.listed = false;
      }
      orders.value.unshift({
        id: `DEMO-${nextNumber++}`, status: 'PLACED', lines,
        total: lines.reduce((sum, line) => sum + line.price * line.quantity, 0),
        events: [{ status: 'PLACED', at: timeNow() }],
      });
      page.value = 'orders';
      role.value = 'buyer';
      return true;
    }
    async function checkout() {
      const rows = selectedRows.value.map(({ id, name, price, quantity }) => ({ id, name, price, quantity }));
      if (!rows.length || !await confirm('所选商品将合并成一笔订单。', '确认结算', '确认下单')) return;
      if (createOrder(rows)) rows.forEach(row => removeCart(row.id));
    }
    function changeStatus(order, status) {
      order.status = status;
      order.events.push({ status, at: timeNow() });
    }
    function advance(order) {
      const status = next(order);
      if (role.value === 'seller' && status) changeStatus(order, status);
    }
    function canCancel(order) {
      if (['COMPLETED', 'CANCELLED'].includes(order.status)) return false;
      return role.value === 'seller' || order.status !== 'SHIPPING';
    }
    async function cancelOrder(order) {
      if (!canCancel(order) || !await confirm('整单取消后，各商品库存按原数量回补。', '确认取消整单', '确认取消')) return;
      if (!canCancel(order)) return;
      for (const line of order.lines) {
        const product = products.value.find(item => item.id === line.id);
        const soldOut = product.stock === 0;
        product.stock += line.quantity;
        if (soldOut) product.listed = true;
      }
      changeStatus(order, 'CANCELLED');
    }
    function applySample() {
      design.value = { template: 'craft', text: '毕业快乐', motif: 'leaf', color: 'forest' };
    }
    function confirmDesign() {
      if (!validDesign.value || !customProduct.value.stock) return;
      confirmed.value = { ...design.value, text: design.value.text.trim() };
      designOpen.value = true;
    }
    function submitCustom() {
      if (!confirmed.value) return;
      const { id, name, price } = customProduct.value;
      if (createOrder([{ id, name, price, quantity: 1, design: confirmed.value, artisan: '木作工坊' }])) {
        designOpen.value = false;
        confirmed.value = null;
      }
    }
    return {
      page, shelf, role, products, cart, favorites, favoriteRows, selected, orders,
      productDetail, design, confirmed, designOpen, artisanOpen, customProduct, validDesign,
      selectedTotal, templates, motifs, colors, money, availability, label, next, designDescription,
      resetDemo, updateQuantity, addCart, removeCart, favorite, moveToFavorites, checkout,
      advance, canCancel, cancelOrder, applySample, confirmDesign, submitCustom,
    };
  },
}).use(ElementPlus).mount('#app');
