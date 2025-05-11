# readscape-jp

## local dev

```
$ asdf install java adoptopenjdk-21.0.5+11.0.LTS
$ asdf local java adoptopenjdk-21.0.5+11.0.LTS
$ asdf install gradle 8.11
$ asdf local gradle 8.11

```

## db migrate

```
$ sh infrastructure/db/migrate.sh
```

## build & run

### build

```
$ cd backend
$ ./gradlew build
```

### run

```
$ ./gradlew bootRun
```
