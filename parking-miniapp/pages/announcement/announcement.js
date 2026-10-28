import request from '../../utils/request.js'
import { cacheGet, cacheSet } from '../../utils/pageCache.js'

const PAGE_SIZE = 20

// 时间显示统一：replace('T',' ') 后截取到分钟（项目内约定）
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')

// 正文摘要：去掉换行/多余空白，超过 60 字加省略号
const SUMMARY_LIMIT = 60
const buildSummary = (content) => {
  const text = String(content || '').replace(/\s+/g, ' ').trim()
  if (!text) return ''
  return text.length > SUMMARY_LIMIT ? text.slice(0, SUMMARY_LIMIT) + '…' : text
}

Page({
  data: {
    loading: true,
    errorMsg: '',
    records: [],
    page: 0,
    hasMore: false
  },

  onLoad() {
    this.loadList()
  },

  /** 回到本页：缓存命中时立即渲染最近一次列表，再静默刷新第一页 */
  onShow() {
    const cached = cacheGet('announceList', 15000)
    if (cached) {
      this.applyListResult(cached.rows, cached.page, cached.hasMore)
      this.loadList(true)
    } else {
      this.loadList()
    }
  },

  onPullDownRefresh() {
    this.loadList(true).then(() => wx.stopPullDownRefresh()).catch(() => wx.stopPullDownRefresh())
  },

  /** 滚动到底部：还有下一页时追加 */
  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return
    this.loadList(false, this.data.page + 1)
  },

  /**
   * 拉取公告列表
   * 统一用 silent=true，错误由页面内的错误块提示，避免下拉刷新时反复弹 toast
   * @param {boolean} silent 为 true 时不改变整页 loading（下拉刷新 / 上拉加载 / onShow 静默刷新）
   * @param {number} page 目标页码，默认第一页
   */
  loadList(silent, page) {
    const target = page || 1
    // onLoad 与 onShow 会连续触发两次请求，用序号丢弃过期响应，避免旧数据覆盖新数据
    const seq = (this.reqSeq || 0) + 1
    this.reqSeq = seq
    if (!silent) this.setData({ loading: true, errorMsg: '' })
    return request.get('/announcement/list?page=' + target + '&pageSize=' + PAGE_SIZE + '&keyword=', {}, true)
      .then((res) => {
        if (seq !== this.reqSeq) return
        const data = res.data || {}
        const rows = (data.records || []).map((a) => ({
          id: a.id,
          title: a.title || '（无标题）',
          timeText: fmtTime(a.publishTime || a.createdAt),
          summary: buildSummary(a.content),
          hasAttachment: !!a.attachmentName,
          attachmentName: a.attachmentName || ''
        }))
        const list = target === 1 ? rows : this.data.records.concat(rows)
        // 分页判断优先用 total（契约字段），缺失时退化为"本页是否满页"
        const total = Number(data.total)
        const hasMore = isNaN(total) ? rows.length >= PAGE_SIZE : list.length < total
        if (target === 1) {
          cacheSet('announceList', { rows: list, page: target, hasMore })
        }
        this.applyListResult(list, target, hasMore)
      })
      .catch(() => {
        if (seq !== this.reqSeq) return
        // 失败时保留已有列表（下拉刷新失败不清空页面），只标记错误
        if (target === 1) {
          this.setData({ loading: false, errorMsg: '公告加载失败，请稍后下拉重试' })
        } else {
          this.setData({ loading: false })
          wx.showToast({ title: '加载更多失败，请稍后重试', icon: 'none' })
        }
      })
  },

  applyListResult(rows, page, hasMore) {
    this.setData({
      records: rows,
      page,
      hasMore,
      loading: false,
      errorMsg: ''
    })
  },

  onRetry() {
    this.loadList()
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: '/pages/announcement-detail/announcement-detail?id=' + id })
  }
})
