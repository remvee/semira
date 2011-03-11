$(document).ready(function() {
  $("li.track a").click(function(event) {
    if (!!(document.createElement('audio').canPlayType)) {
      var container = document.getElementById("audio-container");
      while (container.firstChild) container.removeChild(container.firstChild);

      var mp3 = document.createElement("source");
      mp3.src = this.getAttribute("href-mp3");
      mp3.type = "audio/mpeg";

      var ogg = document.createElement("source");
      ogg.src = this.getAttribute("href-ogg");
      ogg.type = "audio/ogg";

      var audio = document.createElement("audio");
      audio.autoplay = "autoplay";
      audio.controls = "controls";
      audio.preload = "none";
      audio.appendChild(mp3);
      audio.appendChild(ogg);

      container.appendChild(audio);

      event.preventDefault();
    }
  })
})