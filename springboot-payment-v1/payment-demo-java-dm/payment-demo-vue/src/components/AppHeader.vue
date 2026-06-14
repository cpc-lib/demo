<template>
  <!-- 公共头 -->
  <header id="header">
    <section class="container">
      <h1 id="logo">
        <a href="#/" title="苏三的开发日记">
          <img src="../assets/img/logo.png" width="100%" alt="苏三的开发日记">
        </a>
      </h1>
      <div>
        <ul class="nav">
          <router-link to="/" tag="li" active-class="current" exact>
            <a>购买课程</a>
          </router-link>
          <router-link to="/orders" tag="li" active-class="current">
            <a>我的订单</a>
          </router-link>
          <router-link to="/download" tag="li" active-class="current">
            <a>下载账单</a>
          </router-link>
          <router-link to="/payment-config" tag="li" active-class="current">
            <a>支付配置</a>
          </router-link>
        </ul>
      </div>
      <div class="clear"></div>
    </section>
  </header>
</template>
