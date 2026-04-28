<!-- 申请退住-详细信息 -->
<template>
  <!-- 基本信息 -->
  <t-card title="基本信息">
    <div class="info-block">
      <div class="info-item">
        <h1 class="label-wt-s">老人姓名：</h1>
        <span>{{ retreatElder.name }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt">退住日期：</h1>
        <span>{{ getDateInfo(retreatElder.checkOutTime) }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt-s">退住原因：</h1>
        <span>{{ retreatElder.reason }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt">老人身份证号：</h1>
        <span>{{ retreatElder.idCardNo }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt-s">入住期限：</h1>
        <span
          >{{ getDateInfo(retreatElder.checkInStartTime) }} ~
          {{ getDateInfo(retreatElder.checkInEndTime) }}</span
        >
      </div>
      <div class="info-item">
        <h1 class="label-wt">费用期限：</h1>
        <span
          >{{ getDateInfo(retreatElder.costStartTime) }} ~
          {{ getDateInfo(retreatElder.costEndTime) }}</span
        >
      </div>
      <div class="info-item">
        <h1 class="label-wt-s">护理等级：</h1>
        <span>{{ retreatElder.nursingLevelName }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt">入住床位：</h1>
        <span>{{ retreatElder.bedNumber }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt-s">护理员姓名：</h1>
        <span>{{
          retreatElder.nursingName
            ? retreatElder.nursingName
            : '无'
        }}</span>
      </div>
      <div class="info-item">
        <h1 class="label-wt">联系方式：</h1>
        <span>{{ retreatElder.phone }}</span>
      </div>
      <div class="info-item lastText">
        <h1 class="label-wt-s">家庭住址：</h1>
        <span>{{ retreatElder.address }}</span>
      </div>
    </div>
  </t-card>
  <!-- end -->
  <!-- 解除记录 -->
  <t-card title="解除协议">
    <div class="info-block">
      <div class="info-item">
        <h1>解除日期：</h1>
        <span>{{ getDateInfo(baseData.releaseDate) }}</span>
      </div>

      <div class="info-item">
        <h1>解除协议：</h1>
        <span
          >{{ retreatElder.name }}的解除协议.pdf<i class="font-bt"
            ><a class="font-bt" :href="baseData.releasePdfUrl" target="black"
              >查看</a
            ></i
          >
        </span>
      </div>
    </div>
  </t-card>
  <!-- end -->
  <!-- 解除记录 -->
  <t-card v-if="refundVoucher.refundVoucherUrl" title="退款凭证">
    <div class="info-block">
      <div class="info-item">
        <h1>提交人：</h1>
        <span>{{ retreatElder.applicat }}</span>
      </div>
      <div class="info-item">
        <h1>提交时间：</h1>
        <span>{{ refundVoucher.createTime }}</span>
      </div>
      <div class="info-item">
        <h1>退款方式：</h1>
        <span>{{ refundVoucher.tradingChannel }}</span>
      </div>
      <div class="info-item">
        <h1>退款凭证：</h1>
        <span>
          <t-upload
            ref="uploadRef"
            v-model="photoFile"
            action=""
            theme="image"
            :disabled="true"
          ></t-upload>
        </span>
      </div>
      <div class="info-item">
        <h1>退款备注：</h1>
        <span>{{ refundVoucher.remark }}</span>
      </div>
    </div>
  </t-card>
  <!-- end -->
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getDateInfo } from '@/utils/date'

// 获取父组件值、方法
const props = defineProps({
  // 基本信息数据
  baseData: {
    type: Object,
    default: () => {
      return {}
    }
  }
})
const retreatElder = computed(() => props.baseData?.retreatElderVo || {})
const refundVoucher = computed(() => props.baseData?.refundVoucherVo || {})
// ------定义变量------
const photoFile = ref([
  {
    url: ''
  }
])
// 绑定上传的文件
watch(
  () => props.baseData,
  (val) => {
    const data = val?.refundVoucherVo || {}
    photoFile.value[0].url = data.refundVoucherUrl || ''
  },
  { immediate: true }
)
const form = ref() // 表单
// TODO 暂时保留
// const downType = ref('application/pdf') // pdf格式
// // 合同下载文件
// const handleDown = (url, name) => {
//   download(url, downType.value, name)
// }
// 向父组件暴露数据与方法
defineExpose({
  form
})
</script>
