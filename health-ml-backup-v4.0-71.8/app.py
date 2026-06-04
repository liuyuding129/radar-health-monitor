"""
健康评估 ML 微服务 v4.0
模型: XGBoost + Stacking 集成 (13特征: 6原始 + 7衍生)
端口: 5001
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import numpy as np
import os

app = Flask(__name__)
CORS(app)

MODEL_PATH = os.path.join(os.path.dirname(__file__), 'model.pkl')
SCALER_PATH = os.path.join(os.path.dirname(__file__), 'scaler.pkl')
STACKING_PATH = os.path.join(os.path.dirname(__file__), 'stacking_model.pkl')

model = None
scaler = None
stacking_model = None

CLASS_NAMES = ['normal', 'mild_concern', 'stress_high']

ADVICE_MAP = {
    0: {
        'summary': '气血调和，脉象平稳，脏腑功能正常',
        'advice': '建议：保持规律作息，顺应四时变化。可适当练习八段锦、太极拳等养生功法，调和气血',
        'risk': '平和'
    },
    1: {
        'summary': '气血略有失衡，脉象稍有异常，需关注脏腑调养',
        'advice': '建议：注意调畅情志，避免忧思过度。可酌情服用归脾汤、逍遥散等调和之剂，定期复测',
        'risk': '偏颇'
    },
    2: {
        'summary': '肝郁气滞，心火偏旺，脉象弦数，机体处于紧张状态',
        'advice': '建议：疏肝解郁，清心安神。可考虑柴胡疏肝散加减，配合针灸太冲、神门等穴位。注意情志调摄，避免五志过极',
        'risk': '失调'
    }
}


def load_model():
    global model, scaler, stacking_model
    if os.path.exists(MODEL_PATH) and os.path.exists(SCALER_PATH):
        model = joblib.load(MODEL_PATH)
        scaler = joblib.load(SCALER_PATH)
        print(f"[OK] 单模型加载成功 (特征数: {model.n_features_in_})")
    else:
        print("[WARN] 模型文件不存在，请先运行: python train.py")

    if os.path.exists(STACKING_PATH):
        stacking_model = joblib.load(STACKING_PATH)
        print(f"[OK] Stacking 集成模型加载成功")
    else:
        print("[WARN] Stacking 模型不存在，将使用单模型")


def compute_all_features(raw):
    """
    从6个原始特征计算7个衍生特征，返回13个特征的数组
    输入: [heart_rate, respiratory_rate, speed_fluctuation, step_frequency, v_std, current_diff]
    输出: 13个特征的数组
    """
    hr, rr, sf, step, v_std, curr_diff = raw

    # 衍生特征
    hr_rr_ratio = hr / max(rr, 1)
    hr_dev = abs(hr - 72) / 30
    rr_dev = abs(rr - 16) / 8
    sf_dev = sf / 100
    stress_index = hr_dev * 0.35 + rr_dev * 0.25 + sf_dev * 0.40

    hr_fluctuation_risk = hr * sf / 10000

    # 步态稳定性 (v_std越大越不稳定)
    gait_stability = max(0, 1 - v_std / 20)

    # 步态节奏规律性 (current_diff绝对值越大越不规律)
    rhythm_regularity = max(0, 1 - abs(curr_diff))

    # 活动强度（仅用步频）
    activity_intensity = step / 150

    hr_normal_flag = 1 if 60 <= hr <= 100 else 0

    return [
        hr, rr, sf, step, v_std, curr_diff,
        hr_rr_ratio, stress_index, hr_fluctuation_risk, gait_stability,
        rhythm_regularity, activity_intensity, hr_normal_flag
    ]


@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "ok",
        "model_loaded": model is not None,
        "stacking_loaded": stacking_model is not None,
        "model_type": "XGBoost + Stacking",
        "feature_count": model.n_features_in_ if model else 0
    })


@app.route('/predict', methods=['POST'])
def predict():
    """
    预测健康状态 (使用 Stacking 模型，降级到单模型)
    输入: {
        "heart_rate": 75,
        "respiratory_rate": 16,
        "speed_fluctuation": 30,
        "step_frequency": 0,
        "v_std": 5,
        "current_diff": 0
    }
    """
    if model is None:
        return jsonify({"error": "模型未加载"}), 500

    try:
        data = request.get_json()

        # 提取6个原始特征
        raw_features = [
            data.get('heart_rate', 75),
            data.get('respiratory_rate', 16),
            data.get('speed_fluctuation', 30),
            data.get('step_frequency', 0),
            data.get('v_std', 5),
            data.get('current_diff', 0),
        ]

        # 计算13个特征
        features = compute_all_features(raw_features)
        features_array = np.array([features])

        # 标准化
        features_scaled = scaler.transform(features_array)

        # 优先使用 Stacking 模型
        use_model = stacking_model if stacking_model is not None else model
        model_name = "Stacking" if stacking_model is not None else "XGBoost"

        prediction = int(use_model.predict(features_scaled)[0])

        # 概率 (部分模型可能不支持 predict_proba)
        try:
            probabilities = use_model.predict_proba(features_scaled)[0]
            confidence = float(max(probabilities))
            prob_dict = {name: round(float(probabilities[i]), 4)
                        for i, name in enumerate(CLASS_NAMES)}
        except Exception:
            confidence = 1.0
            prob_dict = {CLASS_NAMES[prediction]: 1.0}

        advice_data = ADVICE_MAP.get(prediction, ADVICE_MAP[0])

        return jsonify({
            "status": CLASS_NAMES[prediction],
            "confidence": round(confidence, 4),
            "class_index": prediction,
            "model": model_name,
            "summary": advice_data['summary'],
            "advice": advice_data['advice'],
            "risk": advice_data['risk'],
            "probabilities": prob_dict
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    load_model()
    app.run(host='0.0.0.0', port=5001, debug=True)
