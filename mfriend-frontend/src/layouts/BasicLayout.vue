<template>
  <van-nav-bar
      title="标题"
      left-text="返回"
      right-text="按钮"
      @click-left="onClickLeft"
      @click-right="onClickRight"
  >
    <template #right>
      <van-icon name="search" size="18"/>
    </template>
  </van-nav-bar>

  <div id="content">
    <router-view/>
  </div>

  <van-tabbar >
    <van-tabbar-item  to="/" icon="home-o">主页</van-tabbar-item>
    <van-tabbar-item  to="/team" icon="search">队伍</van-tabbar-item>
    <van-tabbar-item  to="/user" icon="user">个人</van-tabbar-item>
  </van-tabbar>

</template>

<script setup>

import {useRouter} from "vue-router";
// import routes from '../config/route'
const router = useRouter();

const onClickLeft = () => {
  router.back();
};

const onClickRight = () => {
  router.push('/search')
};

</script>
<style scoped>

</style>