# train_model.py
import os
import numpy as np
import librosa
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
import joblib

dataset_folder = "dataset"
labels_map = {
    "healthy": 0,
    "asthma": 1,
    "apnea": 2
}

X = []
y = []

def extract_mfcc_mean(file_path):
    y, sr = librosa.load(file_path, sr=None)
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13)
    return np.mean(mfcc, axis=1)

for filename in os.listdir(dataset_folder):
    if filename.endswith(".wav"):
        file_path = os.path.join(dataset_folder, filename)
        mfcc = extract_mfcc_mean(file_path)

        for label in labels_map:
            if filename.lower().startswith(label):
                X.append(mfcc)
                y.append(labels_map[label])
                break

X = np.array(X)
y = np.array(y)

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

clf = RandomForestClassifier(n_estimators=100, random_state=42)
clf.fit(X_train, y_train)

y_pred = clf.predict(X_test)
print(classification_report(y_test, y_pred, target_names=labels_map.keys()))

joblib.dump(clf, "breathing_model.pkl")
print("✅ Model trained and saved to breathing_model.pkl")
