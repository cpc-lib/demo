<template>
  <main class="container page-shell">
    <header class="page-heading">
      <div>
        <h1>退款审批</h1>
        <p>审核退款申请，并主动查询已通过退款单的渠道状态。</p>
      </div>
      <el-button :loading="loading" @click="loadList">刷新</el-button>
    </header>

    <el-table :data="list" v-loading="loading" border style="width: 100%">
      <el-table-column type="index" label="#" width="50"></el-table-column>
      <el-table-column prop="refundNo" label="退款申请单号" width="210"></el-table-column>
      <el-table-column prop="orderNo" label="订单编号" width="210"></el-table-column>
      <el-table-column label="退款金额" width="110">
        <template slot-scope="scope">
          {{ (scope.row.refund || 0) / 100 }} 元
        </template>
      </el-table-column>
      <el-table-column label="审核状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="approvalTagType(scope.row.approvalStatus)">
            {{ approvalStatusText(scope.row.approvalStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="退款状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="refundTagType(scope.row.refundStatus)">
            {{ refundStatusText(scope.row.refundStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="退款原因" width="150"></el-table-column>
      <el-table-column prop="approveRemark" label="审核备注" width="180"></el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170"></el-table-column>
      <el-table-column label="操作" width="230" align="center">
        <template slot-scope="scope">
          <template v-if="scope.row.approvalStatus === 'PENDING'">
            <el-button type="text" @click="openDecision(scope.row, 'approve')">通过</el-button>
            <el-button type="text" @click="openDecision(scope.row, 'reject')">驳回</el-button>
          </template>
          <el-button
            v-if="scope.row.approvalStatus === 'APPROVED'"
            type="text"
            :loading="actionKey === 'query-' + scope.row.refundNo"
            @click="queryRefund(scope.row)">
            查询退款状态
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :title="decision.action === 'approve' ? '通过退款申请' : '驳回退款申请'"
      :visible.sync="decision.open"
      width="420px"
      @close="closeDecision">
      <p>退款申请单号：{{ decision.refundNo }}</p>
      <el-input
        v-model="decision.remark"
        type="textarea"
        :rows="4"
        maxlength="255"
        show-word-limit
        placeholder="请输入审核备注（可选）">
      </el-input>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDecision">取 消</el-button>
        <el-button type="primary" :loading="Boolean(actionKey)" @click="submitDecision">
          {{ decision.action === 'approve' ? '通过并执行退款' : '确认驳回' }}
        </el-button>
      </div>
    </el-dialog>
  </main>
</template>

<script>
import refundInfoApi from '../api/refundInfo'

export default {
  data() {
    return {
      list: [],
      loading: true,
      actionKey: '',
      decision: { open: false, refundNo: '', action: '', remark: '' }
    }
  },

  created() {
    this.loadList()
  },

  methods: {
    loadList() {
      this.loading = true
      refundInfoApi.list().then(response => {
        this.list = (response.data && response.data.list) || []
      }).catch(error => {
        this.$message.error(this.errorMessage(error, '退款申请单加载失败'))
        this.list = []
      }).finally(() => {
        this.loading = false
      })
    },

    openDecision(row, action) {
      this.decision = {
        open: true,
        refundNo: row.refundNo,
        action,
        remark: ''
      }
    },

    closeDecision() {
      this.decision = { open: false, refundNo: '', action: '', remark: '' }
    },

    async submitDecision() {
      const { refundNo, action, remark } = this.decision
      if (!refundNo || !action) return

      this.actionKey = `${action}-${refundNo}`
      try {
        const api = action === 'approve' ? refundInfoApi.approve : refundInfoApi.reject
        const response = await api(refundNo, remark.trim() || undefined)
        this.$message.success(response.message || (action === 'approve' ? '审核通过，退款已提交处理' : '退款申请已拒绝'))
        this.closeDecision()
        this.loadList()
      } catch (error) {
        this.$message.error(this.errorMessage(error, '退款审核操作失败'))
      } finally {
        this.actionKey = ''
      }
    },

    async queryRefund(row) {
      this.actionKey = `query-${row.refundNo}`
      try {
        const response = await refundInfoApi.query(row.refundNo)
        const latest = response.data && response.data.refundInfo
        if (latest) {
          this.list = this.list.map(item => item.refundNo === row.refundNo
            ? { ...item, ...latest }
            : item)
        }
        this.$message.success(response.message || '退款状态查询完成')
      } catch (error) {
        this.$message.error(this.errorMessage(error, '退款状态查询失败'))
      } finally {
        this.actionKey = ''
      }
    },

    errorMessage(error, fallback) {
      return (error && error.response && error.response.data && error.response.data.message)
        || (typeof error === 'string' ? error : error && error.message)
        || fallback
    },

    approvalStatusText(status) {
      const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }
      return map[status] || status || '-'
    },

    refundStatusText(status) {
      const map = {
        CREATED: '待处理',
        PROCESSING: '退款中',
        SUCCESS: '退款成功',
        FAILED: '退款失败',
        CLOSED: '已关闭',
        ABNORMAL: '退款异常'
      }
      return map[status] || status || '-'
    },

    approvalTagType(status) {
      const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'info' }
      return map[status] || ''
    },

    refundTagType(status) {
      const map = {
        CREATED: 'warning',
        PROCESSING: 'danger',
        SUCCESS: 'success',
        FAILED: 'danger',
        CLOSED: 'info',
        ABNORMAL: 'danger'
      }
      return map[status] || ''
    }
  }
}
</script>
