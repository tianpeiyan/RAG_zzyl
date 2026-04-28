<!-- 护理信息 -->
<template>
  <!-- 基本信息 -->
  <t-card title="基本信息">
    <div class="info-block">
      <div class="info-item">
        <h1 class="label-wt-long">老人姓名：</h1>
        <span>{{ baseData.elderName ?? '--' }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt-long">护理等级：</h1>
        <span>{{ getText(baseData.lname) }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt-long">性别：</h1>
        <span>{{ getSexText(baseData.sex) }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt-long">年龄：</h1>
        <span>{{ getText(baseData.age) }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt-long">床位号：</h1>
        <span>{{ getText(baseData.bedNumber) }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt-long">护理员姓名：</h1>
        <span>{{
          baseData.nursingName ? baseData.nursingName.join(',') : '--'
        }}</span>
      </div>
      <div class="info-item headImg">
        <span class="img">
          <t-image-viewer
            :images="[
              baseData.image ||
                'https://yjy-slwl-oss.oss-cn-hangzhou.aliyuncs.com/61032c27-43eb-49cc-86df-ba8cbeb2c27d.png'
            ]"
          >
            <template #trigger="{ open }">
              <div class="tdesign-demo-image-viewer__ui-image">
                <img
                  alt="test"
                  :src="
                    baseData.image ||
                    'https://yjy-slwl-oss.oss-cn-hangzhou.aliyuncs.com/61032c27-43eb-49cc-86df-ba8cbeb2c27d.png'
                  "
                  class="tdesign-demo-image-viewer__ui-image--img"
                />
                <div
                  class="tdesign-demo-image-viewer__ui-image--hover"
                  @click="open"
                >
                  <span><ZoomInIcon size="1.8em" /></span>
                </div>
              </div>
            </template>
          </t-image-viewer>
        </span>
      </div>
    </div>
  </t-card>
</template>

<script setup lang="ts">
import { ZoomInIcon } from 'tdesign-icons-vue-next'

const getSexText = (sex: any) => {
  if (sex === undefined || sex === null || sex === '') return '--'
  if (sex === 1 || sex === '1') return '女'
  if (sex === 0 || sex === '0') return '男'
  if (sex === '男' || sex === '女') return sex
  return String(sex)
}

const getText = (val: any) => {
  if (val === undefined || val === null || val === '') return '--'
  return String(val)
}

const getNursingNameText = (nursingName: any) => {
  if (!Array.isArray(nursingName) || nursingName.length === 0) return '--'
  return nursingName.join(',')
}
// 获取父组件值、方法
defineProps({
  // 基本信息数据
  baseData: {
    type: Object,
    default: () => {
      return {}
    }
  }
})
// ------定义变量------
// 触发父级事件
</script>
<style lang="less" scoped>
.info-block {
  position: relative;
  padding-right: 120px;
  .headImg {
    position: absolute;
    top: 0;
    right: 0;
    width: auto;
    // padding-bottom: 12px;
    padding-top: 0;
  }
  :deep(.tdesign-demo-image-viewer__ui-image),
  :deep(.img) {
    width: 90px;
    height: 90px;
  }
  :deep(.tdesign-demo-image-viewer__ui-image img),
  :deep(.tdesign-demo-image-viewer__ui-image--img) {
    width: 90px;
    height: 90px;
  }
}
</style>
