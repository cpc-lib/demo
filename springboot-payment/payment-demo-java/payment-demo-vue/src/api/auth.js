import request from '../utils/request'

export default {
  login(data) {
    return request.post('/api/auth/login', data, { skipAuthRefresh: true })
  },
  register(data) {
    return request.post('/api/auth/register', data, { skipAuthRefresh: true })
  },
  logout() {
    return request.post('/api/auth/logout', null, { skipAuthRefresh: true })
  },
  changePassword(data) {
    return request.put('/api/auth/password', data)
  }
}
