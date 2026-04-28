<!-- 解除合同 -->
<template>
  <!-- 基本信息 -->
  <t-card title="解除协议">
    <t-form-item label="解除日期：" class="label-wt" name="releaseDate">
      <t-date-picker
        v-model="formData.releaseDate"
        placeholder="请选择"
        class="wt-sm300"
      />
    </t-form-item>
    <t-form-item label="解除协议：" name="releasePdfUrl">
      <t-upload
        ref="uploadRef"
        v-model="uploadFile"
        action="/api/common/upload"
        :headers="headers"
        :autoUpload="autoUpload"
        :size-limit="sizeLimit"
        tips="请上传pdf文件，大小在60M以内"
        accept=".pdf"
        @remove="remove"
        @fail="handleFail"
        @success="handleSuccess"
        @validate="onValidate"
      ></t-upload>
    </t-form-item>
  </t-card>
  <!-- end -->
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { useUserStore } from '@/store'
// 获取父组件值、方法
const props = defineProps({
  formData: {
    type: Object,
    default: () => {
      return {}
    }
  }
})
// ------定义变量------
// 触发父级事件
const emit: Function = defineEmits(['getVallidate', 'getContractPdf'])
const form = ref() // 表单
const autoUpload = ref(true) // 是否在选择文件后自动发起请求上传文件
const uploadFile = ref([]) // 绑定上传的文件
const userStore = useUserStore()
const headers = computed(() => ({
  Authorization: userStore.token || ''
}))
const sizeLimit = ref({
  size: 60,
  unit: 'MB',
  message: '图片大小超过60m，请重新上传'
}) // 图片的大小限制

// 表单校验
const rules = {
  releaseDate: [
    {
      required: true,
      message: '解除日期为空，请选择解除日期',
      type: 'error',
      trigger: 'change'
    }
  ],
  releasePdfUrl: [
    {
      required: true,
      message: '解除协议为空，请上传解除协议',
      type: 'error',
      trigger: 'change'
    }
  ]
}
// -----定义方法------
// watch(formData.value, (val) => {
//   // if (val.time && uploadFile.value.length > 0) {
//   //   emit('getVallidate', false)
//   // }
// })
// 移除图片时将图片设置为默认图片
const remove = () => {
  uploadFile.value = []
  emit('getContractPdf', '')
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
  const contractUrl = params.response.data
  emit('getContractPdf', contractUrl)
  if (uploadFile.value.length) {
    uploadFile.value[0].response.url = contractUrl
    uploadFile.value[0].url = contractUrl
  }
}
// 超过2m或者文件格式错误报错提示
const onValidate = (params) => {
  const { files, type } = params
  const messageMap = {
    FILE_OVER_SIZE_LIMIT: files[0].response.error,
    FILES_OVER_LENGTH_LIMIT: '文件数量超出限制，仅上传未超出数量的文件',
    FILTER_FILE_SAME_NAME: '不允许上传同名文件',
    BEFORE_ALL_FILES_UPLOAD: 'beforeAllFilesUpload 方法拦截了文件',
    CUSTOM_BEFORE_UPLOAD: 'beforeUpload 方法拦截了文件'
  }
  messageMap[type] && MessagePlugin.warning(messageMap[type])
}
// 清除表单数据
const handleClear = () => {
  props.formData.releaseDate = new Date()
  uploadFile.value = []
  emit('getContractPdf', '')
}
// 向父组件暴露数据与方法
defineExpose({
  form,
  rules,
  handleClear
})
</script>
