# AutoVision Sprint 0

## Local developer setup

### 1. Clone and create Python environment

```bash
cd AutoVision
python -m venv api/.venv
```

On Windows PowerShell:

```powershell
cd AutoVision
py -m venv api\.venv
```

Activate the environment and install backend dependencies:

```bash
cd api
. .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

On Windows PowerShell:

```powershell
cd api
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
```

### 2. Configure local environment files

Copy the project examples and fill in local values:

```bash
cp .env.example .env
cp web/.env.example web/.env.local
```

For Windows PowerShell:

```powershell
Copy-Item .env.example .env
Copy-Item web/.env.example web/.env.local
```

Required values include:

- DATABASE_URL for the development database (`autovision`)
- TEST_DATABASE_URL for the isolated pytest database (`autovision_test`)
- CORS_ALLOWED_ORIGINS for the local frontend origin
- NEXT_PUBLIC_API_BASE_URL for the local API URL
- NEXT_PUBLIC_DEMO_TENANT_ID for Sprint 0 demo mode

### 3. Start PostgreSQL

```bash
docker compose up -d postgres
```

### 4. Run database migration

```bash
cd api
python -m alembic upgrade head
```

### 5. Seed Sprint 0 demo data

```bash
cd api
python -m scripts.seed_demo
```

### 6. Run backend

```bash
cd api
python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 7. Install frontend dependencies

```bash
cd web
npm install
```

### 8. Run frontend

```bash
cd web
npm run dev
```

Open:

- http://localhost:3000
- http://localhost:8000/health

### 9. Run tests

```bash
cd api
python -m pytest -q
```

## Sprint 0 scope

This repository is intentionally scoped to the Sprint 0 foundation for AutoVision:

- backend foundation
- PostgreSQL and migration setup
- deterministic demo seed
- tenant-scoped vehicle queries
- vehicle search/list
- vehicle 360 detail
- frontend demo integration
- isolated pytest database

No Sprint 1+ functionality is included.
