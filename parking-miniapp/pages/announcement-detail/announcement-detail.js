import request from '../../utils/request.js'
import { FILE_BASE } from '../../utils/request.js'

// 时间显示统一：replace('T',' ') 后截取到分钟（项目内约定）
const fmtTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

// 可预览的图片类型（其余类型走 wx.openDocument，失败后再引导复制链接）
const IMAGE_TYPES = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp']

/** 扩展名 → 图标（未知类型给通用图标） */
const EXT_ICON = {
  pdf: '📕',
  doc: '📘',
  docx: '📘',
  xls: '📗',
  xlsx: '📗',
  ppt: '📙',
  pptx: '📙',
  txt: '📄',
  zip: '🗜️',
  rar: '🗜️',
  jpg: '🖼️',
  jpeg: '🖼️',
  png: '🖼️',
  gif: '🖼️',
  webp: '🖼️',
  bmp: '🖼️'
}

/** 字节数换算 B/KB/MB，保留 1 位小数 */
function fmtSize(bytes) {
  const n = Number(bytes)
  if (isNaN(n) || n <= 0) return ''
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}

Page({
  data: {
    id: '',
    loading: true,
    errorMsg: '',
    announcement: null
  },

  onLoad(options) {
    const id = (options && options.id) || ''
    this.setData({ id })
    if (!id) {
      this.setData({ loading: false, errorMsg: '公告不存在或已被删除' })
      return
    }
    this.loadDetail()
  },

  /** 拉取公告详情（后端会累加浏览量，只在进页面时请求一次） */
  loadDetail() {
    this.setData({ loading: true, errorMsg: '' })
    return request.get('/announcement/' + this.data.id, {}, true).then((res) => {
      const a = res.data || {}
      const ext = String(a.attachmentExt || '').toLowerCase()
      // 有下载地址才算真正有附件可用
      const hasAttachment = !!a.attachmentDownloadUrl
      this.setData({
        loading: false,
        errorMsg: '',
        announcement: {
          id: a.id,
          title: a.title || '（无标题）',
          content: a.content || '',
          publisher: a.publisher || '管理员',
          publishTimeText: fmtTime(a.publishTime || a.createdAt),
          viewCount: typeof a.viewCount === 'number' ? a.viewCount : 0,
          hasAttachment,
          attachment: hasAttachment ? {
            name: a.attachmentName || ('附件' + (ext ? '.' + ext : '')),
            sizeText: fmtSize(a.attachmentSize),
            ext,                                        // 内部判断用（小写）
            extLabel: ext ? ext.toUpperCase() : '附件',  // 界面展示用
            isImage: IMAGE_TYPES.indexOf(ext) >= 0,
            actionText: IMAGE_TYPES.indexOf(ext) >= 0 ? '预览图片' : '下载并打开',
            icon: EXT_ICON[ext] || '📎',
            url: FILE_BASE + a.attachmentDownloadUrl
          } : null
        }
      })
    }).catch(() => {
      this.setData({ loading: false, errorMsg: '公告详情加载失败，请稍后重试' })
    })
  },

  onRetry() {
    this.loadDetail()
  },

  /**
   * 打开附件：图片走下载后预览（可长按保存/转发），
   * 文档走下载后 openDocument；不支持的类型引导复制链接到浏览器打开
   */
  openAttachment() {
    const att = this.data.announcement && this.data.announcement.attachment
    if (!att || !att.url) {
      wx.showToast({ title: '该公告没有可用的附件', icon: 'none' })
      return
    }
    const ext = String(att.ext || '').toLowerCase()
    const isImage = IMAGE_TYPES.indexOf(ext) >= 0

    wx.showLoading({ title: '下载中…', mask: true })
    wx.downloadFile({
      url: att.url,
      success: (res) => {
        if (res.statusCode !== 200) {
          wx.hideLoading()
          wx.showToast({ title: '附件下载失败（' + res.statusCode + '）', icon: 'none' })
          return
        }
        if (isImage) {
          wx.hideLoading()
          wx.previewImage({
            urls: [res.tempFilePath],
            fail: () => wx.showToast({ title: '图片预览失败', icon: 'none' })
          })
          return
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          fileType: ext,
          showMenu: true,
          success: () => wx.hideLoading(),
          fail: () => {
            wx.hideLoading()
            this.showUnsupported(ext)
          }
        })
      },
      fail: (err) => {
        wx.hideLoading()
        const msg = (err && err.errMsg) || ''
        wx.showToast({ title: msg.indexOf('domain') >= 0 ? '下载域名未配置，请复制链接到浏览器打开' : '附件下载失败，请检查网络', icon: 'none' })
      }
    })
  },

  /** 小程序不支持的类型：提示并提供"复制链接" */
  showUnsupported(ext) {
    const att = this.data.announcement && this.data.announcement.attachment
    wx.showModal({
      title: '无法直接打开',
      content: '该类型文件（' + String(ext || '').toUpperCase() + '）暂不支持在小程序内打开，可复制下载链接到浏览器打开。',
      confirmText: '复制链接',
      cancelText: '取消',
      success: (r) => {
        if (r.confirm) this.copyLink()
      }
    })
  },

  /** 复制下载链接（兜底入口，任何类型都可用） */
  copyLink() {
    const att = this.data.announcement && this.data.announcement.attachment
    if (!att || !att.url) {
      wx.showToast({ title: '该公告没有可用的附件', icon: 'none' })
      return
    }
    wx.setClipboardData({
      data: att.url,
      success: () => wx.showToast({ title: '下载链接已复制', icon: 'none' }),
      fail: () => wx.showToast({ title: '复制失败，请长按链接手动复制', icon: 'none' })
    })
  }
})
