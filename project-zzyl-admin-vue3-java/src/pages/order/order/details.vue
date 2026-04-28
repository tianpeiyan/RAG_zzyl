<!-- 详情 -->
<template>
  <div class="bg-wt contentBox">
    <div class="headbox">
      <t-button theme="default" variant="text" @click="handleReturn" style="margin-right: 10px">
        &lt; 返回
      </t-button>
      订单详情
    </div>
    <div class="bodybox order detail-base">
      <div class="bg-wt stepContainer mb-24">
        <!-- step -->
        <t-steps :current="defaultCurrent" :readonly="true">
          <t-step-item title="已下单" :content="baseData.createTime" />
          <t-step-item v-if="baseData.status !== 4" title="已支付" content="">
          <template #content>
            {{
              baseData.tradingVo?.updateTime && baseData.status > 0
                ? baseData.tradingVo.updateTime
                : null
            }}
          </template>
        </t-step-item>
          <t-step-item
            v-if="
              ((baseData.status === 0 || baseData.status === 1) &&
                baseData.nursingTaskVo &&
                !baseData.nursingTaskVo.taskImage) ||
              ((baseData.status === 5 ||
                baseData.status === 3 ||
                baseData.status === 2) &&
                baseData.nursingTaskVo &&
                baseData.nursingTaskVo.taskImage)
            "
            title="已执行"
            content=""
          >
            <template #content>
              {{
                baseData.nursingTaskVo &&
                baseData.nursingTaskVo.updateTime &&
                baseData.nursingTaskVo.taskImage
                  ? baseData.nursingTaskVo.updateTime
                  : null
              }}
            </template>
          </t-step-item>
          <t-step-item
            v-if="baseData.status === 4"
            title="已关闭"
            :content="baseData.updateTime"
          />
          <t-step-item
            v-else-if="baseData.status === 5"
            title="已退款"
            :content="baseData.refundRecordVo.updateTime"
          />
          <t-step-item v-else title="已完成">
            <template #content>
              {{
                baseData.updateTime && baseData.status === 3
                  ? baseData.updateTime
                  : null
              }}
            </template>
          </t-step-item>
        </t-steps>
      </div>
      <!-- end -->
      <div class="bg-wt min-steph height">
        <!-- 加载中状态 -->
        <div v-if="loading" class="loading-box">
          <t-loading />
        </div>
        <template v-else-if="baseData.orderNo || baseData.id">
          <!-- 基本信息：只要数据加载了就展示 -->
          <BaseInfo :data="baseData"></BaseInfo>

          <!-- 支付记录：已支付、待执行、已执行、已完成、已退款状态都可能展示 -->
          <RecordPay
            v-if="baseData.status > 0 || baseData.tradingVo?.id"
            :data="baseData"
          ></RecordPay>

          <!-- 执行记录：状态为已执行(2)、已完成(3)，或者已退款(5)但曾经执行过 -->
          <RecordExecutel
            v-if="
              baseData.status === 2 ||
              baseData.status === 3 ||
              (baseData.nursingTaskVo && baseData.nursingTaskVo.taskImage)
            "
            :data="baseData"
          ></RecordExecutel>

          <!-- 取消记录：状态为已关闭(4) -->
          <RecordCancel
            v-if="
              baseData.status === 4 &&
              baseData.reason &&
              baseData.reason !== '超时未关闭'
            "
            :data="baseData"
          ></RecordCancel>

          <!-- 退款记录：有退款信息或状态为已退款(5) -->
          <RecordRefund
            v-if="baseData.refundRecordVo?.id || baseData.status === 5"
            :data="baseData"
          ></RecordRefund>
        </template>
        <!-- 无数据状态 -->
        <div v-else class="no-data-box">
          <NoData />
        </div>
      </div>
    </div>
    <div class="boxBottom fx fx-ct bg-wt">
      <t-button theme="default" variant="outline" class="wt-60" @click="handleReturn">返回</t-button>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'orderDetails'
}
</script>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
// 接口
import { getDetails } from '@/api/order'
// 组件
// 基本信息
import BaseInfo from './components/BaseInfo.vue'
// 支付记录
import RecordPay from './components/RecordPay.vue'
// 执行记录
import RecordExecutel from './components/RecordExecutel.vue'
// 取消记录
import RecordCancel from './components/RecordCancel.vue'
// 退款记录
import RecordRefund from './components/RecordRefund.vue'
import NoData from '@/components/noData/index.vue'
// ------定义变量------
const route = useRoute() // 获取局部
const router = useRouter() // 获取全局
const defaultCurrent = ref(0) // step 当前的选择
const baseData = ref<Object | any>({
  bedVo: {},
  elderVo: {},
  memberVo: {},
  nursingProjectVo: {},
  refundRecordVo: {},
  tradingVo: {}
})
const orderId = ref('')
const loading = ref(false)
// 生命周期
onMounted(() => {
  orderId.value = route.query.id as string
  getBaseData()
})
// ------定义方法------
// // 获取数据
const getBaseData = async () => {
  loading.value = true
  try {
    const res: any = await getDetails(orderId.value) // 获取数据
    if (res.code === 200 && res.data) {
      baseData.value = res.data
      if (baseData.value.status !== undefined && baseData.value.status !== null) {
        const status = Number(baseData.value.status)
        // 只有当状态不为已关闭(4)和已退款(5)时，才直接使用 status 作为步骤条的当前步骤
        if (status < 4) {
          defaultCurrent.value = status
        } else if (status === 4) {
          // 已关闭状态，步骤条显示在第 2 步（索引 1，即 已下单 -> 已关闭）
          defaultCurrent.value = 1
        } else if (status === 5) {
          // 已退款状态，根据是否有执行记录来决定步骤
          // 已下单 -> 已支付 -> [已执行] -> 已退款
          if (
            baseData.value.nursingTaskVo &&
            baseData.value.nursingTaskVo.taskImage
          ) {
            defaultCurrent.value = 3
          } else {
            defaultCurrent.value = 2
          }
        }
      }
    }
  } catch (error) {
    MessagePlugin.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}
// 返回
const handleReturn = () => {
  router.back()
}
</script>

<style lang="less" scoped>
.height {
  min-height: calc(100vh - 380px);
}
.loading-box,
.no-data-box {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 200px;
}
</style>
