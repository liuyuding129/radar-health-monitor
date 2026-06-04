<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import radarApi from './api/radar'
import ScoreCircle from './components/ScoreCircle.vue'
import DataCard from './components/DataCard.vue'
import AdvicePanel from './components/AdvicePanel.vue'

// 设备相关
const connected = ref(false)

// 数据
const radarData = ref(null)
const healthAdvice = ref(null)
const lastUpdate = ref('')
let timer = null

// 定时获取数据
async function fetchData() {
  try {
    const res = await radarApi.getLatest()
    if (res.status === 'ok') {
      radarData.value = res.radarData
      healthAdvice.value = res.healthAdvice
      connected.value = true
      if (res.lastUpdate) {
        lastUpdate.value = new Date(res.lastUpdate).toLocaleString()
      }
    }
  } catch {
    connected.value = false
  }
}

onMounted(async () => {
  fetchData()
  timer = setInterval(fetchData, 2000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="app">
    <!-- 水墨背景 -->
    <div class="ink-bg"></div>

    <!-- 头部 -->
    <header class="header">
      <div class="header-content">
        <div class="header-title">
          <span class="taiji-icon">☯</span>
          <div>
            <h1>悬丝辨息</h1>
            <p>基于毫米波雷达的中医智能健康监测平台</p>
          </div>
        </div>
        <div class="header-right">
          <div class="header-status">
            <div :class="['status-dot', { connected }]"></div>
            <span>{{ connected ? '数据连通' : '待连接' }}</span>
          </div>
        </div>
      </div>
      <div class="header-line"></div>
    </header>

    <!-- 主体 -->
    <main class="main">
      <!-- 综合评分 -->
      <ScoreCircle :score="healthAdvice?.overallScore || 0" />

      <!-- 体征监测区 -->
      <div class="section-divider">
        <span class="section-icon">❋</span>
        <span class="section-text">体征监测 · 心肺脉诊</span>
        <span class="section-line"></span>
      </div>

      <DataCard title="心率" :value="radarData?.heartRate" unit="bpm"
        icon-color="#C94043" :min="30" :max="180" :normalMin="60" :normalMax="100" />

      <DataCard title="呼吸频率" :value="radarData?.respiratoryRate" unit="次/分"
        icon-color="#C8B88A" :min="4" :max="40" :normalMin="12" :normalMax="20" />

      <!-- 步态分析区 -->
      <div class="section-divider">
        <span class="section-icon">❋</span>
        <span class="section-text">步态分析 · 肝肾辨证</span>
        <span class="section-line"></span>
      </div>

      <DataCard title="平均速度" :value="radarData?.speedFluctuation" unit="cm/s"
        icon-color="#3A6B8C" :min="0" :max="200" :normalMin="90" :normalMax="140" />

      <DataCard title="速度标准差" :value="radarData?.vStd" unit="cm/s"
        icon-color="#3A6B8C" :min="0" :max="30" :normalMin="0" :normalMax="15" />

      <DataCard title="频率差" :value="radarData?.currentDiff" unit="Hz"
        icon-color="#4A7C59" :min="0" :max="0.15" :normalMin="0" :normalMax="0.03" />

      <DataCard title="步频" :value="radarData?.stepFrequency" unit="Hz"
        icon-color="#4A7C59" :min="0" :max="3" :normalMin="1.5" :normalMax="2.0" />

      <!-- 环境感知区 -->
      <div class="section-divider">
        <span class="section-icon">❋</span>
        <span class="section-text">环境感知 · 六淫辨证</span>
        <span class="section-line"></span>
      </div>

      <DataCard title="环境温度" :value="radarData?.ambientTemperature" unit="°C"
        icon-color="#C94043" :min="-10" :max="50" :normalMin="18" :normalMax="28" />

      <DataCard title="环境湿度" :value="radarData?.ambientHumidity" unit="%"
        icon-color="#3A6B8C" :min="0" :max="100" :normalMin="40" :normalMax="70" />

      <DataCard title="气压" :value="radarData?.ambientPressure" unit="hPa"
        icon-color="#4A7C59" :min="950" :max="1050" :normalMin="1000" :normalMax="1020" />

      <DataCard title="声音强度" :value="radarData?.soundIntensity" unit="dB"
        icon-color="#3A6B8C" :min="0" :max="120" :normalMin="30" :normalMax="60" />

      <!-- 辨证建议 -->
      <AdvicePanel :advice-texts="healthAdvice?.adviceTexts || []" />
    </main>

    <!-- 底部 -->
    <footer class="footer">
      <div class="footer-line"></div>
      <span v-if="lastUpdate">时辰：{{ lastUpdate }}</span>
      <span v-else>悬丝辨息 · 中医数字伴诊</span>
    </footer>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  min-height: 100vh;
  color: #e0e0e0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

/* 水墨背景 */
.app {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px;
  min-height: 100vh;
}

.ink-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: -1;
  background:
    radial-gradient(ellipse at 20% 20%, rgba(212,162,78,0.04) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(201,64,67,0.03) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(58,107,140,0.03) 0%, transparent 60%),
    linear-gradient(135deg, #0f0f14 0%, #16161e 30%, #1a1520 60%, #0f0f14 100%);
}

/* 头部 */
.header {
  margin-bottom: 28px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 20px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 16px;
}

.taiji-icon {
  font-size: 40px;
  color: rgba(212, 162, 78, 0.7);
  filter: drop-shadow(0 0 8px rgba(212,162,78,0.3));
  animation: taijiPulse 4s ease-in-out infinite;
}

@keyframes taijiPulse {
  0%, 100% { opacity: 0.7; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.1); }
}

.header h1 {
  font-size: 32px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
  color: #d4c5a9;
  letter-spacing: 8px;
  text-shadow: 0 0 20px rgba(212,162,78,0.15);
  line-height: 1.2;
}

.header p {
  color: #665;
  font-size: 13px;
  margin-top: 4px;
  letter-spacing: 2px;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
  color: #888;
  letter-spacing: 2px;
}

.header-line {
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(212, 162, 78, 0.4) 10%,
    rgba(212, 162, 78, 0.2) 50%,
    rgba(212, 162, 78, 0.4) 90%,
    transparent 100%
  );
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #555;
  transition: all 0.3s;
}

.status-dot.connected {
  background: #4A7C59;
  box-shadow: 0 0 8px rgba(74, 124, 89, 0.5);
  animation: dotPulse 2s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% { box-shadow: 0 0 8px rgba(74, 124, 89, 0.3); }
  50% { box-shadow: 0 0 16px rgba(74, 124, 89, 0.6); }
}

/* 分区标题 */
.section-divider {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0 16px;
}

.section-icon {
  color: rgba(212, 162, 78, 0.5);
  font-size: 18px;
}

.section-text {
  font-size: 15px;
  color: #b8a88a;
  letter-spacing: 4px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
  white-space: nowrap;
}

.section-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg,
    rgba(212, 162, 78, 0.25),
    rgba(212, 162, 78, 0.05),
    transparent
  );
}

/* 主体 */
.main {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

/* 底部 */
.footer {
  text-align: center;
  padding: 28px 0 16px;
}

.footer-line {
  width: 120px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212,162,78,0.3), transparent);
  margin: 0 auto 12px;
}

.footer span {
  color: #555;
  font-size: 13px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
  letter-spacing: 2px;
}
</style>
