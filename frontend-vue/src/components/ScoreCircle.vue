<template>
  <div class="score-section">
    <div class="score-card">
      <div class="score-header">
        <span class="decorative-line left"></span>
        <span class="label">综合健康评估</span>
        <span class="decorative-line right"></span>
      </div>
      <div class="taiji-wrapper">
        <!-- 五行色环 -->
        <svg class="wuxing-ring" viewBox="0 0 200 200">
          <circle cx="100" cy="100" r="94" fill="none" stroke="#2a2a2a" stroke-width="1.5" />
          <!-- 火 - 心 (top) -->
          <circle cx="100" cy="10" r="4" fill="#C94043" />
          <text x="100" y="6" text-anchor="middle" fill="#C94043" font-size="8" font-family="KaiTi, serif">心</text>
          <!-- 木 - 肝 (top-right) -->
          <circle cx="182" cy="56" r="4" fill="#4A7C59" />
          <text x="192" y="60" text-anchor="middle" fill="#4A7C59" font-size="8" font-family="KaiTi, serif">肝</text>
          <!-- 水 - 肾 (bottom-right) -->
          <circle cx="158" cy="152" r="4" fill="#3A6B8C" />
          <text x="168" y="158" text-anchor="middle" fill="#3A6B8C" font-size="8" font-family="KaiTi, serif">肾</text>
          <!-- 金 - 肺 (bottom-left) -->
          <circle cx="42" cy="152" r="4" fill="#C8B88A" />
          <text x="32" y="158" text-anchor="middle" fill="#C8B88A" font-size="8" font-family="KaiTi, serif">肺</text>
          <!-- 土 - 脾 (top-left) -->
          <circle cx="18" cy="56" r="4" fill="#D4A24E" />
          <text x="8" y="60" text-anchor="middle" fill="#D4A24E" font-size="8" font-family="KaiTi, serif">脾</text>
        </svg>

        <!-- 太极图 + 评分 -->
        <div class="taiji-inner">
          <svg class="taiji-svg" viewBox="0 0 200 200">
            <!-- 阴（暗面）- 深色 -->
            <path :d="yinPath" :fill="darkColor" />
            <!-- 阳（亮面）- 浅色 -->
            <path :d="yangPath" :fill="lightColor" />
            <!-- 阴中阳点 -->
            <circle cx="100" cy="50" r="12" :fill="lightColor" />
            <!-- 阳中阴点 -->
            <circle cx="100" cy="150" r="12" :fill="darkColor" />
            <!-- 外圈 -->
            <circle cx="100" cy="100" r="96" fill="none" :stroke="scoreColor" stroke-width="3" />
          </svg>
          <div class="score-overlay">
            <span class="score-num" :style="{ color: scoreColor }">{{ score }}</span>
            <span class="score-label">分</span>
          </div>
        </div>
      </div>
      <div class="score-status">
        <span class="status-text" :style="{ color: scoreColor }">{{ statusText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  score: { type: Number, default: 0 }
})

const scoreColor = computed(() => {
  if (props.score >= 80) return '#4A7C59' // 木 - 肝 - 健康绿
  if (props.score >= 60) return '#D4A24E' // 土 - 脾 - 偏颇黄
  return '#C94043' // 火 - 心 - 失调红
})

const statusText = computed(() => {
  if (props.score >= 80) return '气血调和'
  if (props.score >= 60) return '稍有失调，需关注'
  return '气血失衡，建议干预'
})

// 太极阴阳颜色随评分变化
const lightColor = computed(() => {
  if (props.score >= 80) return '#e8f5e9'
  if (props.score >= 60) return '#fff8e1'
  return '#ffebee'
})

const darkColor = computed(() => {
  if (props.score >= 80) return '#1b3a26'
  if (props.score >= 60) return '#3a3018'
  return '#3a1a1a'
})

// 太极阴阳路径 - 旋转角度随评分变化（高分=平衡，低分=失衡）
const rotation = computed(() => {
  return (1 - props.score / 100) * 180
})

const yinPath = computed(() => {
  // 右半圆 + S曲线
  return `M 100,4 A 96,96 0 0,1 100,196 A 48,48 0 0,0 100,100 A 48,48 0 0,1 100,4`
})

const yangPath = computed(() => {
  // 左半圆 + S曲线
  return `M 100,4 A 96,96 0 0,0 100,196 A 48,48 0 0,1 100,100 A 48,48 0 0,0 100,4`
})
</script>

<style scoped>
.score-section {
  grid-column: 1 / -1;
}

.score-card {
  text-align: center;
  padding: 32px 24px;
  background: linear-gradient(180deg, rgba(30,28,26,0.95) 0%, rgba(22,22,30,0.9) 100%);
  border: 1px solid rgba(212,162,78,0.2);
  border-radius: 16px;
  position: relative;
  overflow: hidden;
}

.score-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212,162,78,0.4), transparent);
}

.score-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 24px;
}

.label {
  color: #b8a88a;
  font-size: 16px;
  letter-spacing: 6px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
}

.decorative-line {
  display: inline-block;
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212,162,78,0.3), transparent);
}

.taiji-wrapper {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px 0;
}

.wuxing-ring {
  position: absolute;
  width: 220px;
  height: 220px;
  animation: slowSpin 60s linear infinite;
}

@keyframes slowSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.taiji-inner {
  position: relative;
  width: 200px;
  height: 200px;
}

.taiji-svg {
  width: 200px;
  height: 200px;
  filter: drop-shadow(0 0 20px rgba(212,162,78,0.15));
}

.score-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
}

.score-num {
  font-size: 52px;
  font-weight: 700;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
  text-shadow: 0 0 20px rgba(0,0,0,0.5);
  white-space: nowrap;
}

.score-label {
  display: block;
  font-size: 14px;
  color: #888;
  margin-top: -4px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
}

.score-status {
  margin-top: 20px;
}

.status-text {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 4px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
  text-shadow: 0 0 10px currentColor;
}
</style>
