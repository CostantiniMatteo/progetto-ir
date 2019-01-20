# Information Retrieval Project
The goal of this project is to crawl tweets from Twitter and use the data to build a personalized Search Engine for microblog content.

## Gettin Started

### Docker
The easiest way is to use Docker. To run the system just clone the project, `cd` into the docker folder and run `docker-compose`:

```
$ git clone https://github.com/CostantiniMatteo/progetto-ir.git && cd progetto-ir
$ cd docker
$ docker-compose up
```

The webapp will be deployed at ()[localhost].

### Docker-less

#### Prerequisites

If you don't want to use Docker you'll neet Python 3.x and Java 11 with Maven.
Then to use the Search Engine with the Web App:
```
$ git clone https://github.com/CostantiniMatteo/progetto-ir.git && cd progetto-ir
$ cd tweet-tweet-go
$ maven clean install -DskipTests
$ cd ..
$ java -jar tweet-tweet-go/target/progetto-ir-0.0.1-SNAPSHOT.jar
```

And to run the webapp:
```
$ cd progetto-ir/webapp
$ pip install -r requirements.txt
$ python application.py
```

The webapp will be deployed at ()[localhost].

The Search Engine includes also a App.java with main to interact with the Search Engine from the terminal.

## Authors
* Matteo Angelo Costantini - 795125
* Dario Gerosa - 793636
* Michele Perrotta - 795152
