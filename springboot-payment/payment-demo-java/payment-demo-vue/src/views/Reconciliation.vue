<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title" style="display: flex; justify-content: space-between; align-items: center;">
        <h2>对账管理</h2>
        <div style="display: flex; gap: 10px; align-items: center;">
          <el-select
            v-model="filters.channelCode"
            placeholder="渠道"
            style="width: 120px;"
            clearable
            @change="fetchList">
            <el-option label="微信支付" value="WXPAY"></el-option>
            <el-option label="支付宝" value="ALIPAY"></el-option>
          </el-select>
          <el-select
            v-model="filters.status"
            placeholder="状态"
            style="width: 120px;"
            clearable
            @change="fetchList">
            <el-option label="待执行" value="PENDING"></el-option>
            <el-option label="执行中" value="PROCESSING"></el-option>
            <el-option label="已完成" value="COMPLETED"></el-option>
            <el-option label="执行失败" value="FAILED"></el-option>
          </el-select>
          <el-date-picker
            v-model="filters.billDateStart"
            type="date"
            placeholder="开始日期"
            value-format="yyyy-MM-dd">
          </el-date-picker>
          <el-date-picker
            v-model="filters.billDateEnd"
            type="date"
            placeholder="结束日期"
            value-format="yyyy-MM-dd">
          </el-date-picker>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button type="primary" @click="openExecute">手动对账</el-button>
        </div>
      </header>

      <el-table :data="list" border v-loading="loading" style="width: 100%">
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column prop="billDate" label="对账日期" width="110"></el-table-column>
        <el-table-column prop="channelCode" label="支付渠道" width="100">
          <template slot-scope="scope">
            {{ channelText(scope.row.channelCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="statusTagType(scope.row.status)">
              {{ statusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总笔数" width="80"></el-table-column>
        <el-table-column prop="matchCount" label="匹配数" width="80">
          <template slot-scope="scope">
            <span style="color: #67c23a;">{{ scope.row.matchCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="diffCount" label="差异数" width="80">
          <template slot-scope="scope">
            <span v-if="scope.row.diffCount && scope.row.diffCount > 0" style="color: #f56c6c;">
              {{ scope.row.diffCount }}
            </span>
            <span v-else>{{ scope.row.diffCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="diffAmount" label="差异金额" width="110">
          <template slot-scope="scope">
            <span :style="{ color: scope.row.diffAmount && scope.row.diffAmount !== 0 ? '#f56c6c' : '' }">
              {{ (scope.row.diffAmount || 0) / 100 }} 元
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="channelTotalAmount" label="渠道总金额" width="120">
          <template slot-scope="scope">
            {{ (scope.row.channelTotalAmount || 0) / 100 }} 元
          </template>
        </el-table-column>
        <el-table-column prop="localTotalAmount" label="本地总金额" width="120">
          <template slot-scope="scope">
            {{ (scope.row.localTotalAmount || 0) / 100 }} 元
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="执行时间" width="170"></el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="viewDetails(scope.row)">查看明细</el-button>
            <el-button v-if="scope.row.status === 'COMPLETED'" type="text" @click="downloadReport(scope.row)">下载报告</el-button>
          </template>
        </el-table-column>
      </el-table>

      <header class="comm-title" style="display: flex; justify-content: space-between; align-items: center; margin-top: 30px;">
        <h2>渠道账单（对账依据）</h2>
        <div style="display: flex; gap: 10px; align-items: center;">
          <el-select
            v-model="billFilters.channelCode"
            placeholder="渠道"
            style="width: 120px;"
            clearable
            @change="fetchBillList">
            <el-option label="微信支付" value="WXPAY"></el-option>
            <el-option label="支付宝" value="ALIPAY"></el-option>
          </el-select>
          <el-select
            v-model="billFilters.billSource"
            placeholder="来源"
            style="width: 130px;"
            clearable
            @change="fetchBillList">
            <el-option label="自动拉取" value="AUTO_DOWNLOAD"></el-option>
            <el-option label="手动上传" value="MANUAL_UPLOAD"></el-option>
          </el-select>
          <el-date-picker
            v-model="billFilters.billDateStart"
            type="date"
            placeholder="开始日期"
            value-format="yyyy-MM-dd">
          </el-date-picker>
          <el-date-picker
            v-model="billFilters.billDateEnd"
            type="date"
            placeholder="结束日期"
            value-format="yyyy-MM-dd">
          </el-date-picker>
          <el-button type="primary" @click="fetchBillList">查询</el-button>
          <el-button type="primary" @click="openFetch">自动拉取</el-button>
          <el-button type="primary" @click="openUpload">上传账单</el-button>
        </div>
      </header>

      <el-table :data="billList" border v-loading="billLoading" style="width: 100%">
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column prop="billDate" label="账单日期" width="110"></el-table-column>
        <el-table-column prop="channelCode" label="支付渠道" width="100">
          <template slot-scope="scope">
            {{ channelText(scope.row.channelCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="billType" label="账单类型" width="90"></el-table-column>
        <el-table-column prop="billSource" label="来源" width="100">
          <template slot-scope="scope">
            {{ sourceText(scope.row.billSource) }}
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="记录数" width="80"></el-table-column>
        <el-table-column prop="totalAmount" label="账单总金额" width="120">
          <template slot-scope="scope">
            {{ (scope.row.totalAmount || 0) / 100 }} 元
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" width="160">
          <template slot-scope="scope">
            {{ scope.row.fileName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="importTime" label="导入时间" width="170"></el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="viewBillRecords(scope.row)">查看记录</el-button>
            <el-button type="text" style="color: #f56c6c;" @click="removeBill(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      title="手动执行对账"
      :visible.sync="executeDialogVisible"
      @close="closeExecute"
      width="460px"
      center>
      <el-alert
        title="渠道账单为T+1出账：请先在下方导入对应日期的渠道账单，未导入账单无法对账"
        type="info"
        :closable="false"
        style="margin-bottom: 15px;">
      </el-alert>
      <el-form :model="executeForm" :rules="executeRules" ref="executeForm" label-width="100px">
        <el-form-item label="对账日期" prop="billDate">
          <el-date-picker
            v-model="executeForm.billDate"
            type="date"
            placeholder="选择对账日期"
            value-format="yyyy-MM-dd"
            style="width: 100%;">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="支付渠道" prop="channelCode">
          <el-select v-model="executeForm.channelCode" placeholder="请选择支付渠道" style="width: 100%;">
            <el-option label="微信支付" value="WXPAY"></el-option>
            <el-option label="支付宝" value="ALIPAY"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="账单类型" prop="billType">
          <el-select v-model="executeForm.billType" placeholder="请选择账单类型" style="width: 100%;">
            <el-option label="全部账单" value="ALL"></el-option>
            <el-option label="成功订单" value="SUCCESS"></el-option>
            <el-option label="退款账单" value="REFUND"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeExecute">取 消</el-button>
        <el-button type="primary" @click="doExecute">执行对账</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :title="'对账明细 - ' + (currentRecord?.billDate || '') + ' ' + channelText(currentRecord?.channelCode)"
      :visible.sync="detailDialogVisible"
      width="1100px"
      center>
      <el-table :data="detailList" border v-loading="detailLoading" style="width: 100%">
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column prop="businessType" label="业务类型" width="90">
          <template slot-scope="scope">
            {{ scope.row.businessType === 'PAYMENT' ? '进账' : scope.row.businessType === 'REFUND' ? '退款' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="diffType" label="差异类型" width="110">
          <template slot-scope="scope">
            <el-tag :type="diffTagType(scope.row.diffType)">
              {{ diffText(scope.row.diffType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="本地订单号" width="200">
          <template slot-scope="scope">
            {{ scope.row.orderNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="transactionId" label="渠道交易号" width="200">
          <template slot-scope="scope">
            {{ scope.row.transactionId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="refundNo" label="商户退款单号" width="190">
          <template slot-scope="scope">{{ scope.row.refundNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="refundId" label="微信退款单号" width="190">
          <template slot-scope="scope">{{ scope.row.refundId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="localAmount" label="本地金额" width="100">
          <template slot-scope="scope">
            {{ scope.row.localAmount != null ? (scope.row.localAmount / 100) + ' 元' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="channelAmount" label="渠道金额" width="100">
          <template slot-scope="scope">
            {{ scope.row.channelAmount != null ? (scope.row.channelAmount / 100) + ' 元' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="localStatus" label="本地状态" width="100">
          <template slot-scope="scope">
            {{ scope.row.localStatus || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="channelStatus" label="渠道状态" width="100">
          <template slot-scope="scope">
            {{ scope.row.channelStatus || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      title="自动拉取渠道账单"
      :visible.sync="fetchDialogVisible"
      @close="closeFetch"
      width="480px"
      center>
      <el-alert
        title="渠道账单为T+1出账：微信昨日账单次日10:00后可拉取"
        type="info"
        :closable="false"
        style="margin-bottom: 15px;">
      </el-alert>
      <el-form :model="fetchForm" :rules="fetchRules" ref="fetchForm" label-width="100px">
        <el-form-item label="账单日期" prop="billDate">
          <el-date-picker
            v-model="fetchForm.billDate"
            type="date"
            placeholder="选择账单日期"
            value-format="yyyy-MM-dd"
            style="width: 100%;">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="支付渠道" prop="channelCode">
          <el-select v-model="fetchForm.channelCode" placeholder="请选择支付渠道" style="width: 100%;">
            <el-option label="微信支付" value="WXPAY"></el-option>
            <el-option label="支付宝" value="ALIPAY"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="账单类型" prop="billType">
          <el-select v-model="fetchForm.billType" placeholder="请选择账单类型" style="width: 100%;">
            <el-option label="全部账单" value="ALL"></el-option>
            <el-option label="成功订单" value="SUCCESS"></el-option>
            <el-option label="退款账单" value="REFUND"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="覆盖导入">
          <el-switch v-model="fetchForm.force"></el-switch>
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">同日账单已导入时覆盖重新导入</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeFetch">取 消</el-button>
        <el-button type="primary" :loading="fetchSubmitting" @click="doFetch">拉取导入</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="上传渠道账单"
      :visible.sync="uploadDialogVisible"
      @close="closeUpload"
      width="520px"
      center>
      <el-alert
        title="支持微信交易账单 CSV、TXT、XLSX（T+1 下载），导入后按进账与退款逐笔对账"
        type="info"
        :closable="false"
        style="margin-bottom: 15px;">
      </el-alert>
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadForm" label-width="100px">
        <el-form-item label="账单文件" required>
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept=".csv,.txt,.xlsx"
            :file-list="uploadFileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove">
            <el-button size="small" type="primary">选择账单文件</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="账单日期" prop="billDate">
          <el-date-picker
            v-model="uploadForm.billDate"
            type="date"
            placeholder="选择账单日期"
            value-format="yyyy-MM-dd"
            style="width: 100%;">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="账单类型" prop="billType">
          <el-select v-model="uploadForm.billType" placeholder="请选择账单类型" style="width: 100%;">
            <el-option label="全部账单" value="ALL"></el-option>
            <el-option label="成功订单" value="SUCCESS"></el-option>
            <el-option label="退款账单" value="REFUND"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="覆盖导入">
          <el-switch v-model="uploadForm.force"></el-switch>
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">同日账单已导入时覆盖重新导入</span>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeUpload">取 消</el-button>
        <el-button type="primary" :loading="uploadSubmitting" @click="doUpload">上传导入</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :title="'账单记录 - ' + (currentBill?.billDate || '') + ' ' + channelText(currentBill?.channelCode)"
      :visible.sync="recordDialogVisible"
      width="1000px"
      center>
      <el-table :data="recordList" border v-loading="recordLoading" style="width: 100%">
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column prop="businessType" label="业务类型" width="90">
          <template slot-scope="scope">
            {{ scope.row.businessType === 'PAYMENT' ? '进账' : scope.row.businessType === 'REFUND' ? '退款' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="tradeTime" label="交易时间" width="170"></el-table-column>
        <el-table-column prop="orderNo" label="商户订单号" width="200"></el-table-column>
        <el-table-column prop="transactionId" label="渠道交易号" width="220"></el-table-column>
        <el-table-column prop="refundNo" label="商户退款单号" width="190"></el-table-column>
        <el-table-column prop="refundId" label="微信退款单号" width="190"></el-table-column>
        <el-table-column prop="tradeType" label="交易类型" width="100"></el-table-column>
        <el-table-column prop="status" label="交易状态" width="100"></el-table-column>
        <el-table-column prop="amount" label="订单金额" width="110">
          <template slot-scope="scope">
            {{ scope.row.amount != null ? (scope.row.amount / 100) + ' 元' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款金额" width="110">
          <template slot-scope="scope">
            {{ scope.row.refundAmount != null ? (scope.row.refundAmount / 100) + ' 元' : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import reconciliationApi from '../api/reconciliation'
import billApi from '../api/bill'

export default {
  data() {
    return {
      list: [],
      loading: false,
      executeDialogVisible: false,
      detailDialogVisible: false,
      detailList: [],
      detailLoading: false,
      currentRecord: null,
      filters: {
        channelCode: undefined,
        status: undefined,
        billDateStart: undefined,
        billDateEnd: undefined
      },
      executeForm: {
        billDate: null,
        channelCode: 'WXPAY',
        billType: 'ALL'
      },
      executeRules: {
        billDate: [{ required: true, message: '请选择对账日期', trigger: 'change' }],
        channelCode: [{ required: true, message: '请选择支付渠道', trigger: 'change' }],
        billType: [{ required: true, message: '请选择账单类型', trigger: 'change' }]
      },
      billList: [],
      billLoading: false,
      billFilters: {
        channelCode: undefined,
        billSource: undefined,
        billDateStart: undefined,
        billDateEnd: undefined
      },
      fetchDialogVisible: false,
      fetchSubmitting: false,
      fetchForm: {
        billDate: null,
        channelCode: 'WXPAY',
        billType: 'ALL',
        force: false
      },
      fetchRules: {
        billDate: [{ required: true, message: '请选择账单日期', trigger: 'change' }],
        channelCode: [{ required: true, message: '请选择支付渠道', trigger: 'change' }]
      },
      uploadDialogVisible: false,
      uploadSubmitting: false,
      uploadFileList: [],
      uploadFile: null,
      uploadForm: {
        billDate: null,
        billType: 'ALL',
        force: false
      },
      uploadRules: {
        billDate: [{ required: true, message: '请选择账单日期', trigger: 'change' }]
      },
      recordDialogVisible: false,
      recordList: [],
      recordLoading: false,
      currentBill: null
    }
  },

  created() {
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)
    const yyyy = yesterday.getFullYear()
    const mm = String(yesterday.getMonth() + 1).padStart(2, '0')
    const dd = String(yesterday.getDate()).padStart(2, '0')
    this.executeForm.billDate = `${yyyy}-${mm}-${dd}`
    this.fetchForm.billDate = `${yyyy}-${mm}-${dd}`
    this.uploadForm.billDate = `${yyyy}-${mm}-${dd}`
    this.fetchList()
    this.fetchBillList()
  },

  methods: {
    fetchList() {
      this.loading = true
      const params = {
        pageNum: 1,
        pageSize: 100,
        ...this.filters
      }
      reconciliationApi.list(params).then((response) => {
        this.list = response.data.records || []
      }).finally(() => {
        this.loading = false
      })
    },

    openExecute() {
      this.executeDialogVisible = true
    },

    closeExecute() {
      this.executeDialogVisible = false
      this.$refs.executeForm && this.$refs.executeForm.resetFields()
    },

    doExecute() {
      this.$refs.executeForm.validate((valid) => {
        if (valid) {
          reconciliationApi.execute(this.executeForm).then((response) => {
            this.$message.success(response.message || '对账任务已提交')
            this.closeExecute()
            this.fetchList()
          })
        }
      })
    },

    viewDetails(record) {
      this.currentRecord = record
      this.detailLoading = true
      reconciliationApi.listDetails(record.id, { pageNum: 1, pageSize: 100 }).then((response) => {
        this.detailList = response.data.records || []
        this.detailDialogVisible = true
      }).finally(() => {
        this.detailLoading = false
      })
    },

    downloadReport(record) {
      const url = reconciliationApi.exportUrl(record.id)
      window.open(url, '_blank')
    },

    fetchBillList() {
      this.billLoading = true
      const params = {
        pageNum: 1,
        pageSize: 100,
        ...this.billFilters
      }
      billApi.list(params).then((response) => {
        this.billList = response.data.records || []
      }).finally(() => {
        this.billLoading = false
      })
    },

    openFetch() {
      this.fetchDialogVisible = true
    },

    closeFetch() {
      this.fetchDialogVisible = false
      this.$refs.fetchForm && this.$refs.fetchForm.resetFields()
    },

    doFetch() {
      this.$refs.fetchForm.validate((valid) => {
        if (valid) {
          this.fetchSubmitting = true
          billApi.autoFetch(this.fetchForm).then((response) => {
            this.$message.success('账单导入成功，共 ' + (response.data.recordCount || 0) + ' 条记录')
            this.closeFetch()
            this.fetchBillList()
          }).finally(() => {
            this.fetchSubmitting = false
          })
        }
      })
    },

    openUpload() {
      this.uploadDialogVisible = true
    },

    closeUpload() {
      this.uploadDialogVisible = false
      this.uploadFile = null
      this.uploadFileList = []
      this.$refs.uploadForm && this.$refs.uploadForm.resetFields()
    },

    handleFileChange(file, fileList) {
      this.uploadFile = file.raw
      this.uploadFileList = fileList.slice(-1)
    },

    handleFileRemove() {
      this.uploadFile = null
      this.uploadFileList = []
    },

    doUpload() {
      this.$refs.uploadForm.validate((valid) => {
        if (valid && this.uploadFile) {
          const formData = new FormData()
          formData.append('file', this.uploadFile)
          formData.append('billDate', this.uploadForm.billDate)
          formData.append('channelCode', 'WXPAY')
          formData.append('billType', this.uploadForm.billType || 'ALL')
          formData.append('force', this.uploadForm.force)

          this.uploadSubmitting = true
          billApi.upload(formData).then((response) => {
            this.$message.success('账单导入成功，共 ' + (response.data.recordCount || 0) + ' 条记录')
            this.closeUpload()
            this.fetchBillList()
          }).finally(() => {
            this.uploadSubmitting = false
          })
        } else if (!this.uploadFile) {
          this.$message.warning('请选择账单文件')
        }
      })
    },

    viewBillRecords(bill) {
      this.currentBill = bill
      this.recordLoading = true
      billApi.listRecords(bill.id, { pageNum: 1, pageSize: 100 }).then((response) => {
        this.recordList = response.data.records || []
        this.recordDialogVisible = true
      }).finally(() => {
        this.recordLoading = false
      })
    },

    removeBill(bill) {
      this.$confirm('确认删除 ' + bill.billDate + ' 的' + this.channelText(bill.channelCode) + '账单？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        billApi.remove(bill.id).then(() => {
          this.$message.success('删除成功')
          this.fetchBillList()
        })
      }).catch(() => {})
    },

    sourceText(source) {
      const map = {
        AUTO_DOWNLOAD: '自动拉取',
        MANUAL_UPLOAD: '手动上传'
      }
      return map[source] || source || '-'
    },

    statusText(status) {
      const map = {
        PENDING: '待执行',
        PROCESSING: '执行中',
        COMPLETED: '已完成',
        FAILED: '执行失败'
      }
      return map[status] || status || '-'
    },

    statusTagType(status) {
      const map = {
        PENDING: 'info',
        PROCESSING: 'warning',
        COMPLETED: 'success',
        FAILED: 'danger'
      }
      return map[status] || ''
    },

    diffText(type) {
      const map = {
        MATCH: '完全匹配',
        MISSING_LOCAL: '漏单',
        MISSING_CHANNEL: '多单',
        AMOUNT_MISMATCH: '金额不符',
        STATUS_MISMATCH: '状态不符'
      }
      return map[type] || type || '-'
    },

    diffTagType(type) {
      const map = {
        MATCH: 'success',
        MISSING_LOCAL: 'warning',
        MISSING_CHANNEL: 'warning',
        AMOUNT_MISMATCH: 'danger',
        STATUS_MISMATCH: 'danger'
      }
      return map[type] || ''
    },

    channelText(code) {
      const map = {
        WXPAY: '微信支付',
        ALIPAY: '支付宝'
      }
      return map[code] || code || '-'
    }
  }
}
</script>
