<!-- 签约办理表单 -->
<template>
  <t-card title="">
    <template #title
      >签约办理
      <t-popup
        class="placement bottom center titTpopup"
        content=""
        overlayClassName="titTpopup"
        placement="bottom"
        show-arrow
        destroy-on-close
      >
        <t-icon name="error-circle-filled"></t-icon>
        <template #content>
          入住成功后，系统将会生成首期账单。生成账单后，入住配置和签约办理信息将无法修改，若修改信息，需办理退住流程。
        </template>
      </t-popup></template
    >
    <t-form-item label="合同名称：" name="contractName">
      <t-input
        v-model="formData.contractName"
        placeholder="请输入"
        class="wt-300"
        clearable
        show-limit-number
        :maxlength="20"
      >
      </t-input>
    </t-form-item>
    <t-form-item label="签约日期：" name="signDate">
      <t-date-picker
        v-model="formData.signDate"
        placeholder="请选择"
        class="wt-sm300"
        clearable
      />
    </t-form-item>
    <t-form-item label="丙方姓名：" name="memberName">
      <t-input
        v-model="formData.memberName"
        placeholder="请输入"
        class="wt-300"
        show-limit-number
        clearable
        :maxlength="10"
      >
      </t-input>
    </t-form-item>
    <t-form-item label="丙方联系方式：" name="memberPhone">
      <t-input
        v-model="formData.memberPhone"
        placeholder="请输入"
        class="wt-300"
        clearable
        show-limit-number
        :maxlength="11"
        @change="handlePhone"
      >
      </t-input>
    </t-form-item>
    <t-form-item label="入住合同：" name="pdfUrl">
      <t-upload
        ref="uploadRef"
        v-model="uploadFile"
        action="/api/check-in/upload"
        :headers="headers"
        :autoUpload="autoUpload"
        :size-limit="sizeLimit"
        tips="请上传pdf文件，大小在10MB以内"
        accept=".pdf"
        :before-upload="beforeUpload"
        @remove="remove"
        @fail="handleFail"
        @success="handleSuccess"
      ></t-upload>
    </t-form-item>
  </t-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { getDateInfo } from '@/utils/date'
import { useUserStore } from '@/store'

// 获取父组件值、方法
const props = defineProps({
  formData: {
    type: Object,
    default: () => {
      return {}
    }
  },
  baseData: {
    type: Object,
    default: () => {
      return {}
    }
  }
})
// ------定义变量------
// 触发父级事件
const emit: Function = defineEmits([
  'getVallidate',
  'getContractPdf',
  'handlePhone'
])
const userStore = useUserStore()
const form = ref() // 表单
const autoUpload = ref(true) // 是否在选择文件后自动发起请求上传文件
const uploadFile = ref([]) // 绑定上传的文件
const sizeLimit = ref({
  size: 10,
  unit: 'MB',
  message: '文件大小超过10MB，请重新上传'
})
const headers = computed(() => ({
  Authorization: userStore.token || ''
}))

// 表单校验
const rules = {
  contractName: [
    {
      required: true,
      message: '合同名称为空，请输入合同名称',
      type: 'error',
      trigger: 'blur'
    }
  ],
  signDate: [
    {
      required: true,
      message: '签约日期为空，请选择签约日期',
      type: 'error',
      trigger: 'change'
    }
  ],
  pdfUrl: [
    {
      required: true,
      message: '上传合同为空，请选择上传合同',
      type: 'error',
      trigger: 'change'
    }
  ]
}
// -----定义方法------
watch(props.formData, (val) => {
  if (val.time && uploadFile.value.length > 0) {
    emit('getVallidate', false)
  }
})
onMounted(() => {
  // if (props.baseData.retreat !== undefined) {
  //   formData.value.contractNo = props.baseData.retreat.remark
  //   formData.value.startTime =
  //     props.baseData.retreat.checkInConfigVo.checkInStartTime
  //   formData.value.endTime =
  //     props.baseData.retreat.checkInConfigVo.checkInEndTime
  // }
})
// 移除图片时将图片设置为默认图片
const remove = () => {
  uploadFile.value = []
  formData.value.pdfUrl = ''
}
// 上传图片失败
const handleFail = (file) => {
  const errorMessage =
    file?.response?.msg || file?.error?.message || '文件上传失败'
  MessagePlugin.error(errorMessage)
}
// 上传成功后触发。
const handleSuccess = (params) => {
  if (!params?.response || params.response.code !== 200) {
    MessagePlugin.error(params?.response?.msg || '文件上传失败')
    return
  }
  const pdfUrl = params.response.data
  emit('getContractPdf', pdfUrl)

  if (uploadFile.value.length) {
    uploadFile.value[0].response.url = pdfUrl
    uploadFile.value[0].url = pdfUrl
  }
}
const beforeUpload = (file) => {
  const extension = file.name?.split('.').pop()?.toLowerCase()
  if (!extension || extension !== 'pdf') {
    MessagePlugin.error('仅支持上传PDF文件')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    MessagePlugin.error('文件大小超过10MB，请重新上传')
    return false
  }
  return true
}
// 清除表单数据
const handleClear = () => {
  // 重置表单
  formData.value.checkOutTime = new Date()
}
// 判断是否填写的数字
const handlePhone = (val) => {
  emit('handlePhone', val)
}
// 向父组件暴露数据与方法
defineExpose({
  rules,
  handleClear
})
</script>
