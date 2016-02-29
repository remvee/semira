FROM ubuntu:trusty

# installation dependencies
RUN apt-get update -y && apt-get install -y wget openjdk-7-jre-headless gstreamer-tools gstreamer0.10-plugins-good gstreamer0.10-plugins-bad gstreamer0.10-plugins-ugly gstreamer0.10-fluendo-mp3 lame

# setup app user and directory
RUN yes | adduser app
ADD . /home/app
RUN chown -R app /home/app
WORKDIR /home/app
USER app

# go!
CMD java -jar target/semira.jar
EXPOSE 8080
