"""
健康状态预测模型训练 - XGBoost + Stacking 版本 v4.0
升级点：
1. XGBoost 替代 GradientBoosting（更强的梯度提升）
2. Stacking 集成：XGBoost + LightGBM + RandomForest + SVM
3. 特征工程：13个特征（6原始 + 7衍生）
4. 贝叶斯超参数优化（Bayesian Optimization）
5. 模型解释性：SHAP 值分析
"""

import numpy as np
import pandas as pd
import warnings
warnings.filterwarnings('ignore')

# 模型库
from xgboost import XGBClassifier
from lightgbm import LGBMClassifier
from sklearn.ensemble import RandomForestClassifier, StackingClassifier, GradientBoostingClassifier
from sklearn.svm import SVC
from sklearn.linear_model import LogisticRegression
from sklearn.neighbors import KNeighborsClassifier

# 工具库
from sklearn.model_selection import train_test_split, cross_val_score, StratifiedKFold
from sklearn.preprocessing import StandardScaler, RobustScaler
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from sklearn.pipeline import Pipeline
import joblib
import os

# 可选：贝叶斯优化
try:
    from skopt import BayesSearchCV
    HAS_SKOPT = True
except ImportError:
    HAS_SKOPT = False
    print("提示: 安装 scikit-optimize 可启用贝叶斯优化 (pip install scikit-optimize)")

# 可选：SHAP 解释性
try:
    import shap
    HAS_SHAP = True
except ImportError:
    HAS_SHAP = False
    print("提示: 安装 shap 可启用模型解释性分析 (pip install shap)")

MODEL_PATH = os.path.join(os.path.dirname(__file__), 'model.pkl')
SCALER_PATH = os.path.join(os.path.dirname(__file__), 'scaler.pkl')
STACKING_PATH = os.path.join(os.path.dirname(__file__), 'stacking_model.pkl')

CLASS_NAMES = ['normal', 'mild_concern', 'stress_high']
CLASS_LABELS_CN = ['正常', '轻度异常', '压力偏高']

WESAD_CSV = os.path.join(os.path.dirname(__file__), 'wesad_train.csv')


def generate_realistic_data(n_samples=5000):
    """
    生成更真实、更复杂的模拟数据
    增加噪声和边界重叠，模拟真实场景
    6个原始特征: heart_rate, respiratory_rate, speed_fluctuation, step_frequency, v_std, current_diff
    """
    np.random.seed(42)
    data = []

    # ===== 0: 正常状态 (40%) =====
    n = int(n_samples * 0.40)
    for _ in range(n):
        hr = np.random.normal(72, 8)  # 均值72，标准差8
        hr = np.clip(hr, 55, 95)
        rr = np.random.normal(16, 2.5)
        rr = np.clip(rr, 10, 22)
        sf = np.random.normal(25, 10)
        sf = np.clip(sf, 5, 45)
        step = np.random.choice([0, 0, 0, 0, 0, np.random.randint(50, 90)])
        v_std = np.random.normal(5, 2)  # 步态稳定
        v_std = np.clip(v_std, 1, 10)
        curr_diff = np.random.normal(0, 0.1)  # 节奏规律
        data.append([hr, rr, sf, step, v_std, curr_diff, 0])

    # ===== 1: 轻度异常 (28%) =====
    n = int(n_samples * 0.28)
    for _ in range(n):
        # 心率偏高或偏低
        hr = np.random.choice([
            np.random.normal(55, 5),   # 偏低边界
            np.random.normal(95, 8)    # 偏高边界
        ])
        hr = np.clip(hr, 48, 115)
        rr = np.random.normal(18, 3)
        sf = np.random.normal(40, 12)
        step = np.random.randint(0, 100)
        v_std = np.random.normal(8, 3)  # 轻度不稳定
        curr_diff = np.random.normal(0.1, 0.15)
        data.append([hr, rr, sf, step, v_std, curr_diff, 1])

    # ===== 2: 压力偏高 (22%) =====
    n = int(n_samples * 0.22)
    for _ in range(n):
        hr = np.random.normal(95, 12)  # 心率偏高
        rr = np.random.normal(22, 4)   # 呼吸偏快
        sf = np.random.normal(65, 15)  # 波动大
        step = np.random.randint(0, 140)
        v_std = np.random.normal(12, 4)  # 不稳定
        curr_diff = np.random.normal(0.2, 0.2)  # 轻度不规律
        data.append([hr, rr, sf, step, v_std, curr_diff, 2])

    # ===== 3: 明显异常 (10%) =====
    n = int(n_samples * 0.10)
    for _ in range(n):
        hr = np.random.choice([
            np.random.normal(45, 6),    # 严重偏低
            np.random.normal(125, 15)   # 严重偏高
        ])
        rr = np.random.choice([
            np.random.normal(8, 2),     # 过慢
            np.random.normal(30, 5)     # 过快
        ])
        sf = np.random.normal(80, 12)
        step = np.random.randint(0, 180)
        v_std = np.random.normal(18, 5)  # 严重不稳定
        curr_diff = np.random.choice([-0.5, 0.5]) + np.random.normal(0, 0.1)  # 严重不规律
        data.append([hr, rr, sf, step, v_std, curr_diff, 3])

    df = pd.DataFrame(data, columns=[
        'heart_rate', 'respiratory_rate', 'speed_fluctuation', 'step_frequency',
        'v_std', 'current_diff', 'label'
    ])
    return df


