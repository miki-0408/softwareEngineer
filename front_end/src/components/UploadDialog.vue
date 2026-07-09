<template>
  <el-dialog v-model="visible" :title="title" width="680px" @closed="clearFiles">
    <div style="display:flex;gap:20px;min-height:280px;max-height:420px">
      <!-- 左侧：文件选择区 -->
      <div style="flex:3;display:flex;flex-direction:column;min-width:0;overflow:hidden">
        <div class="flex-center gap-8" style="margin-bottom:10px">
          <input ref="fileInput" type="file" multiple style="display:none" @change="onFilesChange" />
          <input ref="folderInput" type="file" webkitdirectory style="display:none" @change="onFilesChange" />
          <el-tag type="info" size="small" style="max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
            {{ targetPath }}
          </el-tag>
          <div style="flex:1" />
          <el-button size="small" @click="fileInput.click()"><el-icon><Document /></el-icon> 文件</el-button>
          <el-button size="small" @click="folderInput.click()"><el-icon><Folder /></el-icon> 文件夹</el-button>
        </div>

        <div v-if="displayFiles.length" class="upload-file-list" style="flex:1">
          <div v-for="(f, i) in displayFiles" :key="i" class="upload-file-row">
            <el-icon :size="16" color="#909399"><Document /></el-icon>
            <span style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;min-width:0;font-size:13px">
              {{ f.relativePath || f.name }}
            </span>
            <span style="font-size:11px;color:#909399;flex-shrink:0;margin-right:4px">{{ formatSize(f.size) }}</span>
            <el-button size="small" text type="danger" @click="removeFile(i)"><el-icon :size="14"><Close /></el-icon></el-button>
          </div>
        </div>
        <div v-else class="upload-placeholder" style="flex:1">
          <el-icon :size="36" color="#dcdfe6"><UploadFilled /></el-icon>
          <div style="color:#c0c4cc;margin-top:8px">选择文件或文件夹</div>
        </div>
        <div style="font-size:12px;color:#909399;margin-top:6px;text-align:right">
          共 {{ fileList.length }} 个，{{ formatSize(fileList.reduce((s, f) => s + f.size, 0)) }}
          <span v-if="filteredCount < fileList.length" style="color:#e6a23c;margin-left:8px">
            （筛选后 {{ filteredCount }} 个）
          </span>
        </div>
      </div>

      <!-- 右侧：参数区 -->
      <div style="flex:2;border-left:1px solid #ebeef5;padding-left:20px;display:flex;flex-direction:column;gap:10px">
        <div style="font-size:13px;font-weight:600;color:#606266">上传选项</div>

        <div>
          <div style="font-size:12px;color:#909399;margin-bottom:4px">打包方式</div>
          <el-select v-model="packMethod" style="width:100%" size="default" @change="onPackChange">
            <el-option label="不打包" value="none" />
            <el-option label="tar 归档" value="tar" />
          </el-select>
        </div>

        <div>
          <div style="font-size:12px;color:#909399;margin-bottom:4px">压缩算法</div>
          <el-select v-model="compressMethod" style="width:100%" size="default">
            <el-option label="LZ77" value="lz77" />
            <el-option label="Huffman" value="huffman" />
          </el-select>
        </div>

        <div v-if="packMethod === 'tar'">
          <div style="font-size:12px;color:#909399;margin-bottom:4px">归档文件名</div>
          <el-input v-model="tarName" placeholder="默认：第一个文件名.tar" maxlength="200" size="default" />
        </div>

        <!-- 筛选条件 -->
        <div>
          <div class="flex-between" style="cursor:pointer" @click="showFilter = !showFilter">
            <span style="font-size:12px;color:#909399">
              <el-icon :size="12"><Filter /></el-icon> 筛选条件
            </span>
            <el-icon :size="12" style="color:#909399">
              <ArrowDown v-if="!showFilter" /><ArrowUp v-else />
            </el-icon>
          </div>
          <div v-if="showFilter" style="margin-top:8px;display:flex;flex-direction:column;gap:8px">
            <el-input v-model="filter.name" placeholder="文件名包含..." size="small" clearable />
            <div class="flex-center gap-4">
              <el-input v-model="filter.type" placeholder="类型如 .pdf,.doc" size="small" style="flex:1" clearable />
            </div>
            <el-input v-model="filter.path" placeholder="路径包含..." size="small" clearable />
            <div style="display:flex;gap:4px;align-items:center">
              <span style="font-size:11px;color:#909399;white-space:nowrap">大小</span>
              <el-input v-model="filter.minSize" placeholder="最小" size="small" style="width:70px" />
              <span style="font-size:11px;color:#909399">-</span>
              <el-input v-model="filter.maxSize" placeholder="最大" size="small" style="width:70px" />
              <span style="font-size:10px;color:#c0c4cc">KB</span>
            </div>
          </div>
        </div>

        <div v-if="showEncrypt">
          <el-checkbox v-model="encrypt">
            加密上传<span v-if="!autoEncrypt" style="font-size:12px;color:#909399">（移入私密空间）</span>
          </el-checkbox>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="filteredCount === 0" @click="$emit('confirm', buildPayload())">
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { Document, Folder, Close, UploadFilled, Filter, ArrowDown, ArrowUp } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: Boolean,
  targetPath: { type: String, default: '' },
  showEncrypt: { type: Boolean, default: true },
  autoEncrypt: { type: Boolean, default: false },
  submitText: { type: String, default: '上传' },
  title: { type: String, default: '上传文件' }
})

