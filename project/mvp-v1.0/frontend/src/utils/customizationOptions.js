export const templates = [
  { value: 'classic', label: '经典留白' },
  { value: 'modern', label: '现代简约' },
  { value: 'craft', label: '手作纸纹' },
]
export const templateLabels = Object.fromEntries(templates.map(({ value, label }) => [value, label]))

export const defaultOptions = {
  templates: templates.map(option => option.value),
  motifs: [
    { label: '无', value: 'none' },
    { label: '爱心', value: 'heart' },
    { label: '星星', value: 'star' },
    { label: '枝叶', value: 'leaf' },
  ],
  colors: [
    { label: '墨蓝', value: 'ink' },
    { label: '玫瑰红', value: 'rose' },
    { label: '森林绿', value: 'forest' },
  ],
  allowText: true,
  textHint: '',
}