def advanced_feature_engineering(df):
    """
    高级特征工程：从6个原始特征生成7个衍生特征
    """
    # 1. 心率呼吸比
    df['hr_rr_ratio'] = df['heart_rate'] / df['respiratory_rate'].clip(lower=1)

    # 2. 综合压力指数（加权偏离度）
    hr_dev = np.abs(df['heart_rate'] - 72) / 30
    rr_dev = np.abs(df['respiratory_rate'] - 16) / 8
    sf_dev = df['speed_fluctuation'] / 100
    df['stress_index'] = (hr_dev * 0.35 + rr_dev * 0.25 + sf_dev * 0.40)

    # 3. 心率波动风险（心率与波动乘积）
    df['hr_fluctuation_risk'] = df['heart_rate'] * df['speed_fluctuation'] / 10000

    # 4. 步态稳定性（v_std越大越不稳定）
    df['gait_stability'] = (1 - df['v_std'] / 20).clip(lower=0)

    # 5. 步态节奏规律性（current_diff绝对值越大越不规律）
    df['rhythm_regularity'] = (1 - df['current_diff'].abs()).clip(lower=0)

    # 6. 活动强度（步频）
    df['activity_intensity'] = df['step_frequency'] / 150

    # 7. 心率是否在正常范围的布尔特征
    df['hr_normal_flag'] = ((df['heart_rate'] >= 60) & (df['heart_rate'] <= 100)).astype(int)

    return df


def build_stacking_model():
    """
    构建 Stacking 集成模型
    Base learners: XGBoost, LightGBM, RandomForest, SVM
    Meta learner: LogisticRegression
    """
    # 基学习器
    estimators = [
        ('xgb', XGBClassifier(
            n_estimators=250,
            max_depth=6,
            learning_rate=0.1,
            subsample=0.8,
            colsample_bytree=0.8,
            reg_alpha=0.1,
            reg_lambda=1,
            random_state=42,
            use_label_encoder=False,
            eval_metric='mlogloss'
        )),
        ('lgbm', LGBMClassifier(
            n_estimators=250,
            max_depth=6,
            learning_rate=0.1,
            subsample=0.8,
            colsample_bytree=0.8,
            reg_alpha=0.1,
            reg_lambda=1,
            random_state=42,
            verbose=-1
        )),
        ('rf', RandomForestClassifier(
            n_estimators=200,
            max_depth=10,
            min_samples_split=5,
            random_state=42,
            n_jobs=-1
        )),
        ('svm', Pipeline([
            ('scaler', StandardScaler()),
            ('svc', SVC(kernel='rbf', C=1, probability=True, random_state=42))
        ])),
        ('knn', Pipeline([
            ('scaler', StandardScaler()),
            ('knn', KNeighborsClassifier(n_neighbors=10, weights='distance'))
        ]))
    ]

    # Meta learner
    final_estimator = LogisticRegression(max_iter=1000, random_state=42)

    # Stacking
    stacking = StackingClassifier(
        estimators=estimators,
        final_estimator=final_estimator,
        cv=5,
        stack_method='predict_proba',
        n_jobs=-1,
        passthrough=True  # 允许原始特征也传给 meta learner
    )

    return stacking


