# Radar Health Monitor

> Non-contact TCM health monitoring system powered by mmWave radar and dual-engine AI
>
> 无感颐养 — 基于边缘AI的模块化非接触中医智能健康看护装备 · CRAIC 第二十八届中国机器人及人工智能大赛 · 医疗健康装备赛道

> ⚠️ **Disclaimer**: This project has been submitted to multiple academic competitions. If you are participating in the same or similar competitions, please do NOT copy or reuse the code, documentation, or ideas directly. Use it only as a reference for learning purposes.

## 项目简介

无感颐养是一套基于毫米波雷达的非接触式中医智能健康看护系统。通过 60GHz/24GHz 双雷达非接触采集心率、呼吸、步态等 10 维体征数据，结合**知识驱动规则引擎（70%）+ 数据驱动 ML 模型（30%）**双引擎 AI 架构，实现三分类中医健康辨证，输出综合评分与个性化养生建议。

## 系统架构

```
硬件感知层 (STM32 + 雷达)
    │ MQTT (JSON, 1s/次)
    ▼
Java 后端 (Spring Boot 2.7.18 :9999)
    ├─ MQTT 接收 → 字段映射 → 多设备数据融合
    ├─ HealthAdviceEngine 规则引擎 (70%)
    │   └─ 8 模块：心率8级/呼吸6级/步态/环境六淫/声音/七情/评分
    └─ MLModelClient → Python Flask (30%)
        └─ XGBoost + Stacking 集成推理
    │ 加权融合 0.7 × 规则 + 0.3 × ML
    ▼
Vue 3 前端 (:5173)
    └─ 太极评分环 · 10 × DataCard · 中医辨证建议面板
```

## 核心特性

- **全链路非接触**：毫米波雷达零穿戴零打扰，从物理层杜绝隐私泄露
- **双引擎 AI 架构**：规则引擎基于《中医诊断学》编码，可解释；ML 补充情绪压力识别
- **三级降级机制**：Stacking → XGBoost → 纯规则引擎 → 静态兜底，系统永不掉线
- **中医五行 UI**：水墨风暗色主题 + 五行色彩映射 + SVG 太极评分环

## 技术栈

| 层级 | 技术 |
|------|------|
| 硬件 | STM32F103RCT6 · 60GHz FMCW 雷达 · 24GHz 雷达 · BME280 · MAX9814 · ESP8266 |
| 后端 | Java 17 · Spring Boot 2.7.18 · MyBatis-Plus · Druid · MySQL 8 · Redis · MQTT (Paho) |
| ML | Python · Flask · Scikit-learn · XGBoost · LightGBM · Stacking 集成 |
| 前端 | Vue 3 · Vite · Axios |
| 数据集 | WESAD (15 受试者, 700 Hz, 7093 样本) |

## ML 模型

**Stacking 集成架构**：5 个异构基学习器 + Logistic Regression 元模型

```
XGBoost ─┐
LightGBM ┤
RF ──────┼── 5 × 3 类概率 = 15 维 ──┐
SVM ─────┤                            │
KNN ─────┘                            ├─ 28 维 → LR 元模型 → 三分类
                                      │
           13 维原始特征 (passthrough) ─┘
```

| 指标 | 值 |
|------|:---:|
| 加权 F1 | 0.70 |
| 准确率 | 71% |
| 特征维度 | 13 (6 原始 + 7 衍生) |

## 项目结构

```
├── frontend-vue/              # Vue 3 前端
│   ├── src/
│   │   ├── App.vue            # 主界面（太极环 + 数据卡片 + 建议面板）
│   │   ├── api/radar.js       # 后端 API 封装
│   │   └── components/
│   │       ├── ScoreCircle.vue # 太极评分环 (SVG)
│   │       ├── DataCard.vue   # 通用数据卡片 (×10)
│   │       └── AdvicePanel.vue# 中医建议面板
│   └── vite.config.js
│
├── radar-mqtt2.0/             # Java 后端 (Spring Boot)
│   └── src/main/java/com/lz/radar/
│       ├── RadarApplication.java
│       ├── controller/
│       │   └── HealthController.java
│       ├── service/
│       │   ├── HealthAdviceEngine.java  # 规则引擎核心 (717 行)
│       │   └── MLModelClient.java       # ML 服务调用
│       ├── mqtt/
│       │   ├── MqttConnection.java      # MQTT 连接管理
│       │   └── MqttManager.java         # 连接池 + 批量入库
│       ├── config/
│       ├── domain/
│       └── mapper/
│
└── health-ml-backup-v4.0-71.8/  # Python ML 训练 & 推理
    ├── train.py                   # 模型训练 (Stacking 集成)
    ├── app.py                     # Flask 推理服务 (端口 5001)
    └── wesad_train.csv            # 预处理后的训练数据
```

## 快速启动

### 1. 后端 (Java)

```bash
# 需要 JDK 17 + MySQL 8 + Redis
cd radar-mqtt2.0
# 修改 src/main/resources/application.yml 中的数据库和 MQTT 配置
mvn spring-boot:run
# 后端运行在 http://localhost:9999
```

### 2. ML 服务 (Python)

```bash
cd health-ml-backup-v4.0-71.8
pip install flask scikit-learn xgboost lightgbm pandas numpy
python app.py
# ML 服务运行在 http://localhost:5001
```

### 3. 前端 (Vue)

```bash
cd frontend-vue
npm install
npm run dev
# 前端运行在 http://localhost:5173
```

## 团队

慢慢发光队 — CRAIC 第二十八届中国机器人及人工智能大赛

## License

[GNU GPLv3](LICENSE)
