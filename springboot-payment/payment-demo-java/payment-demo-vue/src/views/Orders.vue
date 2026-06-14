<template>
  <div class="bg-fa of">
    <section id="index" class="container">
      <header class="comm-title">
        <h2>订单列表</h2>
      </header>
      <el-table :data="list" border style="width: 100%">
        <el-table-column type="index" width="50"></el-table-column>
        <el-table-column prop="orderNo" label="订单编号" width="230"></el-table-column>
        <el-table-column prop="title" label="订单标题"></el-table-column>
        <el-table-column prop="totalFee" label="订单金额">
          <template slot-scope="scope">
            {{ scope.row.totalFee / 100 }} 元
          </template>
        </el-table-column>
        <el-table-column prop="paymentType" label="支付方式"></el-table-column>
        <el-table-column label="订单状态">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.orderStatus === '未支付'">{{ scope.row.orderStatus }}</el-tag>
            <el-tag v-if="scope.row.orderStatus === '支付成功'" type="success">{{ scope.row.orderStatus }}</el-tag>
            <el-tag v-if="scope.row.orderStatus === '部分退款'" type="warning">{{ scope.row.orderStatus }}</el-tag>
            <el-tag v-if="scope.row.orderStatus === '超时已关闭'" type="warning">{{ scope.row.orderStatus }}</el-tag>
            <el-tag v-if="scope.row.orderStatus === '用户已取消'" type="info">{{ scope.row.orderStatus }}</el-tag>
            <el-tag v-if="scope.row.orderStatus === '退款中'" type="danger">{{ scope.row.orderStatus }}</el-tag>
            <el-tag v-if="scope.row.orderStatus === '已退款'" type="info">{{ scope.row.orderStatus }}</el-tag>
            <el-tag v-if="scope.row.orderStatus === '退款异常'" type="danger">{{ scope.row.orderStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template slot-scope="scope">
            <el-button v-if="scope.row.orderStatus === '未支付'" type="text" @click="cancel(scope.row.orderNo, scope.row.paymentType)">取消</el-button>
            <el-button v-if="canApplyRefund(scope.row.orderStatus)" type="text" @click="refund(scope.row)">退款申请</el-button>
            <el-button type="text" @click="showRefundRecords(scope.row)">退款申请记录</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      :visible.sync="refundDialogVisible"
      @close="closeDialog"
      width="420px"
      center>
      <el-form label-width="100px">
        <el-form-item label="订单编号">
          <span>{{ orderNo }}</span>
        </el-form-item>
        <el-form-item label="订单金额">
          <span>{{ currentOrderTotalFee / 100 }} 元</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number
            v-model="refundAmountYuan"
            :precision="2"
            :step="0.01"
            :min="0.01"
            :max="currentOrderTotalFee / 100"
            controls-position="right"
            style="width: 100%">
          </el-input-number>
        </el-form-item>
        <el-form-item label="退款原因">
          <el-select v-model="reason" placeholder="请选择退款原因" style="width: 100%">
            <el-option label="不喜欢" value="不喜欢"></el-option>
            <el-option label="买错了" value="买错了"></el-option>
            <el-option label="其他" value="其他"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">取 消</el-button>
        <el-button type="primary" @click="toRefunds()" :disabled="refundSubmitBtnDisabled">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="退款申请记录"
      :visible.sync="refundRecordDialogVisible"
      width="900px"
      center>
      <el-table :data="refundRecordList" border style="width: 100%">
        <el-table-column prop="refundNo" label="退款申请单号" width="210"></el-table-column>
        <el-table-column label="退款金额" width="110">
          <template slot-scope="scope">
            {{ (scope.row.refund || 0) / 100 }} 元
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="approvalTagType(scope.row.approvalStatus)">
              {{ approvalStatusText(scope.row.approvalStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退款状态" width="110">
          <template slot-scope="scope">
            <el-tag size="mini" :type="refundTagType(scope.row.refundStatus)">
              {{ refundStatusText(scope.row.refundStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因"></el-table-column>
        <el-table-column prop="approveRemark" label="审核备注"></el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170"></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import aliPay from '../api/aliPay'
import orderInfoApi from '../api/orderInfo'
import wxPayApi from '../api/wxPay'
import refundInfoApi from '../api/refundInfo'

export default {
  data() {
    return {
      list: [],
      refundDialogVisible: false,
      refundRecordDialogVisible: false,
      refundRecordList: [],
      orderNo: '',
      reason: '',
      refundAmountYuan: 0.01,
      currentOrderTotalFee: 0,
      refundSubmitBtnDisabled: false,
      paymentType: ''
    }
  },

  created() {
    this.showOrderList()
  },

  methods: {
    showOrderList() {
      orderInfoApi.list().then((response) => {
        this.list = response.data.list
      })
    },

    canApplyRefund(orderStatus) {
      return orderStatus === '支付成功' || orderStatus === '部分退款' || orderStatus === '退款中'
    },

    cancel(orderNo, paymentType) {
      if (paymentType === '微信') {
        wxPayApi.cancel(orderNo).then((response) => {
          this.$message.success(response.message)
          this.showOrderList()
        })
      } else {
        aliPay.cancel(orderNo).then((response) => {
          this.$message.success(response.message)
          this.showOrderList()
        })
      }
    },

    refund(row) {
      this.refundDialogVisible = true
      this.orderNo = row.orderNo
      this.paymentType = row.paymentType
      this.currentOrderTotalFee = row.totalFee || 0
      this.refundAmountYuan = Number(((row.totalFee || 1) / 100).toFixed(2))
      this.reason = '不喜欢'
    },

    showRefundRecords(row) {
      refundInfoApi.listByOrderNo(row.orderNo).then((response) => {
        this.refundRecordList = response.data.list || []
        this.refundRecordDialogVisible = true
      })
    },

    closeDialog() {
      this.refundDialogVisible = false
      this.orderNo = ''
      this.reason = ''
      this.refundAmountYuan = 0.01
      this.currentOrderTotalFee = 0
      this.refundSubmitBtnDisabled = false
      this.paymentType = ''
    },

    toRefunds() {
      if (!this.refundAmountYuan || Number(this.refundAmountYuan) <= 0) {
        this.$message.error('请输入正确的退款金额')
        return
      }

      this.refundSubmitBtnDisabled = true
      const requestData = {
        orderNo: this.orderNo,
        refundAmount: Math.round(Number(this.refundAmountYuan) * 100),
        reason: this.reason || '正常退款'
      }

      const request = this.paymentType === '微信'
        ? wxPayApi.refunds(requestData)
        : aliPay.refunds(requestData)

      request.then((response) => {
        this.$message.success(response.message || '退款申请提交成功')
        this.closeDialog()
        this.showOrderList()
      }).catch(() => {
        this.refundSubmitBtnDisabled = false
      })
    },

    approvalStatusText(status) {
      const map = {
        PENDING: '待审核',
        APPROVED: '已通过',
        REJECTED: '已拒绝'
      }
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
      const map = {
        PENDING: 'warning',
        APPROVED: 'success',
        REJECTED: 'info'
      }
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
