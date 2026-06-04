<template>
  <div class="advice-section">
    <div class="advice-card">
      <div class="scroll-top"></div>
      <div class="advice-header">
        <span class="header-deco left"></span>
        <span class="header-text">辨证论治</span>
        <span class="header-deco right"></span>
      </div>
      <div class="advice-list" v-if="advices.length > 0">
        <div v-for="(item, index) in advices" :key="index" class="advice-item" :class="item.type">
          <div class="advice-icon">{{ item.icon }}</div>
          <span class="advice-text">{{ item.text }}</span>
        </div>
      </div>
      <div v-else class="no-data">
        <span class="no-data-icon">卦</span>
        <span>等待天机...</span>
      </div>
      <div class="scroll-bottom"></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  adviceTexts: { type: Array, default: () => [] }
})

// 五行分类与图标映射
const categoryMap = [
  { keywords: ['脉象'], icon: '心', type: 'fire', color: '#C94043' },
  { keywords: ['呼吸'], icon: '肺', type: 'metal', color: '#C8B88A' },
  { keywords: ['步态'], icon: '肝', type: 'wood', color: '#4A7C59' },
  { keywords: ['环境', '六淫', '温度', '湿度', '气压'], icon: '脾', type: 'earth', color: '#D4A24E' },
  { keywords: ['闻诊', '声音', '声息'], icon: '肾', type: 'water', color: '#3A6B8C' },
  { keywords: ['七情', '中医智能辨证', '调养建议', '情绪'], icon: '魂', type: 'spirit', color: '#7b2cbf' },
  { keywords: ['综合评估'], icon: '总', type: 'summary', color: '#b8a88a' },
]

const advices = computed(() => {
  return props.adviceTexts.map(text => {
    // 匹配分类
    let matched = categoryMap.find(cat => cat.keywords.some(kw => text.includes(kw)))
    if (!matched) matched = { icon: '辨', type: 'info', color: '#3a86c4' }
    return { text, icon: matched.icon, type: matched.type }
  })
})
</script>

<style scoped>
.advice-section {
  grid-column: 1 / -1;
}

.advice-card {
  position: relative;
  background: rgba(30, 28, 26, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(212, 162, 78, 0.2);
  border-radius: 16px;
  padding: 24px 20px;
  overflow: hidden;
}

.advice-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse at top left, rgba(212,162,78,0.03) 0%, transparent 50%),
    radial-gradient(ellipse at bottom right, rgba(201,64,67,0.03) 0%, transparent 50%);
  pointer-events: none;
}

/* 卷轴装饰 */
.scroll-top,
.scroll-bottom {
  position: relative;
  height: 4px;
  margin: 0 20px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(212, 162, 78, 0.15) 15%,
    rgba(212, 162, 78, 0.3) 50%,
    rgba(212, 162, 78, 0.15) 85%,
    transparent 100%
  );
  border-radius: 2px;
}

.scroll-top { margin-bottom: 20px; }
.scroll-bottom { margin-top: 20px; }

.scroll-top::before,
.scroll-top::after,
.scroll-bottom::before,
.scroll-bottom::after {
  content: '';
  position: absolute;
  width: 6px;
  height: 6px;
  background: rgba(212, 162, 78, 0.4);
  border-radius: 50%;
  top: 50%;
  transform: translateY(-50%);
}

.scroll-top::before, .scroll-bottom::before { left: -3px; }
.scroll-top::after, .scroll-bottom::after { right: -3px; }

.advice-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}

.header-text {
  font-size: 18px;
  color: #b8a88a;
  letter-spacing: 6px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
}

.header-deco {
  display: inline-block;
  width: 40px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212, 162, 78, 0.4));
}

.header-deco.right {
  background: linear-gradient(90deg, rgba(212, 162, 78, 0.4), transparent);
}

.advice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.advice-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 10px;
  border-left: 3px solid transparent;
  transition: all 0.3s ease;
}

.advice-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

/* 五行分类样式 */
.advice-item.fire {
  border-left-color: #C94043;
}
.advice-item.fire .advice-icon { color: #C94043; background: rgba(201,64,67,0.15); }

.advice-item.metal {
  border-left-color: #C8B88A;
}
.advice-item.metal .advice-icon { color: #C8B88A; background: rgba(200,184,138,0.15); }

.advice-item.wood {
  border-left-color: #4A7C59;
}
.advice-item.wood .advice-icon { color: #4A7C59; background: rgba(74,124,89,0.15); }

.advice-item.earth {
  border-left-color: #D4A24E;
}
.advice-item.earth .advice-icon { color: #D4A24E; background: rgba(212,162,78,0.15); }

.advice-item.water {
  border-left-color: #3A6B8C;
}
.advice-item.water .advice-icon { color: #3A6B8C; background: rgba(58,107,140,0.15); }

.advice-item.spirit {
  border-left-color: #7b2cbf;
}
.advice-item.spirit .advice-icon { color: #7b2cbf; background: rgba(123,44,191,0.15); }

.advice-item.summary {
  border-left-color: #b8a88a;
}
.advice-item.summary .advice-icon { color: #b8a88a; background: rgba(184,168,138,0.15); }

.advice-item.info {
  border-left-color: #3a86c4;
}
.advice-item.info .advice-icon { color: #3a86c4; background: rgba(58,134,196,0.15); }

.advice-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  border-radius: 6px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
}

.advice-text {
  font-size: 14px;
  color: #ddd;
  line-height: 1.8;
  flex: 1;
}

.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 50px;
  color: #555;
  font-size: 14px;
  font-family: "KaiTi", "楷体", "STKaiti", serif;
}

.no-data-icon {
  font-size: 32px;
  color: rgba(212, 162, 78, 0.3);
}
</style>