FROM ubuntu

# ensure apt gets existing packages
RUN apt-get update -y

# installation dependencies
RUN apt-get install -y wget

# application dependencies
RUN apt-get install -y openjdk-6-jre-headless

# audio transcoding dependencies
RUN apt-get install -y gstreamer-tools gstreamer0.10-plugins-good gstreamer0.10-plugins-bad gstreamer0.10-plugins-ugly gstreamer0.10-fluendo-mp3 lame

# setup leiningen
ENV LEIN_ROOT 1
RUN wget -O /usr/local/bin/lein https://github.com/technomancy/leiningen/raw/stable/bin/lein
RUN chmod +x /usr/local/bin/lein
RUN lein version

# setup app user
RUN yes | adduser app
USER app

# setup application directory
ADD . /app
WORKDIR /app

# build application
RUN lein deps
RUN lein cljsbuild clean
RUN lein cljsbuild once

# go!
CMD lein run
EXPOSE 8080
