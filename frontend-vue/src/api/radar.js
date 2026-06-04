import axios from 'axios'

const API_BASE = 'http://localhost:9999'

export const radarApi = {
  // 获取设备列表
  async getDeviceList() {
    const res = await axios.get(`${API_BASE}/device/list`)
    if (res.data && res.data.code === 200) {
      return res.data.rows || []
    }
    return []
  },

  // 获取最新健康数据和医学建议（两台雷达合并数据）
  async getLatest() {
    const res = await axios.get(`${API_BASE}/health/data/1`)
    // AjaxResult 格式: { code: 200, msg: "...", data: { radarData, healthAdvice, lastUpdate } }
    if (res.data && res.data.code === 200) {
      return {
        status: 'ok',
        ...res.data.data
      }
    }
    return { status: 'error' }
  }
}

export default radarApi
