import os
import numpy as np
import librosa
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt
import seaborn as sns

dataset_folder = "dataset"
X, y = [], []

# === Chargement et extraction MFCC en format image (2D) ===
for filename in os.listdir(dataset_folder):
    if filename.endswith(".wav"):
        if "healthy" in filename.lower():
            label = "healthy"
        elif "asthma" in filename.lower():
            label = "asthma"
        elif "apnea" in filename.lower():
            label = "apnea"
        else:
            continue

        file_path = os.path.join(dataset_folder, filename)
        try:
            y_audio, sr = librosa.load(file_path, sr=None)
            mfcc = librosa.feature.mfcc(y=y_audio, sr=sr, n_mfcc=40)
            mfcc = np.resize(mfcc, (40, 100))  # Redimensionner à taille fixe
            X.append(mfcc)
            y.append(label)
        except Exception as e:
            print("Erreur:", e)

# === Encodage des labels ===
encoder = LabelEncoder()
y_encoded = encoder.fit_transform(y)
y_cat = tf.keras.utils.to_categorical(y_encoded)

X = np.array(X)
X = X[..., np.newaxis]  # Ajout du canal pour CNN

# === Train/Test split ===
X_train, X_test, y_train, y_test = train_test_split(X, y_cat, test_size=0.2, stratify=y_encoded, random_state=42)

# === Modèle CNN simple ===
model = tf.keras.Sequential([
    tf.keras.layers.Conv2D(32, (3, 3), activation="relu", input_shape=(40, 100, 1)),
    tf.keras.layers.MaxPooling2D((2, 2)),
    tf.keras.layers.Conv2D(64, (3, 3), activation="relu"),
    tf.keras.layers.MaxPooling2D((2, 2)),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(64, activation="relu"),
    tf.keras.layers.Dense(3, activation="softmax")
])

model.compile(optimizer="adam", loss="categorical_crossentropy", metrics=["accuracy"])

# === Entraînement ===
history = model.fit(X_train, y_train, epochs=20, batch_size=16, validation_data=(X_test, y_test))

# === Sauvegarde du modèle ===
model.save("cnn_breath_model.h5")
print("✅ Modèle CNN sauvegardé.")

# === Évaluation ===
y_pred = model.predict(X_test)
y_pred_classes = np.argmax(y_pred, axis=1)
y_true = np.argmax(y_test, axis=1)

print("\n📊 Rapport de classification :")
print(classification_report(y_true, y_pred_classes, target_names=encoder.classes_))

# === Matrice de confusion ===
cm = confusion_matrix(y_true, y_pred_classes)
plt.figure(figsize=(6, 4))
sns.heatmap(cm, annot=True, fmt="d", cmap="Blues", xticklabels=encoder.classes_, yticklabels=encoder.classes_)
plt.xlabel("Prédit")
plt.ylabel("Réel")
plt.title("Matrice de confusion")
plt.tight_layout()
plt.savefig("confusion_matrix.png")
plt.show()
