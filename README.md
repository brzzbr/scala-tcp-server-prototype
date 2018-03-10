# Cosmosim in Scala

### Publishing docker container

#### Sbt way:

``` 
  sbt build-docker-local or 
  sbt build-docker-remote
```

Alternative way if you prefer to push manually

```
docker build -t registry.gitlab.com/cosmosim/cosmosim-server .
docker push registry.gitlab.com/cosmosim/cosmosim-server
```

