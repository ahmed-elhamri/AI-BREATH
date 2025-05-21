from fastapi import FastAPI, File, UploadFile
import uvicorn
import librosa
import numpy as np
import os

app = FastAPI()

# === Nouveau dossier contenant les sons nommés par type ===
dataset_folder = "dataset"

# Fonction pour extraire les MFCC moyens
def extract_mfcc_mean(file_path):
    y, sr = librosa.load(file_path, sr=None)
    mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13)
    return np.mean(mfcc, axis=1)

# Initialiser les listes de MFCC
mfcc_healthy = []
mfcc_asthma = []
mfcc_apnea = []

# Lire tous les fichiers audio du dossier "dataset"
for filename in os.listdir(dataset_folder):
    if filename.endswith(".wav"):
        file_path = os.path.join(dataset_folder, filename)
        mfcc_mean = extract_mfcc_mean(file_path)

        if filename.lower().startswith("healthy"):
            mfcc_healthy.append(mfcc_mean)
        elif filename.lower().startswith("asthma"):
            mfcc_asthma.append(mfcc_mean)
        elif filename.lower().startswith("apnea"):
            mfcc_apnea.append(mfcc_mean)

# Calculer les prototypes
prototype_healthy = np.mean(mfcc_healthy, axis=0)
prototype_asthma = np.mean(mfcc_asthma, axis=0)
prototype_apnea = np.mean(mfcc_apnea, axis=0)

def classify_mfcc(mfcc_vec):
    dist_healthy = np.linalg.norm(mfcc_vec - prototype_healthy)
    dist_asthma = np.linalg.norm(mfcc_vec - prototype_asthma)
    dist_apnea = np.linalg.norm(mfcc_vec - prototype_apnea)

    min_dist = min(dist_healthy, dist_asthma, dist_apnea)

    if min_dist == dist_healthy:
        return "Healthy", dist_healthy
    elif min_dist == dist_asthma:
        return "Asthma", dist_asthma
    else:
        return "Apnea", dist_apnea

@app.post("/analyze_breath")
async def analyze_breath(file: UploadFile = File(...)):
    # Sauvegarder temporairement le fichier reçu
    tmp_path = f"temp_{file.filename}"
    with open(tmp_path, "wb") as buffer:
        buffer.write(await file.read())

    # Extraire MFCC et classifier
    mfcc_vec = extract_mfcc_mean(tmp_path)
    prediction, dist = classify_mfcc(mfcc_vec)

    # Supprimer le fichier temporaire
    os.remove(tmp_path)

    # Construire un rapport utilisateur simple
    if prediction == "Healthy":
        prediction = "NORMALE"
        message = "Votre respiration semble normale. Continuez à maintenir un mode de vie sain."
        # report = {
        #     "result": "NORMALE",
        #     "message": "Votre respiration semble normale. Continuez à maintenir un mode de vie sain."
        # }
    elif prediction == "Asthma":
        message = "Votre respiration présente des signes proches de l'asthme. Consultez un professionnel de santé."
        # report = {
        #     "result": "ASTHME",
        #     "message": "Votre respiration présente des signes proches de l'asthme. Consultez un professionnel de santé."
        # }
    else:
        message = "Votre respiration présente des signes proches de l'apnée. Il est conseillé de consulter un médecin."
        # report = {
        #     "result": "APNÉE",
        #     "message": "Votre respiration présente des signes proches de l'apnée. Il est conseillé de consulter un médecin."
        # }

    return {
        "prediction": prediction,
        # "distance": float(dist),
        # "report": report
        "message" : message
    }

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
