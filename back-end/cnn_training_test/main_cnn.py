from fastapi import FastAPI, File, UploadFile
import uvicorn
import librosa
import numpy as np
import os
from tensorflow.keras.models import load_model
from sklearn.preprocessing import LabelEncoder

app = FastAPI()

# === Chargement du modèle entraîné ===
model = load_model("cnn_breath_model.h5")

# === Préparation de l'encodeur de labels ===
labels = ['apnea', 'asthma', 'healthy']
encoder = LabelEncoder()
encoder.fit(labels)

# === Fonction de prétraitement pour CNN ===
def extract_mfcc_image(file_path):
    y, sr = librosa.load(file_path, sr=None)
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=40)
    mfcc = np.resize(mfcc, (40, 100))  # Redimensionner à taille fixe
    mfcc = mfcc[np.newaxis, ..., np.newaxis]  # (1, 40, 100, 1)
    return mfcc

# === API: recevoir fichier et analyser ===
@app.post("/analyze_breath")
async def analyze_breath(file: UploadFile = File(...)):
    tmp_path = f"temp_{file.filename}"
    with open(tmp_path, "wb") as buffer:
        buffer.write(await file.read())

    try:
        mfcc_img = extract_mfcc_image(tmp_path)
        prediction = model.predict(mfcc_img)
        label_index = np.argmax(prediction)
        label = encoder.inverse_transform([label_index])[0]

        os.remove(tmp_path)

        # === Message personnalisé ===
        if label == "healthy":
            result = "NORMALE"
            message = "Votre respiration semble normale. Continuez à maintenir un mode de vie sain."
        elif label == "asthma":
            result = "ASTHME"
            message = "Votre respiration présente des signes proches de l'asthme. Consultez un professionnel de santé."
        else:
            result = "APNÉE"
            message = "Votre respiration présente des signes proches de l'apnée. Il est conseillé de consulter un médecin."

        return {
            "prediction": result,
            "message": message,
            "confidence": round(float(prediction[0][label_index]) * 100, 2)
        }

    except Exception as e:
        os.remove(tmp_path)
        return {"error": str(e)}

# === Lancement local ===
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
