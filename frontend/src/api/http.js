import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
  timeout: 10000,
})

export function apiErrorMessage(error, fallback = '请求失败，请稍后重试') {
  return error?.response?.data?.message || error?.message || fallback
}

export function assetUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return path.startsWith('/') ? path : '/' + path
}

export default http