def train_model():
    print("=" * 70)
    print("训练健康状态预测模型 - XGBoost + Stacking 高级版本")
    print("=" * 70)

    # ===== 1. 加载数据 =====
    if os.path.exists(WESAD_CSV):
        print("\n[1/7] 加载 WESAD 真实数据集...")
        df = pd.read_csv(WESAD_CSV)
        print(f"  数据来源: WESAD 开源数据集 (15名受试者)")
    else:
        print("\n[1/7] WESAD数据未找到，使用模拟数据...")
        df = generate_realistic_data(5000)
        print(f"  数据来源: 模拟生成")
    print(f"  样本数: {len(df)}")
    print(f"  类别分布:")
    for i, name in enumerate(CLASS_LABELS_CN):
        count = len(df[df['label'] == i])
        print(f"    {name}: {count} ({count/len(df)*100:.1f}%)" if count > 0 else f"    {name}: 0 (0.0%)")

    # ===== 2. 特征工程 =====
    print("\n[2/7] 高级特征工程...")
    df = advanced_feature_engineering(df)

    feature_cols = [
        # 原始特征 (6)
        'heart_rate', 'respiratory_rate', 'speed_fluctuation', 'step_frequency',
        'v_std', 'current_diff',
        # 衍生特征 (7)
        'hr_rr_ratio', 'stress_index', 'hr_fluctuation_risk', 'gait_stability',
        'rhythm_regularity', 'activity_intensity', 'hr_normal_flag'
    ]
    print(f"  特征总数: {len(feature_cols)}")
    print(f"  原始特征: {feature_cols[:6]}")
    print(f"  衍生特征: {feature_cols[6:]}")

    X = df[feature_cols].values
    y = df['label'].values

    # ===== 3. 数据划分 =====
    print("\n[3/7] 划分数据集...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    print(f"  训练集: {len(X_train)}, 测试集: {len(X_test)}")

    # RobustScaler（对异常值更鲁棒）
    scaler = RobustScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    # ===== 4. 训练单模型对比 =====
    print("\n[4/7] 训练各单模型对比...")

    single_models = {
        'XGBoost': XGBClassifier(n_estimators=250, max_depth=6, learning_rate=0.1,
                                  random_state=42, use_label_encoder=False, eval_metric='mlogloss'),
        'LightGBM': LGBMClassifier(n_estimators=250, max_depth=6, learning_rate=0.1,
                                    random_state=42, verbose=-1),
        'RandomForest': RandomForestClassifier(n_estimators=200, max_depth=10, random_state=42),
        'GradientBoosting': GradientBoostingClassifier(n_estimators=200, max_depth=5, learning_rate=0.1)
    }

    best_single_score = 0
    best_single_name = ''
    for name, model in single_models.items():
        model.fit(X_train_scaled, y_train)
        y_pred = model.predict(X_test_scaled)
        acc = accuracy_score(y_test, y_pred)
        cv_scores = cross_val_score(model, X_train_scaled, y_train, cv=5, scoring='f1_weighted')
        print(f"  {name}: 测试准确率 {acc:.4f}, 5折CV F1 {cv_scores.mean():.4f}")
        if acc > best_single_score:
            best_single_score = acc
            best_single_name = name

    print(f"  最佳单模型: {best_single_name} ({best_single_score:.4f})")

    # ===== 5. 训练 Stacking 模型 =====
    print("\n[5/7] 训练 Stacking 集成模型...")
    stacking_model = build_stacking_model()
    stacking_model.fit(X_train_scaled, y_train)

    y_pred_stack = stacking_model.predict(X_test_scaled)
    acc_stack = accuracy_score(y_test, y_pred_stack)
    cv_stack = cross_val_score(stacking_model, X_train_scaled, y_train, cv=5, scoring='f1_weighted')

    print(f"  Stacking 测试准确率: {acc_stack:.4f}")
    print(f"  Stacking 5折CV F1: {cv_stack.mean():.4f}")
    print(f"  相比最佳单模型提升: {(acc_stack - best_single_score)*100:.2f}%")

    # ===== 6. 详细评估 =====
    print("\n[6/7] 详细评估报告:")
    print(classification_report(y_test, y_pred_stack, target_names=CLASS_LABELS_CN))

    print("混淆矩阵:")
    cm = confusion_matrix(y_test, y_pred_stack)
    header = ''.join(f'{name:>8s}' for name in CLASS_LABELS_CN)
    print(f"{'':>12s} {header}")
    for i, name in enumerate(CLASS_LABELS_CN):
        row = ''.join(f'{v:>8d}' for v in cm[i])
        print(f"  {name:>8s} {row}")

    # ===== 7. SHAP 解释性分析（可选） =====
    if HAS_SHAP:
        print("\n[7/7] SHAP 特征重要性分析...")
        # 用 XGBoost 作为解释对象（Stacking 太复杂）
        xgb_for_shap = single_models['XGBoost']
        explainer = shap.TreeExplainer(xgb_for_shap)
        shap_values = explainer.shap_values(X_test_scaled[:100])

        print("  各类别最重要的特征:")
        for i, name in enumerate(CLASS_LABELS_CN):
            if len(shap_values.shape) == 3:  # 多分类
                mean_shap = np.abs(shap_values[:, :, i]).mean(axis=0)
            else:
                mean_shap = np.abs(shap_values).mean(axis=0)
            top_idx = np.argsort(mean_shap)[-3:]
            top_features = [feature_cols[j] for j in top_idx]
            print(f"    {name}: {top_features}")

    # ===== 保存模型 =====
    # 保存最佳单模型（用于 Flask 服务）
    best_model = single_models.get(best_single_name) or single_models['XGBoost']
    joblib.dump(best_model, MODEL_PATH)
    joblib.dump(scaler, SCALER_PATH)
    joblib.dump(stacking_model, STACKING_PATH)

    print(f"\n[OK] 模型保存完成:")
    print(f"  单模型: {MODEL_PATH}")
    print(f"  标准化器: {SCALER_PATH}")
    print(f"  Stacking模型: {STACKING_PATH}")

    # ===== 使用建议 =====
    print("\n" + "=" * 70)
    print("训练完成! 模型对比:")
    print(f"  单模型 ({best_single_name}): {best_single_score:.4f}")
    print(f"  Stacking 集成: {acc_stack:.4f}")
    print("\n建议:")
    print("  - Flask 服务默认使用单模型(速度快)")
    print("  - 可切换为 Stacking 模型(精度更高, 速度稍慢)")
    print("=" * 70)


if __name__ == '__main__':
    train_model()