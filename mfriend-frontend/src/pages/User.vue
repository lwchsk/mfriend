<template>
  <van-cell-group inset>
    <van-cell title="用户名" is-link="/user/edit" :value="user.username" />
    <van-cell title="用户账户" :value="user.userAccount" />
    <van-cell title="用户头像" is-link="/user/edit" :value="user.avatarUrl" />
    <van-cell title="性别" is-link="/user/edit" :value="user.gender" />
    <van-cell title="电话" is-link="/user/edit" :value="user.phone" />
    <van-cell title="邮箱" is-link="/user/edit" :value="user.email" />
    <van-cell title="编号" :value="user.planetCode"/>
    <van-cell title="创建日期" :value="user.createTime" />
  </van-cell-group>
</template>

<script setup>
const user = {
  id: 1,
  username: '鱼皮',
  userAccount: 'dogYupi',
  avatarUrl: 'https://636f-codenav-8grj8px727565176-1256524210.tcb.qcloud.la/img/logo.png',
  gender: '男',
  phone: '123112312',
  email: '12345@qq.com',
  planetCode: '1234',
  createTime: new Date(),
}

</script>

<style scoped>

</style>