<!-- 个人信息 -->
<template>
  <div class="bg-wt contentBox">
    <div class="headbox">
      <t-button theme="default" variant="text" @click="router.back()" style="margin-right: 10px">
        &lt; 返回
      </t-button>
      个人信息
    </div>
    <div class="bodybox">
      <t-form ref="form" :rules="rules" :data="formData" :label-width="70">
        <div class="leftBox">
          <t-form-item label="姓名：" name="realName">
            <t-input
              v-model="formData.realName"
              placeholder="请输入"
              class="wt-300"
              clearable
              show-limit-number
              :maxlength="10"
            />
          </t-form-item>
          <t-form-item label="邮箱：" name="email">
            <t-input
              v-model="formData.email"
              placeholder="请输入"
              class="wt-300"
              clearable
              :maxlength="50"
            />
          </t-form-item>
          <t-form-item label="所属部门：" name="deptNo">
            <t-tree-select
              v-model="formData.deptNo"
              :data="deptOptions"
              placeholder="请选择部门"
              class="wt-300"
              :tree-props="{ keys: { label: 'deptName', value: 'deptNo', children: 'children' } }"
              disabled
            />
          </t-form-item>
          <t-form-item label="所属职位：" name="postNo">
            <t-select
              v-model="formData.postNo"
              :options="postOptions"
              placeholder="请选择职位"
              class="wt-300"
              clearable
              disabled
            />
          </t-form-item>
          <t-form-item label="角色：" name="roleVoIds">
            <t-select
              v-model="formData.roleVoIds"
              :options="roleOptions"
              placeholder="请选择角色"
              class="wt-300"
              multiple
              clearable
              disabled
            />
          </t-form-item>
          <t-form-item label="手机号：" name="mobile">
            <t-input
              v-model="formData.mobile"
              placeholder="请输入"
              class="wt-300"
              clearable
              show-limit-number
              :maxlength="11"
              @change="handlePhone"
            >
            </t-input>
          </t-form-item>
          <t-form-item label="性别：" name="sex">
            <t-radio-group v-model="formData.sex">
              <t-radio
                v-for="(item, index) in sexData"
                :key="index"
                :value="item.id"
                >{{ item.value }}</t-radio
              >
            </t-radio-group>
          </t-form-item>
        </div>
        <div class="rightBox">
          <t-form-item name="avatar">
            <div class="t-form__label file">头像</div>
            <t-upload
              ref="uploadRef1"
              v-model="file"
              action="/api/common/upload"
              :headers="headers"
              :autoUpload="autoUpload"
              :size-limit="sizeLimit"
              tips="图片大小不超过2M,仅支持上传PNG JPG JPEG类型图片"
              theme="image"
              accept=".jpg,.jpeg,.png"
              :before-upload="beforeUpload"
              @remove="remove"
              @fail="handleFail"
              @success="handleSuccess"
            >
            </t-upload>
            <!-- 外部也可点击触发上传功能 -->
            <button class="upbtn" @click="uploadImg">
              <!-- 使用svg图 -->
              <span>
                <svg
                  t="1674963121719"
                  class="icon"
                  viewBox="0 0 1024 1024"
                  version="1.1"
                  xmlns="http://www.w3.org/2000/svg"
                  p-id="2386"
                  width="128"
                  height="128"
                >
                  <path
                    d="M544 736a32 32 0 0 1-64 0V288a32 32 0 0 1 64 0z"
                    p-id="2387"
                  ></path>
                  <path
                    d="M800 832V672a32 32 0 0 1 64 0v192a32 32 0 0 1-32 32H192a32 32 0 0 1-32-32V672a32 32 0 0 1 64 0v160zM496 271.36l-152.32 166.4a32 32 0 1 1-47.36-43.52l176-192a32 32 0 0 1 47.36 0l176 192a32 32 0 0 1-47.36 43.52z"
                    p-id="2388"
                  ></path>
                </svg>
              </span>
              <!-- 判断头像框内是否有内容，有内容显示重新上传，没有内容显示上传文件 -->
              {{ file[0].url !== files[0].url ? '重新上传' : '上传文件' }}
            </button>
          </t-form-item>
        </div>
      </t-form>
      <div class="boxBottom fx fx-ct bg-wt">
        <t-button class="bt wt-60" theme="primary" :loading="loading" @click="updatepersonalData">
          保存
        </t-button>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
export default {
  name: 'UserIndex'
}
</script>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { useRouter } from 'vue-router'
import { sexData } from '@/utils/commonData'
import { useUserStore } from '@/store'
// 引入接口
import {
  getDeptTree,
  getPostOptionsList,
  getRolesOptionsList
} from '@/api/permission'
import { getpersonal, updatepersonal } from '@/api/user'
// 引用正则
import {
  validatePhone,
  validateIdentityNum,
  hasAsterisk,
  validateEmail
} from '@/utils/validate'
import { phoneStar } from '@/utils/index'

const userStore = useUserStore()
const headers = computed(() => ({
  Authorization: userStore.token || ''
}))
const formData = ref<any>({})
const form = ref(null) // 表单
const deptOptions = ref([])
const postOptions = ref([])
const roleOptions = ref([])
const loading = ref(false) // 保存加载状态

