// 预览和展示都使用相同的保守白名单；服务器仍会独立清理输入。
export function safeDescription(value = '') {
  const parsed = new DOMParser().parseFromString(value, 'text/html')
  const allowed = new Set(['P','BR','STRONG','B','EM','I','U','UL','OL','LI','H2','H3','BLOCKQUOTE','DIV'])
  function clean(node) {
    if (node.nodeType === 3) return document.createTextNode(node.textContent)
    const fragment = document.createDocumentFragment()
    if (node.nodeType !== 1) return fragment
    if (['SCRIPT','STYLE','IFRAME','OBJECT','SVG','MATH'].includes(node.tagName)) return fragment
    if (node.tagName === 'IMG' && /^\/uploads\/[a-zA-Z0-9_-]+\.(png|jpg)$/.test(node.getAttribute('src') || '')) {
      const image = document.createElement('img'); image.src = node.getAttribute('src'); image.alt = '商品描述图片'; return image
    }
    const target = allowed.has(node.tagName) ? document.createElement(node.tagName.toLowerCase()) : fragment
    for (const child of node.childNodes) target.append(clean(child))
    return target
  }
  const result = document.createElement('div')
  for (const child of parsed.body.childNodes) result.append(clean(child))
  return result.innerHTML
}
