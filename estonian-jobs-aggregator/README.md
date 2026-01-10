# Estonian Tech Jobs Aggregator

Eesti IT tööpakkumiste agregaator, mis kogub ja analüüsib tööpakkumisi erinevatest allikatest.

## Tehnoloogiad

- **Backend:** Spring Boot 3.2, Java 21, Maven
- **Frontend:** Angular 17 (standalone components, signals)
- **Andmebaas:** PostgreSQL 16
- **Scraping:** Jsoup 1.17
- **Konteineriseerimine:** Docker Compose

## Kiirstart

### Docker Compose abil (soovitatav)

```bash
# Käivita kõik teenused
docker-compose up -d

# Vaata logisid
docker-compose logs -f

# Peata teenused
docker-compose down
```

Rakendus on kättesaadav:
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432

### Arenduskeskkond

#### Backend
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Frontend
```bash
cd frontend
npm install
ng serve
```

## API Endpoints

### Tööpakkumised
- `GET /api/jobs` - Tööpakkumiste nimekiri (pagineeritud)
  - Query params: `page`, `size`, `skill`, `location`, `salaryMin`
- `GET /api/jobs/{id}` - Üksiku tööpakkumise detailid

### Statistika
- `GET /api/stats/skills` - Top 20 nõutuimat oskust
- `GET /api/stats/salaries?skill=java` - Palgastatistika oskuse järgi
- `GET /api/stats/trends?days=30` - Uute kuulutuste trend

### Ettevõtted
- `GET /api/companies?sort=jobCount,desc` - Ettevõtete nimekiri

### Admin
- `POST /api/scraper/trigger` - Manuaalne scraping (ainult dev)

## Projekti struktuur

```
estonian-jobs-aggregator/
├── backend/
│   ├── src/main/java/ee/jobs/aggregator/
│   │   ├── controller/     # REST kontrollerid
│   │   ├── entity/         # JPA entiteedid
│   │   ├── repository/     # Spring Data repos
│   │   ├── service/        # Äriloogika
│   │   ├── scraper/        # CV.ee scraper
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Custom exceptions
│   │   └── config/         # Konfiguratsioon
│   └── src/main/resources/
│       └── application.yml
├── frontend/
│   └── src/app/
│       ├── components/     # Standalone komponendid
│       ├── services/       # API teenused
│       ├── models/         # TypeScript interfaces
│       └── pages/          # Lehekülje komponendid
├── docker-compose.yml
└── README.md
```

## Scraper

Scraper kogub tööpakkumisi järgmistest allikatest:
- CV.ee IT kategooria

Scraping toimub automaatselt iga 6 tunni tagant.
Rate limiting: 1 päring 2 sekundi kohta.

## Arendus

### Nõuded
- Java 21
- Node.js 18+
- Docker & Docker Compose
- Maven 3.9+

### Keskkonnamuutujad

Backend konfiguratsioon toimub `application.yml` kaudu või Docker keskkonnamuutujatega.

## Litsents

MIT License
