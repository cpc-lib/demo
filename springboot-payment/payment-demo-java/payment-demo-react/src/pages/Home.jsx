import { useEffect, useState } from 'react'
import { Button, Empty, message, Skeleton } from 'antd'
import { Navigate, useNavigate } from 'react-router-dom'

import productApi from '@/api/product'
import cartApi from '@/api/cart'
import { useAuth } from '@/auth/AuthContext'

export default function Home() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [addingId, setAddingId] = useState(null)
  const [products, setProducts] = useState([])

  useEffect(() => {
    if (auth.user?.role === 'ADMIN') {
      return
    }
    productApi.list().then((response) => {
      setProducts(response?.data?.productList || [])
    }).catch(() => {
      setProducts([])
    }).finally(() => setLoading(false))
  }, [auth.user?.role])

  const addToCart = async (product) => {
    if (!auth.user) {
      navigate('/login', { state: { from: '/' } })
      return
    }
    setAddingId(product.id)
    try {
      await cartApi.add(product.id, 1)
      await auth.refreshCartCount()
      message.success(`已将“${product.title}”加入购物车`)
    } finally {
      setAddingId(null)
    }
  }

  if (auth.user?.role === 'ADMIN') {
    return <Navigate to="/orders" replace />
  }

  return (
    <main className="catalog-page">
      <section className="catalog-intro container">
        <div>
          <span className="catalog-kicker">课程目录</span>
          <h1>一次选择，多门课程一起结算</h1>
          <p>课程可重复添加并在购物车调整份数。结算价格以创建订单时的最新价格为准。</p>
        </div>
        <div className="catalog-summary" aria-label="购买流程">
          <strong>选择课程</strong>
          <span>加入服务端购物车</span>
          <span>合并创建一笔订单</span>
        </div>
      </section>

      <section className="container catalog-content" aria-busy={loading}>
        {loading ? (
          <div className="course-grid"><Skeleton active /><Skeleton active /></div>
        ) : products.length ? (
          <div className="course-grid">
            {products.map((product, index) => (
              <article className={`course-card course-card-${index % 2}`} key={product.id}>
                <div className="course-index">{String(index + 1).padStart(2, '0')}</div>
                <div>
                  <h2>{product.title}</h2>
                  <p>购买后可在订单中查看本次课程组合与数量快照。</p>
                </div>
                <div className="course-buy-row">
                  <strong>¥{(product.price / 100).toFixed(2)}</strong>
                  <Button type="primary" loading={addingId === product.id} onClick={() => addToCart(product)}>
                    加入购物车
                  </Button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="surface empty-surface"><Empty description="暂无可购买课程" /></div>
        )}
      </section>
    </main>
  )
}
