<template>
  <van-cell-group inset>
    <van-cell title="用户名" is-link="/user/edit" :value="user.username"/>
    <van-cell title="用户账户" :value="user.userAccount"/>
    <van-cell title="用户头像" is-link="/user/edit">
      <van-image :src="user.avatarUrl" width="50" height="50"/>
    </van-cell>
    <van-cell title="性别" to="/user/edit" :value="user.gender" @click="toEdit('gender', '性别', user.gender)"/>
    <van-cell title="电话" is-link="/user/edit" :value="user.phone"/>
    <van-cell title="邮箱" is-link="/user/edit" :value="user.email"/>
    <van-cell title="编号" :value="user.planetCode"/>
    <van-cell title="创建日期" :value="user.createTime"/>
  </van-cell-group>
</template>

<script setup lang="ts">
import {useRouter} from "vue-router";

const router = useRouter();

//todo 路由跳转需要手动刷新才能渲染

const user = {
  id: 1,
  username: '东东侠',
  userAccount: 'ddx',
  avatarUrl: 'https://p1.itc.cn/q_70/images03/20230427/97e4cf398c1c453f98f8135b202479d6.jpeg',
  gender: '男',
  phone: '123112312',
  email: '12345@qq.com',
  planetCode: '1234',
  createTime: new Date(),
}


const toEdit = (editKey: string, editName: string, currentValue: string) => {
  router.push({
    path: '/user/edit',
    query: {
      editKey,
      editName,
      currentValue,
    }
  })
}


</script>

<style scoped>

</style>