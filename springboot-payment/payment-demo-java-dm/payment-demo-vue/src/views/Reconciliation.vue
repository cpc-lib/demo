<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2>对账管理</h2>
      </header>

      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="账单日期">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="queryForm.channelCode" placeholder="全部渠道" clearable>
            <el-option label="微信支付" value="WXPAY"></el-option>
            <el-option label="支付宝" value="ALIPAY"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable>
            <el-option label="待处理" value="PENDING"></el-option>
            <el-option label="处理中" value="PROCESSING"></el-option>
            <el-option label="已完成" value="COMPLETED"></el-option>
            <el-option label="失败" value="FAILED"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadBatches">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" @click="openCreateDialog">手动对账</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="16" class="summary-row">
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="今日批次" :value="summary.todayBatchCount || 0"></el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="已完成" :value="summary.todayCompletedCount || 0">
              <template slot="suffix">个</template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="失败" :value="summary.todayFailedCount || 0">
              <template slot="suffix">个</template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <el-statistic title="待处理" :value="summary.pendingDiscrepancyCount || 0">
              <template slot="suffix">个</template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>

      <el-table :data="batchList" border style="width: 100%" class="batch-table">
        <el-table-column type="index" width="50" label="序号"></el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="200"></el-table-column>
        <el-table-column prop="channelCode" label="渠道" width="100">
          <template slot-scope="scope">
            {{ channelText(scope.row.channelCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="billDate" label="账单日期" width="120"></el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="batchStatusTagType(scope.row.status)">
              {{ batchStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="channelCount" label="渠道笔数" width="100"></el-table-column>
        <el-table-column prop="localCount" label="本地笔数" width="100"></el-table-column>
        <el-table-column prop="matchedCount" label="匹配笔数" width="100"></el-table-column>
        <el-table-column prop="discrepancyCount" label="差异笔数" width="100"></el-table-column>
        <el-table-column label="操作" width="280" align="center">
          <template slot-scope="scope">
            <el-button
              v-if="scope.row.status === 'PENDING' || scope.row.status === 'FAILED'"
              type="text"
              @click="executeBatch(scope.row)">执行</el-button>
            <el-button type="text" @click="showDetails(scope.row)">明细</el-button>
            <el-button type="text" @click="showDiscrepancies(scope.row)">差异</el-button>
            <el-button type="text" @click="showProgress(scope.row)">进度</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog title="手动创建对账批次" :visible.sync="createDialogVisible" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createForm" label-width="100px">
        <el-form-item label="渠道" prop="channelCode">
          <el-select v-model="createForm.channelCode" placeholder="请选择渠道" style="width: 100%">
            <el-option label="微信支付" value="WXPAY"></el-option>
            <el-option label="支付宝" value="ALIPAY"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="应用ID" prop="paymentAppId">
          <el-input v-model="createForm.paymentAppId" placeholder="请输入支付应用ID"></el-input>
        </el-form-item>
        <el-form-item label="账单日期" prop="billDate">
          <el-date-picker
            v-model="createForm.billDate"
            type="date"
            placeholder="选择账单日期"
            value-format="yyyy-MM-dd"
            style="width: 100%">
          </el-date-picker>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </div>
    </el-dialog>

    <el-dialog title="对账明细" :visible.sync="detailDialogVisible" width="1100px">
      <el-table :data="detailList" border style="width: 100%">
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="200"></el-table-column>
        <el-table-column prop="channelOrderNo" label="渠道订单号" width="200"></el-table-column>
        <el-table-column label="金额" width="120">
          <template slot-scope="scope">
            {{ (scope.row.amount || 0) / 100 }} 元
          </template>
        </el-table-column>
        <el-table-column label="匹配状态" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" :type="matchStatusTagType(scope.row.matchStatus)">
              {{ matchStatusText(scope.row.matchStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注"></el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="detailTotal"
          :current-page="detailPageNum"
          :page-size="detailPageSize"
          @current-change="handleDetailPageChange">
        </el-pagination>
      </div>
    </el-dialog>

    <el-dialog title="对账差异" :visible.sync="discrepancyDialogVisible" width="1100px">
      <el-table :data="discrepancyList" border style="width: 100%">
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="200"></el-table-column>
        <el-table-column prop="channelOrderNo" label="渠道订单号" width="200"></el-table-column>
        <el-table-column label="本地金额" width="110">
          <template slot-scope="scope">
            {{ (scope.row.localAmount || 0) / 100 }} 元
          </template>
        </el-table-column>
        <el-table-column label="渠道金额" width="110">
          <template slot-scope="scope">
            {{ (scope.row.channelAmount || 0) / 100 }} 元
          </template>
        </el-table-column>
        <el-table-column prop="discrepancyType" label="差异类型" width="120"></el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" :type="discrepancyStatusTagType(scope.row.status)">
              {{ discrepancyStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template slot-scope="scope">
            <el-button
              v-if="scope.row.status === 'PENDING'"
              type="text"
              @click="openResolveDialog(scope.row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="discrepancyTotal"
          :current-page="discrepancyPageNum"
          :page-size="discrepancyPageSize"
          @current-change="handleDiscrepancyPageChange">
        </el-pagination>
      </div>
    </el-dialog>

    <el-dialog title="处理差异" :visible.sync="resolveDialogVisible" width="500px">
      <el-form :model="resolveForm" :rules="resolveRules" ref="resolveForm" label-width="100px">
        <el-form-item label="订单号">
          <span>{{ resolveForm.orderNo }}</span>
        </el-form-item>
        <el-form-item label="差异类型">
          <span>{{ resolveForm.discrepancyType }}</span>
        </el-form-item>
        <el-form-item label="处理备注" prop="resolveRemark">
          <el-input
            v-model="resolveForm.resolveRemark"
            type="textarea"
            :rows="4"
            placeholder="请输入处理备注">
          </el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResolve">确认处理</el-button>
      </div>
    </el-dialog>

    <el-dialog title="对账执行进度" :visible.sync="progressDialogVisible" width="500px">
      <div class="progress-content">
        <p>批次号：{{ currentBatchNo }}</p>
        <p>当前阶段：{{ progressInfo.stageText || '-' }}</p>
        <el-progress
          :percentage="progressInfo.percentage || 0"
          :status="progressInfo.status === 'FAILED' ? 'exception' : (progressInfo.status === 'COMPLETED' ? 'success' : '')">
        </el-progress>
        <p v-if="progressInfo.message" class="progress-msg">{{ progressInfo.message }}</p>
      </div>
      <div slot="footer">
        <el-button @click="closeProgressDialog">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import reconciliationApi from '../api/reconciliation'

export default {
  data() {
    return {
      queryForm: {
        dateRange: [],
        channelCode: '',
        status: ''
      },
      summary: {},
      batchList: [],
      createDialogVisible: false,
      createForm: {
        channelCode: '',
        paymentAppId: '',
        billDate: ''
      },
      createRules: {
        channelCode: [{ required: true, message: '请选择渠道', trigger: 'change' }],
        paymentAppId: [{ required: true, message: '请输入支付应用ID', trigger: 'blur' }],
        billDate: [{ required: true, message: '请选择账单日期', trigger: 'change' }]
      },
      detailDialogVisible: false,
      detailList: [],
      detailTotal: 0,
      detailPageNum: 1,
      detailPageSize: 10,
      detailBatchNo: '',
      discrepancyDialogVisible: false,
      discrepancyList: [],
      discrepancyTotal: 0,
      discrepancyPageNum: 1,
      discrepancyPageSize: 10,
      discrepancyBatchNo: '',
      resolveDialogVisible: false,
      resolveForm: {
        id: null,
        orderNo: '',
        discrepancyType: '',
        resolveRemark: ''
      },
      resolveRules: {
        resolveRemark: [{ required: true, message: '请输入处理备注', trigger: 'blur' }]
      },
      progressDialogVisible: false,
      currentBatchNo: '',
      progressInfo: {},
      progressTimer: null
    }
  },

  created() {
    this.loadSummary()
    this.loadBatches()
  },

  beforeDestroy() {
    this.clearProgressTimer()
  },

  methods: {
    loadSummary() {
      reconciliationApi.getSummary().then(response => {
        this.summary = response.data || {}
      })
    },

    loadBatches() {
      const billDateStart = this.queryForm.dateRange && this.queryForm.dateRange[0] || ''
      const billDateEnd = this.queryForm.dateRange && this.queryForm.dateRange[1] || ''
      reconciliationApi.listBatches(
        this.queryForm.channelCode,
        this.queryForm.status,
        billDateStart,
        billDateEnd
      ).then(response => {
        this.batchList = response.data || []
      })
    },

    resetQuery() {
      this.queryForm.dateRange = []
      this.queryForm.channelCode = ''
      this.queryForm.status = ''
      this.loadBatches()
    },

    openCreateDialog() {
      this.createForm = {
        channelCode: '',
        paymentAppId: '',
        billDate: ''
      }
      this.createDialogVisible = true
      this.$nextTick(() => this.$refs.createForm && this.$refs.createForm.clearValidate())
    },

    submitCreate() {
      this.$refs.createForm.validate(valid => {
        if (!valid) return
        reconciliationApi.createBatch(
          this.createForm.channelCode,
          this.createForm.paymentAppId,
          this.createForm.billDate
        ).then(response => {
          this.$message.success(response.message || '创建成功')
          this.createDialogVisible = false
          this.loadSummary()
          this.loadBatches()
        })
      })
    },

    executeBatch(row) {
      this.$confirm('确认执行该对账批次？', '提示', { type: 'warning' })
        .then(() => {
          reconciliationApi.executeBatch(row.batchNo).then(response => {
            this.$message.success(response.message || '执行成功')
            this.showProgress(row)
            this.loadSummary()
            this.loadBatches()
          })
        })
        .catch(() => {})
    },

    showDetails(row) {
      this.detailBatchNo = row.batchNo
      this.detailPageNum = 1
      this.loadDetails()
      this.detailDialogVisible = true
    },

    loadDetails() {
      reconciliationApi.listDetails(
        this.detailBatchNo,
        null,
        this.detailPageNum,
        this.detailPageSize
      ).then(response => {
        const data = response.data || {}
        this.detailList = data.list || []
        this.detailTotal = data.total || 0
      })
    },

    handleDetailPageChange(page) {
      this.detailPageNum = page
      this.loadDetails()
    },

    showDiscrepancies(row) {
      this.discrepancyBatchNo = row.batchNo
      this.discrepancyPageNum = 1
      this.loadDiscrepancies()
      this.discrepancyDialogVisible = true
    },

    loadDiscrepancies() {
      reconciliationApi.listDiscrepancies(
        this.discrepancyBatchNo,
        null,
        this.discrepancyPageNum,
        this.discrepancyPageSize
      ).then(response => {
        const data = response.data || {}
        this.discrepancyList = data.list || []
        this.discrepancyTotal = data.total || 0
      })
    },

    handleDiscrepancyPageChange(page) {
      this.discrepancyPageNum = page
      this.loadDiscrepancies()
    },

    openResolveDialog(row) {
      this.resolveForm = {
        id: row.id,
        orderNo: row.orderNo,
        discrepancyType: row.discrepancyType,
        resolveRemark: ''
      }
      this.resolveDialogVisible = true
      this.$nextTick(() => this.$refs.resolveForm && this.$refs.resolveForm.clearValidate())
    },

    submitResolve() {
      this.$refs.resolveForm.validate(valid => {
        if (!valid) return
        reconciliationApi.resolveDiscrepancy(
          this.resolveForm.id,
          this.resolveForm.resolveRemark
        ).then(response => {
          this.$message.success(response.message || '处理成功')
          this.resolveDialogVisible = false
          this.loadDiscrepancies()
          this.loadBatches()
          this.loadSummary()
        })
      })
    },

    showProgress(row) {
      this.currentBatchNo = row.batchNo
      this.progressInfo = {}
      this.progressDialogVisible = true
      this.loadProgress()
      this.startProgressTimer()
    },

    loadProgress() {
      reconciliationApi.getBatchProgress(this.currentBatchNo).then(response => {
        this.progressInfo = response.data || {}
        if (this.progressInfo.status === 'COMPLETED' || this.progressInfo.status === 'FAILED') {
          this.clearProgressTimer()
          this.loadBatches()
          this.loadSummary()
        }
      })
    },

    startProgressTimer() {
      this.clearProgressTimer()
      this.progressTimer = setInterval(() => {
        this.loadProgress()
      }, 3000)
    },

    clearProgressTimer() {
      if (this.progressTimer) {
        clearInterval(this.progressTimer)
        this.progressTimer = null
      }
    },

    closeProgressDialog() {
      this.progressDialogVisible = false
      this.clearProgressTimer()
    },

    channelText(code) {
      const map = { WXPAY: '微信支付', ALIPAY: '支付宝' }
      return map[code] || code || '-'
    },

    batchStatusText(status) {
      const map = {
        PENDING: '待处理',
        PROCESSING: '处理中',
        COMPLETED: '已完成',
        FAILED: '失败'
      }
      return map[status] || status || '-'
    },

    batchStatusTagType(status) {
      const map = {
        PENDING: 'info',
        PROCESSING: 'warning',
        COMPLETED: 'success',
        FAILED: 'danger'
      }
      return map[status] || ''
    },

    matchStatusText(status) {
      const map = {
        MATCHED: '已匹配',
        MISMATCHED: '不匹配',
        LOCAL_ONLY: '仅本地',
        CHANNEL_ONLY: '仅渠道'
      }
      return map[status] || status || '-'
    },

    matchStatusTagType(status) {
      const map = {
        MATCHED: 'success',
        MISMATCHED: 'warning',
        LOCAL_ONLY: 'danger',
        CHANNEL_ONLY: 'danger'
      }
      return map[status] || ''
    },

    discrepancyStatusText(status) {
      const map = {
        PENDING: '待处理',
        RESOLVED: '已处理'
      }
      return map[status] || status || '-'
    },

    discrepancyStatusTagType(status) {
      const map = {
        PENDING: 'warning',
        RESOLVED: 'success'
      }
      return map[status] || ''
    }
  }
}
</script>

<style scoped>
.filter-form {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}
.summary-row {
  margin-bottom: 16px;
}
.summary-row .el-card {
  text-align: center;
}
.pagination-wrap {
  margin-top: 16px;
  text-align: right;
}
.progress-content p {
  margin: 8px 0;
}
.progress-msg {
  color: #909399;
  font-size: 13px;
}
</style>
