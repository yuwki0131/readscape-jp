curl https://start.spring.io/starter.zip \
  -d name=readscape-backend \
  -d groupId=jp.readscape \
  -d artifactId=backend \
  -d description="Readscape backend API" \
  -d packageName=jp.readscape.backend \
  -d javaVersion=21 \
  -d language=java \
  -d bootVersion=3.2.0 \
  -d dependencies=web,data-jpa,postgresql,lombok,validation,actuator \
  -o backend.zip

unzip backend.zip -d backend

cd backend

./gradlew build

./gradlew bootRun
