$filePath = "E:\E-HealthShield\sample_ehr_report.txt"
Set-Content -Path $filePath -Value "Confidential Patient Medical Report: Blood Pressure: 135/85 mmHg, Heart Rate: 72 bpm, Diagnosis: Stage 1 Hypertension."

curl.exe -X POST "http://localhost:8080/api/ehrs/upload" `
  -F "file=@E:\E-HealthShield\sample_ehr_report.txt" `
  -F "patientWallet=0x70997970C51812dc3A010C7d01b50e0d17dc79C8" `
  -F "uploaderWallet=0x90F79bf6EB2c4f870365E785982E1f101E93b906" `
  -F "keywords=cardiology" `
  -F "keywords=hypertension" `
  -F "keywords=arrhythmia"
