# Information Retrieval Project
The goal of this project is to crawl tweets from Twitter and use the data to build a personalized Search Engine for microblog content.

## Index

Since the index is more than 7GB large and the indexing process takes quite a long time (especially if querying the online database), we uploaded a zipped file in Google Drive.

## Gettin Started

### Docker
The easiest way is to use Docker. To run the system just clone the project, `cd` into the docker folder and run `docker-compose`:

```
$ git clone https://github.com/CostantiniMatteo/progetto-ir.git && cd progetto-ir
$ cd docker
$ docker-compose up
```

The webapp will be deployed at [localhost](http://127.0.0.1).

### Docker-less

You need to change the BASE_URL variable in `webapp/app.py` with the commented one.
You also need to change three paths in `tweet-tweet-go/src/main/java/cgp/ttg/engine/UserProfile.java` and `tweet-tweet-go/src/main/java/cgp/ttg/engine/Indexer.java`. Just swap the un-commented ones with the commented paths.

#### Prerequisites

If you don't want to use Docker you'll neet Python 3.x and Java 11 with Maven.
Then to use the Search Engine with the Web App:

```
$ git clone https://github.com/CostantiniMatteo/progetto-ir.git && cd progetto-ir
$ cd tweet-tweet-go
$ mvn clean install -DskipTests
$ cd ..
$ java -jar tweet-tweet-go/target/progetto-ir-0.0.1-SNAPSHOT.jar
```

And to run the webapp:
```
$ cd progetto-ir/webapp
$ pip install -r requirements.txt
$ python application.py
```

The webapp will be deployed at [localhost](http://127.0.0.1:5000), port 5000.

The Search Engine includes also a App.java with main to interact with the Search Engine from the terminal.

## Authors
* Matteo Angelo Costantini - 795125
* Dario Gerosa - 793636
* Michele Perrotta - 795152
