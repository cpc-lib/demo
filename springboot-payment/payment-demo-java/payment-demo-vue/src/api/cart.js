import request from '../utils/request'

export default {
  get() {
    return request.get('/api/cart')
  },
  add(productId, quantity) {
    return request.post('/api/cart/items', { productId, quantity: quantity || 1 })
  },
  update(productId, quantity) {
    return request.put(`/api/cart/items/${productId}`, { quantity })
  },
  remove(productId) {
    return request.delete(`/api/cart/items/${productId}`)
  },
  clear() {
    return request.delete('/api/cart')
  }
}
