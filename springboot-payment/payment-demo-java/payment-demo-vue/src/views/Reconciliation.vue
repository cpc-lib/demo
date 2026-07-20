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
    </section>

    <el-dialog
      title="手动执行对账"
      :visible.sync="executeDialogVisible"
      @close="closeExecute"
      width="460px"
      center>
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
        <el-table-column prop="channelTradeNo" label="渠道交易号" width="200">
          <template slot-scope="scope">
            {{ scope.row.channelTradeNo || '-' }}
          </template>
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
  </div>
</template>

<script>
import reconciliationApi from '../api/reconciliation'

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
      }
    }
  },

  created() {
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)
    const yyyy = yesterday.getFullYear()
    const mm = String(yesterday.getMonth() + 1).padStart(2, '0')
    const dd = String(yesterday.getDate()).padStart(2, '0')
    this.executeForm.billDate = `${yyyy}-${mm}-${dd}`
    this.fetchList()
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
