<template>
  <header class="site-header">
    <div class="header-inner">
      <RouterLink class="brand" to="/" aria-label="返回商品首页">
        <span class="brand-mark">拾</span>
        <span class="brand-copy"><strong>拾物</strong><small>单卖家 · 先到先得</small></span>
      </RouterLink>
      <nav class="header-nav" aria-label="主导航">
        <RouterLink to="/" class="nav-link">逛商品</RouterLink>
        <RouterLink to="/seller/login" class="nav-link nav-seller">卖家后台</RouterLink>
      </nav>
    </div>
  </header>
</template>

