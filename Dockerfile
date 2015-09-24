FROM ubuntu:trusty

# installation dependencies
RUN apt-get update -y && apt-get install -y wget openjdk-7-jre-headless gstreamer-tools gstreamer0.10-plugins-good gstreamer0.10-plugins-bad gstreamer0.10-plugins-ugly gstreamer0.10-fluendo-mp3 lame

# setup leiningen
ENV LEIN_ROOT 1
RUN wget -O /usr/local/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/2.5.1/bin/lein
RUN chmod +x /usr/local/bin/lein
RUN lein version

# setup app user and directory
RUN yes | adduser app
ADD . /home/app
RUN chown -R app /home/app
WORKDIR /home/app
USER app

# build application
RUN lein uberjar

# go!
CMD jar -jar target/semira.jar
EXPOSE 8080
