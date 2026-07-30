import axios from 'axios'

const http = axios.create({
  baseURL: '/',
  timeout: 15000
})

export default http
