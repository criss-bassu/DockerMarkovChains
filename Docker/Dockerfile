FROM openjdk:17
WORKDIR /
ADD practica2.jar practica2.jar
ADD puntoCorte.txt puntoCorte.txt
ADD mayor.txt mayor.txt
ADD menor.txt menor.txt
ADD matrizP.txt matrizP.txt
# Make port 4567 available to the world outside this container
EXPOSE 4567
# Define environment variable
ENV NAME REDIS_HOST
CMD java -jar practica2.jar