const emit = defineEmits(['confirm', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const fileInput = ref(null)
const folderInput = ref(null)
const fileList = ref([])
const packMethod = ref('none')
const compressMethod = ref('lz77')
const tarName = ref('')
const encrypt = ref(false)
const showFilter = ref(false)

const filter = reactive({
  path: '',
  type: '',
  name: '',
  minSize: '',
  maxSize: ''
})

// 筛选后的文件列表
const filteredFiles = computed(() => {
  let files = fileList.value
  if (filter.path) {
    const p = filter.path.toLowerCase()
    files = files.filter(f => (f.relativePath || f.name).toLowerCase().includes(p))
  }
  if (filter.type) {
    const types = filter.type.split(',').map(t => t.trim().toLowerCase()).filter(Boolean)
    if (types.length) files = files.filter(f => types.some(t => f.name.toLowerCase().endsWith(t)))
  }
  if (filter.name) {
    const n = filter.name.toLowerCase()
    files = files.filter(f => f.name.toLowerCase().includes(n))
  }
  if (filter.minSize) {
    const min = parseFloat(filter.minSize) * 1024
    if (!isNaN(min)) files = files.filter(f => f.size >= min)
  }
  if (filter.maxSize) {
    const max = parseFloat(filter.maxSize) * 1024
    if (!isNaN(max)) files = files.filter(f => f.size <= max)
  }
  return files
})

const displayFiles = computed(() => filteredFiles.value)
const filteredCount = computed(() => filteredFiles.value.length)

function clearFiles() {
  fileList.value = []
  packMethod.value = 'none'
  compressMethod.value = 'lz77'
  tarName.value = ''
  encrypt.value = props.autoEncrypt
  showFilter.value = false
  filter.path = ''; filter.type = ''; filter.name = ''; filter.minSize = ''; filter.maxSize = ''
  if (fileInput.value) fileInput.value.value = ''
  if (folderInput.value) folderInput.value.value = ''
}

function onFilesChange(e) {
  for (const f of e.target.files) {
    fileList.value.push({
      name: f.name, size: f.size,
      relativePath: f.webkitRelativePath || f.name,
      raw: f
    })
  }
  e.target.value = ''
}

function removeFile(index) {
  // 找到原始索引
  const item = displayFiles.value[index]
  const origIdx = fileList.value.indexOf(item)
  if (origIdx >= 0) fileList.value.splice(origIdx, 1)
}

function onPackChange(val) {
  if (val === 'tar' && fileList.value.length) {
    tarName.value = fileList.value[0].name.replace(/\.[^.]+$/, '') + '.tar'
  }
}

function ensureTarSuffix(name) {
  return name.endsWith('.tar') ? name : name + '.tar'
}

function buildPayload() {
  return {
    files: filteredFiles.value,
    packMethod: packMethod.value,
    compressMethod: compressMethod.value,
    tarName: tarName.value ? ensureTarSuffix(tarName.value) : undefined,
    encrypt: props.autoEncrypt || encrypt.value,
    filters: { ...filter }
  }
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}
</script>

<style scoped>
.upload-file-list {
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.upload-file-row {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 10px; border-bottom: 1px solid #f5f5f5;
  min-width: 0;
}
.upload-file-row:last-child { border-bottom: none; }
.upload-placeholder {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  border: 1px dashed #dcdfe6; border-radius: 6px;
}
</style>
