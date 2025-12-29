#!/bin/bash
#chmod +x rebuild.sh
set -e  # Nếu bất kỳ lệnh nào fail thì script sẽ dừng ngay
git fetch
git pull
echo "📥 Pulling latest changes from Git..."
echo "🔨 Building Spring Boot app..."
mvn clean package -DskipTests

echo "🛑 Stopping containers..."
docker compose down

echo "🏗️ Rebuilding containers..."
docker compose up --build -d

echo "✅ Done!"
echo "You can check the logs with: docker compose logs -f"
echo "Access the application at: http://localhost:8080"
echo "Access the database at: localhost:5432 (user: user, password: password, db: mydb)"