const autoUpload = ref(true) // 是否在选择文件后自动发起请求上传文件
const sizeLimit = ref({
  size: 60,
  unit: 'MB',
  message: '图片大小超过60m，请重新上传'
}) // 图片的大小限制
const isChick = ref(false) // 是否触发了保存按钮
const router = useRouter() // 获取全局
// 显示的图片
const file = ref([
  {
    url: ''
  }
])
// 默认图片
const files = ref([
  {
    url: 'https://yjy-oss-videos.oss-accelerate.aliyuncs.com/grzxhz.jpg'
  }
])
const uploadRef1 = ref() // 上传图片
// 移除图片时将图片设置为默认图片
const remove = () => {
  file.value = files.value
}
// 获取接口数据
const getpersonalData = async () => {
  const res: any = await getpersonal()
  if (res.code === 200) {
    formData.value = res.data

    // 同步到全局状态，但保留当前的头像如果后端返回为空
    const userInfo = { ...res.data }
    if (!userInfo.avatar && userStore.userAvatar?.avatar) {
      userInfo.avatar = userStore.userAvatar.avatar
    }
    userStore.setUserInfo(userInfo)

    formData.value.sex = Number(formData.value.sex)
    if (formData.value.mobile) {
      formData.value.phone = formData.value.mobile
      formData.value.mobile = phoneStar(formData.value.mobile)
    }
    if (formData.value.avatar && formData.value.avatar !== '') {
      file.value = [{ url: formData.value.avatar }]
    } else {
      file.value = [{ url: files.value[0].url }]
      formData.value.avatar = files.value[0].url
    }
    // Ensure roleVoIds is an array
    if (!formData.value.roleVoIds) {
      formData.value.roleVoIds = []
    }
  }
}
// 更新信息
const updatepersonalData = () => {
  if (loading.value) {
    return
  }

  form.value.validate().then(async (valid) => {
    if (valid === true) {
      loading.value = true
      const isAsterisk = hasAsterisk(formData.value.mobile)
      const parent: any = {
        realName: formData.value.realName,
        email: formData.value.email,
        mobile: isAsterisk ? formData.value.phone : formData.value.mobile, // 判断手机是否有星，如果有星说明手机号没有被编辑过，直接用后端返回的手机号即可，否则用新改的手机号
        avatar: file.value[0].url,
        sex: formData.value.sex,
        id: formData.value.id
      }
      // 如果有部门、职位、角色信息，也带上，但通常个人中心不修改这些
      if (formData.value.deptNo) parent.deptNo = formData.value.deptNo
      if (formData.value.postNo) parent.postNo = formData.value.postNo
      if (formData.value.roleVoIds) parent.roleVoIds = formData.value.roleVoIds

      const isPhone = validatePhone(parent.mobile)
      if (!isPhone) {
        MessagePlugin.error('手机号格式错误，请重新输入')
        loading.value = false
        return false
      }
      try {
        const res: any = await updatepersonal(parent)
        // 兼容处理：有些请求返回原生响应 res.data，有些返回业务数据 res
        const businessData = res.code !== undefined ? res : res.data
        if (businessData && businessData.code === 200) {
          MessagePlugin.success('保存成功')
          // 更新 Pinia 中的头像
          userStore.setUserAvatar({
            ...userStore.userAvatar,
            avatar: parent.avatar
          })
          // 重新获取个人信息以同步数据
          await getpersonalData()
        } else {
          MessagePlugin.error((businessData && businessData.msg) || '保存失败')
        }
      } catch (e) {
        console.error('更新个人信息失败:', e)
      } finally {
        loading.value = false
      }
    }
  })
}
// 上传图片按钮
const uploadImg = () => {
  // 执行triggerUpload方法
  uploadRef1.value.triggerUpload()
}
// 限制图片的大小
const beforeUpload = (file) => {
  if (file.size > 2 * 1024 * 1024) {
    MessagePlugin.error('图片大小超过2M，请重新上传')
    return false
  }
  return true
}
// 上传成功后触发。
const handleSuccess = ({ file: uploadFile, response }) => {
  if (response.code !== 200) {
    MessagePlugin.error(response.msg || '上传失败')
    return
  }
  const avatarUrl = response.data
  // 确保 avatarUrl 是字符串
  if (typeof avatarUrl !== 'string') {
    console.error('上传返回的头像地址不是字符串:', avatarUrl)
    return
  }
  formData.value.avatar = avatarUrl
  if (uploadFile) {
    uploadFile.url = avatarUrl
  }
  file.value = [{ url: avatarUrl }]
}
// 上传失败后触发。
const handleFail = (params) => {
  MessagePlugin.error(params.response.msg || '上传失败')
}
// 判断是否填写的数字
const handlePhone = (val) => {
  const isNumber = validateIdentityNum(val)
  if (!isNumber) {
    formData.value.mobile = ''
  }
}
// 获取所有选项数据
const getAllOptions = async () => {
  try {
    const deptRes = await getDeptTree()
    deptOptions.value = deptRes.data
    
    const postRes = await getPostOptionsList()
    postOptions.value = (postRes.data as any).map((item) => ({
      label: item.postName,
      value: item.postNo
    }))
    
    const roleRes = await getRolesOptionsList()
    roleOptions.value = (roleRes.data as any).map((item) => ({
      label: item.roleName,
      value: String(item.id)
    }))
  } catch (error) {
    console.error('获取选项数据失败', error)
  }
}

// 生命周期
onMounted(() => {
  getpersonalData()
  getAllOptions()
})
// 校验
const rules = ref({
  realName: [
    {
      required: true,
      message: '姓名为空,请输入姓名',
      trigger: 'blur'
    }
  ],
  avatar: [
    {
      required: true,
      message: '头像为空，请上传头像',
      type: 'error',
      trigger: 'change'
    }
  ],
  email: [
    {
      validator: validateEmail,
      message: '邮箱格式错误',
      trigger: 'blur'
    }
  ]
})
</script>
<style lang="less" src="./index.less"></style>
