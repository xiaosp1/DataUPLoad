import http from './http'

export function login(payload) {
  return http.post('/web/auth/login', payload)
}
