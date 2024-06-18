<template>
  <form action="/">
    <van-search
        v-model="searchText"
        show-action
        placeholder="请输入要搜索的标签"
        @search="onSearch"
        @cancel="onCancel"
    />
  </form>

  <van-tag :show="show" v-for="tag in activeIds" closeable size="medium" type="primary" @close="doClose(tag)">
    {{ tag }}
  </van-tag>
  <van-tree-select
      v-model:active-id="activeIds"
      v-model:main-active-index="activeIndex"
      :items="tagList"
  />

</template>

<script setup>
import {ref} from 'vue';
const searchText = ref('');
const originTagList = [
  {
    text: '性别',
    children: [
      {text: '男', id: '男'},
      {text: '女', id: '女'},
    ],
  },
  {
    text: '学历',
    children: [
      {text: '大专', id: '大专'},
      {text: '本科', id: '本科'},
      {text: '高中', id: '高中'},
    ],
  },
];

// 标签列表
let tagList = ref(originTagList);


/**
 * 搜索过滤
 * @param val
 */
const onSearch = (val) => {
  tagList.value = originTagList.map(parentTag => {
    const tempChildren = [...parentTag.children];
    const tempParentTag = {...parentTag};
    tempParentTag.children = tempChildren.filter(item => item.text.includes(searchText.value));
    return tempParentTag;
  });

}

//todo 搜索所有的标签而不是点哪搜哪

const onCancel = () => {
  searchText.value = '';
  tagList.value = originTagList;
};

const show = ref(true);



// 移除标签
const doClose = (tag) => {
  activeIds.value = activeIds.value.filter(items => {
    return items !== tag;
  })
}

const activeIds = ref([]);
const activeIndex = ref(0);


</script>

<style scoped>

</style>