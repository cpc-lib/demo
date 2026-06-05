export default function AppFooter() {
  return (
    <footer id="footer">
      <section className="container">
        <div>
          <h4 className="hLh30">
            <span className="fsize18 f-fM c-999">友情链接</span>
          </h4>
          <ul className="of flink-list">
            <li>
              <a href="http://www.atguigu.com" title="尚硅谷" target="_blank" rel="noreferrer">尚硅谷</a>
            </li>
          </ul>
          <div className="clear" />
        </div>
        <div className="b-foot">
          <section className="fl col-7">
            <section className="mr20">
              <section className="b-f-link">
                <a href="#" title="关于我们" target="_blank" rel="noreferrer">关于我们</a>|
                <a href="#" title="联系我们" target="_blank" rel="noreferrer">联系我们</a>|
                <a href="#" title="帮助中心" target="_blank" rel="noreferrer">帮助中心</a>|
                <a href="#" title="资源下载" target="_blank" rel="noreferrer">资源下载</a>|
                <span>服务热线：010-56253825(北京) 0755-85293825(深圳)</span>
                <span>Email：info@atguigu.com</span>
              </section>
              <section className="b-f-link mt10">
                <span>©2018课程版权均归谷粒学院所有 京ICP备17055252号</span>
              </section>
            </section>
          </section>
          <div className="clear" />
        </div>
      </section>
    </footer>
  )
}
