// 创建应用程序的路由器
import Vue from 'vue'
import VueRouter from 'vue-router'
// 此时就可以在Vue实例中配置路由器了
Vue.use(VueRouter)

// 引入组件
import Index from '../views/index'
import Orders from '../views/Orders'
import Download from '../views/Download'
import Success from '../views/Success'
import PaymentConfig from '../views/PaymentConfig'
import Reconciliation from '../views/Reconciliation'
import Refunds from '../views/Refunds'
import Login from '../views/Login'
import Cart from '../views/Cart'
import Account from '../views/Account'
import { authState, refreshSession } from '../auth/session'

const router = new VueRouter({
    routes:[
        {
            path: '/',
            component: Index
        },
        {
            path: '/orders',
            component: Orders,
            meta: { requiresAuth: true }
        },
        {
            path: '/login',
            component: Login
        },
        {
            path: '/cart',
            component: Cart,
            meta: { requiresAuth: true, role: 'USER' }
        },
        {
            path: '/account',
            component: Account,
            meta: { requiresAuth: true }
        },
        {
            path: '/download',
            component: Download,
            meta: { requiresAuth: true, role: 'ADMIN' }
        },
        {
            path: '/payment-config',
            component: PaymentConfig,
            meta: { requiresAuth: true, role: 'ADMIN' }
        },
        {
            path: '/reconciliation',
            component: Reconciliation,
            meta: { requiresAuth: true, role: 'ADMIN' }
        },
        {
            path: '/refunds',
            component: Refunds,
            meta: { requiresAuth: true, role: 'ADMIN' }
        },
        {
            path: '/success',
            component: Success,
            meta: { requiresAuth: true, role: 'USER' }
        }
    ]
})

router.beforeEach(async (to, from, next) => {
  if (!authState.bootstrapped) {
    try {
      await refreshSession()
    } catch (_) {
      // 未登录是正常启动状态，由下面的路由元数据决定是否跳转。
    }
  }

  if (to.meta.requiresAuth && !authState.user) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.path === '/' && authState.user && authState.user.role === 'ADMIN') {
    next('/orders')
    return
  }
  if (to.meta.role && (!authState.user || authState.user.role !== to.meta.role)) {
    next(authState.user && authState.user.role === 'ADMIN' ? '/orders' : '/')
    return
  }
  if (to.path === '/login' && authState.user) {
    next('/')
    return
  }
  next()
})

export default router
