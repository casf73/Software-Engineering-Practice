import { reactive } from 'vue'
import { customerApi } from '../api/customerApi'

export const customerSession = reactive({ loaded: false, customer: null })
let pending
export function setCustomerSession(data) {
  customerSession.customer = data.authenticated ? data.customer : null
  customerSession.loaded = true
}
export function clearCustomerSession() {
  customerSession.customer = null
  customerSession.loaded = true
}
export async function loadCustomerSession(force = false) {
  if (pending) return pending
  if (customerSession.loaded && !force) return customerSession.customer
  pending = customerApi.session().then(({ data }) => {
    setCustomerSession(data)
    return customerSession.customer
  }).catch(error => {
    clearCustomerSession()
    customerSession.loaded = false
    throw error
  }).finally(() => { pending = undefined })
  return pending
}
