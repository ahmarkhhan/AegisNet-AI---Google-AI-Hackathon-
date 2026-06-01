# Server Management Commands

This document contains the PowerShell commands required to manually stop and restart the AegisNet Web Platform (Angular frontend and Spring Boot backend).

## 🛑 Stop Commands

The safest way to stop the services is by terminating the processes that are occupying their specific ports. Run these commands from any PowerShell window:

### Stop the Angular Frontend (Port 4200)
```powershell
Get-NetTCPConnection -LocalPort 4200 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

### Stop the Spring Boot Backend (Port 8080)
```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

---

## 🚀 Restart Commands

### 1. Restarting the Frontend
Open a new PowerShell terminal, navigate to the `frontend` folder, and start the development server:

```powershell
cd d:\development\antigravity\AegisNet-AI---Google-AI-Hackathon-\frontend
npm start
```

### 2. Restarting the Backend
Open another PowerShell terminal, navigate to the `backend` folder, set the `JAVA_HOME` variable to the required JDK, and use the project's local hidden Maven executable:

```powershell
cd d:\development\antigravity\AegisNet-AI---Google-AI-Hackathon-\backend
$env:JAVA_HOME="C:\Program Files\RedHat\java-21-openjdk-21.0.11.0.10-1"

# Use the local maven installation to run the backend
.\.maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```
