<template>
  <div class="data-card" :class="cardClass">
    <div class="card-border-top"></div>
    <div class="card-header">
      <span class="card-icon" :style="{ color: iconColor }">
        {{ chineseIcon }}
      </span>
      <span class="card-title">{{ title }}</span>
    </div>
    <div class="value-wrapper">
      <span class="value">{{ displayValue }}</span>
      <span class="unit">{{ unit }}</span>
    </div>
    <div class="progress-bar">
      <div class="progress-fill" :style="{ width: normalizedPercentage + '%', background: gradientColor }"></div>
      <div class="progress-glow" :style="{ width: normalizedPercentage + '%' }"></div>
    </div>
    <div class="status-bar">
      <span class="status" :class="statusClass">{{ statusText }}</span>
    </div>
    <div class="card-border-bottom"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: String,
  value: [Number, String],
  unit: String,
  icon: Object,
  min: { type: Number, default: 0 },
  max: { type: Number, default: 200 },
  normalMin: Number,
  normalMax: Number,
  iconColor: { type: String, default: '#18a058' }
})

// 根据卡片类型判断五行属性
const cardClass = computed(() => {
  const title = props.title
  if (title.includes('心率')) return 'element-fire'
  if (title.includes('呼吸')) return 'element-metal'
  if (title.includes('速度') || title.includes('步频') || title.includes('频率差')) return 'element-water'
  if (title.includes('温度') || title.includes('湿度')) return 'element-earth'
  if (title.includes('气压') || title.includes('声音')) return 'element-wood'
  return ''
})

// 汉字图标替代ionicons
const chineseIcon = computed(() => {
  const title = props.title
  if (title.includes('心率')) return '心'
  if (title.includes('呼吸')) return '肺'
  if (title.includes('速度')) return '风'
  if (title.includes('步频')) return '步'
  if (title.includes('频率差')) return '律'
  if (title.includes('温度')) return '温'
  if (title.includes('湿度')) return '湿'
  if (title.includes('气压')) return '气'
  if (title.includes('声音')) return '闻'
  return '象'
})

const displayValue = computed(() => {
  if (props.value === null || props.value === undefined) return '--'
  return props.value
})

const normalizedPercentage = computed(() => {
  if (props.value === null || props.value === undefined) return 0
  return Math.min(100, Math.max(0, ((props.value - props.min) / (props.max - props.min)) * 100))
})

const statusClass = computed(() => {
  if (props.value === null || props.value === undefined) return 'unknown'
  if (props.normalMin !== undefined && props.normalMax !== undefined) {
    if (props.value < props.normalMin) return 'low'
    if (props.value > props.normalMax) return 'high'
    return 'normal'
  }
  return 'unknown'
})

const statusText = computed(() => {
  if (statusClass.value === 'normal') return '调和'
  if (statusClass.value === 'low') return '偏低'
  if (statusClass.value === 'high') return '偏高'
  return '待测'
})

// 五行渐变色
const gradientColor = computed(() => {
  const cls = cardClass.value
  if (cls === 'element-fire') return 'linear-gradient(90deg, #C94043, #ff6b6b)'
  if (cls === 'element-metal') return 'linear-gradient(90deg, #C8B88A, #f5f0dc)'
  if (cls === 'element-water') return 'linear-gradient(90deg, #3A6B8C, #74b9ff)'
  if (cls === 'element-earth') return 'linear-gradient(90deg, #D4A24E, #ffe66d)'
  if (cls === 'element-wood') return 'linear-gradient(90deg, #4A7C59, #a8edea)'
  if (statusClass.value === 'normal') return 'linear-gradient(90deg, #4A7C59, #6db89c)'
  return 'linear-gradient(90deg, #3a3a4a, #5a5a6a)'
})
</script>

<style scoped>
.data-card {
  position: relative;
  padding: 20px;
  background: rgba(30, 28, 26, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(212, 162, 78, 0.15);
  border-radius: 12px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.data-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(ellipse at top, rgba(212,162,78,0.05) 0%, transparent 70%);
  pointer-events: none;
}

.data-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
  border-color: rgba(212, 162, 78, 0.3);
}

/* 五行元素卡片特殊样式 */
.data-card.element-fire:hover { box-shadow: 0 12px 32px rgba(201, 64, 67, 0.2); }
.data-card.element-metal:hover { box-shadow: 0 12px 32px rgba(200, 184, 138, 0.2); }
.data-card.element-water:hover { box-shadow: 0 12px 32px rgba(58, 107, 140, 0.2); }
.data-card.element-earth:hover { box-shadow: 0 12px 32px rgba(212, 162, 78, 0.2); }
.data-card.element-wood:hover { box-shadow: 0 12px 32px rgba(74, 124, 89, 0.2); }

/* 中式边框装饰 */
.card-border-top,
.card-border-bottom {
  position: absolute;
  left: 20px;
  right: 20px;
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(212, 162, 78, 0.3) 20%,
    rgba(212, 162, 78, 0.3) 80%,
    transparent 100%
  );
}

.card-border-top { top: 8px; }
.card-border-bottom { bottom: 8px; }

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.card-icon {
  font-size: 20px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
  font-weight: 600;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: rgba(212, 162, 78, 0.1);
}

.card-title {
  color: #b8a88a;
  font-weight: 500;
  font-size: 14px;
  letter-spacing: 2px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
}

.value-wrapper {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 16px 0;
}

.value {
  font-size: 42px;
  font-weight: 700;
  color: #f5f0dc;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
  text-shadow: 0 2px 8px rgba(0,0,0,0.3);
}

.unit {
  font-size: 13px;
  color: #888;
  letter-spacing: 1px;
}

.progress-bar {
  position: relative;
  height: 6px;
  background: rgba(58, 58, 58, 0.4);
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.progress-glow {
  position: absolute;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  border-radius: 3px;
  animation: glowMove 2s ease-in-out infinite;
}

@keyframes glowMove {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}

.status-bar {
  text-align: right;
}

.status {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 10px;
  letter-spacing: 1px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
}

.status.normal {
  background: rgba(74, 124, 89, 0.2);
  color: #6db89c;
}

.status.low,
.status.high {
  background: rgba(212, 162, 78, 0.2);
  color: #D4A24E;
}

.status.unknown {
  background: rgba(100, 100, 100, 0.2);
  color: #888;
}
</style>