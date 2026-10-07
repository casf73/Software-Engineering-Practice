const intentLabels = {
  WAITING: '排队中',
  IN_TRADE: '交易中',
  TRADE_SUCCESS: '交易成功',
  TRADE_FAILED: '交易失败',
  VOIDED: '已作废',
  CANCELLED: '已撤销',
  PRODUCT_DELISTED: '商品已下架',
}

const productLabels = { ON_SALE: '在售', IN_TRADE: '交易中', DELISTED: '商品下架' }

export function intentLabel(status) { return intentLabels[status] || '状态未知' }
export function productLabel(status) { return productLabels[status] || '暂未上架' }

export function intentTagType(status) {
  if (status === 'TRADE_SUCCESS') return 'success'
  if (status === 'IN_TRADE') return 'warning'
  if (['TRADE_FAILED', 'VOIDED', 'CANCELLED', 'PRODUCT_DELISTED'].includes(status)) return 'info'
  return 'primary'
}

export function productTagType(status) {
  if (status === 'ON_SALE') return 'success'
  if (status === 'IN_TRADE') return 'warning'
  return 'info'
}

