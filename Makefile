all:
		javac -cp "./lib/*:./bin/" -sourcepath "./src" -d ./bin src/crawlers/CrawlerBoss.java

run:
		java -cp "./lib/*:./bin/" crawlers.CrawlerBoss

