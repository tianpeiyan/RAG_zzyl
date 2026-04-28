<!-- 资料上传 -->
<template>
  <!-- 基本信息 -->
  <t-form-item label="一寸照片：" name="oneInchPhoto">
    <t-upload
      ref="uploadRef"
      v-model="uploadFile1"
      action="/api/check-in/upload"
      :headers="headers"
      :autoUpload="autoUpload"
      theme="image"
      :size-limit="sizeLimit"
      tips="图片大小不超过10MB,仅支持上传PNG JPG JPEG类型图片"
      accept=".jpg,.jpeg,.png"
      :before-upload="beforeUpload"
      @remove="remove(1)"
      @fail="handleFail"
      @success="handleSuccess($event, 1)"
    ></t-upload>
  </t-form-item>
  <t-form-item label="身份证人像面：" name="idCardPortraitImg">
    <t-upload
      ref="uploadRef"
      v-model="uploadFile2"
      action="/api/check-in/upload"
      :headers="headers"
      :autoUpload="autoUpload"
      theme="image"
      :size-limit="sizeLimit"
      tips="图片大小不超过10MB,仅支持上传PNG JPG JPEG类型图片"
      accept=".jpg,.jpeg,.png"
      :before-upload="beforeUpload"
      @remove="remove(2)"
      @fail="handleFail"
      @success="handleSuccess($event, 2)"
    ></t-upload>
  </t-form-item>
  <t-form-item label="身份证国徽面：" name="idCardNationalEmblemImg">
    <t-upload
      ref="uploadRef"
      v-model="uploadFile3"
      action="/api/check-in/upload"
      :headers="headers"
      :autoUpload="autoUpload"
      theme="image"
      :size-limit="sizeLimit"
      tips="图片大小不超过10MB,仅支持上传PNG JPG JPEG类型图片"
      accept=".jpg,.jpeg,.png"
      :before-upload="beforeUpload"
      @remove="remove(3)"
      @fail="handleFail"
      @success="handleSuccess($event, 3)"
    ></t-upload>
  </t-form-item>
  <!-- end -->
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { useUserStore } from '@/store'
// 获取父组件值、方法
const props = defineProps({
  formData: {
    type: Object,
    default: () => {
      return {}
    }
  },
  urls: {
    type: Object,
    default: () => {
      return {}
    }
  }
})
// ------定义变量------
const emit = defineEmits(['getUpdateImg'])
const userStore = useUserStore()
const autoUpload = ref(true) // 是否在选择文件后自动发起请求上传文件
const uploadFile1 = ref([]) // 绑定上传的文件
const uploadFile2 = ref([]) // 绑定上传的文件
const uploadFile3 = ref([]) // 绑定上传的文件
const headers = computed(() => ({
  Authorization: userStore.token || ''
}))
const sizeLimit = ref({
  size: 10,
  unit: 'MB',
  message: '图片大小超过10MB，请重新上传'
})
const rules = {
  oneInchPhoto: [
    {
      required: true,
      message: '一寸照片为空，请上传一寸照片',
      type: 'error',
      trigger: 'change'
    }
  ],
  idCardPortraitImg: [
    {
      required: true,
      message: '身份证人像面为空，请上传身份证人像面',
      type: 'error',
      trigger: 'change'
    }
  ],
  idCardNationalEmblemImg: [
    {
      required: true,
      message: '身份证国徽面为空，请上传身份证国徽面',
      type: 'error',
      trigger: 'change'
    }
  ]
}
// -----定义方法------
// 移除图片时将图片设置为默认图片
const remove = (val) => {
  if (val === 1) {
    uploadFile1.value = []
  } else if (val === 2) {
    uploadFile2.value = []
    formData.value.url2 = ''
  } else {
    uploadFile3.value = []
    formData.value.url3 = ''
  }
  emit('getUpdateImg', '', val)
}
// 上传图片失败
const handleFail = (file) => {
  const errorMessage =
    file?.response?.msg || file?.error?.message || '文件上传失败'
  MessagePlugin.error(errorMessage)
}
// 上传成功后触发。
const handleSuccess = (params, val) => {
  if (!params?.response || params.response.code !== 200) {
    MessagePlugin.error(params?.response?.msg || '文件上传失败')
    return
  }
  emit('getUpdateImg', params.response.data, val)
  if (val === 1) {
    const url1 = params.response.data
    if (uploadFile1.value.length) {
      uploadFile1.value[0].response.url = url1
      uploadFile1.value[0].url = url1
    }
  } else if (val === 2) {
    const url2 = params.response.data
    if (uploadFile2.value.length) {
      uploadFile2.value[0].response.url = url2
      uploadFile2.value[0].url = url2
    }
  } else {
    const url3 = params.response.data
    if (uploadFile3.value.length) {
      uploadFile3.value[0].response.url = url3
      uploadFile3.value[0].url = url3
    }
  }
}
// 限制图片的大小
const beforeUpload = (file) => {
  const extension = file.name?.split('.').pop()?.toLowerCase()
  if (!extension || !['jpg', 'jpeg', 'png'].includes(extension)) {
    MessagePlugin.error('仅支持上传PNG JPG JPEG类型图片')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    MessagePlugin.error('图片大小超过10MB，请重新上传')
    return false
  }
  return true
}
// 向父组件暴露数据与方法
defineExpose({
  rules
})
</script>
