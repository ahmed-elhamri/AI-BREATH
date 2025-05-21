# main.py
from fastapi import FastAPI, File, UploadFile
import uvicorn
import librosa
import numpy as np
import os
import joblib
import tempfile

# === Load trained model ===
model = joblib.load("breathing_model.pkl")
labels_reverse = {
    0: "NORMALE",
    1: "ASTHME",
    2: "APNÉE"
}

# === FastAPI app ===
app = FastAPI()

# === MFCC Extraction ===
def extract_mfcc_mean(file_path):
    y, sr = librosa.load(file_path, sr=None)
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13)
    return np.mean(mfcc, axis=1)

# === Endpoint ===
@app.post("/analyze_breath")
async def analyze_breath(file: UploadFile = File(...)):
    # Save file temporarily
    with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as tmp:
        tmp.write(await file.read())
        tmp_path = tmp.name

    try:
        mfcc_vec = extract_mfcc_mean(tmp_path).reshape(1, -1)
        prediction = model.predict(mfcc_vec)[0]
        diagnosis = labels_reverse[prediction]

        if diagnosis == "NORMALE":
            message = "Votre respiration semble normale. Continuez à maintenir un mode de vie sain."
        elif diagnosis == "ASTHME":
            message = "Votre respiration présente des signes proches de l'asthme. Consultez un professionnel de santé."
        else:
            message = "Votre respiration présente des signes proches de l'apnée. Il est conseillé de consulter un médecin."

        return {
            "prediction": diagnosis,
            "message": message
        }
    except Exception as e:
        return {"error": str(e)}
    finally:
        os.remove(tmp_path)

# === Run server ===
if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000)
